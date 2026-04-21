import no.unit.nva.gradle.NvaConventionsExtension

plugins {
    id("com.diffplug.spotless")
    id("nva.configuration")
}

val nva = extensions.getByType<NvaConventionsExtension>()

spotless {
    // Wire spotlessCheck into check ourselves (conditional on nva.spotless.enabled) in afterEvaluate block
    isEnforceCheck = false

    // Java formatting only applies when java plugin is present
    plugins.withType<JavaPlugin> {
        java {
            targetExclude("**/build/**")
            toggleOffOn() // Ignores sections between `spotless:off` / `spotless:on`
            googleJavaFormat().reflowLongStrings().formatJavadoc(true).reorderImports(true)
        }
    }

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

// Workaround for https://github.com/diffplug/spotless/issues/2391
// Spotless tasks share state in build/spotless-lints/ that isn't safe under intra-project parallelism
// (Gradle 9 + org.gradle.parallel=true + configuration cache). Forces a total ordering
// across per-format tasks so they never execute concurrently within a subproject.
val spotlessFormatsInOrder = listOf("Java", "GroovyGradle", "Markdown", "Yaml", "Misc")
val spotlessTaskNamesByFormat =
    spotlessFormatsInOrder.associateWith { format ->
        setOf("spotless$format", "spotless${format}Apply", "spotless${format}Check")
    }
spotlessFormatsInOrder.zipWithNext().forEach { (earlierFormat, laterFormat) ->
    val earlierTaskNames = spotlessTaskNamesByFormat.getValue(earlierFormat)
    val laterTaskNames = spotlessTaskNamesByFormat.getValue(laterFormat)
    tasks.matching { it.name in laterTaskNames }.configureEach {
        mustRunAfter(tasks.matching { it.name in earlierTaskNames })
    }
}
