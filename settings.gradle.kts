pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "zeigmal"

// :core is deliberately a plain JVM module, not an Android library. The card
// record, the station's rules, the presence filter and the provider are the
// parts that have to be exactly right, so they are tested on the JVM in
// milliseconds with no emulator in the way. Nothing in it may import android.*;
// its build fails if it does. Same arrangement as :boardpackage in
// Lautstark/vorlaut-app.
include(":core")
include(":app")
