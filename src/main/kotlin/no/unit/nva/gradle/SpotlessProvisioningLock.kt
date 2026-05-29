package no.unit.nva.gradle

import com.diffplug.gradle.spotless.SpotlessTask
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType

/**
 * Concurrency throttle for Spotless tasks. Spotless caches a single google-java-format classloader
 * shared across all subprojects; concurrent first-time provisioning of that classloader
 * intermittently fails with NoClassDefFoundError. Spotless also shares state in build/spotless-lints/
 * that is unsafe under intra-project parallelism (diffplug/spotless#2391). Both are avoided by
 * letting at most one Spotless task run at a time across the whole build.
 */
abstract class SpotlessProvisioningLock : BuildService<BuildServiceParameters.None>

/**
 * Wires every Spotless task in this project to a build-wide lock with maxParallelUsages = 1.
 * registerIfAbsent is keyed globally by name, so all projects share one permit and no two Spotless
 * tasks ever run concurrently.
 */
fun Project.serializeSpotlessTasks() {
    val lock =
        gradle.sharedServices.registerIfAbsent(
            "nvaSpotlessProvisioningLock",
            SpotlessProvisioningLock::class,
        ) {
            maxParallelUsages.set(1)
        }
    tasks.withType<SpotlessTask>().configureEach {
        usesService(lock)
    }
}
