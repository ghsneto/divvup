package br.app.divvup.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/** Configuração comum a app e bibliotecas Android: SDKs (minSdk 26) e Java 17. */
internal fun Project.configureAndroidApplication(ext: ApplicationExtension) {
    ext.compileSdk = libs.versionInt("compileSdk")
    ext.defaultConfig.minSdk = libs.versionInt("minSdk")
    ext.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    ext.compileOptions.targetCompatibility = JavaVersion.VERSION_17
    ext.lint.configureLint()
    ext.lint.checkDependencies = true
    configureKotlinJvmTarget()
    configureRobolectricJvm()
    configurarGuardaDeLiterais()
}

internal fun Project.configureAndroidLibrary(ext: LibraryExtension) {
    ext.compileSdk = libs.versionInt("compileSdk")
    ext.defaultConfig.minSdk = libs.versionInt("minSdk")
    ext.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    ext.compileOptions.targetCompatibility = JavaVersion.VERSION_17
    ext.testOptions.unitTests.isReturnDefaultValues = true
    ext.lint.configureLint()
    configureKotlinJvmTarget()
    configureRobolectricJvm()
    configurarGuardaDeLiterais()
}

/** Robolectric (SDK 36) em JDK 21 precisa acessar internos do java.base. */
private fun Project.configureRobolectricJvm() {
    tasks.withType<Test>().configureEach {
        jvmArgs(
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
        )
    }
}

/**
 * Lint estrito: aviso vira erro. Os avisos de "versão mais nova disponível" ficam desligados
 * porque só adotamos versões com pelo menos duas semanas de publicadas.
 */
internal fun Lint.configureLint() {
    warningsAsErrors = true
    abortOnError = true
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}
