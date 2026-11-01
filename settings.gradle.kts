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
    ":core:data",
    ":core:sync",
    ":core:ui",
    ":core:testing",
    ":sudoku:engine",
    ":sudoku:data",
    ":sudoku:ui",
    ":sudoku:game",
    ":sudoku:home",
    ":sudoku:settings",
    ":solitaire:engine",
    ":solitaire:data",
    ":solitaire:ui",
    ":solitaire:game",
    ":solitaire:home",
    ":solitaire:settings",
    ":twotone:engine",
    ":twotone:data",
    ":twotone:ui",
    ":twotone:game",
    ":twotone:home",
    ":twotone:settings",
    ":sightlines:engine",
    ":sightlines:data",
    ":sightlines:ui",
    ":sightlines:game",
    ":sightlines:home",
    ":sightlines:settings",
    ":fiveletters:engine",
    ":fiveletters:data",
    ":fiveletters:ui",
    ":fiveletters:game",
    ":fiveletters:home",
    ":fiveletters:settings",
    ":kakuro:engine",
    ":kakuro:data",
    ":kakuro:ui",
    ":kakuro:game",
    ":kakuro:home",
    ":kakuro:settings",
    ":slideout:engine",
    ":slideout:data",
    ":slideout:ui",
    ":slideout:game",
    ":slideout:home",
    ":slideout:settings",
    ":wordwheel:engine",
    ":wordwheel:data",
    ":wordwheel:ui",
    ":wordwheel:game",
    ":wordwheel:home",
    ":wordwheel:settings",
)
