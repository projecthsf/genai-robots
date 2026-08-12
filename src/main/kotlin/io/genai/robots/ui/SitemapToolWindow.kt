package io.genai.robots.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

/** Bottom tool window that hosts the latest sitemap-validation result. Populated on demand. */
class SitemapToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val placeholder = JBLabel(
            "Right-click a robots.txt and choose “Validate Sitemap(s)” to see results here."
        ).apply { border = JBUI.Borders.empty(16) }
        setContent(toolWindow, placeholder)
    }

    companion object {
        const val ID = "Sitemap Validation"

        /** Open the tool window on [file] and start validating (with Stop/Rerun controls). */
        fun show(project: Project, file: VirtualFile) {
            val tw = ToolWindowManager.getInstance(project).getToolWindow(ID) ?: return
            setContent(tw, SitemapValidationPanel(project, file))
            tw.setAvailable(true, null)
            tw.activate(null)
        }

        private fun setContent(tw: ToolWindow, component: JComponent) {
            val content = ContentFactory.getInstance().createContent(component, "", false)
            tw.contentManager.removeAllContents(true)
            tw.contentManager.addContent(content)
        }
    }
}
