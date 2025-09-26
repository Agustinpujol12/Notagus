import java.util.Properties
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
}

android {
    namespace = "com.agustinpujol.notagus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agustinpujol.notagus"
        minSdk = 24
        targetSdk = 36

        // Cambiá estos cuando quieras instalar por encima de otra versión
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ---- Firma: lee credenciales desde keystore.properties EN LA RAÍZ ----
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            load(FileInputStream(keystorePropertiesFile))
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val ksPath = keystoreProperties["storeFile"] as String
                storeFile = rootProject.file(ksPath)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                println("WARNING: keystore.properties no encontrado; release sin firma.")
            }
        }
    }

    buildTypes {
        getByName("debug") { /* firma debug por defecto */ }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/LICENSE*")
        resources.excludes.add("META-INF/NOTICE*")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core:1.13.1") // Proyecto en Java
    implementation("androidx.work:work-runtime:2.9.0") // o la última estable

    // Room (Java)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

/* ========= Archivado seguro de APKs =========
   Copia el APK firmado desde build/outputs y lo deja en release-archives / debug-archives
   con nombre único (versionName + versionCode + timestamp).
*/
val vName = android.defaultConfig.versionName ?: "1.0.0"
val vCode = android.defaultConfig.versionCode ?: 1

tasks.register<Copy>("archiveApkRelease") {
    group = "distribution"
    description = "Copia y renombra el APK release a release-archives/"
    dependsOn("assembleRelease")

    val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

    // ÚNICO origen confiable
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(layout.projectDirectory.dir("release-archives"))

    doFirst { file("$rootDir/release-archives").mkdirs() }

    // p.ej.: Notagus-release-v1.0.0(1)-20250821-1234.apk
    rename { _ -> "Notagus-release-v${vName}(${vCode})-${ts}.apk" }
}

tasks.register<Copy>("archiveApkDebug") {
    group = "distribution"
    description = "Copia y renombra el APK debug a debug-archives/"
    dependsOn("assembleDebug")

    val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(layout.projectDirectory.dir("debug-archives"))

    doFirst { file("$rootDir/debug-archives").mkdirs() }

    rename { _ -> "Notagus-debug-v${vName}(${vCode})-${ts}.apk" }
}
