plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.llwmht.sidepanel"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.llwmht.sidepanel"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets["main"].assets.directories.add("../web")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
