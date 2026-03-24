package no.unit.nva.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class PmdConfig {
    abstract val ignoreFailures: Property<Boolean>
    abstract val rulesetFile: RegularFileProperty

    init {
        ignoreFailures.convention(false)
    }
}
