plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
    id("com.autonomousapps.dependency-analysis") version libs.versions.dependency.analysis
    id("com.diffplug.spotless") version libs.versions.spotless
    id("io.gitlab.arturbosch.detekt") version libs.versions.detekt
    id("io.github.gradle-nexus.publish-plugin") version libs.versions.nexus.publish
}

group = "com.github.bibsysdev"
version = "1.0.2"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api(libs.dependency.updates.plugin)
    implementation(libs.dependency.analysis.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.errorprone.plugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    dependsOn("spotlessApply")
}

tasks.named("build") {
    dependsOn("spotlessApply")
    dependsOn("buildHealth")
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }
    }
}

detekt {
    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
    )
    config.setFrom(files("detekt.yml"))
    buildUponDefaultConfig = true
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "src/**/*.gradle.kts")
        ktlint()
    }
    format("markdown") {
        target("**/*.md")
        prettier().config(mapOf("proseWrap" to "preserve"))
        endWithNewline()
    }
    format("yaml") {
        target("**/*.yaml", "**/*.yml")
        prettier().config(mapOf("printWidth" to 120))
        endWithNewline()
    }
    format("misc") {
        target(".gitignore", ".gitattributes", ".editorconfig")
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// --- Publishing ---

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("NVA Gradle Plugins")
            description.set("Shared Gradle convention plugins for NVA microservices")
            url.set("https://github.com/BIBSYSDEV/nva-gradle-plugins")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://www.opensource.org/licenses/mit-license.php")
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

// Only publish the main artifact to Sonatype, not plugin marker publications
// (markers use plugin IDs like "nva.configuration" as group, which isn't a registered namespace)
tasks.withType<PublishToMavenRepository>().configureEach {
    if (name.contains("PluginMarkerMaven") && name.contains("SonatypeRepository")) {
        enabled = false
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(
                providers.environmentVariable("OSSRH_USERNAME").orElse(
                    providers.gradleProperty("ossrh.username"),
                ),
            )
            password.set(
                providers.environmentVariable("OSSRH_PASSWORD").orElse(
                    providers.gradleProperty("ossrh.password"),
                ),
            )
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("SIGNING_KEY").orNull
    val signingPassword = providers.environmentVariable("SIGNING_PASSWORD").orNull

    // Only require signing when a GPG key is available (CI)
    isRequired = signingKey != null

    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications)
}
