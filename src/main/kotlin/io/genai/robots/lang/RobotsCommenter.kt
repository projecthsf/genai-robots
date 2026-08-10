package io.genai.robots.lang

import com.intellij.lang.Commenter

/** Enables ⌘/ line-comment toggling with `#`. */
class RobotsCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "#"
    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
