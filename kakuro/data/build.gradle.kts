plugins {
    alias(libs.plugins.puzzles.android.library)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    sourceSets {
        named("test") {
            assets.directories.add("$projectDir/schemas")
        }
    }
}

dependencies {
    api(projects.kakuro.engine)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(projects.core.testing)
}
