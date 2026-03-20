plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
    id("io.gitlab.arturbosch.detekt") version libs.versions.detekt
    id("com.diffplug.spotless") version libs.versions.spotless
}

group = "com.github.bibsysdev"
version = "1.0.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.spotless.plugin)
    implementation(libs.errorprone.plugin)
    implementation(libs.dependency.updates.plugin)
    implementation(libs.nebula.lint.plugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

// --- Functional tests ---

val functionalTestSourceSet = sourceSets.create("functionalTest")

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(functionalTest)
}

detekt {
    source.setFrom(
        "src/main/kotlin",
        "src/functionalTest/kotlin",
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

    repositories {
        maven {
            name = "OSSRH"
            val isSnapshot = version.toString().endsWith("SNAPSHOT")
            url =
                if (isSnapshot) {
                    uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
            credentials {
                username = providers.environmentVariable("OSSRH_USERNAME").orNull
                    ?: providers.gradleProperty("ossrh.username").orNull
                password = providers.environmentVariable("OSSRH_PASSWORD").orNull
                    ?: providers.gradleProperty("ossrh.password").orNull
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY").orNull
    val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD").orNull

    // Sign when GPG key is available (CI) or when explicitly publishing a release
    isRequired = signingKey != null || !version.toString().endsWith("SNAPSHOT")

    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["mavenJava"])
}
