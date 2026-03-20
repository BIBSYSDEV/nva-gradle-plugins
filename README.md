# NVA Gradle Plugins

Shared Gradle convention plugins for [NVA](https://nva.sikt.no/) (Nasjonalt vitenarkiv) microservices. These precompiled script plugins replace copy-pasted build logic across NVA repositories.

## Prerequisites

- Java 21 (Amazon Corretto recommended)
- Gradle 9.x

## Usage

Add the plugin dependency to your root `build.gradle` (or `settings.gradle` plugin management):

```groovy
// settings.gradle
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/BIBSYSDEV/nva-gradle-plugins")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
```

```groovy
// build.gradle (root project)
plugins {
    id 'nva.root-module-conventions'
}
```

```groovy
// build.gradle (submodules)
plugins {
    id 'nva.java-conventions'
}
```

## Plugins

| Plugin ID                     | Apply to     | Description                                                         |
| ----------------------------- | ------------ | ------------------------------------------------------------------- |
| `nva.configuration`           | any          | Creates the `nva {}` extension. Applied automatically by others.    |
| `nva.java-conventions`        | submodules   | Java 21, Error Prone, PMD, JaCoCo, Spotless, JUnit 5                |
| `nva.formatting-conventions`  | any          | Spotless: Google Java Format, Groovy Gradle, Markdown, YAML         |
| `nva.gradlelint`              | root project | Nebula lint for unused/malformed dependencies (Groovy scripts only) |
| `nva.root-module-conventions` | root project | Aggregated JaCoCo coverage, dependency updates, lint                |

## Configuration

All plugins read from the shared `nva {}` extension:

```groovy
nva {
    pmdVersion = '7.15.0'           // PMD tool version
    jacocoVersion = '0.8.13'        // JaCoCo tool version
    spotlessEnabled = true           // Apply formatting before build/test
    spotlessEnforced = true          // Fail build if formatting needed
    pmdIgnoreFailures = false        // Allow PMD violations without failing
}
```

## Development

```bash
./gradlew build                # Build, test, and run all checks
./gradlew functionalTest       # Run plugin functional tests only
./gradlew publishToMavenLocal  # Publish to local Maven for testing
```

## License

MIT
