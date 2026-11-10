plugins {
    alias(libs.plugins.divvup.android.feature)
    alias(libs.plugins.divvup.screenshot)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.app.divvup.feature.onboarding"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.kotlinx.serialization.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
