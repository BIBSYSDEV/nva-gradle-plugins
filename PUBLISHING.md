# Publishing & Usage Guide

## Publishing a release

1. Update `version` in `build.gradle.kts`
2. Commit and push to main
3. Create a GitHub release:
   ```bash
   gh release create v1.0.1 --title "v1.0.1" --notes "Description of changes" --target main
   ```
4. The `publish.yml` workflow builds, tests, signs, and publishes to Maven Central automatically
5. Artifacts appear on Maven Central within ~10-30 minutes

## Testing locally

Publish to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

Then in the consuming project's `settings.gradle`, add `mavenLocal()` to the plugin repositories:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
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

If the consuming project has a `build-logic` module, also add `mavenLocal()` to its `repositories` block.

Remember to remove `mavenLocal()` before committing.

## GitHub repository secrets

These are already configured. If they need to be rotated:

| Secret             | Description                               |
| ------------------ | ----------------------------------------- |
| `OSSRH_USERNAME`   | Sonatype Central user token username      |
| `OSSRH_PASSWORD`   | Sonatype Central user token password      |
| `SIGNING_KEY`      | ASCII-armored GPG private key for signing |
| `SIGNING_PASSWORD` | Passphrase for the GPG key                |

When pasting the GPG key, ensure there is a trailing newline after `-----END PGP PRIVATE KEY BLOCK-----`.
