package io.genai.robots.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import io.genai.robots.lang.RobotsFile
import io.genai.robots.model.AiCrawlers

/** Insert `block` on its own line at the caret and move the caret past it. */
private fun insertAtCaret(editor: Editor, project: Project, block: String) {
    val doc = editor.document
    val offset = editor.caretModel.offset
    val text = (if (offset > 0 && doc.charsSequence[offset - 1] != '\n') "\n" else "") + block
    WriteCommandAction.runWriteCommandAction(project) {
        doc.insertString(offset, text)
        editor.caretModel.moveToOffset(offset + text.length)
    }
}

/** Visible only while editing a robots.txt. */
private fun robotsEditor(e: AnActionEvent): Editor? {
    if (e.getData(CommonDataKeys.PSI_FILE) !is RobotsFile) return null
    return e.getData(CommonDataKeys.EDITOR)
}

/** "New User-agent…": suggest a crawler name, then insert a `User-agent:` + `Disallow: /` block. */
class NewUserAgentAction : AnAction() {

    private val suggestions =
        listOf("*") + AiCrawlers.LIST.map { it.token } + listOf("Googlebot", "Bingbot") + listOf("Other…")

    override fun actionPerformed(e: AnActionEvent) {
        val editor = robotsEditor(e) ?: return
        val project = e.project ?: return
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(suggestions)
            .setTitle("New User-agent")
            .setItemChosenCallback { chosen ->
                val name = if (chosen == "Other…")
                    Messages.showInputDialog(project, "User-agent name:", "New User-agent", null)?.trim().orEmpty()
                else chosen
                if (name.isNotEmpty()) insertAtCaret(editor, project, "User-agent: $name\nDisallow: /\n")
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    override fun update(e: AnActionEvent) { e.presentation.isEnabledAndVisible = robotsEditor(e) != null }
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

/** "New Sitemap…": prompt for a URL and insert a `Sitemap:` line. */
class NewSitemapAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = robotsEditor(e) ?: return
        val project = e.project ?: return
        val url = Messages.showInputDialog(
            project, "Sitemap URL:", "New Sitemap", null, "https://example.com/sitemap.xml", null
        )?.trim().orEmpty()
        if (url.isNotEmpty()) insertAtCaret(editor, project, "Sitemap: $url\n")
    }

    override fun update(e: AnActionEvent) { e.presentation.isEnabledAndVisible = robotsEditor(e) != null }
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
