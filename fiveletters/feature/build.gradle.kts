plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.puzzles.android.compose)
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(projects.core.ui)
    implementation(projects.fiveletters.data)
    implementation(projects.fiveletters.ui)

    testImplementation(libs.mockk)
    testImplementation(projects.core.testing)
}
