package no.unit.nva.gradle

import org.gradle.api.provider.Property

abstract class SpotlessConfig {
    abstract val enabled: Property<Boolean>

    init {
        enabled.convention(true)
    }
}
