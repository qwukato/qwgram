plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val tgAppId: String = System.getenv("TG_APP_ID") ?: "0"
val tgAppHash: String = System.getenv("TG_APP_HASH") ?: ""

android {
    namespace = "app.qwgram.next"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.qwgram.next"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
        buildConfigField("int", "TG_APP_ID", tgAppId)
        buildConfigField("String", "TG_APP_HASH", "\"$tgAppHash\"")
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    signingConfigs {
        create("qwgram") {
            val ks = rootProject.file("keystore.p12")
            if (ks.exists()) {
                storeFile = ks
                storeType = "pkcs12"
                storePassword = "qwgram"
                keyAlias = "qwgram"
                keyPassword = "qwgram"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (rootProject.file("keystore.p12").exists()) signingConfigs.getByName("qwgram") else signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/tdlib/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs { useLegacyPackaging = true }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3:1.5.0-alpha08")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("androidx.emoji2:emoji2:1.5.0")
    implementation("com.airbnb.android:lottie-compose:6.6.2")        // animated stickers / emoji (TGS = gzipped Lottie)
    implementation("androidx.media3:media3-exoplayer:1.5.1")         // video playback
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("androidx.emoji2:emoji2-bundled:1.5.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-process:2.9.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
