plugins {
    alias(libs.plugins.divvup.android.library)
    alias(libs.plugins.divvup.hilt)
}

android {
    namespace = "br.app.divvup.core.data"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))
}
