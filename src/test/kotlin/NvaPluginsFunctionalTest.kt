import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NvaPluginsFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private val settingsFile get() = File(projectDir, "settings.gradle.kts")
    private val gradleProperties get() = File(projectDir, "gradle.properties")

    @BeforeEach
    fun setup() {
        settingsFile.writeText("""rootProject.name = "test-project"""")
        gradleProperties.writeText("org.gradle.configuration-cache=false\n")
    }

    private fun runner(vararg args: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")
            .forwardOutput()

    private fun kotlinBuildFile(content: String) {
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    private fun writeHelloJavaSource() {
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

    private fun javaConventionsBuildFile(extraConfig: String = "") {
        kotlinBuildFile(
            """
            plugins {
                id("nva.java-conventions")
            }

            repositories {
                mavenCentral()
            }

            nva {
                spotlessEnabled.set(false)
                spotlessEnforced.set(false)
            }

            dependencies {
                errorprone("com.google.errorprone:error_prone_core:2.36.0")
            }
            $extraConfig
            """.trimIndent(),
        )
    }

    @Test
    fun configurationPluginCreatesNvaExtension() {
        kotlinBuildFile(
            $$"""
            plugins {
                id("nva.configuration")
            }

            tasks.register("verifyExtension") {
                doLast {
                    val nva = project.extensions.getByName("nva")
                    println("Extension found: ${nva.javaClass.simpleName}")
                }
            }
            """.trimIndent(),
        )

        val result = runner("verifyExtension").build()

        assertTrue(result.output.contains("Extension found:"))
    }

    @Test
    fun javaConventionsPluginAppliesJavaLibrary() {
        javaConventionsBuildFile()
        writeHelloJavaSource()

        val result = runner("compileJava").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")?.outcome)
    }

    @Test
    fun javaConventionsPluginConfiguresJunit5() {
        javaConventionsBuildFile(
            """
            nva {
                pmdIgnoreFailures.set(true)
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            """.trimIndent(),
        )
        writeHelloJavaSource()

        val testDir = File(projectDir, "src/test/java/com/example")
        testDir.mkdirs()
        File(testDir, "HelloTest.java").writeText(
            """
            package com.example;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            class HelloTest {
                @Test
                void shouldGreet() {
                    assertEquals("hello", new Hello().greet());
                }
            }
            """.trimIndent(),
        )

        val result = runner("test").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":test")?.outcome)
    }

    @Test
    fun formattingConventionsPluginAppliesSpotless() {
        kotlinBuildFile(
            """
            plugins {
                id("nva.formatting-conventions")
            }

            nva {
                spotlessEnabled.set(false)
                spotlessEnforced.set(false)
            }
            """.trimIndent(),
        )

        val result = runner("tasks", "--group=verification").build()

        assertTrue(result.output.contains("spotless"))
    }

    @Test
    fun rootModuleConventionsPluginRegistersVerifyCoverageTask() {
        check(settingsFile.delete()) { "Failed to delete settings file" }
        File(projectDir, "settings.gradle").writeText(
            """
            rootProject.name = 'test-root'
            include 'sub'
            """.trimIndent(),
        )

        File(projectDir, "build.gradle").writeText(
            """
            plugins {
                id 'nva.root-module-conventions'
            }

            nva {
                spotlessEnabled = false
                spotlessEnforced = false
            }
            """.trimIndent(),
        )

        val subDir = File(projectDir, "sub")
        subDir.mkdirs()
        File(subDir, "build.gradle").writeText(
            """
            plugins {
                id 'nva.java-conventions'
            }

            nva {
                spotlessEnabled = false
                spotlessEnforced = false
            }
            """.trimIndent(),
        )

        val result = runner("tasks", "--group=test coverage").build()

        assertTrue(result.output.contains("verifyCoverage"))
        assertTrue(result.output.contains("showCoverageReport"))
    }

    @Test
    fun nvaExtensionDefaultsAreApplied() {
        kotlinBuildFile(
            $$"""
            plugins {
                id("nva.configuration")
            }

            tasks.register("printDefaults") {
                doLast {
                    val nva = project.extensions.getByType(no.unit.nva.gradle.NvaConventionsExtension::class.java)
                    println("pmdVersion=${nva.pmdVersion.get()}")
                    println("jacocoVersion=${nva.jacocoVersion.get()}")
                    println("spotlessEnabled=${nva.spotlessEnabled.get()}")
                    println("spotlessEnforced=${nva.spotlessEnforced.get()}")
                    println("pmdIgnoreFailures=${nva.pmdIgnoreFailures.get()}")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printDefaults").build()

        assertTrue(result.output.contains("pmdVersion=7.15.0"))
        assertTrue(result.output.contains("jacocoVersion=0.8.13"))
        assertTrue(result.output.contains("spotlessEnabled=true"))
        assertTrue(result.output.contains("spotlessEnforced=true"))
        assertTrue(result.output.contains("pmdIgnoreFailures=false"))
    }
}
