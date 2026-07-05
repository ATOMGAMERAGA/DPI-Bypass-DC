package net.atom.dpibypass.strategy

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Yerel ByeDPI SOCKS5 proxy üzerinden bir hedefe TLS el sıkışması deneyerek
 * stratejinin çalışıp çalışmadığını ve gecikmesini ölçer.
 *
 * Yöntem (DNS hijack'ten bağımsız, saf DPI testi):
 *   1. Hedef host, DoH ile IP'ye çözülür (çağıran tarafça).
 *   2. SOCKS5 ile IP:443'e CONNECT edilir (ByeDPI ilk paketlere desync uygular).
 *   3. Orijinal host adı SNI olarak kullanılıp TLS el sıkışması yapılır.
 * El sıkışması tamamlanırsa strateji o hedefte çalışıyor demektir.
 */
object Socks5TestClient {

    private const val TAG = "Socks5TestClient"

    /**
     * @return başarılıysa toplam gecikme (ms), başarısızsa null.
     */
    fun testTls(
        proxyPort: Int,
        targetIp: String,
        sniHost: String,
        targetPort: Int = 443,
        timeoutMs: Int = 3000,
    ): Long? {
        val start = System.nanoTime()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", proxyPort), timeoutMs)
            socket.soTimeout = timeoutMs
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            if (!socks5Handshake(out, input)) return null
            if (!socks5Connect(out, input, targetIp, targetPort)) return null

            // Tünel kuruldu; şimdi TLS el sıkışmasını dene.
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val ssl = factory.createSocket(socket, sniHost, targetPort, true) as SSLSocket
            ssl.soTimeout = timeoutMs
            ssl.startHandshake()
            ssl.session.peerCertificates // el sıkışmasını doğrula
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            ssl.close()
            return elapsedMs
        } catch (e: Exception) {
            Log.d(TAG, "Test başarısız ($sniHost @ $targetIp, port $proxyPort): ${e.message}")
            return null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun socks5Handshake(out: OutputStream, input: InputStream): Boolean {
        // VER=5, NMETHODS=1, METHOD=0 (no auth)
        out.write(byteArrayOf(0x05, 0x01, 0x00))
        out.flush()
        val reply = readFully(input, 2) ?: return false
        return reply[0].toInt() == 0x05 && reply[1].toInt() == 0x00
    }

    private fun socks5Connect(
        out: OutputStream,
        input: InputStream,
        ip: String,
        port: Int,
    ): Boolean {
        val addr = InetAddress.getByName(ip) // IP literali → DNS sorgusu yapmaz
        val addrBytes = addr.address
        val atyp = if (addrBytes.size == 4) 0x01 else 0x04
        val request = ArrayList<Byte>()
        request.add(0x05) // VER
        request.add(0x01) // CMD = CONNECT
        request.add(0x00) // RSV
        request.add(atyp.toByte())
        addrBytes.forEach { request.add(it) }
        request.add(((port shr 8) and 0xFF).toByte())
        request.add((port and 0xFF).toByte())
        out.write(request.toByteArray())
        out.flush()

        // Yanıt: VER REP RSV ATYP BND.ADDR BND.PORT
        val head = readFully(input, 4) ?: return false
        if (head[0].toInt() != 0x05 || head[1].toInt() != 0x00) return false
        val boundLen = when (head[3].toInt() and 0xFF) {
            0x01 -> 4
            0x04 -> 16
            0x03 -> {
                val lenByte = readFully(input, 1) ?: return false
                lenByte[0].toInt() and 0xFF
            }
            else -> return false
        }
        // BND.ADDR + BND.PORT(2) tüket
        readFully(input, boundLen + 2) ?: return false
        return true
    }

    private fun readFully(input: InputStream, n: Int): ByteArray? {
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(buf, read, n - read)
            if (r < 0) return null
            read += r
        }
        return buf
    }
}
