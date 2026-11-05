package no.unit.nva.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Writes the PMD ruleset for the Pmd tasks. Without exclusions it is the base ruleset as is. With
 * exclusions it references the base ruleset and excludes the given rules from it, so callers don't
 * need to know which PMD category each rule belongs to.
 */
@DisableCachingByDefault(
    because = "PMD resolves ruleset references against the working directory, so the output holds an absolute path",
)
abstract class WritePmdRuleset : DefaultTask() {
    @get:Input
    abstract val baseRulesetContent: Property<String>

    @get:Input
    abstract val excludedRules: SetProperty<String>

    @get:OutputFile
    abstract val baseRulesetFile: RegularFileProperty

    @get:OutputFile
    abstract val rulesetFile: RegularFileProperty

    @TaskAction
    fun write() {
        val baseRuleset = baseRulesetFile.get().asFile
        baseRuleset.writeText(baseRulesetContent.get())
        val excludedRuleNames = excludedRules.get()
        val ruleset =
            if (excludedRuleNames.isEmpty()) {
                baseRulesetContent.get()
            } else {
                rulesetExcluding(excludedRuleNames, baseRuleset)
            }
        rulesetFile.get().asFile.writeText(ruleset)
    }

    private fun rulesetExcluding(
        excludedRuleNames: Set<String>,
        baseRuleset: File,
    ): String {
        val excludeElements =
            excludedRuleNames.sorted().joinToString("\n") { ruleName ->
                """|    <exclude name="${ruleName.escapeXmlAttribute()}"/>"""
            }
        return """
            |<?xml version="1.0"?>
            |<ruleset name="NVA rules with project exclusions"
            |  xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
            |  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            |  xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.net/ruleset_2_0_0.xsd">
            |  <description>The bundled NVA ruleset without the rules excluded by this project.</description>
            |  <rule ref="${baseRuleset.absolutePath.escapeXmlAttribute()}">
            $excludeElements
            |  </rule>
            |</ruleset>
            |
            """.trimMargin()
    }

    private fun String.escapeXmlAttribute(): String =
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
