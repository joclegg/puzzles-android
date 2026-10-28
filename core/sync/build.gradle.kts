plugins {
    alias(libs.plugins.puzzles.android.library)
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
