plugins {
    id("nebula.lint")
}

gradleLint.rules =
    listOf(
        "dependency-parentheses",
        "dependency-tuple",
    )

// Nebula lint doesn't support configuration cache, mark its tasks as incompatible
tasks.withType<com.netflix.nebula.lint.plugin.LintGradleTask>().configureEach {
    notCompatibleWithConfigurationCache("Nebula lint uses Task.project at execution time")
}
tasks.withType<com.netflix.nebula.lint.plugin.FixGradleLintTask>().configureEach {
    notCompatibleWithConfigurationCache("Nebula lint uses Task.project at execution time")
}
