plugins {
    alias(libs.plugins.divvup.android.feature)
    alias(libs.plugins.divvup.screenshot)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.app.divvup.feature.auth"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    testImplementation(libs.kotlinx.coroutines.test)
}
