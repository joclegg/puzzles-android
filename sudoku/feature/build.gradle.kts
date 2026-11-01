plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(projects.core.ui)
    implementation(projects.sudoku.data)
    implementation(projects.sudoku.ui)

    testImplementation(libs.mockk)
    testImplementation(projects.core.testing)
}
