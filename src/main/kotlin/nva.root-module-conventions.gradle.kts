import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import no.unit.nva.gradle.NvaConventionsExtension

// Backtick syntax = Gradle core plugins, id("...") = community/custom plugins
plugins {
    base
    `jacoco-report-aggregation`
    id("com.autonomousapps.dependency-analysis")
    id("com.github.ben-manes.versions")
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
    val severity = if (nva.dependencyAnalysisEnforced.get()) "fail" else "warn"
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

    if (nva.dependencyAnalysisEnforced.get()) {
        tasks.named("check") {
            dependsOn(tasks.named("buildHealth"))
        }
    }

    tasks.named<JacocoCoverageVerification>("verifyCoverage") {
        violationRules {
            rule {
                limit {
                    counter = "METHOD"
                    value = "COVEREDRATIO"
                    minimum = nva.jacocoMinMethodCoverage.get()
                }
            }

            rule {
                limit {
                    counter = "CLASS"
                    value = "COVEREDRATIO"
                    minimum = nva.jacocoMinClassCoverage.get()
                }
            }
        }
    }
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

fun isNonStable(version: String): Boolean {
    val stableKeyword =
        listOf("RELEASE", "FINAL", "GA").any {
            version.uppercase().contains(it)
        }
    val stableVersionPattern = Regex("^[0-9,.v-]+(-r|-jre)?$")
    return !stableKeyword && !stableVersionPattern.matches(version)
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
