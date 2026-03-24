package no.unit.nva.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.util.Properties
import javax.inject.Inject

abstract class NvaConventionsExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val java: JavaConfig = objects.newInstance(JavaConfig::class.java)
        val spotless: SpotlessConfig = objects.newInstance(SpotlessConfig::class.java)
        val pmd: PmdConfig = objects.newInstance(PmdConfig::class.java)
        val errorprone: ErrorproneConfig = objects.newInstance(ErrorproneConfig::class.java)
        val dependencyAnalysis: DependencyAnalysisConfig = objects.newInstance(DependencyAnalysisConfig::class.java)
        val spectral: SpectralConfig = objects.newInstance(SpectralConfig::class.java)
        val jacoco: JacocoConfig = objects.newInstance(JacocoConfig::class.java)

        fun java(action: Action<JavaConfig>) = action.execute(java)

        fun spotless(action: Action<SpotlessConfig>) = action.execute(spotless)

        fun pmd(action: Action<PmdConfig>) = action.execute(pmd)

        fun errorprone(action: Action<ErrorproneConfig>) = action.execute(errorprone)

        fun dependencyAnalysis(action: Action<DependencyAnalysisConfig>) = action.execute(dependencyAnalysis)

        fun spectral(action: Action<SpectralConfig>) = action.execute(spectral)

        fun jacoco(action: Action<JacocoConfig>) = action.execute(jacoco)

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
