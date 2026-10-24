pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "TextSeek"

include(":app")
include("core:data")
include(":core:database")
include(":core:testing")
include(":core:ui")
include(":core:hardware")
include(":core:common")
include(":domain")
include(":feature-searchsetting")
include(":feature-searchgroup")
include(":feature-camerascreen")
include(":feature-settings")
include(":feature-reportissue")
include(":feature-debug")
include(":feature-acknowledgements")
include(":core")
include(":core:resources")
include(":feature-overview")
include(":feature-onboarding")
