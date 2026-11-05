import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Shared setup for tests that apply the plugins to a throwaway Gradle project. */
open class GradleFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    protected val settingsFile get() = File(projectDir, "settings.gradle.kts")
    protected val gradleProperties get() = File(projectDir, "gradle.properties")

    @BeforeEach
    fun setup() {
        settingsFile.writeText("""rootProject.name = "test-project"""")
        gradleProperties.writeText("org.gradle.configuration-cache=false\n")
    }

    protected fun runner(vararg args: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")
            .forwardOutput()

    protected fun kotlinBuildFile(content: String) {
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    protected fun writeProjectFile(
        relativePath: String,
        content: String,
    ) {
        val file = File(projectDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    protected fun writeHelloJavaSource() {
        val srcDir = File(projectDir, "src/main/java/com/example")
        srcDir.mkdirs()
        File(srcDir, "Hello.java").writeText(
            """
            package com.example;

            public class Hello {
                public String greet() {
                    return "hello";
                }
            }
            """.trimIndent(),
        )
    }

    protected fun javaConventionsBuildFile(extraConfig: String = "") {
        kotlinBuildFile(
            """
            plugins {
                id("nva.java-conventions")
            }

            repositories {
                mavenCentral()
            }

            nva {
                spotless {
                    enabled.set(false)
                }
            }
            $extraConfig
            """.trimIndent(),
        )
    }
}
