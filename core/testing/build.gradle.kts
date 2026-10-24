plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ascolith.textseek.core.testing"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    // FakeUri lives in src/testFixtures rather than src/main: it is only useful to JVM
    // unit tests, and androidTest APKs consume this module via implementation, so anything
    // in main sources would be compiled into an APK.
    testFixtures {
        enable = true
    }

    buildFeatures {
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
    implementation(libs.androidx.test.runner)
    implementation(libs.hilt.android.testing)
    implementation(libs.kotlinx.coroutines.test)
}
