plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)

    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)

    // Generates R.raw.aboutlibraries from the dependency graph as part of each build
    alias(libs.plugins.aboutlibraries.android)
}

// Release signing credentials come from the environment on CI, or from Gradle properties
// (~/.gradle/gradle.properties) locally. RELEASE_KEYSTORE_FILE is a path to the keystore. When
// absent, release builds fall back to the debug certificate so a fresh clone can still install
// and test the minified build.
// Read via the provider API rather than System.getenv() so the configuration cache tracks them.
fun secret(name: String): String? =
    providers.environmentVariable(name).orNull ?: providers.gradleProperty(name).orNull

val keystorePath = secret("RELEASE_KEYSTORE_FILE")
val keystorePassword = secret("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = secret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD")

val signingCredentials = listOf(keystorePath, keystorePassword, releaseKeyAlias, releaseKeyPassword)
val hasReleaseSigning = signingCredentials.all { !it.isNullOrBlank() }
val hasPartialSigning = signingCredentials.any { !it.isNullOrBlank() } && !hasReleaseSigning

val isCi = providers.environmentVariable("CI").isPresent

// Checked on the signing task, so aggregates like `build` and abbreviations are covered
if (isCi && !hasReleaseSigning) {
    val message = "Release build on CI is missing signing credentials. Required: " +
        "RELEASE_KEYSTORE_FILE, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD."
    tasks.matching { it.name == "validateSigningRelease" }.configureEach {
        doFirst { throw GradleException(message) }
    }
}
if (!isCi && hasPartialSigning) {
    logger.warn("Partial release signing credentials found; falling back to the debug certificate.")
}

// Versioning is derived from git tags of the form v<major>.<minor>.<patch>. Nothing is
// hardcoded, so tagging is the only step needed to cut a release.
data class Semver(val major: Int, val minor: Int, val patch: Int) {
    // Play requires a strictly increasing code, ceiling 2,100,000,000. This scheme allows
    // 999 minors/patches and majors up to 2100, and is reproducible from the tag alone.
    val code: Int get() = major * 1_000_000 + minor * 1_000 + patch
}

val semverRegex = Regex("""^v?(\d+)\.(\d+)\.(\d+)""")

// Pre-release tags such as v1.2.3-rc1 would share 1.2.3's versionCode, so only exact tags release
val releaseTagRegex = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

fun parseSemver(raw: String?): Semver? =
    raw?.trim()?.let(semverRegex::find)?.destructured?.let { (major, minor, patch) ->
        Semver(major.toInt(), minor.toInt(), patch.toInt())
    }

// isIgnoreExitValue so a tagless checkout or missing git degrades to the fallback rather than
// failing configuration. Provider API keeps this configuration-cache safe.
fun git(vararg args: String): String? =
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.let { output ->
        output.result.get().exitValue
            .takeIf { it == 0 }
            ?.let { output.standardOutput.asText.get().trim() }
            ?.takeIf { it.isNotBlank() }
    }

val gitDescribe = git("describe", "--tags", "--match", "v[0-9]*", "--always")
val explicitVersion = secret("RELEASE_VERSION")
val githubRefName = providers.environmentVariable("GITHUB_REF_NAME").orNull

// Precedence: explicit override, then the CI tag ref, then git describe.
val resolvedVersion = listOfNotNull(explicitVersion, githubRefName, gitDescribe)
    .firstOrNull { parseSemver(it) != null }
    ?: gitDescribe

val appSemver = parseSemver(resolvedVersion) ?: Semver(0, 0, 0)
val appVersionCode = appSemver.code.coerceAtLeast(1)
val appVersionName = when {
    // An exact tag resolves to a bare semver; anything else keeps git describe's native
    // "-<commits>-g<sha>" suffix, which already encodes how far past the tag the build is.
    parseSemver(resolvedVersion) != null -> resolvedVersion!!.removePrefix("v")
    resolvedVersion != null -> "0.0.0-$resolvedVersion"
    else -> "0.0.0"
}

// Guards against a tag that would not increase the code Play already has. Tags are a proxy for
// what has been released; Play remains the final authority.
// Parsed at configuration time: the task body may only capture plain values, since script-level
// functions cannot be serialized into the configuration cache.
val highestOtherVersionCode = git("tag", "--list", "v[0-9]*").orEmpty()
    .lines()
    .filter(releaseTagRegex::matches)
    .mapNotNull { tag -> parseSemver(tag)?.let { tag.removePrefix("v") to it } }
    .filterNot { (name, _) -> name == appVersionName }
    .maxOfOrNull { (_, semver) -> semver.code }
    ?: 0

val isReleaseTag = releaseTagRegex.matches(resolvedVersion.orEmpty())

tasks.register("verifyReleaseVersion") {
    group = "verification"
    description = "Fails unless the version is an exact release tag whose versionCode exceeds every earlier one."

    val computedCode = appVersionCode
    val computedName = appVersionName
    val highestOther = highestOtherVersionCode
    val isRelease = isReleaseTag

    doLast {
        if (!isRelease) {
            error("Version $computedName is not an exact v<major>.<minor>.<patch> release tag.")
        }
        if (computedCode <= highestOther) {
            error(
                "versionCode $computedCode (from $computedName) does not exceed the highest " +
                    "already-tagged versionCode $highestOther. Play rejects non-increasing version codes.",
            )
        }
        logger.lifecycle("Release version $computedName (versionCode $computedCode)")
    }
}

android {
    namespace = "com.ascolith.textseek"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ascolith.textseek"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Enable room auto-migrations
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("appfunctions:aggregateAppFunctions", "true")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric needs the merged resources to render Compose UI in local tests
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        renderScript = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

aboutLibraries {
    collect {
        // Entries for bundled assets that aren't Gradle dependencies, such as fonts
        configPath = file("aboutlibraries")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature-searchsetting"))
    implementation(project(":feature-searchgroup"))
    implementation(project(":feature-camerascreen"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-reportissue"))
    implementation(project(":feature-acknowledgements"))
    implementation(project(":feature-overview"))
    implementation(project(":feature-onboarding"))
    debugImplementation(project(":feature-debug"))
    implementation(project(":core:resources"))

    // https://github.com/Kotlin/kotlinx.serialization
    implementation(libs.kotlinx.serialization.json)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // https://developer.android.com/ai/appfunctions
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)

    // https://developer.android.com/jetpack/androidx/releases/camera
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.extensions)
    implementation(libs.camera.mlkit.vision)

    // https://developers.google.com/ml-kit/vision/text-recognition/v2/android
    implementation(libs.mlkit.text.recognition)

    // Local tests: jUnit, coroutines
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Local Compose UI tests
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    // https://github.com/orbit-mvi/orbit-mvi
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)
    testImplementation(libs.orbit.test)

    // https://github.com/touchlab/Kermit
    implementation(libs.kermit)
}
