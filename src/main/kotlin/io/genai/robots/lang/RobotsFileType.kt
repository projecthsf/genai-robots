package io.genai.robots.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Recognises the file named `robots.txt` as the RobotsTxt language. */
object RobotsFileType : LanguageFileType(RobotsLanguage) {
    val ICON: Icon = IconLoader.getIcon("/icons/robotstxt.svg", RobotsFileType::class.java)

    override fun getName(): String = "robots.txt"
    override fun getDescription(): String = "Robots exclusion file (robots.txt)"
    // "txt" so JetBrains Marketplace's plugin-advertiser indexes this file type and offers the plugin
    // when a robots.txt is opened. An empty value leaves the plugin out of the "install a plugin"
    // suggestion list entirely (verified against j-plugins/robots-txt-plugin, which uses "txt").
    override fun getDefaultExtension(): String = "txt"
    override fun getIcon(): Icon = ICON
}
