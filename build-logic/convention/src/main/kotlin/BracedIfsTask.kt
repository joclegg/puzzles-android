import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class BracedIfsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations =
            sources.files.flatMap { file ->
                val code = file.readText().blankStringsAndLineComments()
                unbracedBranches(code).map { offset -> "${file.path}:${code.lineAt(offset)}" }
            }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Every if and else needs a multi-line braced block, never an inline branch:\n" + violations.joinToString("\n"),
            )
        }
        report.get().asFile.writeText("OK\n")
    }

    private fun unbracedBranches(code: String): List<Int> =
        keyword
            .findAll(code)
            .mapNotNull { match ->
                when (match.value) {
                    "if" -> {
                        afterCondition(code, match.range.last + 1)?.takeUnless { opensBlockOnItsOwnLine(code, it) }
                    }

                    else -> {
                        afterWhitespace(code, match.range.last + 1).takeUnless {
                            opensBlockOnItsOwnLine(code, it) ||
                                continuesBranch(code, it)
                        }
                    }
                }
            }.toList()

    private fun afterCondition(
        code: String,
        from: Int,
    ): Int? {
        val open = afterWhitespace(code, from)
        if (code.getOrNull(open) != '(') {
            return null
        }
        var depth = 0
        for (index in open until code.length) {
            when (code[index]) {
                '(' -> {
                    depth++
                }

                ')' -> {
                    depth--
                    if (depth == 0) {
                        return afterWhitespace(code, index + 1)
                    }
                }
            }
        }
        return null
    }

    private fun afterWhitespace(
        code: String,
        from: Int,
    ): Int {
        var index = from
        while (index < code.length && code[index].isWhitespace()) {
            index++
        }
        return index
    }

    private fun opensBlockOnItsOwnLine(
        code: String,
        index: Int,
    ): Boolean = code.getOrNull(index) == '{' && code.substring(index + 1).substringBefore('\n').isBlank()

    private fun continuesBranch(
        code: String,
        index: Int,
    ): Boolean = code.startsWith("if", index) || code.startsWith("->", index)

    private fun String.lineAt(offset: Int) = take(offset).count { it == '\n' } + 1

    private fun String.blankStringsAndLineComments(): String =
        replace(rawString) { blank(it.value) }
            .replace(string) { blank(it.value) }
            .replace(lineComment) { blank(it.value) }

    private fun blank(text: String) = text.replace(nonNewline, " ")

    private companion object {
        const val SLASH = '/'
        const val QUOTES = "\"\"\""
        val keyword = Regex("(?<![\\w.])(if|else)\\b")
        val rawString = Regex("$QUOTES[\\s\\S]*?$QUOTES")
        val string = Regex("\"(\\\\.|[^\"\\\\\\n])*\"")
        val lineComment = Regex("$SLASH$SLASH[^\\n]*")
        val nonNewline = Regex("[^\\n]")
    }
}
