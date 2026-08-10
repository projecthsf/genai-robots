package io.genai.robots.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** Opens any `robots.txt` as a split editor: raw text on the left, decoded RobotsReader view on the right. */
class RobotsEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        file.name.equals("robots.txt", ignoreCase = true)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val preview = RobotsPreviewFileEditor(project, file, textEditor.editor)
        // TextEditorWithPreview became a Kotlin class in 2024.2; every constructor call from code
        // compiled against a recent SDK binds to Kotlin's default-arg synthetic ctor, which does not
        // exist in the 233–241 Java-era class. Hence sinceBuild=242 (see build.gradle).
        return TextEditorWithPreview(
            textEditor, preview, "RobotsReader",
            TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW
        )
    }

    override fun getEditorTypeId(): String = "genai-robots-reader"

    // Replace the plain text editor; our combined editor already includes it on the left.
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
