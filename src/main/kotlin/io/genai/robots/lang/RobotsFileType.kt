package io.genai.robots.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Recognises the file named `robots.txt` as the RobotsTxt language. */
object RobotsFileType : LanguageFileType(RobotsLanguage) {
    val ICON: Icon = IconLoader.getIcon("/icons/robotstxt.svg", RobotsFileType::class.java)

    override fun getName(): String = "robots.txt"
    override fun getDescription(): String = "Robots exclusion file (robots.txt)"
    // Empty on purpose: this type is matched by the exact file name "robots.txt", not an extension.
    // A non-empty value (e.g. "txt") would make the Marketplace advertise this plugin for *.txt,
    // nagging on every plain-text file. Filename-based advertising still works (like Dockerfile).
    override fun getDefaultExtension(): String = ""
    override fun getIcon(): Icon = ICON
}
