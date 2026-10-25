plugins {
    alias(libs.plugins.puzzles.jvm.library)
}

tasks.register<JavaExec>("generateBank") {
    group = "wordwheel"
    description = "Regenerates the Word Wheel word list and puzzles bundled in the app's assets."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.joclegg.puzzles.wordwheel.engine.BankGeneratorKt")
    workingDir = rootDir
    args(
        "wordwheel/engine/data",
        providers.gradleProperty("bankDir").getOrElse("app/src/main/assets/wordwheel"),
        *providers
            .gradleProperty("bankSizes")
            .getOrElse("200,200,200")
            .split(',')
            .toTypedArray(),
    )
}
