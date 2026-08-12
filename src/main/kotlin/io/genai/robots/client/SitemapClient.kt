package io.genai.robots.client

import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.zip.GZIPInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/** One `<url>` entry from a sitemap. */
data class SitemapUrl(val loc: String, val lastmod: String?)

enum class SitemapKind { URLSET, INDEX, UNKNOWN }

/** The result of fetching+parsing one sitemap file. */
data class FetchedSitemap(
    val url: String,
    val ok: Boolean,
    val error: String?,
    val kind: SitemapKind,
    val bytes: Int,
    val urls: List<SitemapUrl>,          // page URLs (when URLSET)
    val childSitemaps: List<String>,     // child sitemap URLs (when INDEX)
)

/**
 * Fetches sitemaps over HTTPS: follows `<sitemapindex>` one level into its children, transparently
 * gunzips `.gz`, and pull-parses the XML with StAX (external entities disabled — no XXE). Bounded so
 * a huge site can't hang the IDE.
 */
object SitemapClient {

    const val MAX_CHILDREN = 50
    const val MAX_URLS_PER_FILE = 50_000

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val xmlFactory: XMLInputFactory = XMLInputFactory.newFactory().apply {
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
    }

    /** Fetch each root sitemap and (for index files) their children. De-dupes and bounds fan-out.
     *  [cancelled] is polled between fetches so a long run can be stopped. */
    fun fetchAll(rootUrls: List<String>, cancelled: () -> Boolean = { false }): List<FetchedSitemap> {
        val out = mutableListOf<FetchedSitemap>()
        val seen = HashSet<String>()
        val queue = ArrayDeque(rootUrls.map { it.trim() }.filter { it.isNotEmpty() })
        var children = 0
        while (queue.isNotEmpty()) {
            if (cancelled()) break
            val u = queue.removeFirst()
            if (!seen.add(u)) continue
            val fs = fetchOne(u)
            out.add(fs)
            if (fs.kind == SitemapKind.INDEX) {
                for (child in fs.childSitemaps) {
                    if (children >= MAX_CHILDREN) break
                    if (child !in seen) { queue.addLast(child); children++ }
                }
            }
        }
        return out
    }

    private fun fetchOne(url: String): FetchedSitemap {
        return try {
            val req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "RobotsReader-IDE/0.1 (+https://genai.io.vn)")
                .header("Accept", "application/xml, text/xml, */*")
                .GET().build()
            val res: HttpResponse<ByteArray> = http.send(req, HttpResponse.BodyHandlers.ofByteArray())
            if (res.statusCode() !in 200..299)
                return FetchedSitemap(url, false, "HTTP ${res.statusCode()}", SitemapKind.UNKNOWN, 0, emptyList(), emptyList())
            var body = res.body()
            val bytes = body.size
            if (isGzip(body) || url.endsWith(".gz")) body = gunzip(body)
            val (kind, urls, children) = parse(body)
            FetchedSitemap(url, true, null, kind, bytes, urls, children)
        } catch (e: Exception) {
            FetchedSitemap(url, false, e.message ?: e.javaClass.simpleName, SitemapKind.UNKNOWN, 0, emptyList(), emptyList())
        }
    }

    private fun isGzip(b: ByteArray) = b.size >= 2 && b[0] == 0x1f.toByte() && b[1] == 0x8b.toByte()
    private fun gunzip(b: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(b)).use { it.readBytes() }

    private fun parse(body: ByteArray): Triple<SitemapKind, List<SitemapUrl>, List<String>> {
        val urls = mutableListOf<SitemapUrl>()
        val children = mutableListOf<String>()
        var kind = SitemapKind.UNKNOWN
        val reader = xmlFactory.createXMLStreamReader(ByteArrayInputStream(body))
        var inUrl = false; var inSitemap = false
        var loc: String? = null; var lastmod: String? = null
        try {
            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> when (reader.localName.lowercase()) {
                        "urlset" -> kind = SitemapKind.URLSET
                        "sitemapindex" -> kind = SitemapKind.INDEX
                        "url" -> { inUrl = true; loc = null; lastmod = null }
                        "sitemap" -> { inSitemap = true; loc = null }
                        "loc" -> {
                            // Only the page/child-sitemap <loc> (sitemaps.org namespace). Ignore
                            // <image:loc>/<video:loc>/<news:loc> so image URLs aren't counted as pages.
                            val ns = reader.namespaceURI ?: ""
                            val text = reader.elementText.trim()
                            if (ns.isEmpty() || ns.contains("sitemaps.org", ignoreCase = true)) loc = text
                        }
                        "lastmod" -> if (inUrl) lastmod = reader.elementText.trim()
                    }
                    XMLStreamConstants.END_ELEMENT -> when (reader.localName.lowercase()) {
                        "url" -> { inUrl = false; loc?.let { if (urls.size < MAX_URLS_PER_FILE) urls.add(SitemapUrl(it, lastmod)) } }
                        "sitemap" -> { inSitemap = false; loc?.let { children.add(it) } }
                    }
                }
            }
        } finally {
            reader.close()
        }
        return Triple(kind, urls, children)
    }
}
