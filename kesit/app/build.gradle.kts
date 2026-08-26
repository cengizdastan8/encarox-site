import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release imzasi icin isteges bagli olarak ortam degiskenleri kullanilir.
// Tanimli degilse debug anahtariyla imzalanir; APK yine de telefona kurulabilir.
val keystoreB64: String? = System.getenv("KESIT_KEYSTORE_B64")
val releaseKeystore = layout.buildDirectory.file("kesit-release.jks").get().asFile.also { file ->
    if (!keystoreB64.isNullOrBlank()) {
        file.parentFile.mkdirs()
        file.writeBytes(Base64.getDecoder().decode(keystoreB64.trim()))
    }
}

android {
    namespace = "com.encarox.kesit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.encarox.kesit"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        resourceConfigurations += listOf("tr", "en")
    }

    signingConfigs {
        if (!keystoreB64.isNullOrBlank()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KESIT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KESIT_KEY_ALIAS")
                keyPassword = System.getenv("KESIT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Media3'un @UnstableApi yuzeyini bilerek kullaniyoruz.
        disable += "UnsafeOptInUsageError"
        abortOnError = false
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)
}
