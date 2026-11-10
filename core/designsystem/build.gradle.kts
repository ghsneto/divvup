plugins {
    alias(libs.plugins.divvup.android.library)
    alias(libs.plugins.divvup.android.compose)
    alias(libs.plugins.divvup.screenshot)
}

android {
    namespace = "br.app.divvup.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    implementation(project(":core:domain"))
}
