package io.genai.robots.action

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import io.genai.robots.lang.RobotsFileType

/** "New ▸ robots.txt": opens the guided builder, then writes robots.txt into the chosen folder. */
class CreateRobotsFileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val view = e.getData(LangDataKeys.IDE_VIEW) ?: return
        val dir = view.orChooseDirectory ?: return

        dir.findFile("robots.txt")?.let { existing ->
            Messages.showInfoMessage(project, "This folder already has a robots.txt — opening it.", "robots.txt Exists")
            existing.virtualFile?.let { FileEditorManager.getInstance(project).openFile(it, true) }
            return
        }

        val dialog = RobotsNewFileDialog(project)
        if (!dialog.showAndGet()) return
        val content = dialog.content()

        WriteCommandAction.runWriteCommandAction(project) {
            val psi = PsiFileFactory.getInstance(project).createFileFromText("robots.txt", RobotsFileType, content)
            val created = dir.add(psi) as? PsiFile ?: return@runWriteCommandAction
            created.virtualFile?.let { FileEditorManager.getInstance(project).openFile(it, true) }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(LangDataKeys.IDE_VIEW) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
