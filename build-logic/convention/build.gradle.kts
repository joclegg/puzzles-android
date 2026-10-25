plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "puzzles.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "puzzles.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "puzzles.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("root") {
            id = "puzzles.root"
            implementationClass = "RootConventionPlugin"
        }
        register("jvmLibrary") {
            id = "puzzles.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
