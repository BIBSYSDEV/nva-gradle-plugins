import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PmdConventionsFunctionalTest : GradleFunctionalTest() {
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
                pmd { rulesetFile.set(file("custom-pmd.xml")) }
            }
            """.trimIndent(),
        )

        // Should pass with empty ruleset, would fail with bundled ruleset
        val result = runner("pmdMain").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pmdMain")?.outcome)
    }

    private fun writeClassViolatingSystemPrintln() {
        writeProjectFile(
            "src/main/java/com/example/Printer.java",
            """
            package com.example;

            public class Printer {
                public void print() {
                    System.out.println("hello");
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun pmdIgnoresRulesExcludedFromBundledRuleset() {
        writeClassViolatingSystemPrintln()
        javaConventionsBuildFile()
        runner("pmdMain").buildAndFail()
        assertContains(File(projectDir, "build/reports/pmd/main.xml").readText(), "rule=\"SystemPrintln\"")

        javaConventionsBuildFile(
            """
            nva {
                pmd { excludedRules.add("SystemPrintln") }
            }
            """.trimIndent(),
        )
        val resultWithExclusion = runner("pmdMain").build()

        assertEquals(TaskOutcome.SUCCESS, resultWithExclusion.task(":pmdMain")?.outcome)
    }

    @Test
    fun pmdExcludedRulesCannotBeCombinedWithCustomRuleset() {
        writeProjectFile("custom-pmd.xml", "<ruleset/>")
        javaConventionsBuildFile(
            """
            nva {
                pmd {
                    rulesetFile.set(file("custom-pmd.xml"))
                    excludedRules.add("SystemPrintln")
                }
            }
            """.trimIndent(),
        )

        val result = runner("pmdMain").buildAndFail()

        assertContains(result.output, "nva.pmd.excludedRules only applies to the bundled PMD ruleset")
    }

    @Test
    fun pmdSkipsTestSourcesWhenTestAnalysisIsDisabled() {
        writeProjectFile(
            "src/test/java/com/example/PrinterTest.java",
            """
            package com.example;

            public class PrinterTest {
                public void print() {
                    System.out.println("hello");
                }
            }
            """.trimIndent(),
        )
        javaConventionsBuildFile(
            """
            nva {
                pmd { analyzeTests.set(false) }
            }
            """.trimIndent(),
        )

        val result = runner("pmdTest").build()

        assertEquals(TaskOutcome.SKIPPED, result.task(":pmdTest")?.outcome)
    }

    private fun writeGeneratedClassWithPmdViolations() {
        val generatedDir = File(projectDir, "src/main/java/com/example/generated")
        generatedDir.mkdirs()
        File(generatedDir, "Generated.java").writeText(
            """
            package com.example.generated;

            public class Generated {
                public void x() {
                    int a = 1;
                    System.out.println(a);
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun pmdFailsWhenGeneratedCodeIsNotExcluded() {
        javaConventionsBuildFile()
        writeHelloJavaSource()
        writeGeneratedClassWithPmdViolations()

        val result = runner("pmdMain").buildAndFail()

        assertNotEquals(TaskOutcome.SUCCESS, result.task(":pmdMain")?.outcome)
    }

    @Test
    fun pmdSkipsCodeMatchingGeneratedCodePatterns() {
        javaConventionsBuildFile(
            """
            nva {
                generatedCode.set(listOf("com/example/generated/**"))
            }
            """.trimIndent(),
        )
        writeHelloJavaSource()
        writeGeneratedClassWithPmdViolations()

        val result = runner("pmdMain").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pmdMain")?.outcome)
    }
}
