import net.ltgt.gradle.errorprone.errorprone
import no.unit.nva.gradle.NvaConventionsExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// Backtick syntax = Gradle core plugins, id("...") = community/custom plugins
plugins {
    `java-library`
    jacoco
    pmd
    id("com.autonomousapps.dependency-analysis")
    id("com.bakdata.mockito")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    id("nva.configuration")
}

val nva = extensions.getByType<NvaConventionsExtension>()

java {
    toolchain {
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
        events("skipped", "failed")
        showCauses = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("log4j2.configurationFile", "classpath:nva-log4j2.xml")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotless {
    // Wire spotlessCheck into check ourselves (conditional on nva.spotless.enabled) in afterEvaluate block
    isEnforceCheck = false

    java {
        targetExclude("**/build/**")
        googleJavaFormat().reflowLongStrings().formatJavadoc(true).reorderImports(true)
    }
}

// Defer reading extension values so consumers can override them after plugin application
afterEvaluate {
    if (nva.spotless.enabled.get()) {
        tasks.named("check") { dependsOn("spotlessCheck") }
        tasks.matching { it.name == "spotlessCheck" }.configureEach {
            dependsOn("spotlessApply")
        }
    }
}

// Defer consumer-configurable values so they can be set after plugin application
afterEvaluate {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(nva.java.languageVersion.get()))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            allErrorsAsWarnings.set(nva.errorprone.allErrorsAsWarnings.get())
            excludedPaths.set(".*/build/.*")
        }
    }

    pmd {
        isIgnoreFailures = nva.pmd.ignoreFailures.get()
    }

    tasks.withType<Pmd>().configureEach {
        exclude("**/build/**")
        nva.generatedCode.get().forEach { exclude(it) }
        ruleSetFiles =
            if (nva.pmd.rulesetFile.isPresent) {
                files(nva.pmd.rulesetFile)
            } else {
                files(resources.text.fromString(NvaConventionsExtension.loadBundledResource("/pmd-ruleset.xml")))
            }
    }
}
