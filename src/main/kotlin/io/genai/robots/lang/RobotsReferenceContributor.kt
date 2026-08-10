package io.genai.robots.lang

import com.intellij.openapi.paths.WebReference
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import io.genai.robots.lang.psi.RobotsTokens

/** Turns http(s) values (e.g. `Sitemap:` URLs) into web references — ⌘-click opens them. */
class RobotsReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withElementType(
                com.intellij.psi.tree.TokenSet.create(RobotsTokens.URL)
            ),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val url = element.text?.trim().orEmpty()
                    if (url.isEmpty()) return PsiReference.EMPTY_ARRAY
                    return arrayOf(WebReference(element, url))
                }
            }
        )
    }
}
