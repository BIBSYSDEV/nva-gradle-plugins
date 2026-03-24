import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import no.unit.nva.gradle.NvaConventionsExtension

// Backtick syntax = Gradle core plugins, id("...") = community/custom plugins
plugins {
    base
    `jacoco-report-aggregation`
    id("com.autonomousapps.dependency-analysis")
    id("com.github.ben-manes.versions")
    id("io.github.michael-nestler.spectral")
    id("nva.configuration")
    id("nva.formatting-conventions")
}

// Automatically aggregate coverage from all subprojects
dependencies {
    subprojects.forEach { subproject ->
        "jacocoAggregation"(subproject)
    }
}

jacoco {
    toolVersion = NvaConventionsExtension.JACOCO_VERSION
}

reporting {
    reports {
        create<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName.set("test")
        }
    }
}

val nva = extensions.getByType<NvaConventionsExtension>()
val testCodeCoverageReportTask = tasks.named<JacocoReport>("testCodeCoverageReport")

tasks.register<JacocoCoverageVerification>("verifyCoverage") {
    group = "test coverage"
    description = "Verify test coverage"
    dependsOn(testCodeCoverageReportTask)

    executionData.setFrom(testCodeCoverageReportTask.get().executionData)
    sourceDirectories.setFrom(testCodeCoverageReportTask.get().sourceDirectories)
    classDirectories.setFrom(testCodeCoverageReportTask.get().classDirectories)
}

afterEvaluate {
    configureDependencyAnalysis()
    configureSpectral()
    configureCoverageThresholds()
}

tasks.register("showCoverageReport") {
    group = "test coverage"
    description = "Show clickable link to test coverage report"
    dependsOn(testCodeCoverageReportTask)
    outputs.upToDateWhen { false }

    val reportDir = layout.buildDirectory.dir("reports/jacoco/testCodeCoverageReport/html")

    doLast {
        logger.quiet("Combined coverage report:")
        logger.quiet("file://${reportDir.get().asFile}/index.html")
    }
}

tasks.named("check") {
    dependsOn(tasks.named("verifyCoverage"))
    finalizedBy(tasks.named("showCoverageReport"))
}

tasks.named("verifyCoverage") {
    finalizedBy(tasks.named("showCoverageReport"))
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    checkForGradleUpdate = true
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
    gradleReleaseChannel = "current"
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

fun Project.configureDependencyAnalysis() {
    val severity = if (nva.dependencyAnalysis.enforced.get()) "fail" else "warn"
    dependencyAnalysis {
        issues {
            all {
                onAny {
                    severity(severity)
                }
                onRuntimeOnly {
                    exclude("org.apache.logging.log4j:log4j-core")
                }
            }
        }
    }

    if (nva.dependencyAnalysis.enforced.get()) {
        tasks.named("check") {
            dependsOn(tasks.named("buildHealth"))
        }
    }
}

fun Project.configureSpectral() {
    val docs = nva.spectral.documents
    if (!docs.isPresent || docs.get().isEmpty()) return

    val spectral =
        extensions.getByType(
            io.github.michaelnestler.spectral.gradle.SpectralExtension::class.java,
        )
    spectral.version.set(NvaConventionsExtension.SPECTRAL_VERSION)
    spectral.documents.from(docs.get().map { fileTree(".").matching { include(it) } })
    spectral.ruleset.set(resolveSpectralRuleset())

    tasks.named("check") {
        dependsOn(tasks.named("spectral"))
    }
}

fun Project.resolveSpectralRuleset(): File {
    if (nva.spectral.rulesetFile.isPresent) {
        return nva.spectral.rulesetFile
            .get()
            .asFile
    }
    val bundledRuleset = layout.buildDirectory.file("spectral-ruleset.yaml")
    val writeRuleset =
        tasks.register("writeSpectralRuleset") {
            outputs.file(bundledRuleset)
            doLast {
                bundledRuleset.get().asFile.writeText(
                    NvaConventionsExtension.loadBundledResource("/spectral-ruleset.yaml"),
                )
            }
        }
    tasks.named("spectral") {
        dependsOn(writeRuleset)
    }
    return bundledRuleset.get().asFile
}

fun Project.configureCoverageThresholds() {
    tasks.named<JacocoCoverageVerification>("verifyCoverage") {
        violationRules {
            rule {
                limit {
                    counter = "METHOD"
                    value = "COVEREDRATIO"
                    minimum = nva.jacoco.minMethodCoverage.get()
                }
            }

            rule {
                limit {
                    counter = "CLASS"
                    value = "COVEREDRATIO"
                    minimum = nva.jacoco.minClassCoverage.get()
                }
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword =
        listOf("RELEASE", "FINAL", "GA").any {
            version.uppercase().contains(it)
        }
    val stableVersionPattern = Regex("^[0-9,.v-]+(-r|-jre)?$")
    return !stableKeyword && !stableVersionPattern.matches(version)
}
