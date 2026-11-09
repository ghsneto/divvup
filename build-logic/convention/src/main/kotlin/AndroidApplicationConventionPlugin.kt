import br.app.divvup.buildlogic.configureAndroidApplication
import br.app.divvup.buildlogic.libs
import br.app.divvup.buildlogic.versionInt
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * App Android do Divvup. Flavors `dev` e `prod` na dimensão `ambiente`:
 * `dev` aponta para o Emulator / projeto divvup-dev; `prod` para divvup-prod.
 * O applicationId é sempre `br.app.divvup` (invariante do CLAUDE.md).
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureAndroidApplication(this)
            defaultConfig.targetSdk = libs.versionInt("targetSdk")

            flavorDimensions += "ambiente"
            productFlavors {
                create("dev") {
                    dimension = "ambiente"
                    versionNameSuffix = "-dev"
                }
                create("prod") {
                    dimension = "ambiente"
                }
            }
        }
    }
}
