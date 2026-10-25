plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    api(projects.sightlines.engine)
    implementation(projects.sightlines.data)
}
