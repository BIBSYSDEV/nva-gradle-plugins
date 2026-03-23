import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    private fun writeSubmoduleJavaSourceAndTest(
        sourceBody: String,
        testBody: String,
    ) {
        val srcDir = File(projectDir, "sub/src/main/java/com/example")
        srcDir.mkdirs()
        File(srcDir, "Covered.java").writeText(
            """
            package com.example;

            public class Covered {
                $sourceBody
            }
            """.trimIndent(),
        )

        val testDir = File(projectDir, "sub/src/test/java/com/example")
        testDir.mkdirs()
        File(testDir, "CoveredTest.java").writeText(
            """
            package com.example;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            class CoveredTest {
                @Test
                void testCovered() {
                    $testBody
                }
            }
            """.trimIndent(),
        )
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

    private fun rootWithSubmoduleBuildFiles(submoduleExtraConfig: String = "") {
        settingsFile.writeText(
            """
            rootProject.name = "test-root"
            include("sub")
            """.trimIndent(),
        )

        kotlinBuildFile(
            """
            plugins {
                id("nva.root-module-conventions")
            }

            nva {
                spotlessEnabled.set(false)
                spotlessEnforced.set(false)
            }
            """.trimIndent(),
        )

        val subDir = File(projectDir, "sub")
        subDir.mkdirs()
        File(subDir, "build.gradle.kts").writeText(
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
                pmdIgnoreFailures.set(true)
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            $submoduleExtraConfig
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
        rootWithSubmoduleBuildFiles()

        val result = runner("tasks", "--group=test coverage").build()

        assertTrue(result.output.contains("verifyCoverage"))
        assertTrue(result.output.contains("showCoverageReport"))
    }

    @Test
    fun javaConventionsPluginUsesCustomPmdRuleset() {
        // Minimal ruleset with NO rules — if the bundled ruleset is used instead,
        // PMD will find violations and fail (since pmdIgnoreFailures is false)
        val customRuleset = File(projectDir, "custom-pmd.xml")
        customRuleset.writeText(
            """
            <?xml version="1.0"?>
            <ruleset name="Empty"
                     xmlns="https://pmd.sourceforge.net/ruleset/2.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="https://pmd.sourceforge.net/ruleset/2.0.0
                     https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
                <description>Empty ruleset to verify custom ruleset is applied</description>
            </ruleset>
            """.trimIndent(),
        )

        // Use code that would trigger bundled PMD rules (missing Javadoc, short variable name, etc.)
        val srcDir = File(projectDir, "src/main/java/com/example")
        srcDir.mkdirs()
        File(srcDir, "Foo.java").writeText(
            """
            package com.example;

            public class Foo {
                public void x() {
                    int a = 1;
                    System.out.println(a);
                }
            }
            """.trimIndent(),
        )

        javaConventionsBuildFile(
            """
            nva {
                pmdRulesetFile.set(file("custom-pmd.xml"))
            }
            """.trimIndent(),
        )

        // Should pass with empty ruleset, would fail with bundled ruleset
        val result = runner("pmdMain").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pmdMain")?.outcome)
    }

    @Test
    fun errorProneDetectsDeadException() {
        javaConventionsBuildFile(
            """
            nva {
                pmdIgnoreFailures.set(true)
            }
            """.trimIndent(),
        )

        val srcDir = File(projectDir, "src/main/java/com/example")
        srcDir.mkdirs()
        File(srcDir, "DeadExceptionExample.java").writeText(
            """
            package com.example;

            public class DeadExceptionExample {
                public void bad() {
                    new RuntimeException("this is dead");
                }
            }
            """.trimIndent(),
        )

        val result = runner("compileJava").build()

        assertContains(result.output, "DeadException")
    }

    @Test
    fun verifyCoverageFailsWhenCoverageIsInsufficient() {
        rootWithSubmoduleBuildFiles()
        writeSubmoduleJavaSourceAndTest(
            sourceBody =
                """
                public String covered() { return "covered"; }
                public String uncovered() { return "uncovered"; }
                """.trimIndent(),
            testBody =
                """
                assertEquals("covered", new Covered().covered());
                """.trimIndent(),
        )

        val result = runner("verifyCoverage").buildAndFail()

        assertNotEquals(TaskOutcome.SUCCESS, result.task(":verifyCoverage")?.outcome)
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
                    println("spotlessEnabled=${nva.spotlessEnabled.get()}")
                    println("spotlessEnforced=${nva.spotlessEnforced.get()}")
                    println("pmdIgnoreFailures=${nva.pmdIgnoreFailures.get()}")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printDefaults").build()

        assertTrue(result.output.contains("spotlessEnabled=true"))
        assertTrue(result.output.contains("spotlessEnforced=true"))
        assertTrue(result.output.contains("pmdIgnoreFailures=false"))
    }
}
