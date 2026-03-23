import net.ltgt.gradle.errorprone.errorprone
import no.unit.nva.gradle.NvaConventionsExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// Backtick syntax = Gradle core plugins, id("...") = community/custom plugins
plugins {
    `java-library`
    jacoco
    pmd
    id("com.autonomousapps.dependency-analysis")
    id("net.ltgt.errorprone")
    id("nva.configuration")
    id("nva.formatting-conventions")
}

val nva = extensions.getByType<NvaConventionsExtension>()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

dependencies {
    "errorprone"("com.google.errorprone:error_prone_core:${NvaConventionsExtension.ERRORPRONE_CORE_VERSION}")
}

jacoco {
    toolVersion = NvaConventionsExtension.JACOCO_VERSION
}

pmd {
    toolVersion = NvaConventionsExtension.PMD_VERSION
    ruleSets = emptyList()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    failFast = false
    testLogging {
        events("skipped", "passed", "failed")
        showCauses = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Defer consumer-configurable values so they can be set after plugin application
afterEvaluate {
    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            allErrorsAsWarnings.set(nva.errorproneAllErrorsAsWarnings.get())
        }
    }

    pmd {
        isIgnoreFailures = nva.pmdIgnoreFailures.get()
    }

    tasks.withType<Pmd>().configureEach {
        ruleSetFiles =
            if (nva.pmdRulesetFile.isPresent) {
                files(nva.pmdRulesetFile)
            } else {
                files(
                    resources.text.fromString(
                        NvaConventionsExtension::class.java
                            .getResourceAsStream("/pmd-ruleset.xml")
                            ?.reader()
                            ?.readText()
                            ?: error("Could not load pmd-ruleset.xml from plugin resources"),
                    ),
                )
            }
    }
}
