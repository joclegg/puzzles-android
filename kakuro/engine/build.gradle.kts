plugins {
    alias(libs.plugins.puzzles.jvm.library)
}

tasks.register<JavaExec>("generateBank") {
    group = "kakuro"
    description = "Regenerates the Kakuro puzzles bundled in the app's assets."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.joclegg.puzzles.kakuro.engine.BankGeneratorKt")
    workingDir = rootDir
    args(
        providers.gradleProperty("bankDir").getOrElse("app/src/main/assets/kakuro"),
        *providers
            .gradleProperty("bankSizes")
            .getOrElse("200,200,150,60")
            .split(',')
            .toTypedArray(),
    )
}
