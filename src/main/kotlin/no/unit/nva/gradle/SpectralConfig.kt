package no.unit.nva.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty

abstract class SpectralConfig {
    abstract val documents: ListProperty<String>
    abstract val rulesetFile: RegularFileProperty
}
