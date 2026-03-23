# NVA Gradle Plugins

Shared Gradle convention plugins for [NVA](https://nva.sikt.no/) (Nasjonalt vitenarkiv) microservices. These precompiled script plugins replace copy-pasted build logic across NVA repositories.

Published to Maven Central as `com.github.bibsysdev:nva-gradle-plugins`.

## Prerequisites

- Java 21 (Amazon Corretto recommended)
- Gradle 9.x

## Usage

### settings.gradle

Since the plugins are published as a library (not to the Gradle Plugin Portal), a `resolutionStrategy` is needed to map plugin IDs to the Maven artifact:

```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith('nva.')) {
                useModule("com.github.bibsysdev:nva-gradle-plugins:${requested.version}")
            }
        }
    }
}
```

### build.gradle (root project)

```groovy
plugins {
    id 'nva.root-module-conventions' version '1.0.1'
}
```

### build.gradle (submodules)

```groovy
plugins {
    id 'nva.java-conventions' version '1.0.1'
}

repositories {
    mavenCentral()
}
```

### Usage from a build-logic module

If your repo has a `build-logic` module with its own precompiled script plugins, declare the dependency there instead of using versions in plugin blocks:

```groovy
// build-logic/build.gradle
repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation 'com.github.bibsysdev:nva-gradle-plugins:1.0.1'
}
```

```groovy
// build-logic/src/main/groovy/myproject.java-conventions.gradle
plugins {
    id 'nva.java-conventions'  // no version — resolved from build-logic dependency
}
```

## Plugins

| Plugin ID                     | Apply to     | Description                                                      |
| ----------------------------- | ------------ | ---------------------------------------------------------------- |
| `nva.configuration`           | any          | Creates the `nva {}` extension. Applied automatically by others. |
| `nva.java-conventions`        | submodules   | Java 21, Error Prone, PMD, JaCoCo, Spotless, JUnit 5             |
| `nva.formatting-conventions`  | any          | Spotless: Google Java Format, Groovy Gradle, Markdown, YAML      |
| `nva.root-module-conventions` | root project | Aggregated JaCoCo coverage, dependency updates                   |

## Configuration

All plugins read from the shared `nva {}` extension:

```groovy
nva {
    spotlessEnabled = true                           // Apply formatting before build/test
    spotlessEnforced = true                          // Fail build if formatting needed
    errorproneAllErrorsAsWarnings = true               // Treat Error Prone errors as warnings
    pmdIgnoreFailures = false                        // Allow PMD violations without failing
    pmdRulesetFile = rootProject.file('pmd.xml')    // Custom PMD ruleset (default: bundled)
    dependencyAnalysisEnforced = false                // Fail build on dependency analysis issues
    jacocoMinMethodCoverage = 1.000                  // Minimum method coverage ratio (0.0-1.0)
    jacocoMinClassCoverage = 1.000                   // Minimum class coverage ratio (0.0-1.0)
}
```

To configure across all submodules from the root `build.gradle`:

```groovy
subprojects {
    pluginManager.withPlugin('nva.java-conventions') {
        nva {
            pmdIgnoreFailures = true
        }
    }
}
```

## Development

```bash
./gradlew build                # Build, test, and run all checks
./gradlew test                 # Run tests only
./gradlew publishToMavenLocal  # Publish to local Maven for testing
```

See [PUBLISHING.md](PUBLISHING.md) for release instructions.
