package io.genai.robots.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.genai.robots.lang.psi.RobotsTokens

/**
 * Hand-written lexer for the line-based robots.txt format.
 *
 * Two states: at line start we read a field name (`User-agent`, `Disallow`, …) then a colon;
 * after the colon we read the rest of the line as a value (or a URL). Newlines reset to field mode.
 */
class RobotsLexer : LexerBase() {

    private companion object {
        const val FIELD = 0     // expecting a field name
        const val VALUE = 1     // after a colon, expecting a value
    }

    private var buf: CharSequence = ""
    private var end = 0
    private var start = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null
    private var state = FIELD

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        buf = buffer; end = endOffset; start = startOffset; state = initialState; locate()
    }

    override fun getState(): Int = state
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = start
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buf
    override fun getBufferEnd(): Int = end
    override fun advance() { start = tokenEnd; locate() }

    private fun isSpace(c: Char) = c == ' ' || c == '\t' || c == '\r'

    private fun locate() {
        if (start >= end) { tokenType = null; tokenEnd = start; return }
        val c = buf[start]
        when {
            c == '\n' -> { tokenEnd = start + 1; tokenType = TokenType.WHITE_SPACE; state = FIELD }
            isSpace(c) -> { var i = start; while (i < end && isSpace(buf[i])) i++; tokenEnd = i; tokenType = TokenType.WHITE_SPACE }
            c == '#' -> { var i = start; while (i < end && buf[i] != '\n') i++; tokenEnd = i; tokenType = RobotsTokens.COMMENT }
            state == VALUE -> {
                var i = start
                while (i < end && buf[i] != '\n' && buf[i] != '#') i++
                while (i > start && isSpace(buf[i - 1])) i--   // trim trailing spaces
                tokenEnd = i
                val text = buf.subSequence(start, i).toString()
                tokenType = if (text.startsWith("http://") || text.startsWith("https://")) RobotsTokens.URL else RobotsTokens.VALUE
                // stay in VALUE until a newline resets us; trailing '#'/spaces handled above
            }
            c == ':' -> { tokenEnd = start + 1; tokenType = RobotsTokens.COLON; state = VALUE }
            else -> {
                var i = start
                while (i < end && buf[i] != ':' && buf[i] != '#' && buf[i] != '\n' && !isSpace(buf[i])) i++
                tokenEnd = i
                val word = buf.subSequence(start, i).toString().lowercase()
                tokenType = if (word in RobotsTokens.DIRECTIVES) RobotsTokens.DIRECTIVE else RobotsTokens.KEY
            }
        }
    }
}
