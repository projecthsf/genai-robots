package io.genai.robots.lang

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import io.genai.robots.model.AiCrawlers

/** Suggests directive keywords at line start, and crawler tokens / path patterns after the colon. */
class RobotsCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                val doc = parameters.editor.document
                val offset = parameters.offset
                val lineStart = doc.getLineStartOffset(doc.getLineNumber(offset))
                val prefix = doc.getText(TextRange(lineStart, offset))
                val colon = prefix.indexOf(':')
                if (colon < 0) {
                    DIRECTIVES.forEach { result.addElement(LookupElementBuilder.create("$it: ").withPresentableText(it)) }
                    return
                }
                when (prefix.substring(0, colon).trim().lowercase()) {
                    "user-agent", "useragent" -> {
                        result.addElement(LookupElementBuilder.create("*").withTypeText("all crawlers"))
                        AiCrawlers.LIST.forEach {
                            result.addElement(LookupElementBuilder.create(it.token).withTypeText(it.operator))
                        }
                    }
                    "allow", "disallow" -> listOf("/", "/*", "/$", "/path/").forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                    "content-signal" -> {
                        result.addElement(LookupElementBuilder.create("ai-train=no, search=yes, ai-input=no")
                            .withTypeText("Cloudflare Content Signals"))
                        listOf("search=yes", "search=no", "ai-input=yes", "ai-input=no", "ai-train=yes", "ai-train=no")
                            .forEach { result.addElement(LookupElementBuilder.create(it)) }
                    }
                }
            }
        })
    }
}

private val DIRECTIVES = listOf("User-agent", "Disallow", "Allow", "Sitemap", "Crawl-delay", "Host", "Content-Signal")
