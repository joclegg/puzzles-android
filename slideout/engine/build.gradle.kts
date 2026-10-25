plugins {
    alias(libs.plugins.puzzles.jvm.library)
}

tasks.register<JavaExec>("generateBank") {
    group = "slideout"
    description = "Regenerates the Slide Out puzzles bundled in the app's assets."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.joclegg.puzzles.slideout.engine.BankGeneratorKt")
    workingDir = rootDir
    args(
        providers.gradleProperty("bankDir").getOrElse("app/src/main/assets/slideout"),
        *providers
            .gradleProperty("bankSizes")
            .getOrElse("150,150,150,100,60")
            .split(',')
            .toTypedArray(),
    )
}
