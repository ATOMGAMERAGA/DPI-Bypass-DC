plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sürüm tek kaynaktan (gradle.properties) okunur — CI/release.yml sadece orayı
// günceller, buradaki türetme sayesinde her yere yansır.
val versionNameProp: String = providers.gradleProperty("VERSION_NAME").get()
val versionCodeProp: Int = providers.gradleProperty("VERSION_CODE").get().toInt()

android {
    namespace = "net.atom.dpibypass"
    compileSdk = 35
    ndkVersion = "26.3.11579264"

    defaultConfig {
        applicationId = "net.atom.dpibypass"
        minSdk = 26          // Android 8.0 — Quick Settings TileService için gerekli
        targetSdk = 35       // Android 15
        versionName = versionNameProp
        versionCode = versionCodeProp

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // arm64 öncelikli; diğerleri uyumluluk için.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            // İmzalama bilgileri ortam değişkenlerinden okunur (CI/release.yml).
            // Yerelde tanımlı değilse debug imzasına düşülür (aşağıda).
            System.getenv("RELEASE_STORE_FILE")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Keystore ortam değişkenleri varsa release imzası, yoksa build'in
            // yine de tamamlanabilmesi için imzasız bırakılır.
            signingConfig = if (System.getenv("RELEASE_STORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    lint {
        // Lint yine çalışır ve rapor üretir; ancak kozmetik bir lint "error"ı CI'ı
        // düşürmesin (native/anti-sansür yapısında yanlış-pozitifler olabilir).
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        // hev-socks5-tunnel ndk-build çıktısı ile CMake çıktısını birlikte paketle.
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Bağımlılık meta verisini APK/bundle'a gömme (F-Droid / reproducible dostu).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // DataStore (ayarlar / ağ profilleri)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // DoH ve ASN lookup için hafif HTTP istemcisi
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ---------------------------------------------------------------------------
// hev-socks5-tunnel native kütüphanesi ndk-build ile derlenir (kendi Android.mk
// yapısını kullanır). byedpi ise CMake ile (externalNativeBuild). İkisinin
// çıktısı da jniLibs'e girer.
// ---------------------------------------------------------------------------
tasks.register<Exec>("runNdkBuild") {
    group = "build"
    val ndkDir = android.ndkDirectory
    executable = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        "$ndkDir\\ndk-build.cmd"
    } else {
        "$ndkDir/ndk-build"
    }
    setArgs(
        listOf(
            "NDK_PROJECT_PATH=build/intermediates/ndkBuild",
            "NDK_LIBS_OUT=src/main/jniLibs",
            "APP_BUILD_SCRIPT=src/main/jni/Android.mk",
            "NDK_APPLICATION_MK=src/main/jni/Application.mk",
            "-j",
            Runtime.getRuntime().availableProcessors().toString()
        )
    )
    workingDir = projectDir
}

tasks.preBuild {
    dependsOn("runNdkBuild")
}
