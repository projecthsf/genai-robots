package io.genai.robots.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import io.genai.robots.model.AiCrawlers
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/** Guided builder for a new robots.txt: rows of User-agent rules and Sitemaps, each add/removable. */
class RobotsNewFileDialog(project: Project) : DialogWrapper(project) {

    private val agents = mutableListOf<AgentRow>()
    private val sitemaps = mutableListOf<SitemapRow>()
    private val agentsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val sitemapsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val agentOptions = (listOf("*") + AiCrawlers.LIST.map { it.token } +
        listOf("Googlebot", "Bingbot") + listOf("Other")).toTypedArray()

    init {
        title = "New robots.txt"
        addAgent(); addSitemap()
        init()
    }

    override fun createCenterPanel(): JComponent {
        val content = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); border = JBUI.Borders.empty(8) }
        content.add(sectionLabel("User-agent rules"))
        content.add(agentsPanel)
        content.add(addButton("Add agent") { addAgent() })
        content.add(strut(10))
        content.add(sectionLabel("Sitemaps"))
        content.add(sitemapsPanel)
        content.add(addButton("Add sitemap") { addSitemap() })

        return JPanel(java.awt.BorderLayout()).apply {
            add(JBScrollPane(content).apply {
                border = JBUI.Borders.empty()
                preferredSize = Dimension(JBUI.scale(560), JBUI.scale(420))
            }, java.awt.BorderLayout.CENTER)
        }
    }

    // ----- rows -----

    private fun addAgent() {
        val row = AgentRow()
        agents.add(row); agentsPanel.add(row.panel); refresh()
    }

    private fun addSitemap() {
        val row = SitemapRow()
        sitemaps.add(row); sitemapsPanel.add(row.panel); refresh()
    }

    private fun refresh() {
        agentsPanel.revalidate(); agentsPanel.repaint()
        sitemapsPanel.revalidate(); sitemapsPanel.repaint()
    }

    private inner class AgentRow {
        val agentCombo = ComboBox(agentOptions).apply { maximumSize = preferredSize }
        val customField = JBTextField(12).apply { isVisible = false }
        val ruleCombo = ComboBox(arrayOf("Allow", "Disallow")).apply { selectedItem = "Disallow"; maximumSize = preferredSize }
        val pathField = JBTextField("/", 14)
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(2)))

        init {
            agentCombo.addActionListener { customField.isVisible = agentCombo.selectedItem == "Other"; panel.revalidate() }
            panel.add(agentCombo)
            panel.add(customField)
            panel.add(ruleCombo)
            panel.add(JBLabel("path"))
            panel.add(pathField)
            panel.add(removeButton { agents.remove(this); agentsPanel.remove(panel); refresh() })
        }

        fun name(): String = (if (agentCombo.selectedItem == "Other") customField.text.trim() else agentCombo.selectedItem as String)
        fun ruleKeyword(): String = ruleCombo.selectedItem as String
        fun path(): String = pathField.text.trim().ifEmpty { "/" }
        fun isOtherBlank(): Boolean = agentCombo.selectedItem == "Other" && customField.text.isBlank()
    }

    private inner class SitemapRow {
        val urlField = JBTextField("/sitemap.xml", 32)
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(2)))

        init {
            panel.add(JBLabel("Sitemap"))
            panel.add(urlField)
            panel.add(removeButton { sitemaps.remove(this); sitemapsPanel.remove(panel); refresh() })
        }

        fun value(): String = urlField.text.trim()
    }

    // ----- result -----

    fun content(): String = buildString {
        agents.forEach { r ->
            append("User-agent: ").append(r.name().ifBlank { "*" }).append('\n')
            append(r.ruleKeyword()).append(": ").append(r.path()).append("\n\n")
        }
        sitemaps.mapNotNull { it.value().ifEmpty { null } }.forEach { append("Sitemap: ").append(it).append('\n') }
    }.trim() + "\n"

    override fun doValidate(): ValidationInfo? {
        if (agents.isEmpty() && sitemaps.all { it.value().isBlank() })
            return ValidationInfo("Add at least one user-agent rule or sitemap.")
        agents.firstOrNull { it.isOtherBlank() }?.let { return ValidationInfo("Enter a custom user-agent name.", it.customField) }
        return null
    }

    // ----- small ui helpers -----

    private fun sectionLabel(text: String) = leftAlign(JBLabel(text).apply {
        font = font.deriveFont(Font.BOLD); border = JBUI.Borders.empty(2, 2, 4, 0)
    })

    private fun addButton(text: String, action: () -> Unit) = leftAlign(
        JButton(text, AllIcons.General.Add).apply { addActionListener { action() } })

    private fun removeButton(action: () -> Unit) = JButton(AllIcons.General.Remove).apply {
        toolTipText = "Remove"; addActionListener { action() }
    }

    private fun strut(h: Int) = leftAlign(javax.swing.Box.createVerticalStrut(JBUI.scale(h)) as JComponent)

    private fun <T : Component> leftAlign(c: T): T { (c as? JComponent)?.alignmentX = Component.LEFT_ALIGNMENT; return c }
}
