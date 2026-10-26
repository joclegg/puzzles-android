import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class NetworkPermissionTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifest = mergedManifest.get().asFile.readText()
        val banned = listOf("android.permission.ACCESS_NETWORK_STATE").filter { it in manifest }
        if (banned.isNotEmpty()) {
            throw GradleException(
                "Merged manifest requests $banned, but the app only needs INTERNET for the opt-in NYT word. Remove it with tools:node=\"remove\".",
            )
        }
        report.get().asFile.writeText("OK\n")
    }
}
