# Publishing & Usage Guide

## Prerequisites

The `com.github.bibsysdev` group ID must be registered on Sonatype OSSRH (already done for nva-commons).

### GitHub repository secrets

Configure these in the repo's Settings > Secrets and variables > Actions:

| Secret                 | Description                               |
| ---------------------- | ----------------------------------------- |
| `OSSRH_USERNAME`       | Sonatype OSSRH / Nexus username           |
| `OSSRH_PASSWORD`       | Sonatype OSSRH / Nexus password           |
| `GPG_SIGNING_KEY`      | ASCII-armored GPG private key for signing |
| `GPG_SIGNING_PASSWORD` | Passphrase for the GPG key                |

To export your GPG key for the secret:

```bash
gpg --armor --export-secret-keys YOUR_KEY_ID
```

## Publishing

### Automatic (via GitHub release)

1. Update `version` in `build.gradle.kts` (remove `-SNAPSHOT`)
2. Commit and push
3. Create a GitHub release:
   ```bash
   gh release create v1.0.0 --title "v1.0.0" --notes "Initial release"
   ```
4. The `publish.yml` workflow builds, tests, signs, and publishes to Maven Central staging
5. Log into https://s01.oss.sonatype.org, find the staging repository, **Close** and then **Release** it

### Manual

```bash
OSSRH_USERNAME=your-user OSSRH_PASSWORD=your-pass \
GPG_SIGNING_KEY="$(gpg --armor --export-secret-keys YOUR_KEY_ID)" \
GPG_SIGNING_PASSWORD=your-passphrase \
./gradlew publish
```

### Snapshots

Snapshot versions (ending in `-SNAPSHOT`) are published to the OSSRH snapshots repository and do not require staging/release.

## Consuming in another repo

No authentication required — Maven Central is public.

### settings.gradle

```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### build.gradle (root project)

```groovy
plugins {
    id 'nva.root-module-conventions' version '1.0.0'
}
```

### build.gradle (each submodule)

```groovy
plugins {
    id 'nva.java-conventions' version '1.0.0'
}

repositories {
    mavenCentral()
}

dependencies {
    errorprone 'com.google.errorprone:error_prone_core:2.36.0'
}
```

## Notes

- The `errorprone` dependency must be declared by consuming projects
- Nebula lint (`nva.gradlelint` / `nva.root-module-conventions`) only works with Groovy build scripts
- After publishing a release, it takes ~10-30 minutes for artifacts to appear on Maven Central
