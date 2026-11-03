import com.github.triplet.gradle.androidpublisher.ReleaseStatus

plugins {
    alias(libs.plugins.puzzles.android.application)
    alias(libs.plugins.puzzles.android.compose)
    alias(libs.plugins.play.publisher)
}

val releaseKeystore = providers.environmentVariable("PUZZLES_KEYSTORE").orNull

android {
    sourceSets {
        getByName("debug") {
            assets.directories.add("../e2e/banks")
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
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    playConfigs {
        register("debug") {
            enabled.set(false)
        }
    }
}

play {
    track.set("internal")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    defaultToAppBundles.set(true)
    useApplicationDefaultCredentials.set(true)
}

val releaseSigningTasks = setOf("packageRelease", "signReleaseBundle")

tasks.configureEach {
    if (name in releaseSigningTasks && releaseKeystore == null) {
        doFirst {
            throw GradleException("Set PUZZLES_KEYSTORE to sign a release build")
        }
    }
}

dependencies {
    implementation(projects.core.sync)
    implementation(projects.core.ui)
    implementation(projects.sudoku.data)
    implementation(projects.sudoku.ui)
    implementation(projects.sudoku.feature)
    implementation(projects.solitaire.data)
    implementation(projects.solitaire.ui)
    implementation(projects.solitaire.feature)
    implementation(projects.twotone.data)
    implementation(projects.twotone.ui)
    implementation(projects.twotone.feature)
    implementation(projects.sightlines.data)
    implementation(projects.sightlines.ui)
    implementation(projects.sightlines.feature)
    implementation(projects.fiveletters.data)
    implementation(projects.fiveletters.ui)
    implementation(projects.fiveletters.feature)
    implementation(projects.kakuro.data)
    implementation(projects.kakuro.ui)
    implementation(projects.kakuro.feature)
    implementation(projects.slideout.data)
    implementation(projects.slideout.ui)
    implementation(projects.slideout.feature)
    implementation(projects.wordwheel.data)
    implementation(projects.wordwheel.ui)
    implementation(projects.wordwheel.feature)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
}
