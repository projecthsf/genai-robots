package io.genai.robots.ui

import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.genai.robots.model.CheckResult
import io.genai.robots.model.Severity
import io.genai.robots.model.SitemapIssue
import io.genai.robots.model.SitemapReport
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Renders a [SitemapReport]: a summary line, a "what was checked" checklist, then the findings. */
object SitemapResultView {

    private val RED = JBColor(0xC0392B, 0xE06C6C)
    private val AMBER = JBColor(0xB8860B, 0xD9A441)
    private val GREEN = JBColor(0x2E7D32, 0x6FBF73)
    private fun muted() = UIUtil.getContextHelpForeground()
    private const val MAX_ROWS = 400

    fun build(report: SitemapReport): JComponent {
        val content = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); border = JBUI.Borders.empty(12, 16) }

        content.add(left(JBLabel("<html><b>Validated ${report.fetchedOk}/${report.sitemaps.size} sitemap(s)</b> · ${report.totalUrls} URLs</html>")
            .apply { font = font.deriveFont(JBUI.scale(14f)) }))
        content.add(strut(12))

        content.add(left(sectionTitle("CHECKS")))
        content.add(strut(6))
        for (c in report.checks) content.add(left(checkRow(c)))

        if (report.issues.isNotEmpty()) {
            content.add(strut(16))
            content.add(left(sectionTitle("FINDINGS (${report.issues.size})")))
            content.add(strut(8))
            var shown = 0
            for (issue in report.issues) {
                if (shown >= MAX_ROWS) {
                    content.add(left(JBLabel("…and ${report.issues.size - shown} more").apply { foreground = muted() })); break
                }
                content.add(left(issueRow(issue)))
                content.add(strut(8))
                shown++
            }
        } else {
            content.add(strut(12))
            content.add(left(JBLabel("No problems found — every URL is crawlable, unique, https, on-host, and within limits.")
                .apply { foreground = GREEN }))
        }

        val wrap = JPanel(BorderLayout()).apply { add(content, BorderLayout.NORTH) }
        return JBScrollPane(wrap).apply { border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16 }
    }

    private fun checkRow(c: CheckResult): JComponent {
        val row = JPanel().apply { layout = BoxLayout(this, BoxLayout.X_AXIS); isOpaque = false; border = JBUI.Borders.empty(2, 0) }
        val glyph = if (c.ok) "✓" else when (c.severity) { Severity.ERROR -> "✕"; Severity.WARN -> "!"; Severity.INFO -> "·" }
        val color = if (c.ok) GREEN else when (c.severity) { Severity.ERROR -> RED; Severity.WARN -> AMBER; Severity.INFO -> muted() }
        row.add(JBLabel(glyph).apply { foreground = color; font = Font(Font.MONOSPACED, Font.BOLD, JBUI.scale(13)) })
        row.add(Box.createHorizontalStrut(JBUI.scale(10)))
        row.add(JBLabel(c.label))
        row.add(Box.createHorizontalStrut(JBUI.scale(8)))
        row.add(JBLabel(c.detail).apply { foreground = muted() })
        return row
    }

    private fun issueRow(issue: SitemapIssue): JComponent {
        val row = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false }
        val top = JPanel().apply { layout = BoxLayout(this, BoxLayout.X_AXIS); isOpaque = false }
        top.add(chip(issue.severity))
        top.add(Box.createHorizontalStrut(JBUI.scale(8)))
        top.add(JBLabel(issue.message))
        row.add(left(top))
        issue.url?.let {
            val link = HyperlinkLabel(it).apply { setHyperlinkTarget(it) }
            link.border = JBUI.Borders.emptyLeft(JBUI.scale(52))
            row.add(left(link))
        }
        issue.detail?.let {
            row.add(left(JBLabel(it).apply {
                foreground = muted(); font = font.deriveFont(JBUI.scale(11f)); border = JBUI.Borders.emptyLeft(JBUI.scale(52))
            }))
        }
        return row
    }

    private fun chip(sev: Severity): JComponent {
        val (text, color) = when (sev) {
            Severity.ERROR -> "ERROR" to RED
            Severity.WARN -> "WARN" to AMBER
            Severity.INFO -> "INFO" to muted()
        }
        return JBLabel(text).apply { foreground = color; font = Font(Font.MONOSPACED, Font.BOLD, JBUI.scale(11)) }
    }

    private fun sectionTitle(text: String) = JBLabel(text).apply {
        font = font.deriveFont(Font.BOLD, JBUI.scale(11f)); foreground = muted()
    }

    private fun strut(h: Int) = left(Box.createVerticalStrut(JBUI.scale(h)) as JComponent)
    private fun <T : Component> left(c: T): T { (c as? JComponent)?.alignmentX = Component.LEFT_ALIGNMENT; return c }
}
