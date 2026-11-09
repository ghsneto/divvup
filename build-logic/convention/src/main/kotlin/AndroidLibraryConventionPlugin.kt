import br.app.divvup.buildlogic.configureAndroidLibrary
import br.app.divvup.buildlogic.lib
import br.app.divvup.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidLibrary(this)
        }

        dependencies {
            add("testImplementation", libs.lib("junit4"))
            add("testImplementation", libs.lib("kotlin-test"))
        }
    }
}
