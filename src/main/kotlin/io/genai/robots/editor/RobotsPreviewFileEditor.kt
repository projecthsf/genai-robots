package io.genai.robots.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import io.genai.robots.model.RobotsParser
import io.genai.robots.ui.RobotsRendered
import io.genai.robots.ui.RobotsView
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

/** The right-hand "preview" side of the robots.txt split editor: the decoded RobotsReader view. */
class RobotsPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile,
    private val editor: Editor?,
) : UserDataHolderBase(), FileEditor {

    companion object {
        /** Set on fetched in-memory files so the header can show the source domain. */
        val DOMAIN_KEY: Key<String> = Key.create("genai.robots.domain")
    }

    private val panel = JPanel(BorderLayout())
    private val document = FileDocumentManager.getInstance().getDocument(file)
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var rendered: RobotsRendered? = null

    init {
        document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: DocumentEvent) = scheduleRender()
        }, this)
        editor?.caretModel?.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = syncHighlight()
        }, this)
        render()
    }

    private fun scheduleRender() {
        alarm.cancelAllRequests()
        alarm.addRequest({ render() }, 250)
    }

    private fun render() {
        val text = document?.text ?: ""
        val model = RobotsParser.parse(text)
        val domain = file.getUserData(DOMAIN_KEY) ?: domainFromFile()
        val r = RobotsView.build(model, domain)
        rendered = r
        panel.removeAll()
        panel.add(r.component, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
        syncHighlight()
    }

    /** Highlight the block matching the raw editor's current caret line. */
    private fun syncHighlight() {
        val line = editor?.caretModel?.logicalPosition?.line ?: return
        rendered?.highlightForLine(line)
    }

    /** For an on-disk robots.txt, guess a domain from the parent folder name if it looks like one. */
    private fun domainFromFile(): String? {
        val parent = file.parent?.name ?: return null
        return if (parent.contains('.') && !parent.contains(' ')) parent else null
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = panel
    override fun getName(): String = "RobotsReader"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) {}
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {}
}
