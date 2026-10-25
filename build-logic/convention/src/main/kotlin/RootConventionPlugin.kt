import org.gradle.api.Plugin
import org.gradle.api.Project

class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            val verifyNoComments =
                tasks.register("verifyNoComments", NoCommentsTask::class.java) {
                    sources.from(
                        fileTree(rootDir) {
                            include("**/*.kt", "**/*.kts")
                            exclude("**/build/**", "**/.gradle/**")
                        },
                    )
                    report.set(layout.buildDirectory.file("reports/no-comments.txt"))
                }
            val verifyBracedIfs =
                tasks.register("verifyBracedIfs", BracedIfsTask::class.java) {
                    sources.from(
                        fileTree(rootDir) {
                            include("**/*.kt", "**/*.kts")
                            exclude("**/build/**", "**/.gradle/**")
                        },
                    )
                    report.set(layout.buildDirectory.file("reports/braced-ifs.txt"))
                }
            pluginManager.withPlugin("com.diffplug.spotless") {
                tasks.named("spotlessCheck") { dependsOn(verifyNoComments, verifyBracedIfs) }
            }
        }
}
