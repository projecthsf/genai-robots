package io.genai.robots.model

import io.genai.robots.client.FetchedSitemap
import io.genai.robots.client.SitemapClient
import io.genai.robots.client.SitemapKind
import java.net.URI

enum class Severity { ERROR, WARN, INFO }

/** One validation finding. `url` is the offending page URL; `detail` is a secondary line (e.g. where). */
data class SitemapIssue(val severity: Severity, val message: String, val url: String? = null, val detail: String? = null)

/** One line in the "what was checked" summary. */
data class CheckResult(val label: String, val ok: Boolean, val detail: String, val severity: Severity)

data class SitemapReport(
    val sitemaps: List<FetchedSitemap>,
    val totalUrls: Int,
    val blockedCount: Int,
    val checks: List<CheckResult>,
    val issues: List<SitemapIssue>,
) {
    val fetchedOk: Int get() = sitemaps.count { it.ok }
    fun bySeverity(s: Severity) = issues.filter { it.severity == s }
}

/**
 * Validates fetched sitemaps against a parsed robots.txt. Produces (1) a checklist of everything it
 * inspected — so passing checks are visible, not just failures — and (2) detailed findings.
 * Headline check: URLs listed in the sitemap but blocked by the site's own robots.txt.
 */
object SitemapValidator {

    private val LASTMOD = Regex("""^\d{4}-\d{2}-\d{2}([Tt]\d{2}:\d{2}(:\d{2}(\.\d+)?)?([Zz]|[+-]\d{2}:\d{2})?)?$""")

    fun validate(robots: RobotsModel, fetched: List<FetchedSitemap>): SitemapReport {
        val issues = mutableListOf<SitemapIssue>()
        val star = robots.groups.firstOrNull { g -> g.agents.any { it == "*" } }

        val occurrences = LinkedHashMap<String, MutableList<String>>()   // loc -> sitemap files it appeared in
        val hostCounts = HashMap<String, Int>()
        val urlHost = mutableListOf<Pair<String, String>>()
        val fetchErrors = mutableListOf<String>()
        var total = 0; var blocked = 0; var http = 0; var malformed = 0; var badLastmod = 0; var limitHits = 0

        for (fs in fetched) {
            if (!fs.ok) { issues += SitemapIssue(Severity.ERROR, "Couldn't read sitemap: ${fs.error}", fs.url); fetchErrors += fs.url; continue }
            if (fs.kind == SitemapKind.UNKNOWN)
                issues += SitemapIssue(Severity.WARN, "Not a recognized sitemap (no <urlset>/<sitemapindex>)", fs.url)
            if (fs.urls.size >= SitemapClient.MAX_URLS_PER_FILE) {
                issues += SitemapIssue(Severity.ERROR, "Hit the 50,000-URL limit — split this sitemap", fs.url); limitHits++
            }
            if (fs.bytes > 50 * 1024 * 1024) {
                issues += SitemapIssue(Severity.ERROR, "Over the 50 MB limit (${fs.bytes / (1024 * 1024)} MB)", fs.url); limitHits++
            }
            for (su in fs.urls) {
                total++
                val loc = su.loc.trim()
                occurrences.getOrPut(loc) { mutableListOf() }.add(fs.url)
                val uri = runCatching { URI(loc) }.getOrNull()
                if (uri == null || uri.host == null) { issues += SitemapIssue(Severity.WARN, "Malformed URL", loc); malformed++; continue }
                if (uri.scheme.equals("http", true)) { issues += SitemapIssue(Severity.WARN, "Uses http (should be https)", loc); http++ }
                val host = uri.host.lowercase()
                hostCounts.merge(host, 1, Int::plus); urlHost += loc to host
                if (star != null) {
                    val path = uri.rawPath?.ifEmpty { "/" } ?: "/"
                    if (!RobotsParser.isAllowed(star, path)) {
                        blocked++
                        issues += SitemapIssue(Severity.ERROR, "Listed in sitemap but blocked by robots.txt", loc,
                            "A crawler that obeys robots.txt won't fetch this page.")
                    }
                }
                su.lastmod?.let { if (!LASTMOD.matches(it)) { issues += SitemapIssue(Severity.INFO, "Invalid <lastmod>: $it", loc); badLastmod++ } }
            }
        }

        // Duplicates — report each with WHERE it appears.
        val dups = occurrences.filter { it.value.size > 1 }
        for ((loc, srcs) in dups) {
            val where = srcs.groupingBy { base(it) }.eachCount().entries
                .joinToString(", ") { (name, count) -> if (count > 1) "$name ×$count" else name }
            issues += SitemapIssue(Severity.WARN, "Duplicate URL", loc, "Appears in: $where")
        }

        // Host mismatch relative to the dominant host.
        val mainHost = hostCounts.maxByOrNull { it.value }?.key
        val offHost = if (mainHost != null) urlHost.filter { it.second != mainHost } else emptyList()
        offHost.forEach { issues += SitemapIssue(Severity.WARN, "Different host than $mainHost", it.first) }

        val checks = listOf(
            check("Sitemaps fetched & parsed", fetchErrors.isEmpty(), "${fetched.size - fetchErrors.size}/${fetched.size}", Severity.ERROR),
            check("URLs blocked by robots.txt", blocked == 0, if (blocked == 0) "none" else "$blocked", Severity.ERROR),
            check("Within 50k-URL / 50 MB limits", limitHits == 0, if (limitHits == 0) "ok" else "$limitHits exceeded", Severity.ERROR),
            check("Duplicate URLs", dups.isEmpty(), if (dups.isEmpty()) "none" else "${dups.size}", Severity.WARN),
            check("HTTPS (not http)", http == 0, if (http == 0) "all https" else "$http http", Severity.WARN),
            check("Same host", offHost.isEmpty(), if (offHost.isEmpty()) (mainHost ?: "—") else "${offHost.size} off-host", Severity.WARN),
            check("Malformed URLs", malformed == 0, if (malformed == 0) "none" else "$malformed", Severity.WARN),
            check("Valid <lastmod>", badLastmod == 0, if (badLastmod == 0) "ok" else "$badLastmod invalid", Severity.INFO),
        )

        return SitemapReport(fetched, total, blocked, checks, issues.sortedBy { it.severity.ordinal })
    }

    private fun check(label: String, ok: Boolean, detail: String, sev: Severity) = CheckResult(label, ok, detail, sev)
    private fun base(url: String) = url.substringAfterLast('/').ifEmpty { url }
}
