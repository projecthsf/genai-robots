package io.genai.robots.lang

import com.intellij.lang.Language

/** The robots.txt (Robots Exclusion Protocol, RFC 9309) language. */
object RobotsLanguage : Language("GenAIRobotsTxt") {
    private fun readResolve(): Any = RobotsLanguage
    override fun getDisplayName(): String = "robots.txt"
}
