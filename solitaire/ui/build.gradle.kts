plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    api(projects.solitaire.engine)
    implementation(projects.solitaire.data)
}
