package no.unit.nva.gradle

import org.gradle.api.provider.Property

abstract class DependencyAnalysisConfig {
    abstract val enforced: Property<Boolean>

    init {
        enforced.convention(false)
    }
}
