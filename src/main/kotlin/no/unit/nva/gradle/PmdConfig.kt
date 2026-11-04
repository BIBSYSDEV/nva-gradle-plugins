package no.unit.nva.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class PmdConfig {
    abstract val ignoreFailures: Property<Boolean>
    abstract val rulesetFile: RegularFileProperty

    /** Rule names to exclude from the bundled ruleset, e.g. "LawOfDemeter". Not combinable with [rulesetFile]. */
    abstract val excludedRules: SetProperty<String>

    init {
        ignoreFailures.convention(false)
        excludedRules.convention(emptySet())
    }
}
