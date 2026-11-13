package io.github.mlkitglossary.core

public class TermMatch internal constructor(
    public val kind: SlotKind,
    public val matchedText: String,
    public val output: String,
    public val gender: Gender?,
)

public class SentenceAnalysis internal constructor(
    public val source: String,
    public val matches: List<TermMatch>,
    /** What would be sent to the translation engine. */
    public val protectedText: String,
)

/** Offline (no engine) check of a glossary against sample sentences. */
public class GlossaryAnalysis internal constructor(
    public val sentences: List<SentenceAnalysis>,
    /** Glossary terms that matched none of the sentences. */
    public val unusedTerms: List<String>,
    /** Keys that collapse into the same term (case/whitespace) so only one entry applies. */
    public val duplicateKeys: List<String>,
) {
    public fun format(): String = buildString {
        val matched = sentences.count { s -> s.matches.any { it.kind == SlotKind.GLOSSARY } }
        appendLine("Glossary analysis: ${sentences.size} texts, $matched with glossary matches")
        if (duplicateKeys.isNotEmpty()) appendLine("Duplicate keys (only the last applies): ${duplicateKeys.joinToString(", ")}")
        if (unusedTerms.isNotEmpty()) appendLine("Never matched: ${unusedTerms.joinToString(", ")}")
        sentences.forEachIndexed { i, s ->
            appendLine()
            appendLine("#${i + 1} ${s.source}")
            appendLine("  sent: ${s.protectedText}")
            s.matches.forEach { appendLine("  ${it.kind}: \"${it.matchedText}\" -> \"${it.output}\"${it.gender?.let { g -> " ($g)" } ?: ""}") }
        }
    }
}

/** Checks which glossary terms and preserve patterns apply to [texts], without calling any engine. */
public fun Glossary.analyze(
    texts: List<String>,
    protector: PlaceholderProtector = PlaceholderProtector(),
): GlossaryAnalysis {
    val sentences = texts.map { text ->
        val p = protector.protect(text, this)
        val matches = p.slots.map { TermMatch(it.kind, it.original, it.output, it.gender) }
        SentenceAnalysis(text, matches, p.text)
    }
    val used = sentences.flatMap { s -> s.matches.filter { it.kind == SlotKind.GLOSSARY }.map { normalizedKey(it.matchedText) } }
        .map { key -> terms.keys.firstOrNull { it == key } ?: terms.keys.firstOrNull { matchPluralBase(key, it) } }
        .toSet()
    val unused = terms.keys.filter { it !in used }
    return GlossaryAnalysis(sentences, unused, duplicateKeys)
}

private fun normalizedKey(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

private fun Glossary.matchPluralBase(key: String, term: String): Boolean =
    matchPlurals && listOf("'s", "’s", "es", "s").any { key.endsWith(it) && key.removeSuffix(it) == term }
