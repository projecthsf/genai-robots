package io.genai.robots.model

/** One Allow/Disallow rule inside a group. `path` "" (empty) means "no constraint".
 *  `line` is the 0-based document line it came from (-1 when unknown). */
data class Rule(val allow: Boolean, val path: String, val line: Int = -1)

/** A record group: one or more user-agents sharing a set of rules. `startLine`/`endLine` are
 *  0-based document line indices (for caret ↔ preview sync); -1 when unknown. */
data class Group(
    val agents: List<String>,
    val rules: List<Rule>,
    val crawlDelay: String? = null,
    val startLine: Int = -1,
    val endLine: Int = -1,
)

/** A `Sitemap:` directive and the 0-based line it came from. */
data class Sitemap(val url: String, val line: Int = -1)

/** A parsed robots.txt. */
data class RobotsModel(
    val groups: List<Group>,
    val sitemaps: List<Sitemap>,
    val host: String?,
    val unknown: List<String>,   // directives we don't specifically render
)

enum class GroupVerdict { ALLOW_ALL, BLOCK_ALL, SPECIFIC }

/** Parses robots.txt text (RFC 9309 grouping). Lenient: never throws on malformed input. */
object RobotsParser {

    fun parse(text: String): RobotsModel {
        val groups = mutableListOf<Group>()
        val sitemaps = mutableListOf<Sitemap>()
        val unknown = mutableListOf<String>()
        var host: String? = null

        var agents = mutableListOf<String>()
        var rules = mutableListOf<Rule>()
        var delay: String? = null
        var startLine = -1
        var endLine = -1

        fun flush() {
            if (agents.isNotEmpty() || rules.isNotEmpty())
                groups.add(Group(agents.toList(), rules.toList(), delay, startLine, endLine))
            agents = mutableListOf(); rules = mutableListOf(); delay = null; startLine = -1; endLine = -1
        }

        text.lineSequence().forEachIndexed { idx, raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) return@forEachIndexed
            val colon = line.indexOf(':')
            if (colon < 0) return@forEachIndexed
            val field = line.substring(0, colon).trim().lowercase()
            val value = line.substring(colon + 1).trim()
            when (field) {
                "user-agent", "useragent" -> {
                    if (rules.isNotEmpty()) flush()   // a new group starts
                    if (startLine == -1) startLine = idx
                    endLine = idx
                    agents.add(value)
                }
                "allow" -> { if (startLine == -1) startLine = idx; endLine = idx; rules.add(Rule(true, value, idx)) }
                "disallow" -> { if (startLine == -1) startLine = idx; endLine = idx; rules.add(Rule(false, value, idx)) }
                "crawl-delay" -> { endLine = idx; delay = value }
                "sitemap" -> sitemaps.add(Sitemap(value, idx))
                "host" -> host = value
                else -> unknown.add(line)
            }
        }
        flush()
        return RobotsModel(groups, sitemaps, host, unknown)
    }

    /**
     * Does `group` allow `path`? RFC 9309 longest-match: the most specific matching rule wins,
     * Allow beating Disallow on a length tie. Supports `*` (any run) and a trailing `$` (end anchor).
     */
    fun isAllowed(group: Group, path: String): Boolean {
        var bestAllow = -1
        var bestDisallow = -1
        for (r in group.rules) {
            val len = matchLength(r.path, path) ?: continue
            if (r.allow) bestAllow = maxOf(bestAllow, len) else bestDisallow = maxOf(bestDisallow, len)
        }
        if (bestDisallow < 0) return true
        return bestAllow >= bestDisallow
    }

    /** Specificity (>=0) if `pattern` matches `path` as a prefix pattern, else null. Empty = no match. */
    private fun matchLength(pattern: String, path: String): Int? {
        if (pattern.isEmpty()) return null
        val hasWild = pattern.contains('*') || pattern.endsWith('$')
        if (!hasWild) return if (path.startsWith(pattern)) pattern.length else null
        var p = pattern
        var anchorEnd = false
        if (p.endsWith('$')) { anchorEnd = true; p = p.dropLast(1) }
        val sb = StringBuilder("^")
        for (c in p) when {
            c == '*' -> sb.append(".*")
            c.isLetterOrDigit() || c == '_' -> sb.append(c)
            else -> sb.append('\\').append(c)
        }
        if (anchorEnd) sb.append('$')
        return if (Regex(sb.toString()).containsMatchIn(path)) pattern.trimEnd('$').length else null
    }

    /** Plain-language classification of a whole group. */
    fun verdict(group: Group): GroupVerdict {
        val realDisallows = group.rules.filter { !it.allow && it.path.isNotEmpty() }
        val realAllows = group.rules.filter { it.allow && it.path.isNotEmpty() }
        if (realDisallows.isEmpty()) return GroupVerdict.ALLOW_ALL
        if (realAllows.isEmpty() && realDisallows.all { it.path == "/" }) return GroupVerdict.BLOCK_ALL
        return GroupVerdict.SPECIFIC
    }
}
