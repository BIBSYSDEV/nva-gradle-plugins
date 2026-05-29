import com.diffplug.gradle.spotless.SpotlessTask
import no.unit.nva.gradle.NvaConventionsExtension
import no.unit.nva.gradle.serializeSpotlessTasks

plugins {
    id("com.diffplug.spotless")
    id("nva.configuration")
}

val nva = extensions.getByType<NvaConventionsExtension>()

spotless {
    // Wire spotlessCheck into check ourselves (conditional on nva.spotless.enabled) in afterEvaluate block
    isEnforceCheck = false

    groovyGradle {
        target("**/*.gradle")
        targetExclude("**/build/**")
        greclipse()
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("markdown") {
        target("**/*.md")
        targetExclude("**/build/**")
        prettier().config(mapOf("proseWrap" to "preserve"))
        endWithNewline()
    }

    format("yaml") {
        target("**/*.yaml", "**/*.yml")
        targetExclude("**/build/**")
        prettier().config(mapOf("printWidth" to 120))
        endWithNewline()
    }

    format("misc") {
        target(".gitignore", ".gitattributes", ".editorconfig")
        targetExclude("**/build/**")
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
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

// Root broad-scan formats (markdown, yaml, .gradle, misc) walk every subproject's tree.
// Run them after all subproject tasks have finished so build/ contents are stable and
// don't race with in-flight :test / :compile / :jar tasks writing under <sub>/build/.
tasks.withType<SpotlessTask>().configureEach {
    mustRunAfter(subprojects.map { it.tasks })
}

// Serialize all Spotless tasks build-wide: avoids the google-java-format classloader provisioning
// race and the build/spotless-lints/ intra-project race (diffplug/spotless#2391) with one mechanism.
serializeSpotlessTasks()
