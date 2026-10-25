import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class NoCommentsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations =
            sources.files.flatMap { file ->
                val isTest = file.invariantSeparatorsPath.contains("/src/test/")
                file.readLines().mapIndexedNotNull { index, line ->
                    val allowed = isTest && line.trim() in testSectionMarkers
                    "${file.path}:${index + 1}: ${line.trim()}".takeIf { !allowed && commentStart.containsMatchIn(line.withoutStrings()) }
                }
            }
        if (violations.isNotEmpty()) {
            throw GradleException("Comments are not allowed; name things so the code explains itself:\n" + violations.joinToString("\n"))
        }
        report.get().asFile.writeText("OK\n")
    }

    private fun String.withoutStrings() = replace(stringLiteral, "\"\"")

    private companion object {
        const val SLASH = '/'
        val commentStart = Regex("(^|\\s)$SLASH[$SLASH*]")
        val stringLiteral = Regex("\"(\\\\.|[^\"\\\\])*\"")
        val testSectionMarkers = setOf("$SLASH$SLASH Arrange", "$SLASH$SLASH Act", "$SLASH$SLASH Assert")
    }
}
