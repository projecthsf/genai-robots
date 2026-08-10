package io.genai.robots.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile
import io.genai.robots.client.RobotsFetcher
import io.genai.robots.editor.RobotsPreviewFileEditor
import io.genai.robots.lang.RobotsFileType

/** Tools ▸ "Open robots.txt from Website…": fetch a live site's robots.txt and open it decoded. */
class FetchRobotsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val input = Messages.showInputDialog(
            project, "Domain (e.g. example.com):", "Open robots.txt from Website", Messages.getQuestionIcon()
        )?.takeIf { it.isNotBlank() } ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { RobotsFetcher.fetch(input) }
            ApplicationManager.getApplication().invokeLater {
                result.onSuccess { fetched ->
                    val vf = LightVirtualFile("robots.txt", RobotsFileType, fetched.body)
                    vf.putUserData(RobotsPreviewFileEditor.DOMAIN_KEY, fetched.host)
                    FileEditorManager.getInstance(project).openFile(vf, true)
                }.onFailure { ex ->
                    Messages.showErrorDialog(project, ex.message ?: "Could not fetch robots.txt", "Fetch Failed")
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
