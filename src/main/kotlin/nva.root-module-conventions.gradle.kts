import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import no.unit.nva.gradle.NvaConventionsExtension

// Backtick syntax = Gradle core plugins, id("...") = community/custom plugins
plugins {
    base
    `jacoco-report-aggregation`
    id("nva.configuration")
    id("nva.formatting-conventions")
    id("com.github.ben-manes.versions")
}

val nva = extensions.getByType<NvaConventionsExtension>()

// Automatically aggregate coverage from all subprojects
dependencies {
    subprojects.forEach { subproject ->
        "jacocoAggregation"(subproject)
    }
}

jacoco {
    toolVersion = nva.jacocoVersion.get()
}

reporting {
    reports {
        create<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName.set("test")
        }
    }
}

val testCodeCoverageReportTask = tasks.named<JacocoReport>("testCodeCoverageReport")

tasks.register<JacocoCoverageVerification>("verifyCoverage") {
    group = "test coverage"
    description = "Verify test coverage"
    dependsOn(testCodeCoverageReportTask)

    // Get data from the aggregated report task
    executionData.setFrom(testCodeCoverageReportTask.get().executionData)
    sourceDirectories.setFrom(testCodeCoverageReportTask.get().sourceDirectories)
    classDirectories.setFrom(testCodeCoverageReportTask.get().classDirectories)

    violationRules {
        rule {
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "1.000".toBigDecimal()
            }
        }

        rule {
            limit {
                counter = "CLASS"
                value = "COVEREDRATIO"
                minimum = "1.000".toBigDecimal()
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
        // Don't suggest upgrading from a stable version to a non-stable version
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}
