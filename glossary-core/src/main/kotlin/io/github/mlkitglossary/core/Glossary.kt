package io.github.mlkitglossary.core

/**
 * Immutable map of source term -> text that should appear in the translated output.
 *
 * Matching is case-insensitive, respects word boundaries and prefers the longest term
 * ("Bank Account" wins over "Bank"). Whitespace inside a multi-word term matches any run
 * of whitespace. Blank keys are ignored; if two keys differ only by case, the later one wins.
 */
public class Glossary(entries: Map<String, String>) {

    internal val terms: Map<String, String> = entries
        .filterKeys { it.isNotBlank() }
        .entries
        .associate { (k, v) -> k.trim().lowercase() to v }

    public val size: Int get() = terms.size

    public fun isEmpty(): Boolean = terms.isEmpty()

    internal val pattern: Regex? = if (terms.isEmpty()) null else buildPattern(terms.keys)

    internal fun replacementFor(matched: String): String? =
        terms[matched.trim().lowercase().replace(WHITESPACE, " ")]

    public companion object {
        public val EMPTY: Glossary = Glossary(emptyMap())

        private val WHITESPACE = Regex("\\s+")
        private const val WORD = "[\\p{L}\\p{N}_]"

        private fun buildPattern(keys: Set<String>): Regex {
            val alternatives = keys
                .map { it.replace(WHITESPACE, " ") }
                .sortedByDescending { it.length }
                .joinToString("|") { term ->
                    val body = term.split(' ').joinToString("\\s+") { Regex.escape(it) }
                    val before = if (term.first().isWordChar()) "(?<!$WORD)" else ""
                    val after = if (term.last().isWordChar()) "(?!$WORD)" else ""
                    "(?:$before$body$after)"
                }
            return Regex(alternatives, setOf(RegexOption.IGNORE_CASE))
        }

        private fun Char.isWordChar(): Boolean = isLetterOrDigit() || this == '_'
    }
}
