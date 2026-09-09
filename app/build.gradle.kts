plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// No release signing yet. The keystore, the certificate-fingerprint check and
// the tag-driven versioning come in the step docs/mvp-plan.md names, and they
// come as a copy of Lautstark/vorlaut-app's arrangement rather than a new one.
// Until then a local `./gradlew :app:assembleDebug` is how the phone gets a
// build, and `adb install -r` is how it gets the next one.
android {
    namespace = "de.lautstark.zeigmal"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.lautstark.zeigmal"
        // 26 because vorlaut-app chose it and nothing here needs more; the
        // Galaxy A51 this is built for runs Android 13 (API 33).
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("release.versionCode").getOrElse("1").toInt()
        versionName = providers.gradleProperty("release.versionName").getOrElse("0.1.0")
    }

    buildFeatures {
        compose = true
        // BuildConfig.VERSION_NAME on the diagnostics screen.
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        sarifReport = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":cardset"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    testImplementation(libs.junit)
}
