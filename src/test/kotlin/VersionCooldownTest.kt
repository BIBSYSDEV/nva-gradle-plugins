import no.unit.nva.gradle.VersionCooldown
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionCooldownTest {
    @Test
    fun recentPublicationDateIsWithinCooldown() {
        assertTrue(VersionCooldown.isWithinCooldown(Instant.now()))
    }

    @Test
    fun oldPublicationDateIsNotWithinCooldown() {
        val eightDaysAgo = Instant.now().minus(Duration.ofDays(8))

        assertFalse(VersionCooldown.isWithinCooldown(eightDaysAgo))
    }

    @Test
    fun knownOldMavenCentralArtifactIsNotWithinCooldown() {
        assertFalse(VersionCooldown.isWithinCooldown("junit", "junit", "4.13.2"))
    }

    @Test
    fun unknownArtifactFailsOpenAndIsNotWithinCooldown() {
        assertFalse(VersionCooldown.isWithinCooldown("no.example.nonexistent", "no-such-module", "0.0.0"))
    }
}
