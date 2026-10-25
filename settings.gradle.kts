pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "puzzles"

include(
    ":app",
    ":core:testing",
    ":sudoku:engine",
    ":sudoku:data",
    ":sudoku:ui",
    ":sudoku:game",
    ":sudoku:home",
    ":sudoku:stats",
    ":sudoku:settings",
    ":solitaire:engine",
    ":solitaire:data",
    ":solitaire:ui",
    ":solitaire:game",
    ":solitaire:home",
    ":solitaire:stats",
    ":solitaire:settings",
    ":twotone:engine",
    ":twotone:data",
    ":twotone:ui",
    ":twotone:game",
    ":twotone:home",
    ":twotone:stats",
    ":twotone:settings",
    ":sightlines:engine",
    ":sightlines:data",
    ":sightlines:ui",
    ":sightlines:game",
    ":sightlines:home",
    ":sightlines:stats",
    ":sightlines:settings",
)
