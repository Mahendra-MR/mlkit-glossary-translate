plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.mlkitglossary.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mlkitglossary.sample"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":mlkit-glossary-translate"))
    implementation(libs.kotlinx.coroutines.android)
}
