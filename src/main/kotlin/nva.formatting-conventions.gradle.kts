import com.diffplug.gradle.spotless.SpotlessExtension
import no.unit.nva.gradle.NvaConventionsExtension

plugins {
    id("com.diffplug.spotless")
    id("nva.configuration")
}

spotless {
    isEnforceCheck = false

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

afterEvaluate {
    val nva = extensions.getByType<NvaConventionsExtension>()

    if (nva.spotlessEnabled.get()) {
        tasks.named("build") {
            dependsOn("spotlessApply")
        }

        tasks.matching { it.name == "test" }.configureEach {
            dependsOn("spotlessApply")
        }
    }

    if (nva.spotlessEnforced.get()) {
        configure<SpotlessExtension> {
            isEnforceCheck = true
        }
    }
}
