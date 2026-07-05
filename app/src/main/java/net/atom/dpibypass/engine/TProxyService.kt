package net.atom.dpibypass.engine

/**
 * hev-socks5-tunnel (TUN <-> SOCKS5 köprüsü) için JNI köprüsü.
 *
 * Native taraf (hev-jni.c) natif metotları `<PKGNAME>/TProxyService` sınıfına
 * RegisterNatives ile bağlar. PKGNAME, jni/Application.mk içinde
 * `net/atom/dpibypass/engine` olarak tanımlıdır; bu yüzden bu sınıfın paketi
 * ve adı birebir eşleşmek zorundadır.
 */
object TProxyService {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    external fun TProxyStartService(configPath: String, fd: Int)

    @JvmStatic
    external fun TProxyStopService()

    @JvmStatic
    external fun TProxyGetStats(): LongArray
}
