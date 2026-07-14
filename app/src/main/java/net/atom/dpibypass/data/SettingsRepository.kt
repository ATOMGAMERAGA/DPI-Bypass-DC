package net.atom.dpibypass.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.atom.dpibypass.dns.DohProvider
import net.atom.dpibypass.isp.Isp
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dpi_settings")

/** Tüm kullanıcı ayarlarının anlık görüntüsü. */
data class Settings(
    val operationMode: OperationMode = OperationMode.Auto,
    val selectedIsp: Isp = Isp.Other,
    val selectedStrategyId: String = "P1",
    val advancedEnabled: Boolean = false,
    val advancedArgs: String = "",
    val dohProvider: DohProvider = DohProvider.Cloudflare,
    val customDohUrl: String = "",
    val appFilterMode: AppFilterMode = AppFilterMode.All,
    val selectedApps: Set<String> = emptySet(),
    val autoConnectOnBoot: Boolean = false,
    val theme: ThemePref = ThemePref.System,
    val haptics: Boolean = true,
    // Opt-in: UDP'yi (QUIC/HTTP-3 dahil) tünelde bırakma; varsayılan kapalı ki
    // DNS ve sesli görüşme çalışsın. Bkz. Bölüm 14.
    val disableQuic: Boolean = false,
    val extraBlockedHosts: String = "",
) {
    /** Etkin DoH endpoint URL'i (özel varsa o, yoksa seçili sağlayıcı). */
    fun effectiveDohUrl(): String =
        customDohUrl.trim().ifBlank { dohProvider.url }
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { p -> p.toSettings() }

    private fun Preferences.toSettings(): Settings = Settings(
        operationMode = OperationMode.fromName(this[Keys.OPERATION_MODE]),
        selectedIsp = Isp.fromId(this[Keys.SELECTED_ISP]),
        selectedStrategyId = this[Keys.SELECTED_STRATEGY] ?: "P1",
        advancedEnabled = this[Keys.ADVANCED_ENABLED] ?: false,
        advancedArgs = this[Keys.ADVANCED_ARGS] ?: "",
        dohProvider = DohProvider.fromName(this[Keys.DOH_PROVIDER]),
        customDohUrl = this[Keys.CUSTOM_DOH_URL] ?: "",
        appFilterMode = AppFilterMode.fromName(this[Keys.APP_FILTER_MODE]),
        selectedApps = this[Keys.SELECTED_APPS] ?: emptySet(),
        autoConnectOnBoot = this[Keys.AUTO_CONNECT_BOOT] ?: false,
        theme = ThemePref.fromName(this[Keys.THEME]),
        haptics = this[Keys.HAPTICS] ?: true,
        // Varsayılan KAPALI: UDP tünelde kalır ki DNS ve Discord sesli sohbeti
        // (UDP/RTP ses akışı) çalışsın. Açılırsa UDP düşer ve sesli sohbet susar.
        disableQuic = this[Keys.DISABLE_QUIC] ?: false,
        extraBlockedHosts = this[Keys.EXTRA_BLOCKED_HOSTS] ?: "",
    )

    suspend fun setOperationMode(mode: OperationMode) = put(Keys.OPERATION_MODE, mode.name)
    suspend fun setSelectedIsp(isp: Isp) = put(Keys.SELECTED_ISP, isp.name)
    suspend fun setSelectedStrategy(id: String) = put(Keys.SELECTED_STRATEGY, id)
    suspend fun setAdvancedEnabled(enabled: Boolean) = put(Keys.ADVANCED_ENABLED, enabled)
    suspend fun setAdvancedArgs(args: String) = put(Keys.ADVANCED_ARGS, args)
    suspend fun setDohProvider(provider: DohProvider) = put(Keys.DOH_PROVIDER, provider.name)
    suspend fun setCustomDohUrl(url: String) = put(Keys.CUSTOM_DOH_URL, url)
    suspend fun setAppFilterMode(mode: AppFilterMode) = put(Keys.APP_FILTER_MODE, mode.name)
    suspend fun setSelectedApps(apps: Set<String>) =
        context.dataStore.edit { it[Keys.SELECTED_APPS] = apps }
    suspend fun setAutoConnectOnBoot(v: Boolean) = put(Keys.AUTO_CONNECT_BOOT, v)
    suspend fun setTheme(t: ThemePref) = put(Keys.THEME, t.name)
    suspend fun setHaptics(v: Boolean) = put(Keys.HAPTICS, v)
    suspend fun setDisableQuic(v: Boolean) = put(Keys.DISABLE_QUIC, v)
    suspend fun setExtraBlockedHosts(v: String) = put(Keys.EXTRA_BLOCKED_HOSTS, v)

    /**
     * Ağ profili: bir ağ anahtarına (SSID/operatör) en iyi stratejiyi bağlar; aynı
     * ağa tekrar bağlanınca test etmeden kullanılır. JSON olarak saklanır.
     */
    suspend fun saveNetworkProfile(networkKey: String, strategyId: String) {
        context.dataStore.edit { prefs ->
            val json = JSONObject(prefs[Keys.NETWORK_PROFILES] ?: "{}")
            json.put(networkKey, strategyId)
            prefs[Keys.NETWORK_PROFILES] = json.toString()
        }
    }

    fun networkProfile(networkKey: String): Flow<String?> =
        context.dataStore.data.map { prefs ->
            val json = JSONObject(prefs[Keys.NETWORK_PROFILES] ?: "{}")
            if (json.has(networkKey)) json.getString(networkKey) else null
        }

    private suspend fun put(key: Preferences.Key<String>, value: String) =
        context.dataStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) =
        context.dataStore.edit { it[key] = value }

    private object Keys {
        val OPERATION_MODE = stringPreferencesKey("operation_mode")
        val SELECTED_ISP = stringPreferencesKey("selected_isp")
        val SELECTED_STRATEGY = stringPreferencesKey("selected_strategy")
        val ADVANCED_ENABLED = booleanPreferencesKey("advanced_enabled")
        val ADVANCED_ARGS = stringPreferencesKey("advanced_args")
        val DOH_PROVIDER = stringPreferencesKey("doh_provider")
        val CUSTOM_DOH_URL = stringPreferencesKey("custom_doh_url")
        val APP_FILTER_MODE = stringPreferencesKey("app_filter_mode")
        val SELECTED_APPS = stringSetPreferencesKey("selected_apps")
        val AUTO_CONNECT_BOOT = booleanPreferencesKey("auto_connect_boot")
        val THEME = stringPreferencesKey("theme")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DISABLE_QUIC = booleanPreferencesKey("disable_quic")
        val EXTRA_BLOCKED_HOSTS = stringPreferencesKey("extra_blocked_hosts")
        val NETWORK_PROFILES = stringPreferencesKey("network_profiles")
    }
}
