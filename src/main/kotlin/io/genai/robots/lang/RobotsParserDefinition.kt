package io.genai.robots.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.genai.robots.lang.psi.ROBOTS_FILE
import io.genai.robots.lang.psi.RobotsTokens

/** The PSI file for a robots.txt document. */
class RobotsFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RobotsLanguage) {
    override fun getFileType() = RobotsFileType
    override fun toString() = "robots.txt file"
}

/**
 * Minimal parser: the lexer already does all the classification, so the PSI tree is a flat list of
 * leaf tokens under the file. Highlighting, references and structure work off the tokens/text; we
 * don't need a grammar.
 */
class RobotsParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = RobotsLexer()
    override fun getFileNodeType(): IFileElementType = ROBOTS_FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun getWhitespaceTokens(): TokenSet = WHITESPACE
    override fun createFile(viewProvider: FileViewProvider): PsiFile = RobotsFile(viewProvider)
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val mark = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        mark.done(root)
        builder.treeBuilt
    }

    companion object {
        private val COMMENTS = TokenSet.create(RobotsTokens.COMMENT)
        private val WHITESPACE = TokenSet.create(TokenType.WHITE_SPACE)
    }
}
