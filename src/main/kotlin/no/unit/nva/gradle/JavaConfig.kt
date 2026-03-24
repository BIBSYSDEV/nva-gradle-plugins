package no.unit.nva.gradle

import org.gradle.api.provider.Property

abstract class JavaConfig {
    abstract val languageVersion: Property<Int>

    init {
        languageVersion.convention(NvaConventionsExtension.JAVA_VERSION.toInt())
    }
}
