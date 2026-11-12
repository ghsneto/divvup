plugins {
    alias(libs.plugins.divvup.android.feature)
    alias(libs.plugins.divvup.screenshot)
}

android {
    namespace = "br.app.divvup.feature.deck"
}

dependencies {
    implementation(project(":core:data"))
    testImplementation(libs.kotlinx.coroutines.test)
}
