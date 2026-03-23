package no.unit.nva.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.math.BigDecimal
import java.util.Properties

/**
 * Extension for configuring NVA convention plugins.
 *
 * Usage in consuming projects:
 * ```kotlin
 * nva {
 *     spotlessEnabled.set(true)
 *     pmdIgnoreFailures.set(false)
 * }
 * ```
 */
abstract class NvaConventionsExtension {
    /** Enable Spotless formatting (applies formatting before build/test) */
    abstract val spotlessEnabled: Property<Boolean>

    /** Enforce Spotless checks (fail build if formatting needed) */
    abstract val spotlessEnforced: Property<Boolean>

    /** Allow PMD to report violations without failing the build */
    abstract val pmdIgnoreFailures: Property<Boolean>

    /** Custom PMD ruleset file. When set, overrides the bundled ruleset. */
    abstract val pmdRulesetFile: RegularFileProperty

    /** Treat all Error Prone errors as warnings instead of failing the build. Default: true */
    abstract val errorproneAllErrorsAsWarnings: Property<Boolean>

    /** Fail the build on dependency analysis issues (buildHealth). Default: false */
    abstract val dependencyAnalysisEnforced: Property<Boolean>

    /** Glob patterns for OpenAPI documents to lint with Spectral. Spectral is only active when set. */
    abstract val spectralDocuments: ListProperty<String>

    /** Custom Spectral ruleset file. When set, overrides the bundled ruleset. */
    abstract val spectralRulesetFile: RegularFileProperty

    /** Minimum method coverage ratio for JaCoCo verification (0.0 to 1.0). Default: 1.0 */
    abstract val jacocoMinMethodCoverage: Property<BigDecimal>

    /** Minimum class coverage ratio for JaCoCo verification (0.0 to 1.0). Default: 1.0 */
    abstract val jacocoMinClassCoverage: Property<BigDecimal>

    init {
        spotlessEnabled.convention(true)
        spotlessEnforced.convention(true)
        pmdIgnoreFailures.convention(false)
        errorproneAllErrorsAsWarnings.convention(true)
        dependencyAnalysisEnforced.convention(false)
        jacocoMinMethodCoverage.convention(BigDecimal("1.000"))
        jacocoMinClassCoverage.convention(BigDecimal("1.000"))
    }

    companion object {
        private val props =
            Properties().apply {
                NvaConventionsExtension::class.java
                    .getResourceAsStream("/nva-plugin.properties")
                    ?.let { load(it) }
                    ?: error("Could not load nva-plugin.properties from plugin resources")
            }

        val PMD_VERSION: String = props.getProperty("pmd.version")
        val JACOCO_VERSION: String = props.getProperty("jacoco.version")
        val ERRORPRONE_CORE_VERSION: String = props.getProperty("errorprone.core.version")
        val SPECTRAL_VERSION: String = props.getProperty("spectral.version")

        fun loadBundledResource(path: String): String =
            NvaConventionsExtension::class.java
                .getResourceAsStream(path)
                ?.reader()
                ?.readText()
                ?: error("Could not load $path from plugin resources")
    }
}
