package io.github.mlkitglossary.core

/** One glossary rule: what to show in the output, and optionally the grammatical gender of that output. */
public data class GlossaryEntry(val replacement: String, val gender: Gender? = null)

/**
 * Immutable map of source term -> [GlossaryEntry] shown in the translated output.
 *
 * Matching is case-insensitive, respects word boundaries and prefers the longest term
 * ("Bank Account" wins over "Bank"). Whitespace inside a multi-word term matches any run
 * of whitespace. Blank keys are ignored; if two keys differ only by case, the later one wins.
 *
 * With [matchPlurals], English-style `s`/`es` plurals and `'s` possessives also match
 * (`loans`, `loan's`); the replacement is used unchanged since it cannot be inflected generically.
 */
public class Glossary private constructor(
    entries: Map<String, GlossaryEntry>,
    public val matchPlurals: Boolean,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
) {

    /** Plain `term -> replacement` glossary without gender information. */
    public constructor(entries: Map<String, String>, matchPlurals: Boolean = false) :
        this(entries.mapValues { GlossaryEntry(it.value) }, matchPlurals, Unit)

    internal val terms: Map<String, GlossaryEntry> = entries
        .filterKeys { it.isNotBlank() }
        .entries
        .associate { (k, v) -> normalize(k) to v }

    public val size: Int get() = terms.size

    public fun isEmpty(): Boolean = terms.isEmpty()

    internal val pattern: Regex? = if (terms.isEmpty()) null else buildPattern(terms.keys, matchPlurals)

    internal fun entryFor(matched: String): GlossaryEntry? {
        val key = normalize(matched)
        terms[key]?.let { return it }
        if (!matchPlurals) return null
        return SUFFIXES.firstNotNullOfOrNull { suffix ->
            if (key.endsWith(suffix)) terms[key.removeSuffix(suffix)] else null
        }
    }

    public class Builder {
        private val map = LinkedHashMap<String, GlossaryEntry>()
        private var plurals = false

        public fun term(source: String, replacement: String, gender: Gender? = null): Builder = apply {
            map[source] = GlossaryEntry(replacement, gender)
        }

        public fun matchPlurals(enabled: Boolean = true): Builder = apply { plurals = enabled }

        public fun build(): Glossary = Glossary(map, plurals, Unit)
    }

    public companion object {
        public val EMPTY: Glossary = Glossary(emptyMap<String, String>())

        public fun of(entries: Map<String, GlossaryEntry>, matchPlurals: Boolean = false): Glossary =
            Glossary(entries, matchPlurals, Unit)

        public inline fun build(block: Builder.() -> Unit): Glossary = Builder().apply(block).build()

        private val WHITESPACE = Regex("\\s+")
        private const val WORD = WordChars.CLASS
        private const val SUFFIX_PATTERN = "(?:es|s|['’]s)?"
        private val SUFFIXES = listOf("'s", "’s", "es", "s")

        private fun normalize(term: String) = term.trim().lowercase().replace(WHITESPACE, " ")

        private fun buildPattern(keys: Set<String>, plurals: Boolean): Regex {
            val alternatives = keys
                .sortedByDescending { it.length }
                .joinToString("|") { term ->
                    val body = term.split(' ').joinToString("\\s+") { Regex.escape(it) }
                    val before = if (term.first().isWordChar()) "(?<!$WORD)" else ""
                    val after = if (term.last().isWordChar()) {
                        (if (plurals) SUFFIX_PATTERN else "") + "(?!$WORD)"
                    } else {
                        ""
                    }
                    "(?:$before$body$after)"
                }
            return Regex(alternatives, setOf(RegexOption.IGNORE_CASE))
        }

        private fun Char.isWordChar(): Boolean = WordChars.isBoundaryChar(this)
    }
}
