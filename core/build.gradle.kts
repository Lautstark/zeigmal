plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// A plain JVM module on purpose. The card record, the station's rules and the provider
// are the parts that have to be exactly right, so they
// must be runnable in a plain `test` task, with no emulator and no Robolectric
// between the assertion and the parser.
//
// The one rule that keeps that true: nothing under src/main may import
// android.*. `forbidAndroidImports` below fails the build if it does, because
// the first accidental `android.util.Log` would quietly make this module
// untestable on the JVM again.
kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed")
        showStandardStreams = false
    }
}

val forbidAndroidImports =
    tasks.register("forbidAndroidImports") {
        description = "Fails if the core has picked up an Android dependency."
        group = "verification"
        val sources = fileTree("src/main/kotlin") { include("**/*.kt") }
        val root = projectDir
        val marker = layout.buildDirectory.file("checks/no-android-imports.txt")
        inputs.files(sources)
        outputs.file(marker)
        doLast {
            val offenders =
                sources.files.filter { file ->
                    file.readLines().any { it.trimStart().startsWith("import android") }
                }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "The core must stay free of Android imports, so it can be " +
                        "unit-tested on the JVM. Offending files:\n" +
                        offenders.joinToString("\n") { "  " + it.relativeTo(root) },
                )
            }
            marker
                .get()
                .asFile
                .apply { parentFile.mkdirs() }
                .writeText("ok\n")
        }
    }

tasks.named("check") { dependsOn(forbidAndroidImports) }
