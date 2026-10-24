plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ascolith.textseek.core.ui"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        renderScript = false
        shaders = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(project(":domain"))
    implementation(project(":core:resources"))
    implementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // https://github.com/skydoves/colorpicker-compose
    implementation(libs.compose.colorpicker)

    // https://github.com/airbnb/lottie-android
    implementation(libs.lottie.compose)

    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Local tests
    testImplementation(libs.junit)
}
