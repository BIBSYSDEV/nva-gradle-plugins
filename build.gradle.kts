plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
}

group = "com.github.bibsysdev"
version = "1.0.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Plugin dependencies - these become available to precompiled script plugins
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.1.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.1.0")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.53.0")
    implementation("com.netflix.nebula:gradle-lint-plugin:20.5.6")
}

kotlin {
    jvmToolchain(21)
}

// Publishing configuration (to be expanded for Maven Central)
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("NVA Gradle Plugins")
                description.set("Shared Gradle convention plugins for NVA microservices")
                url.set("https://github.com/BIBSYSDEV/nva-gradle-plugins")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }

                developers {
                    developer {
                        name.set("NVA Team")
                        organization.set("Sikt")
                        organizationUrl.set("https://sikt.no/")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/BIBSYSDEV/nva-gradle-plugins.git")
                    developerConnection.set("scm:git:ssh://github.com/BIBSYSDEV/nva-gradle-plugins.git")
                    url.set("https://github.com/BIBSYSDEV/nva-gradle-plugins/tree/main")
                }
            }
        }
    }
}
