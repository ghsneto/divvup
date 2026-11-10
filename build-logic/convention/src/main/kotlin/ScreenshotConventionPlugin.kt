import br.app.divvup.buildlogic.lib
import br.app.divvup.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Testes de screenshot (Roborazzi + Robolectric, gráficos nativos). As imagens aprovadas ficam em
 * `src/test/screenshots` e são verificadas na CI (`verifyRoborazzi*`). Gere com `recordRoborazzi*`.
 */
class ScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")

        extensions.findByType(LibraryExtension::class.java)?.testOptions?.unitTests?.isIncludeAndroidResources = true

        extensions.configure<RoborazziExtension> {
            outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
        }

        tasks.withType<Test>().configureEach {
            systemProperty("robolectric.graphicsMode", "NATIVE")
            systemProperty("robolectric.pixelCopyRenderMode", "hardware")
        }

        dependencies {
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("roborazzi"))
            add("testImplementation", libs.lib("roborazzi-compose"))
            add("testImplementation", platform(libs.lib("androidx-compose-bom")))
            add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
            if (path != ":core:testing") add("testImplementation", project(":core:testing"))
        }
    }
}
