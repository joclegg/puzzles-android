plugins {
    alias(libs.plugins.puzzles.jvm.library)
}

tasks.register<JavaExec>("generateBank") {
    group = "sudoku"
    description = "Regenerates the puzzle bank."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.joclegg.puzzles.sudoku.engine.BankGeneratorKt")
    workingDir = rootDir
    args(
        providers.gradleProperty("bankDir").getOrElse("puzzles-data/sudoku"),
        providers.gradleProperty("bankSize").getOrElse("300"),
    )
}
