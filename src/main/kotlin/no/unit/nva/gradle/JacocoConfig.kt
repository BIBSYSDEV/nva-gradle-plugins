package no.unit.nva.gradle

import org.gradle.api.provider.Property
import java.math.BigDecimal

abstract class JacocoConfig {
    abstract val minMethodCoverage: Property<BigDecimal>
    abstract val minClassCoverage: Property<BigDecimal>

    init {
        minMethodCoverage.convention(BigDecimal("1.000"))
        minClassCoverage.convention(BigDecimal("1.000"))
    }
}
