package no.unit.nva.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring NVA convention plugins.
 *
 * Usage in consuming projects:
 * ```kotlin
 * nva {
 *     pmdVersion.set("7.15.0")
 *     spotlessEnabled.set(true)
 * }
 * ```
 */
abstract class NvaConventionsExtension {
    /** PMD static analysis tool version */
    abstract val pmdVersion: Property<String>

    /** JaCoCo code coverage tool version */
    abstract val jacocoVersion: Property<String>

    /** Enable Spotless formatting (applies formatting before build/test) */
    abstract val spotlessEnabled: Property<Boolean>

    /** Enforce Spotless checks (fail build if formatting needed) */
    abstract val spotlessEnforced: Property<Boolean>

    /** Allow PMD to report violations without failing the build */
    abstract val pmdIgnoreFailures: Property<Boolean>

    /** Error Prone static analysis tool version */
    abstract val errorProneCoreVersion: Property<String>

    /** Custom PMD ruleset file. When set, overrides the bundled ruleset. */
    abstract val pmdRulesetFile: RegularFileProperty

    init {
        pmdVersion.convention("7.15.0")
        jacocoVersion.convention("0.8.13")
        errorProneCoreVersion.convention("2.36.0")
        spotlessEnabled.convention(true)
        spotlessEnforced.convention(true)
        pmdIgnoreFailures.convention(false)
    }
}
