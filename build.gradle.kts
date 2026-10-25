plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.puzzles.root)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("$rootDir/.editorconfig")
            .customRuleSets(
                listOf(
                    libs.compose.rules.ktlint
                        .get()
                        .toString(),
                ),
            )

        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "sudoku/ui/src/main/kotlin/com/joclegg/puzzles/sudoku/core/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "solitaire/ui/src/main/kotlin/com/joclegg/puzzles/solitaire/core/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "twotone/ui/src/main/kotlin/com/joclegg/puzzles/twotone/core/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "sightlines/ui/src/main/kotlin/com/joclegg/puzzles/sightlines/core/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "fiveletters/ui/src/main/kotlin/com/joclegg/puzzles/fiveletters/core/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "kakuro/ui/src/main/kotlin/com/joclegg/puzzles/kakuro/core/ui/Theme.kt"
        }
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath("$rootDir/.editorconfig")
    }
}
