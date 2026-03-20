plugins {
    id("nebula.lint")
}

gradleLint.rules =
    listOf(
        "dependency-parentheses",
        "dependency-tuple",
    )
