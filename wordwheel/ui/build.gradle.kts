plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    api(projects.core.ui)
    api(projects.wordwheel.engine)
    implementation(projects.wordwheel.data)
}
