package br.app.divvup.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.versionInt(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

/** Kotlin compila para JVM 17 e trata avisos como erros. */
internal fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            if (this is KotlinJvmCompilerOptions) {
                jvmTarget.set(JvmTarget.JVM_17)
            }
            allWarningsAsErrors.set(true)
        }
    }
}
