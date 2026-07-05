package net.atom.dpibypass.isp

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ISS parmak izi — kesin değil, sadece strateji havuzunu önceliklendirmek için
 * ipucu. Nihai karar canlı testtedir (StrategyTester).
 *
 * - Mobil veri: SIM MCC+MNC (TelephonyManager.simOperator).
 * - Wi-Fi / ev: dış IP'nin ASN/ISP org adı (hafif lookup).
 */
class IspDetector(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    /** SIM operatöründen (MCC+MNC) ISS tahmini. Mobil veride güvenilir. */
    fun detectFromSim(): Isp? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val operator = tm?.simOperator // MCC+MNC, ör. "28601"
            Log.d(TAG, "simOperator=$operator")
            Isp.fromSimCode(operator)
        } catch (e: Exception) {
            Log.w(TAG, "SIM operatörü okunamadı: ${e.message}")
            null
        }
    }

    /**
     * Wi-Fi/ev interneti için dış IP'nin ASN/org adını çözer. Engel/başarısızlıkta
     * null döner (çağıran doğrudan canlı teste düşebilir).
     */
    suspend fun detectFromAsn(): Isp? = withContext(Dispatchers.IO) {
        val org = fetchOrg() ?: return@withContext null
        Log.d(TAG, "ASN org=$org")
        Isp.fromAsnOrg(org)
    }

    /**
     * En iyi tahmin: önce SIM, yoksa ASN, yoksa null.
     */
    suspend fun bestGuess(): Isp? = detectFromSim() ?: detectFromAsn()

    private fun fetchOrg(): String? {
        // ipinfo.io/org → "AS9121 Turk Telekom" gibi bir dize döner.
        return try {
            val request = Request.Builder()
                .url("https://ipinfo.io/org")
                .header("Accept", "text/plain")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()?.trim()?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ASN lookup başarısız: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "IspDetector"
    }
}
