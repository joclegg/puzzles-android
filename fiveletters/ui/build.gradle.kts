plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    api(projects.fiveletters.engine)
    implementation(projects.fiveletters.data)
}
