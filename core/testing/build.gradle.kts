plugins {
    alias(libs.plugins.divvup.android.library)
}

android {
    namespace = "br.app.divvup.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(libs.junit4)
    api(libs.kotlin.test)
}
