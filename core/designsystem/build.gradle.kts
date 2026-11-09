plugins {
    alias(libs.plugins.divvup.android.library)
    alias(libs.plugins.divvup.android.compose)
}

android {
    namespace = "br.app.divvup.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
}
