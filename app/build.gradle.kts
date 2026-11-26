plugins {
    alias(libs.plugins.android.application)
}

// Release signing comes from the environment (CI secrets, or the owner's shell); without it the release
// build is unsigned. Nothing secret lives in the repository.
val releaseKeystore: String? = System.getenv("SIDEPANEL_KEYSTORE")

android {
    namespace = "io.github.llwmht.sidepanel"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.llwmht.sidepanel"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("SIDEPANEL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIDEPANEL_KEY_ALIAS")
                keyPassword = System.getenv("SIDEPANEL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets["main"].assets.directories.add("../web")
}

dependencies {
    implementation(libs.nanohttpd)
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
