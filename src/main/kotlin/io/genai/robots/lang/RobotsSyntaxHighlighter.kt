package io.genai.robots.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import io.genai.robots.lang.psi.RobotsTokens

/** Colours robots.txt tokens using the IDE's language colour scheme. */
class RobotsSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = RobotsLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when (tokenType) {
        RobotsTokens.DIRECTIVE -> arrayOf(DIRECTIVE)
        RobotsTokens.KEY -> arrayOf(KEY)
        RobotsTokens.COLON -> arrayOf(COLON)
        RobotsTokens.VALUE -> arrayOf(VALUE)
        RobotsTokens.URL -> arrayOf(URL)
        RobotsTokens.COMMENT -> arrayOf(COMMENT)
        else -> TextAttributesKey.EMPTY_ARRAY
    }

    companion object {
        val DIRECTIVE = createTextAttributesKey("ROBOTS_DIRECTIVE", DefaultLanguageHighlighterColors.KEYWORD)
        val KEY = createTextAttributesKey("ROBOTS_KEY", DefaultLanguageHighlighterColors.METADATA)
        val COLON = createTextAttributesKey("ROBOTS_COLON", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val VALUE = createTextAttributesKey("ROBOTS_VALUE", DefaultLanguageHighlighterColors.STRING)
        val URL = createTextAttributesKey("ROBOTS_URL", DefaultLanguageHighlighterColors.NUMBER)
        val COMMENT = createTextAttributesKey("ROBOTS_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    }
}

class RobotsSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, file: VirtualFile?): SyntaxHighlighter = RobotsSyntaxHighlighter()
}
