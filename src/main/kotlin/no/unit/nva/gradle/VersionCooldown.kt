package no.unit.nva.gradle

import java.net.HttpURLConnection.HTTP_OK
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Hides versions published less than [COOLDOWN_DAYS] ago from the dependencyUpdates report,
 * so fresh releases get time to be yanked or hotfixed before anyone upgrades to them.
 *
 * Publication dates are read from the Last-Modified header of the version's POM. Lookups
 * fail open: a version whose date cannot be determined (artifact hosted elsewhere, network
 * trouble) counts as old enough and stays in the report.
 */
object VersionCooldown {
    private const val COOLDOWN_DAYS = 7L
    private const val REQUEST_TIMEOUT_SECONDS = 5L

    private val repositoryBaseUrls =
        listOf(
            "https://repo1.maven.org/maven2",
            "https://plugins.gradle.org/m2",
        )
    private val publicationDates = ConcurrentHashMap<String, Instant>()
    private val httpClient by lazy {
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    fun isWithinCooldown(
        group: String,
        module: String,
        version: String,
    ): Boolean {
        val publishedAt =
            publicationDates.computeIfAbsent("$group:$module:$version") {
                fetchPublicationDate(group, module, version)
            }
        return isWithinCooldown(publishedAt)
    }

    internal fun isWithinCooldown(publishedAt: Instant): Boolean {
        val cooldownThreshold = Instant.now().minus(Duration.ofDays(COOLDOWN_DAYS))
        return publishedAt.isAfter(cooldownThreshold)
    }

    private fun fetchPublicationDate(
        group: String,
        module: String,
        version: String,
    ): Instant {
        val pomPath = "${group.replace('.', '/')}/$module/$version/$module-$version.pom"
        return repositoryBaseUrls.firstNotNullOfOrNull { baseUrl -> lastModified("$baseUrl/$pomPath") }
            ?: Instant.EPOCH
    }

    private fun lastModified(url: String): Instant? =
        runCatching {
            val request =
                HttpRequest
                    .newBuilder(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() == HTTP_OK) {
                response
                    .headers()
                    .firstValue("Last-Modified")
                    .map { Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(it)) }
                    .orElse(null)
            } else {
                null
            }
        }.getOrNull()
}
