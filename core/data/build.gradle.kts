plugins {
    alias(libs.plugins.divvup.android.library)
    alias(libs.plugins.divvup.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.app.divvup.core.data"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
