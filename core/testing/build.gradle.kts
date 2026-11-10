plugins {
    alias(libs.plugins.divvup.android.library)
    alias(libs.plugins.divvup.android.compose)
}

android {
    namespace = "br.app.divvup.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    api(project(":core:domain"))
    api(project(":core:data"))
    api(libs.kotlinx.coroutines.test)
    api(libs.junit4)
    api(libs.kotlin.test)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
}
