import com.diffplug.gradle.spotless.SpotlessExtension
import no.unit.nva.gradle.NvaConventionsExtension

plugins {
    id("com.diffplug.spotless")
    id("nva.configuration")
}

val nva = extensions.getByType<NvaConventionsExtension>()

spotless {
    // Java formatting only applies when java plugin is present
    plugins.withType<JavaPlugin> {
        java {
            toggleOffOn() // Ignores sections between `spotless:off` / `spotless:on`
            googleJavaFormat().reflowLongStrings().formatJavadoc(true).reorderImports(true)
        }
    }

    groovyGradle {
        target("**/*.gradle")
        greclipse()
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
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

// Defer reading extension values so consumers can override them after plugin application
afterEvaluate {
    configure<SpotlessExtension> {
        isEnforceCheck = nva.spotless.enforced.get()
    }

    if (nva.spotless.enabled.get()) {
        tasks.withType<JavaCompile>().configureEach {
            dependsOn("spotlessApply")
        }
    }
}
