import br.app.divvup.buildlogic.configureKotlinJvmTarget
import br.app.divvup.buildlogic.configureLint
import br.app.divvup.buildlogic.lib
import br.app.divvup.buildlogic.libs
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Módulo Kotlin puro (sem Android nem Firebase), como `:core:domain` e `:core:model`. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("com.android.lint")
        extensions.configure<Lint> { configureLint() }

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        configureKotlinJvmTarget()

        dependencies {
            add("testImplementation", libs.lib("junit4"))
            add("testImplementation", libs.lib("kotlin-test"))
        }
    }
}
