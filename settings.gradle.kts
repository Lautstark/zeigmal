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

// :cardset is deliberately a plain JVM module, not an Android library. It reads
// the Kartensatz - the manifest and the card mapping - and that is the part that
// has to be exactly right, so it is tested on the JVM in milliseconds with no
// emulator in the way. Nothing in it may import android.*; its build fails if
// it does. Same arrangement as :boardpackage in Lautstark/vorlaut-app.
include(":cardset")
include(":app")
