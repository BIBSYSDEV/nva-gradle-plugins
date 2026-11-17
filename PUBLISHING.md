# Publishing & Usage Guide

## Publishing a release

1. Update `version` in `build.gradle.kts`
2. Commit and push to main
3. Create a GitHub release with a tag matching that version:
   ```bash
   gh release create v1.0.1 --title v1.0.1 --generate-notes --target main
   ```
4. The `publish.yml` workflow checks that the tag matches `version`, then builds, tests, signs, and publishes to Maven Central automatically
5. Artifacts appear on Maven Central within ~10-30 minutes
6. Optional: Edit the generated release notes on GitHub

If you use [mise](https://mise.jdx.dev), `mise run publish:release` does step 3 for you.
It shows the latest release and the `version` in `build.gradle.kts` on main, then creates the release for that version once you confirm.
`mise run publish:versions` shows the same two versions on its own.

## Testing locally

Publish to your local Maven repository:

```bash
./gradlew publishToMavenLocal -PlocalBuild
```

`-PlocalBuild` adds a `-local` suffix to `version` (e.g. `1.0.14-local`), so the local build cannot be mistaken for the release on Maven Central.
Use that version in the consuming project.
`mise run publish:local` runs the same command.

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
