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
    ":core:sync",
    ":core:ui",
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
    ":fiveletters:engine",
    ":fiveletters:data",
    ":fiveletters:ui",
    ":fiveletters:game",
    ":fiveletters:home",
    ":fiveletters:stats",
    ":fiveletters:settings",
    ":kakuro:engine",
    ":kakuro:data",
    ":kakuro:ui",
    ":kakuro:game",
    ":kakuro:home",
    ":kakuro:stats",
    ":kakuro:settings",
    ":slideout:engine",
    ":slideout:data",
    ":slideout:ui",
    ":slideout:game",
    ":slideout:home",
    ":slideout:stats",
    ":slideout:settings",
    ":wordwheel:engine",
    ":wordwheel:data",
    ":wordwheel:ui",
    ":wordwheel:game",
    ":wordwheel:home",
    ":wordwheel:stats",
    ":wordwheel:settings",
)
