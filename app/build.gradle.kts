plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Jeder GitHub-Actions-Lauf bekommt eine höhere Versionsnummer,
// damit sich neue APKs über die alte installieren lassen.
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "de.tanzfit"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.tanzfit"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    // Fester Schlüssel im Repo: so passt jede neue APK zur installierten
    // und Updates funktionieren, ohne dass deine Übungen verloren gehen.
    signingConfigs {
        create("tanzfit") {
            storeFile = file("tanzfit.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("tanzfit")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("tanzfit")
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
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.code.gson:gson:2.11.0")
}
