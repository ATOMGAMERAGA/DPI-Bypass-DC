# JNI ile çağrılan sınıflar/metotlar R8 tarafından kaldırılmamalı.
# hev-socks5-tunnel native tarafı bu sınıfa RegisterNatives yapar.
-keep class net.atom.dpibypass.engine.TProxyService { *; }
-keep class net.atom.dpibypass.engine.ByeDpiProxy { *; }

# JNI'den çağrılan tüm native metotları koru.
-keepclasseswithmembernames class * {
    native <methods>;
}
