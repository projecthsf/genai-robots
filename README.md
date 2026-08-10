# RobotsReader

A JetBrains IDE plugin that turns `robots.txt` from raw text into something you can **read, build, and understand** — with a live, plain-language preview beside the file.

## Features

- **Decoded split preview** — open any `robots.txt` and a preview panel explains each `User-agent`
  group in plain language (*Allows everything* / *Blocks everything* / *Specific rules*) plus a
  **Sitemaps** section. Click a line in the editor and the matching block highlights (caret sync).
- **Full language support** — syntax highlighting, a **Structure view** (groups → rules), `#`
  commenting, and `Sitemap:` URLs as clickable web references.
- **Hover docs** — hover any directive (`User-agent`, `Disallow`, `Allow`, `Sitemap`,
  `Crawl-delay`, `Content-Signal`, …) or a crawler token for an explanation sourced from Google's
  robots.txt guide, with a link back to it.
- **Completion** — directive keywords at line start; known crawler names (ClaudeBot, GPTBot,
  Google-Extended, …) after `User-agent:`; path patterns and `Content-Signal` values.
- **Guided creation** — **New ▸ robots.txt** opens a builder (agent rows + sitemaps, each
  add/removable). In the editor, right-click ▸ **New User-agent…** / **New Sitemap…** to insert.
- **Fetch any site** — **Tools ▸ Open robots.txt from Website…** fetches and decodes a live domain's file.

Understands modern crawler directives including the AI-crawler tokens and Cloudflare's
`Content-Signal` (`ai-train` / `ai-input` / `search`).

## Build

```bash
./gradlew buildPlugin      # -> build/distributions/robots-reader-<version>.zip
./gradlew runIde           # launch a sandbox IDE with the plugin preloaded
```

Requires a JDK 21 toolchain (bytecode is emitted at Java 17 so it loads on JBR-17 IDEs, build 233+).

## Install from disk

**Settings → Plugins → ⚙ → Install Plugin from Disk…** → pick the built ZIP → restart.
Works in any JetBrains IDE (IntelliJ IDEA, PhpStorm, GoLand, WebStorm, …).

## Guidance only

`robots.txt` is advisory: well-behaved crawlers obey it; it is not a security control. This plugin
decodes and documents a file — it does not enforce anything.
