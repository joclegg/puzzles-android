plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.play.publisher) apply false
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
            path = "core/ui/src/main/kotlin/com/joclegg/puzzles/core/ui/DailyCalendar.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "core/ui/src/main/kotlin/com/joclegg/puzzles/core/ui/WinDialog.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "fiveletters/ui/src/main/kotlin/com/joclegg/puzzles/fiveletters/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "kakuro/ui/src/main/kotlin/com/joclegg/puzzles/kakuro/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "sightlines/ui/src/main/kotlin/com/joclegg/puzzles/sightlines/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "slideout/ui/src/main/kotlin/com/joclegg/puzzles/slideout/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "solitaire/ui/src/main/kotlin/com/joclegg/puzzles/solitaire/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "sudoku/ui/src/main/kotlin/com/joclegg/puzzles/sudoku/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "twotone/ui/src/main/kotlin/com/joclegg/puzzles/twotone/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:compositionlocal-allowlist"
            path = "wordwheel/ui/src/main/kotlin/com/joclegg/puzzles/wordwheel/ui/Theme.kt"
        }
        suppressLintsFor {
            step = "ktlint"
            shortCode = "compose:modifier-missing-check"
            path = "core/ui/src/main/kotlin/com/joclegg/puzzles/core/ui/home/GameHomeScreen.kt"
        }
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath("$rootDir/.editorconfig")
    }
}
