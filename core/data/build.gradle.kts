plugins {
    alias(libs.plugins.puzzles.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
