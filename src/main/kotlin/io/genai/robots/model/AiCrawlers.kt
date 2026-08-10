package io.genai.robots.model

/** A known AI crawler and who runs it. */
data class AiCrawler(val token: String, val operator: String, val note: String? = null)

/** How a specific user-agent is treated by a robots.txt. */
data class CrawlerStatus(val allowed: Boolean, val via: Via)
enum class Via { BY_NAME, VIA_STAR, DEFAULT }

/**
 * Curated list of AI crawlers (training + answer/search bots), mirroring the RobotsReader web tool.
 * Order is intentional (grouped by operator). Blocking training bots does not affect Search ranking.
 */
object AiCrawlers {

    val LIST: List<AiCrawler> = listOf(
        AiCrawler("Amazonbot", "Amazon"),
        AiCrawler("ClaudeBot", "Anthropic"),
        AiCrawler("Claude-Web", "Anthropic"),
        AiCrawler("anthropic-ai", "Anthropic"),
        AiCrawler("Applebot-Extended", "Apple AI-training opt-out",
            "AI-training token — NOT Applebot; blocking it keeps Siri/Spotlight working."),
        AiCrawler("Bytespider", "ByteDance / TikTok", "Aggressive AI scraper."),
        AiCrawler("cohere-ai", "Cohere"),
        AiCrawler("CCBot", "Common Crawl", "Feeds many AI training datasets."),
        AiCrawler("Diffbot", "Diffbot"),
        AiCrawler("Google-Extended", "Google (Gemini) AI training",
            "AI-training token — NOT Googlebot; blocking it does not affect Search ranking."),
        AiCrawler("ImageSiftBot", "ImageSift / Hive"),
        AiCrawler("meta-externalagent", "Meta AI"),
        AiCrawler("GPTBot", "OpenAI", "OpenAI's training crawler."),
        AiCrawler("ChatGPT-User", "OpenAI",
            "Fetches a page when a ChatGPT user asks about it (not training)."),
        AiCrawler("OAI-SearchBot", "OpenAI", "OpenAI's search index crawler."),
        AiCrawler("PerplexityBot", "Perplexity"),
        AiCrawler("omgilibot", "Webz.io"),
        AiCrawler("YouBot", "You.com"),
    )

    /** token(lowercased) -> operator, for decoding any user-agent shown in the "All rules" section. */
    val OPERATORS: Map<String, String> = buildMap {
        LIST.forEach { put(it.token.lowercase(), it.operator) }
        // common non-AI crawlers, for nicer labels in the rules section
        put("googlebot", "Google Search")
        put("googlebot-image", "Google Images")
        put("bingbot", "Microsoft Bing")
        put("msnbot", "Microsoft")
        put("slurp", "Yahoo")
        put("duckduckbot", "DuckDuckGo")
        put("baiduspider", "Baidu")
        put("yandex", "Yandex")
        put("yandexbot", "Yandex")
        put("applebot", "Apple (Siri/Spotlight)")
        put("facebookexternalhit", "Meta link preview")
        put("twitterbot", "X / Twitter")
        put("linkedinbot", "LinkedIn")
        put("ahrefsbot", "Ahrefs SEO")
        put("semrushbot", "Semrush SEO")
        put("cloudflarebrowserrenderingcrawler", "Cloudflare")
    }

    fun operatorFor(token: String): String? = OPERATORS[token.lowercase()]

    /** Resolve how `token` is treated: matched by its own group, by `*`, or default-allowed. */
    fun status(token: String, model: RobotsModel): CrawlerStatus {
        val named = model.groups.firstOrNull { g -> g.agents.any { it.equals(token, ignoreCase = true) } }
        if (named != null) return CrawlerStatus(RobotsParser.isAllowed(named, "/"), Via.BY_NAME)
        val star = model.groups.firstOrNull { g -> g.agents.any { it == "*" } }
        if (star != null) return CrawlerStatus(RobotsParser.isAllowed(star, "/"), Via.VIA_STAR)
        return CrawlerStatus(allowed = true, via = Via.DEFAULT)
    }
}
