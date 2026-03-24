package no.unit.nva.gradle

import org.gradle.api.provider.Property

abstract class ErrorproneConfig {
    abstract val allErrorsAsWarnings: Property<Boolean>

    init {
        allErrorsAsWarnings.convention(true)
    }
}
