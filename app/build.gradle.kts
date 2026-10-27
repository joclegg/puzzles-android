plugins {
    alias(libs.plugins.puzzles.android.application)
    alias(libs.plugins.puzzles.android.compose)
}

val releaseKeystore = providers.environmentVariable("PUZZLES_KEYSTORE").orNull

android {
    sourceSets {
        getByName("main") {
            assets.directories.add("../puzzles-data")
        }
    }
    namespace = "com.joclegg.puzzles"
    defaultConfig {
        applicationId = "com.joclegg.puzzles"
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "0.1.0"
    }
    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("PUZZLES_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("PUZZLES_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("PUZZLES_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig =
                if (releaseKeystore != null) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }
}

dependencies {
    implementation(projects.sudoku.data)
    implementation(projects.sudoku.ui)
    implementation(projects.sudoku.game)
    implementation(projects.sudoku.home)
    implementation(projects.sudoku.stats)
    implementation(projects.sudoku.settings)
    implementation(projects.solitaire.data)
    implementation(projects.solitaire.ui)
    implementation(projects.solitaire.game)
    implementation(projects.solitaire.home)
    implementation(projects.solitaire.stats)
    implementation(projects.solitaire.settings)
    implementation(projects.twotone.data)
    implementation(projects.twotone.ui)
    implementation(projects.twotone.game)
    implementation(projects.twotone.home)
    implementation(projects.twotone.stats)
    implementation(projects.twotone.settings)
    implementation(projects.sightlines.data)
    implementation(projects.sightlines.ui)
    implementation(projects.sightlines.game)
    implementation(projects.sightlines.home)
    implementation(projects.sightlines.stats)
    implementation(projects.sightlines.settings)
    implementation(projects.fiveletters.data)
    implementation(projects.fiveletters.ui)
    implementation(projects.fiveletters.game)
    implementation(projects.fiveletters.home)
    implementation(projects.fiveletters.stats)
    implementation(projects.fiveletters.settings)
    implementation(projects.kakuro.data)
    implementation(projects.kakuro.ui)
    implementation(projects.kakuro.game)
    implementation(projects.kakuro.home)
    implementation(projects.kakuro.stats)
    implementation(projects.kakuro.settings)
    implementation(projects.slideout.data)
    implementation(projects.slideout.ui)
    implementation(projects.slideout.game)
    implementation(projects.slideout.home)
    implementation(projects.slideout.stats)
    implementation(projects.slideout.settings)
    implementation(projects.wordwheel.data)
    implementation(projects.wordwheel.ui)
    implementation(projects.wordwheel.game)
    implementation(projects.wordwheel.home)
    implementation(projects.wordwheel.stats)
    implementation(projects.wordwheel.settings)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
}
