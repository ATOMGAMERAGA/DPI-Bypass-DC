package net.atom.dpibypass.vpn

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.InetAddresses
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.atom.dpibypass.R
import net.atom.dpibypass.data.ActiveProfile
import net.atom.dpibypass.data.ConnectionState
import net.atom.dpibypass.data.OperationMode
import net.atom.dpibypass.data.Settings
import net.atom.dpibypass.data.SettingsRepository
import net.atom.dpibypass.data.AppFilterMode
import net.atom.dpibypass.dns.DohResolver
import net.atom.dpibypass.engine.ByeDpiProxy
import net.atom.dpibypass.engine.TProxyService
import net.atom.dpibypass.isp.IspDetector
import net.atom.dpibypass.strategy.Strategy
import net.atom.dpibypass.strategy.StrategyPool
import net.atom.dpibypass.strategy.StrategyTester
import net.atom.dpibypass.strategy.Socks5TestClient
import net.atom.dpibypass.util.NotificationUtils
import net.atom.dpibypass.util.shellSplit
import java.io.File
import java.net.URI

/**
 * Ana tünel servisi:
 *   VpnService (TUN) -> hev-socks5-tunnel -> ByeDPI (SOCKS5 + DPI desync) -> İnternet
 *
 * Foreground servis olarak kalıcı bildirimle çalışır. Otomatik modda strateji
 * testini yapar, watchdog ile arka planda ölürse kendini toparlar.
 *
 * PERFORMANS PRENSİBİ: trafik uzak sunucuya YÖNLENDİRİLMEZ; yalnızca bağlantı
 * kurulumundaki ilk paketler yerelde parçalanır. Böylece hız/ping kaybı olmaz.
 */
class DpiVpnService : LifecycleVpnService() {

    private lateinit var settingsRepo: SettingsRepository
    private var byeDpiProxy = ByeDpiProxy()
    private var proxyJob: Job? = null
    private var watchdogJob: Job? = null
    private var tunFd: ParcelFileDescriptor? = null
    private val mutex = Mutex()
    private var stopping = false

    private var currentArgs: Array<String> = emptyArray()
    private var currentProfile: ActiveProfile? = null
    private var currentDohUrl: String = net.atom.dpibypass.dns.DohProvider.Cloudflare.url

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        NotificationUtils.registerChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> lifecycleScope.launch { start() }
            ACTION_STOP -> lifecycleScope.launch { stop() }
            else -> Log.w(TAG, "Bilinmeyen action: ${intent?.action}")
        }
        return START_STICKY
    }

    override fun onRevoke() {
        Log.i(TAG, "VPN izni geri alındı")
        lifecycleScope.launch { stop() }
    }

    // ---- yaşam döngüsü ----

    private suspend fun start() {
        if (VpnState.state.value == ConnectionState.Connected) {
            Log.w(TAG, "Zaten bağlı")
            return
        }
        // Foreground'a erken gir (ANR/limit riskini azalt), sonra çalış.
        goForeground(getString(R.string.status_connecting), connected = false)

        try {
            val settings = settingsRepo.settings.first()
            currentDohUrl = settings.effectiveDohUrl()
            val (strategy, latency) = resolveStrategy(settings)
            val ispName = resolveIspName(settings)
            currentProfile = ActiveProfile(strategy.id, strategy.name, ispName, latency?.toInt())
            currentArgs = strategy.toArgv(PROXY_PORT)

            mutex.withLock {
                VpnState.update(ConnectionState.Connecting)
                startProxy(currentArgs)
                startTun2Socks(settings)
            }

            VpnState.update(ConnectionState.Connected, currentProfile)
            goForeground(
                getString(R.string.notification_connected, currentProfile!!.shortLabel()),
                connected = true,
            )
            startWatchdog()
            Log.i(TAG, "Bağlandı: ${currentProfile?.shortLabel()}")
        } catch (e: Exception) {
            Log.e(TAG, "Başlatma başarısız", e)
            VpnState.update(ConnectionState.Failed, null)
            stop()
        }
    }

    private suspend fun stop() {
        Log.i(TAG, "Durduruluyor")
        watchdogJob?.cancel()
        watchdogJob = null
        mutex.withLock {
            stopping = true
            try {
                stopTun2Socks()
                stopProxy()
            } catch (e: Exception) {
                Log.e(TAG, "Durdurma hatası", e)
            } finally {
                stopping = false
            }
        }
        VpnState.update(ConnectionState.Disconnected, null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---- strateji seçimi ----

    private suspend fun resolveStrategy(settings: Settings): Pair<Strategy, Long?> {
        // 1) Gelişmiş serbest argümanlar
        if (settings.advancedEnabled && settings.advancedArgs.isNotBlank()) {
            val custom = Strategy(
                "CUSTOM", getString(R.string.strategy_custom), "",
                settings.advancedArgs.trim(),
            )
            return custom to null
        }
        // 2) Manuel: seçili preset
        if (settings.operationMode == OperationMode.Manual) {
            return (StrategyPool.byId(settings.selectedStrategyId) ?: StrategyPool.P1) to null
        }
        // 3) Otomatik: canlı test ile en iyi (en düşük ping'li çalışan) stratejiyi bul
        VpnState.update(ConnectionState.Testing)
        val doh = DohResolver(settings.effectiveDohUrl())
        val tester = StrategyTester(lifecycleScope, doh)
        val detector = IspDetector(applicationContext)
        val family = (detector.detectFromSim() ?: settings.selectedIsp).family
        val ordered = StrategyPool.orderedFor(family)
        val hosts = StrategyTester.DEFAULT_BLOCKED_HOSTS + extraHosts(settings)
        val best = tester.run(ordered, hosts)
        return if (best != null) {
            // Ağ profiline kaydet (aynı ağa tekrar bağlanınca tekrar test etmemek için).
            best.first to best.second
        } else {
            // Hiçbiri çalışmadı → yine de en olası preset ile dene, kullanıcıyı bilgilendir.
            Log.w(TAG, "Otomatik test hiçbir çalışan strateji bulamadı; ilk preset kullanılıyor")
            ordered.first() to null
        }
    }

    private fun extraHosts(settings: Settings): List<String> =
        settings.extraBlockedHosts
            .split(',', '\n', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private suspend fun resolveIspName(settings: Settings): String {
        if (settings.operationMode == OperationMode.Manual) return settings.selectedIsp.displayName
        val detector = IspDetector(applicationContext)
        return (detector.bestGuess() ?: settings.selectedIsp).displayName
    }

    // ---- ByeDPI proxy ----

    private fun startProxy(args: Array<String>) {
        if (proxyJob != null) throw IllegalStateException("Proxy zaten çalışıyor")
        byeDpiProxy = ByeDpiProxy()
        proxyJob = lifecycleScope.launch(Dispatchers.IO) {
            val code = byeDpiProxy.startProxy(args)
            withContext(Dispatchers.Main) {
                if (code != 0 && !stopping) {
                    Log.e(TAG, "Proxy $code koduyla durdu")
                }
            }
        }
    }

    private suspend fun stopProxy() {
        if (proxyJob == null) return
        try {
            byeDpiProxy.stopProxy()
        } catch (e: Exception) {
            Log.w(TAG, "stopProxy: ${e.message}")
        }
        proxyJob?.join()
        proxyJob = null
    }

    // ---- hev-socks5-tunnel ----

    private fun startTun2Socks(settings: Settings) {
        if (tunFd != null) throw IllegalStateException("TUN zaten açık")

        val udpLine = if (settings.disableQuic) "" else "\n  udp: 'udp'"
        val config = """
            |misc:
            |  task-stack-size: 81920
            |socks5:
            |  mtu: 8500
            |  address: 127.0.0.1
            |  port: $PROXY_PORT$udpLine
        """.trimMargin()

        val configFile = File.createTempFile("tun2socks", ".yml", cacheDir).apply {
            writeText(config)
        }

        val fd = buildTun(settings).establish()
            ?: throw IllegalStateException("VPN establish() null döndü")
        tunFd = fd
        TProxyService.TProxyStartService(configFile.absolutePath, fd.fd)
    }

    private fun stopTun2Socks() {
        try {
            TProxyService.TProxyStopService()
        } catch (e: Exception) {
            Log.w(TAG, "TProxyStopService: ${e.message}")
        }
        tunFd?.close()
        tunFd = null
    }

    private fun buildTun(settings: Settings): Builder {
        val builder = Builder()
        builder.setSession(getString(R.string.app_name))
        builder.addAddress("10.10.10.10", 32)
        builder.addRoute("0.0.0.0", 0)

        // DoH sağlayıcısının IP'sini DNS sunucusu olarak ver; sorgu ISS'in hijack
        // ettiği yerel DNS yerine bu sunucuya, desync tüneli üzerinden gider.
        val dnsIp = dohServerIp(settings.effectiveDohUrl())
        builder.addDnsServer(dnsIp)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        applySplitTunneling(builder, settings)
        return builder
    }

    /** Uygulama ayırma (split tunneling). Kendi paketini daima hariç tut (döngü olmasın). */
    private fun applySplitTunneling(builder: Builder, settings: Settings) {
        val self = applicationContext.packageName
        when (settings.appFilterMode) {
            AppFilterMode.All -> {
                safeDisallow(builder, self)
            }
            AppFilterMode.Include -> {
                // Yalnızca seçili uygulamalar tünelde; kendi paketimizi eklemeyerek hariç tutuyoruz.
                settings.selectedApps.filter { it != self }.forEach { pkg ->
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "Uygulama bulunamadı: $pkg")
                    }
                }
            }
            AppFilterMode.Exclude -> {
                safeDisallow(builder, self)
                settings.selectedApps.filter { it != self }.forEach { safeDisallow(builder, it) }
            }
        }
    }

    private fun safeDisallow(builder: Builder, pkg: String) {
        try {
            builder.addDisallowedApplication(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Uygulama bulunamadı: $pkg")
        }
    }

    private fun dohServerIp(url: String): String {
        return try {
            val host = URI(url).host ?: return DEFAULT_DNS
            if (isNumericAddress(host)) host else DEFAULT_DNS
        } catch (e: Exception) {
            DEFAULT_DNS
        }
    }

    private fun isNumericAddress(host: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            InetAddresses.isNumericAddress(host)
        } else {
            host.matches(Regex("^[0-9.]+$")) || host.contains(":")
        }

    // ---- watchdog ----

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = lifecycleScope.launch(Dispatchers.IO) {
            var failures = 0
            var backoff = 2000L
            while (isActive && !stopping) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                if (stopping) break

                val proxyDead = proxyJob?.isActive != true
                val healthy = !proxyDead && healthCheck()

                if (healthy) {
                    failures = 0
                    backoff = 2000L
                } else {
                    failures++
                    Log.w(TAG, "Watchdog: sağlıksız (proxyDead=$proxyDead, ardışık=$failures)")
                    if (failures >= 2) {
                        Log.w(TAG, "Watchdog: proxy yeniden başlatılıyor (backoff=${backoff}ms)")
                        restartProxy()
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(30_000L)
                        failures = 0
                    }
                }
            }
        }
    }

    /** Kontrol domaine tünel üzerinden bağlanabiliyor muyuz? */
    private fun healthCheck(): Boolean {
        return try {
            val doh = DohResolver(currentDohUrl)
            val ip = doh.resolve(StrategyTester.CONTROL_HOST).firstOrNull() ?: return false
            Socks5TestClient.testTls(PROXY_PORT, ip, StrategyTester.CONTROL_HOST, timeoutMs = 4000) != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun restartProxy() {
        mutex.withLock {
            if (stopping) return
            try {
                stopProxy()
                startProxy(currentArgs)
            } catch (e: Exception) {
                Log.e(TAG, "Proxy yeniden başlatılamadı", e)
            }
        }
    }

    // ---- foreground/bildirim ----

    private fun goForeground(content: String, connected: Boolean) {
        val notification = NotificationUtils.buildNotification(
            this,
            getString(R.string.app_name),
            content,
            connected,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationUtils.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "DpiVpnService"
        const val ACTION_START = "net.atom.dpibypass.START"
        const val ACTION_STOP = "net.atom.dpibypass.STOP"
        const val PROXY_PORT = 1080
        private const val DEFAULT_DNS = "1.1.1.1"
        private const val HEALTH_CHECK_INTERVAL_MS = 60_000L
    }
}
