pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "divvup"

include(":app")

include(":core:designsystem")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:testing")

include(":feature:auth")
include(":feature:onboarding")
include(":feature:house")
include(":feature:deck")
include(":feature:tasks")
include(":feature:ranking")
include(":feature:notifications")
include(":feature:profile")
include(":feature:discover")
