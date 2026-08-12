package io.genai.robots.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import io.genai.robots.lang.RobotsFile
import io.genai.robots.ui.SitemapToolWindowFactory

/**
 * "Validate Sitemap(s)": fetch every Sitemap: URL in this robots.txt and check the pages. Available
 * from the editor (right-click / Tools) and from the Project view (right-click a robots.txt file).
 */
class ValidateSitemapsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = robotsFile(e) ?: return
        SitemapToolWindowFactory.show(project, file)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && robotsFile(e) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** The robots.txt from the editor (open file) or the Project-view selection. */
    private fun robotsFile(e: AnActionEvent): VirtualFile? {
        (e.getData(CommonDataKeys.PSI_FILE) as? RobotsFile)?.virtualFile?.let { return it }
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        return if (!vf.isDirectory && vf.name.endsWith("robots.txt", ignoreCase = true)) vf else null
    }
}
