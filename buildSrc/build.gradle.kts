// Compiles the plugin's own VersionCooldown so the root build script can apply the same cooldown
// to this repository's dependencyUpdates report, without keeping a second copy of it.
plugins {
    `embedded-kotlin`
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            srcDir("../src/main/kotlin/no/unit/nva/gradle")
            include("VersionCooldown.kt")
        }
    }
}
