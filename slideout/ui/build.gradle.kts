plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    api(projects.slideout.engine)
    implementation(projects.slideout.data)
}
