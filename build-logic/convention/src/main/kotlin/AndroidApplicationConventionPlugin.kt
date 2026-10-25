import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                compileSdk = libs.version("compile-sdk").toInt()
                defaultConfig {
                    minSdk = libs.version("min-sdk").toInt()
                    targetSdk = libs.version("target-sdk").toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    val name = variant.name.replaceFirstChar { it.uppercase() }
                    val verify =
                        tasks.register("verify${name}NoInternet", NoInternetPermissionTask::class.java) {
                            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                            report.set(layout.buildDirectory.file("reports/no-internet/${variant.name}.txt"))
                        }
                    tasks.named("check") { dependsOn(verify) }
                    tasks.matching { it.name == "assemble$name" }.configureEach { dependsOn(verify) }
                }
            }
        }
}
