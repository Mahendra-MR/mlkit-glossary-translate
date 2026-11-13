package io.github.mlkitglossary.core

/** Cheap structural checks on HTML-like text, used to decide whether translated markup can be trusted. */
internal object MarkupValidator {
    private val TAG = Regex("<(/?)([A-Za-z][A-Za-z0-9]*)[^<>]*?(/?)>")
    private val VOID = setOf("br", "hr", "img", "input", "meta", "link", "wbr")

    /** Tag names in document order, e.g. `[b, /b]`; void and self-closing tags are skipped. */
    private fun structure(text: String): List<String> = TAG.findAll(text).mapNotNull { m ->
        val closing = m.groupValues[1] == "/"
        val name = m.groupValues[2].lowercase()
        val selfClosing = m.groupValues[3] == "/"
        if (name in VOID || selfClosing) null else if (closing) "/$name" else name
    }.toList()

    private fun isBalanced(tags: List<String>): Boolean {
        val stack = ArrayDeque<String>()
        for (t in tags) {
            if (t.startsWith("/")) {
                if (stack.removeLastOrNull() != t.substring(1)) return false
            } else {
                stack.addLast(t)
            }
        }
        return stack.isEmpty()
    }

    /** True when [translated] has the same tags as [source] and, if source nests properly, so does it. */
    fun preservesStructure(source: String, translated: String): Boolean {
        val a = structure(source)
        val b = structure(translated)
        if (a.sorted() != b.sorted()) return false
        return !isBalanced(a) || isBalanced(b)
    }
}
