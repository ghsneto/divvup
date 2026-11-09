import br.app.divvup.buildlogic.lib
import br.app.divvup.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Módulo `:feature:*`: biblioteca Android + Compose + Hilt, com acesso ao design system e ao domínio. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("divvup.android.library")
        pluginManager.apply("divvup.android.compose")
        pluginManager.apply("divvup.hilt")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:model"))
            add("implementation", libs.lib("androidx-hilt-navigation-compose"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("testImplementation", project(":core:testing"))
        }
    }
}
