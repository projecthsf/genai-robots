package io.genai.robots.client

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Fetched robots.txt: the normalized host it came from + the raw body. */
data class FetchedRobots(val host: String, val body: String)

/** Fetches `https://<domain>/robots.txt` over plain HTTPS. Runs OFF the EDT (network). */
object RobotsFetcher {

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** Accepts "example.com", "http://example.com/foo", "https://example.com" — returns host only. */
    fun normalizeHost(input: String): String {
        var s = input.trim()
        s = s.removePrefix("https://").removePrefix("http://")
        s = s.substringBefore('/').substringBefore('?').trim()
        return s.trimEnd('.')
    }

    fun fetch(input: String): FetchedRobots {
        val host = normalizeHost(input)
        require(host.isNotEmpty() && host.contains('.')) { "Enter a domain like example.com" }
        val req = HttpRequest.newBuilder(URI.create("https://$host/robots.txt"))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", "RobotsReader-IDE/0.1 (+https://genai.io.vn)")
            .header("Accept", "text/plain, */*")
            .GET().build()
        val res: HttpResponse<String> = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() == 404) return FetchedRobots(host, "# $host returned 404 — no robots.txt (everything is allowed).\n")
        if (res.statusCode() !in 200..299) throw RuntimeException("HTTP ${res.statusCode()} fetching https://$host/robots.txt")
        return FetchedRobots(host, res.body())
    }
}
