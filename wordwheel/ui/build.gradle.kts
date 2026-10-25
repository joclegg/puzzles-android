plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    api(projects.wordwheel.engine)
    implementation(projects.wordwheel.data)
}
