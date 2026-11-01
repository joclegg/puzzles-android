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
    ":sudoku:feature",
    ":solitaire:engine",
    ":solitaire:data",
    ":solitaire:ui",
    ":solitaire:feature",
    ":twotone:engine",
    ":twotone:data",
    ":twotone:ui",
    ":twotone:feature",
    ":sightlines:engine",
    ":sightlines:data",
    ":sightlines:ui",
    ":sightlines:feature",
    ":fiveletters:engine",
    ":fiveletters:data",
    ":fiveletters:ui",
    ":fiveletters:feature",
    ":kakuro:engine",
    ":kakuro:data",
    ":kakuro:ui",
    ":kakuro:feature",
    ":slideout:engine",
    ":slideout:data",
    ":slideout:ui",
    ":slideout:feature",
    ":wordwheel:engine",
    ":wordwheel:data",
    ":wordwheel:ui",
    ":wordwheel:feature",
)
