plugins {
    id("nebula.lint")
}

gradleLint.rules = listOf(
    "unused-dependency",
    "dependency-parentheses",
    "dependency-tuple"
)
