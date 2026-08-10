package io.genai.robots.ui

import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.genai.robots.model.AiCrawlers
import io.genai.robots.model.Group
import io.genai.robots.model.GroupVerdict
import io.genai.robots.model.RobotsModel
import io.genai.robots.model.RobotsParser
import io.genai.robots.model.Sitemap
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.Border

/** One group's rendered block plus the source lines it came from, for caret ↔ preview highlight. */
class RobotsBlock(
    val range: IntRange,
    val panel: JPanel,
    private val normal: Border,
    private val highlighted: Border,
    private val bg: Color,
) {
    fun setHighlighted(on: Boolean) {
        panel.isOpaque = on
        panel.background = if (on) bg else null
        panel.border = if (on) highlighted else normal
        panel.revalidate(); panel.repaint()
    }
}

/** Result of rendering: the scrollable component + a line→block index used for caret sync. */
class RobotsRendered(val component: JComponent, private val blocks: List<RobotsBlock>) {
    private var current: RobotsBlock? = null

    /** Highlight the group whose source lines contain (or most recently precede) [line]; scroll to it. */
    fun highlightForLine(line: Int) {
        val target = blocks.lastOrNull { it.range.first in 0..line }
        if (target !== current) {
            current?.setHighlighted(false)
            target?.setHighlighted(true)
            current = target
        }
        target?.let { it.panel.scrollRectToVisible(Rectangle(0, 0, it.panel.width, it.panel.height)) }
    }
}

/** Renders a parsed robots.txt as a visual mirror of its User-agent groups and rules. */
object RobotsView {

    private val RED = JBColor(0xC0392B, 0xE06C6C)
    private val GREEN = JBColor(0x2E7D32, 0x6FBF73)
    private val ACCENT = JBColor(0x4C8DF6, 0x5C9DFF)
    private val HL_BG: Color = JBColor(0xEAF1FF, 0x2B3A55)
    private val NORMAL_BORDER: Border = JBUI.Borders.compound(
        JBUI.Borders.customLine(JBColor.border(), 0, 2, 0, 0), JBUI.Borders.empty(2, 10, 2, 0))!!
    private val HL_BORDER: Border = JBUI.Borders.compound(
        JBUI.Borders.customLine(ACCENT, 0, 3, 0, 0), JBUI.Borders.empty(2, 9, 2, 0))!!

    private fun muted() = UIUtil.getContextHelpForeground()
    private fun mono(size: Int = 13) = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(size))

    fun build(model: RobotsModel, @Suppress("UNUSED_PARAMETER") domain: String?): RobotsRendered {
        val root = JPanel(GridBagLayout())
        root.isOpaque = true
        root.background = UIUtil.getPanelBackground()

        val blocks = mutableListOf<RobotsBlock>()
        val section = vbox()
        if (model.groups.isEmpty() && model.sitemaps.isEmpty()) {
            section.add(leftAlign(JBLabel("This robots.txt has no rules — every crawler is allowed everywhere.")
                .apply { foreground = muted() }))
        } else {
            for (g in model.groups) {
                val panel = groupBlock(g)
                val end = if (g.endLine >= g.startLine) g.endLine else g.startLine
                blocks.add(RobotsBlock(g.startLine..end, panel, NORMAL_BORDER, HL_BORDER, HL_BG))
                section.add(leftAlign(panel))
                section.add(strut(12))
            }
            if (model.sitemaps.isNotEmpty()) {
                section.add(leftAlign(sectionTitle("SITEMAPS")))
                section.add(strut(6))
                for (s in model.sitemaps) {
                    val panel = sitemapBlock(s)
                    blocks.add(RobotsBlock(s.line..s.line, panel, NORMAL_BORDER, HL_BORDER, HL_BG))
                    section.add(leftAlign(panel))
                    section.add(strut(8))
                }
            }
        }
        blocks.sortBy { it.range.first }   // keep ascending so caret sync picks the nearest block

        root.add(section, GridBagConstraints().apply {
            gridx = 0; gridy = 0; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL; anchor = GridBagConstraints.NORTHWEST
        })
        root.add(JPanel().apply { isOpaque = false }, GridBagConstraints().apply {
            gridx = 0; gridy = 1; weightx = 1.0; weighty = 1.0; fill = GridBagConstraints.BOTH
        })
        root.border = JBUI.Borders.empty(14, 20)

        val scroll = JBScrollPane(root).apply {
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = 16
        }
        return RobotsRendered(scroll, blocks)
    }

    private fun groupBlock(g: Group): JPanel {
        val agents = if (g.agents.isEmpty()) listOf("(no user-agent)") else g.agents
        val agentLabel = JBLabel(agents.joinToString("   ")).apply { font = mono(13).deriveFont(Font.BOLD) }
        val ops = g.agents.mapNotNull { if (it == "*") "all other crawlers" else AiCrawlers.operatorFor(it) }.distinct()
        val block = vbox(agentLabel)
        if (ops.isNotEmpty()) block.add(leftAlign(JBLabel(ops.joinToString(" · "))
            .apply { foreground = muted(); font = font.deriveFont(JBUI.scale(11f)) }))
        block.add(strut(4))

        when (RobotsParser.verdict(g)) {
            GroupVerdict.ALLOW_ALL -> block.add(leftAlign(verdictLine("Allows everything", GREEN, "Allow /")))
            GroupVerdict.BLOCK_ALL -> block.add(leftAlign(verdictLine("Blocks everything", RED, "Disallow /")))
            GroupVerdict.SPECIFIC -> {
                block.add(leftAlign(JBLabel("Specific rules").apply { font = font.deriveFont(Font.BOLD) }))
                for (r in g.rules) {
                    val kw = if (r.allow) "Allow" else "Disallow"
                    val color = if (r.allow) GREEN else RED
                    val line = JPanel().apply { layout = BoxLayout(this, BoxLayout.X_AXIS); isOpaque = false }
                    line.add(JBLabel(kw).apply { foreground = color; font = mono(12) })
                    line.add(JBLabel("  " + (r.path.ifEmpty { "(empty = allow all)" })).apply { font = mono(12) })
                    block.add(leftAlign(line))
                }
            }
        }
        g.crawlDelay?.let {
            block.add(leftAlign(JBLabel("Crawl-delay: $it").apply { foreground = muted(); font = mono(12) }))
        }
        block.border = NORMAL_BORDER
        return block
    }

    private fun sitemapBlock(s: Sitemap): JPanel {
        val link = HyperlinkLabel(s.url).apply { setHyperlinkTarget(s.url) }
        val block = vbox(link, leftAlign(JBLabel("sitemap").apply { foreground = muted(); font = font.deriveFont(JBUI.scale(11f)) }))
        block.border = NORMAL_BORDER
        return block
    }

    private fun sectionTitle(text: String) = JBLabel(text).apply {
        font = font.deriveFont(java.awt.Font.BOLD, JBUI.scale(11f)); foreground = muted()
    }

    private fun verdictLine(text: String, color: JBColor, code: String): JComponent {
        val p = JPanel().apply { layout = BoxLayout(this, BoxLayout.X_AXIS); isOpaque = false }
        p.add(JBLabel(text).apply { foreground = color; font = font.deriveFont(Font.BOLD) })
        p.add(JBLabel("   $code").apply { foreground = muted(); font = mono(12) })
        return p
    }

    // ---------- layout helpers ----------

    private fun strut(h: Int) = Box.createVerticalStrut(JBUI.scale(h)) as JComponent

    private fun vbox(vararg comps: Component): JPanel {
        val p = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false }
        comps.forEach { p.add(leftAlign(it)) }
        return p
    }

    private fun <T : Component> leftAlign(c: T): T {
        (c as? JComponent)?.alignmentX = Component.LEFT_ALIGNMENT
        return c
    }
}
