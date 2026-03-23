import net.ltgt.gradle.errorprone.errorprone
import no.unit.nva.gradle.NvaConventionsExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// Backtick syntax = Gradle core plugins, id("...") = community/custom plugins
plugins {
    `java-library`
    id("nva.configuration")
    id("nva.formatting-conventions")
    jacoco
    pmd
    id("net.ltgt.errorprone")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        // Suppress build failures from lint errors (remove this if possible)
        allErrorsAsWarnings.set(true)
    }
}

afterEvaluate {
    val nva = extensions.getByType<NvaConventionsExtension>()

    pmd {
        toolVersion = nva.pmdVersion.get()
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
        ruleSets = emptyList()
        isIgnoreFailures = nva.pmdIgnoreFailures.get()
    }
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

tasks.named("check") {
    dependsOn(tasks.named("test"))
}
