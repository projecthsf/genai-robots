package io.genai.robots.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.genai.robots.model.RobotsParser
import io.genai.robots.client.SitemapClient
import io.genai.robots.model.SitemapReport
import io.genai.robots.model.SitemapValidator
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/** Tool-window content for one robots.txt: validates its sitemaps with Stop / Rerun controls. */
class SitemapValidationPanel(private val project: Project, private val file: VirtualFile) : JPanel(BorderLayout()) {

    private val runButton = JButton("Validate", AllIcons.Actions.Execute)
    private val stopButton = JButton("Stop", AllIcons.Actions.Suspend)
    private val status = JBLabel(" ")
    private val center = JPanel(BorderLayout())

    @Volatile private var running = false
    private var cancel = AtomicBoolean(false)

    init {
        val bar = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4)))
        bar.add(JBLabel(file.name).apply { font = font.deriveFont(java.awt.Font.BOLD) })
        bar.add(JBLabel("  "))
        bar.add(runButton)
        bar.add(stopButton)
        bar.add(status)
        add(bar, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)
        runButton.addActionListener { start() }
        stopButton.addActionListener { stop() }
        updateButtons()
        start()   // auto-run when the panel opens
    }

    private fun start() {
        if (running) return
        running = true
        cancel = AtomicBoolean(false)
        val myCancel = cancel
        status.text = "Validating…"
        setCenter(message("Fetching sitemaps…"))
        updateButtons()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                val text = ReadAction.compute<String, RuntimeException> {
                    FileDocumentManager.getInstance().getDocument(file)?.text ?: VfsUtilCore.loadText(file)
                }
                val model = RobotsParser.parse(text)
                val urls = model.sitemaps.map { it.url }.filter { it.startsWith("http") }
                if (urls.isEmpty()) null
                else {
                    val fetched = SitemapClient.fetchAll(urls) { myCancel.get() }
                    if (myCancel.get()) null else SitemapValidator.validate(model, fetched)
                }
            }
            ApplicationManager.getApplication().invokeLater({
                running = false
                runButton.text = "Rerun"
                runButton.icon = AllIcons.Actions.Refresh
                updateButtons()
                when {
                    myCancel.get() -> { status.text = "Stopped"; setCenter(message("Stopped — click Rerun to try again.")) }
                    result.isFailure -> { status.text = "Error"; setCenter(message("Validation failed: ${result.exceptionOrNull()?.message}")) }
                    result.getOrNull() == null -> { status.text = "No sitemaps"; setCenter(message("This robots.txt has no Sitemap: lines to validate.")) }
                    else -> { val r = result.getOrThrow() as SitemapReport; status.text = summary(r); setCenter(SitemapResultView.build(r)) }
                }
            }, ModalityState.any())
        }
    }

    private fun stop() {
        if (!running) return
        cancel.set(true)
        status.text = "Stopping…"
        stopButton.isEnabled = false
    }

    private fun updateButtons() {
        runButton.isEnabled = !running
        stopButton.isEnabled = running
    }

    private fun summary(r: SitemapReport) =
        "${r.fetchedOk}/${r.sitemaps.size} sitemaps · ${r.totalUrls} URLs · ${r.bySeverity(io.genai.robots.model.Severity.ERROR).size} error(s)"

    private fun setCenter(c: JComponent) {
        center.removeAll(); center.add(c, BorderLayout.CENTER); center.revalidate(); center.repaint()
    }

    private fun message(text: String): JComponent =
        JPanel(BorderLayout()).apply { add(JBLabel(text).apply { border = JBUI.Borders.empty(16) }, BorderLayout.NORTH) }
}
