package io.genai.robots.lang.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import io.genai.robots.lang.RobotsLanguage

/** A lexer token type in the RobotsTxt language. */
class RobotsTokenType(debugName: String) : IElementType(debugName, RobotsLanguage) {
    override fun toString(): String = "RobotsTxt:" + super.toString()
}

/** All token types produced by [io.genai.robots.lang.RobotsLexer]. */
object RobotsTokens {
    val COMMENT = RobotsTokenType("COMMENT")       // # ...
    val DIRECTIVE = RobotsTokenType("DIRECTIVE")   // User-agent, Disallow, Allow, Sitemap, …
    val KEY = RobotsTokenType("KEY")               // an unknown field name
    val COLON = RobotsTokenType("COLON")           // :
    val VALUE = RobotsTokenType("VALUE")           // path / user-agent token / number
    val URL = RobotsTokenType("URL")               // an http(s) value (e.g. a Sitemap)
    val BAD = RobotsTokenType("BAD")

    /** Field names that colour as directives (lowercased). */
    val DIRECTIVES: Set<String> = setOf(
        "user-agent", "useragent", "allow", "disallow", "sitemap", "crawl-delay",
        "host", "clean-param", "request-rate", "visit-time", "noindex", "content-signal"
    )
}

val ROBOTS_FILE = IFileElementType(RobotsLanguage)
