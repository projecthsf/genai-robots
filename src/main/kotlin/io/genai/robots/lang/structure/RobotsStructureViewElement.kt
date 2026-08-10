package io.genai.robots.lang.structure

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import io.genai.robots.lang.RobotsFile
import io.genai.robots.lang.RobotsFileType
import io.genai.robots.model.AiCrawlers
import io.genai.robots.model.RobotsParser
import javax.swing.Icon

/**
 * A Structure-view node backed by the file's parsed model: the file → one node per User-agent group
 * → one node per Allow/Disallow rule. Navigation jumps to the source line. Rebuilt as you edit.
 */
class RobotsStructureViewElement private constructor(
    private val file: RobotsFile,
    private val label: String,
    private val icon: Icon,
    private val line: Int,                         // -1 for the file root
    private val kids: List<RobotsStructureViewElement>,
) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = offset()?.let { file.findElementAt(it) } ?: file
    override fun getAlphaSortKey(): String = label
    override fun getPresentation(): ItemPresentation = PresentationData(label, null, icon, null)
    override fun getChildren(): Array<TreeElement> = kids.toTypedArray()

    override fun navigate(requestFocus: Boolean) {
        val vf = file.virtualFile ?: return
        OpenFileDescriptor(file.project, vf, offset() ?: 0).navigate(requestFocus)
    }
    override fun canNavigate(): Boolean = file.virtualFile != null
    override fun canNavigateToSource(): Boolean = canNavigate()

    private fun offset(): Int? {
        if (line < 0) return null
        val doc = file.viewProvider.document ?: return null
        if (line >= doc.lineCount) return null
        return doc.getLineStartOffset(line)
    }

    companion object {
        fun forFile(file: RobotsFile): RobotsStructureViewElement {
            val model = RobotsParser.parse(file.text)
            val groups = model.groups.map { g ->
                val agents = if (g.agents.isEmpty()) "(no user-agent)" else g.agents.joinToString("  ")
                val ops = g.agents.mapNotNull { if (it == "*") null else AiCrawlers.operatorFor(it) }.distinct()
                val header = if (ops.isEmpty()) agents else "$agents  —  ${ops.joinToString(", ")}"
                val rules = g.rules.map { r ->
                    val text = "${if (r.allow) "Allow" else "Disallow"}: ${r.path.ifEmpty { "(all)" }}"
                    val icon = if (r.allow) AllIcons.Actions.Checked else AllIcons.Actions.Cancel
                    RobotsStructureViewElement(file, text, icon, r.line, emptyList())
                }
                RobotsStructureViewElement(file, header, AllIcons.Nodes.Package, g.startLine, rules)
            }
            val sitemaps = model.sitemaps.map { s ->
                RobotsStructureViewElement(file, "Sitemap: ${s.url}", AllIcons.General.Web, s.line, emptyList())
            }
            return RobotsStructureViewElement(file, file.name, RobotsFileType.ICON, -1, groups + sitemaps)
        }
    }
}
