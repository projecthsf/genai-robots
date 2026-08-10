package io.genai.robots.lang

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.genai.robots.lang.psi.RobotsTokens
import io.genai.robots.model.AiCrawlers

/** Hover / Quick-Documentation for robots.txt directives and crawler tokens (per Google's guide). */
class RobotsDocumentationProvider : AbstractDocumentationProvider() {

    private data class Doc(val title: String, val body: String, val example: String? = null)

    override fun getCustomDocumentationElement(
        editor: Editor, file: PsiFile, contextElement: PsiElement?, targetOffset: Int
    ): PsiElement? = when (contextElement?.node?.elementType) {
        RobotsTokens.DIRECTIVE, RobotsTokens.KEY, RobotsTokens.VALUE, RobotsTokens.URL -> contextElement
        else -> null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val doc = docFor(element) ?: return null
        return doc.title + " — " + stripHtml(doc.body).take(120)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val doc = docFor(element) ?: return null
        return buildString {
            append(DocumentationMarkup.DEFINITION_START).append(doc.title).append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START).append(doc.body)
            doc.example?.let { append("<br><br><b>Example</b><br><code>").append(it).append("</code>") }
            append(DocumentationMarkup.CONTENT_END)
            append(DocumentationMarkup.CONTENT_START)
            append("<small>Reference: <a href=\"$GOOGLE_DOC\">Google · Create a robots.txt file</a></small>")
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    private fun docFor(element: PsiElement?): Doc? {
        element ?: return null
        val text = element.text?.trim().orEmpty()
        return when (element.node?.elementType) {
            RobotsTokens.DIRECTIVE, RobotsTokens.KEY -> DIRECTIVES[text.lowercase()]
                ?: Doc(text, "A non-standard or unrecognized directive. Most crawlers ignore lines they don't understand.")
            RobotsTokens.VALUE, RobotsTokens.URL -> valueDoc(element, text)
            else -> null
        }
    }

    private fun valueDoc(element: PsiElement, text: String): Doc? = when (fieldOfLine(element)) {
        "user-agent", "useragent" -> crawlerDoc(text)
        "allow", "disallow" -> Doc(
            "Path pattern",
            "A URL path relative to the site root, starting with <code>/</code>. <code>*</code> matches any sequence of " +
                "characters and <code>$</code> matches the end of the URL. The most specific (longest) matching rule wins."
        )
        "sitemap" -> Doc("Sitemap URL", "The full, absolute URL of a sitemap file that lists your crawlable pages.")
        "content-signal" -> Doc(
            "Content signals",
            "A comma-separated list of <code>signal=yes|no</code> preferences. Known signals: " +
                "<code>search</code> (appear in search results), <code>ai-input</code> (use as input to AI answers), " +
                "<code>ai-train</code> (use to train AI models). <code>yes</code> permits, <code>no</code> declines."
        )
        else -> null
    }

    private fun crawlerDoc(token: String): Doc {
        if (token == "*") return Doc(
            "* (all crawlers)",
            "Matches every crawler that isn't named by its own group. Note: Google's AdsBot crawlers must be named " +
                "explicitly — they don't obey the <code>*</code> group."
        )
        val ai = AiCrawlers.LIST.firstOrNull { it.token.equals(token, ignoreCase = true) }
        val operator = ai?.operator ?: AiCrawlers.operatorFor(token)
        val body = buildString {
            append("A crawler user-agent token; the rules in this group apply to requests from it.")
            if (operator != null) append("<br><br><b>Operator:</b> ").append(operator)
            ai?.note?.let { append("<br>").append(it) }
        }
        return Doc(token, body)
    }

    /** The directive keyword on the same line as [element] (text before the colon). */
    private fun fieldOfLine(element: PsiElement): String? {
        val doc = element.containingFile?.viewProvider?.document ?: return null
        val line = doc.getLineNumber(element.textRange.startOffset)
        val lineText = doc.getText(TextRange(doc.getLineStartOffset(line), doc.getLineEndOffset(line)))
        val colon = lineText.indexOf(':')
        return if (colon > 0) lineText.substring(0, colon).trim().lowercase() else null
    }

    private fun stripHtml(s: String) = s.replace(Regex("<[^>]+>"), "")

    companion object {
        private const val GOOGLE_DOC =
            "https://developers.google.com/search/docs/crawling-indexing/robots/create-robots-txt"

        private val DIRECTIVES: Map<String, Doc> = mapOf(
            "user-agent" to Doc(
                "User-agent",
                "Starts a group of rules and names the crawler they apply to. The group applies until the next " +
                    "<code>user-agent</code> line. Use <code>*</code> to match all crawlers. You can stack several " +
                    "<code>user-agent</code> lines to share one set of rules. Each group must have at least one rule.",
                "User-agent: Googlebot"
            ),
            "disallow" to Doc(
                "Disallow",
                "A path the crawler must <b>not</b> fetch, relative to the site root and starting with <code>/</code>. " +
                    "<code>Disallow: /</code> blocks the whole site; an empty value (<code>Disallow:</code>) blocks nothing. " +
                    "Supports <code>*</code> (any sequence) and <code>$</code> (end of URL).",
                "Disallow: /private/"
            ),
            "allow" to Doc(
                "Allow",
                "A path the crawler <b>may</b> fetch, overriding a <code>disallow</code> — typically to expose a sub-path " +
                    "of a blocked directory. When rules conflict, the most specific (longest) matching path wins.",
                "Allow: /private/public-page.html"
            ),
            "sitemap" to Doc(
                "Sitemap",
                "The full, absolute URL of a sitemap that lists the pages to crawl. It is independent of " +
                    "<code>user-agent</code> groups and may be repeated. Helps crawlers discover your URLs.",
                "Sitemap: https://example.com/sitemap.xml"
            ),
            "crawl-delay" to Doc(
                "Crawl-delay",
                "Non-standard. Suggests the number of seconds to wait between requests. <b>Google ignores it</b> — set the " +
                    "crawl rate in Search Console instead; some other crawlers (e.g. Bing) honor it.",
                "Crawl-delay: 10"
            ),
            "host" to Doc(
                "Host",
                "Non-standard (introduced by Yandex) to indicate a preferred mirror host. Ignored by Google.",
                "Host: example.com"
            ),
            "clean-param" to Doc(
                "Clean-param",
                "Non-standard (Yandex). Tells the crawler to ignore specified URL query parameters. Ignored by Google.",
                "Clean-param: ref /articles/"
            ),
            "noindex" to Doc(
                "Noindex",
                "<b>Not supported in robots.txt.</b> Google does not obey <code>noindex</code> here — use a " +
                    "<code>noindex</code> meta tag or the <code>X-Robots-Tag</code> HTTP header instead."
            ),
            "request-rate" to Doc("Request-rate", "Non-standard. Suggests a crawl rate. Not supported by Google."),
            "visit-time" to Doc("Visit-time", "Non-standard. Suggests preferred crawl hours (UTC). Not supported by Google."),
            "content-signal" to Doc(
                "Content-Signal",
                "Non-standard. Introduced by <b>Cloudflare</b> (Content Signals Policy, 2025) to express how content may " +
                    "be used, per signal, without blocking the crawler outright. Value is a comma-separated list of " +
                    "<code>signal=yes|no</code> pairs — known signals: <code>search</code>, <code>ai-input</code> " +
                    "(AI answer input), <code>ai-train</code> (AI model training). It expresses a preference, not access control; " +
                    "pair it with <code>Allow</code>/<code>Disallow</code> to actually restrict crawling.",
                "Content-Signal: ai-train=no, search=yes, ai-input=no"
            )
        )
    }
}
