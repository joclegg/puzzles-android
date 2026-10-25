import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.findByType<ApplicationExtension>()?.buildFeatures?.compose = true
            extensions.findByType<LibraryExtension>()?.buildFeatures?.compose = true
            dependencies {
                val bom = platform(libs.library("compose-bom"))
                add("implementation", bom)
                add("testImplementation", bom)
                add("implementation", libs.library("compose-ui"))
                add("implementation", libs.library("compose-ui-graphics"))
                add("implementation", libs.library("compose-ui-tooling-preview"))
                add("implementation", libs.library("compose-material3"))
                add("debugImplementation", libs.library("compose-ui-tooling"))
                add("debugImplementation", libs.library("compose-ui-test-manifest"))
                add("testImplementation", libs.library("compose-ui-test-junit4"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("androidx-test-ext-junit"))
            }
        }
}
