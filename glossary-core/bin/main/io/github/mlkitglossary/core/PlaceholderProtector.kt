package io.github.mlkitglossary.core

/**
 * Pure-Kotlin protect/restore step around a translation engine.
 *
 * [protect] swaps glossary terms and [preservePatterns] matches for opaque tokens like `GLS0Q`;
 * [restore] puts the preferred output back after translation, tolerating tokens that the engine
 * re-spaced, re-cased or had digits converted to another script.
 */
public class PlaceholderProtector(
    private val preservePatterns: List<Regex> = listOf(DEFAULT_NUMBER_PATTERN),
    private val agreement: GenderAgreement = GenderAgreement.NONE,
) {

    public fun protect(text: String, glossary: Glossary): ProtectedText {
        if (text.isEmpty() || TOKEN.containsMatchIn(text)) return ProtectedText(text, emptyList())

        val candidates = mutableListOf<Candidate>()
        glossary.pattern?.findAll(text)?.forEach { m ->
            val entry = glossary.entryFor(m.value) ?: return@forEach
            candidates += Candidate(m.range, SlotKind.GLOSSARY, m.value, entry.replacement, entry.gender)
        }
        preservePatterns.forEach { p ->
            p.findAll(text).filter { it.value.isNotEmpty() }.forEach { m ->
                candidates += Candidate(m.range, SlotKind.PRESERVED, m.value, m.value, null)
            }
        }
        if (candidates.isEmpty()) return ProtectedText(text, emptyList())

        val ordered = candidates.sortedWith(
            compareBy<Candidate> { it.range.first }.thenByDescending { it.range.last },
        )
        val sb = StringBuilder()
        val slots = mutableListOf<Slot>()
        var cursor = 0
        for (c in ordered) {
            if (c.range.first < cursor) continue
            sb.append(text, cursor, c.range.first)
            sb.append(token(slots.size))
            slots += Slot(slots.size, c.kind, c.original, c.output, c.gender)
            cursor = c.range.last + 1
        }
        sb.append(text, cursor, text.length)
        return ProtectedText(sb.toString(), slots)
    }

    public fun restore(translated: String, protectedText: ProtectedText): RestoreResult {
        val slots = protectedText.slots
        if (slots.isEmpty()) return RestoreResult(translated, emptyList(), 0)

        val seen = BooleanArray(slots.size)
        var unrecognized = 0
        val sb = StringBuilder()
        var cursor = 0
        for (m in TOKEN.findAll(translated)) {
            sb.append(translated, cursor, m.range.first)
            cursor = m.range.last + 1
            val idx = m.groupValues[1].toIntOrNull { Character.digit(it, 10) }
            if (idx == null || idx !in slots.indices) {
                unrecognized++
                sb.append(m.value)
                continue
            }
            seen[idx] = true
            val slot = slots[idx]
            slot.gender?.let { applyAgreement(sb, it) }
            sb.append(slot.output)
        }
        sb.append(translated, cursor, translated.length)
        val missing = slots.filter { !seen[it.index] }
        return RestoreResult(sb.toString(), missing, unrecognized)
    }

    private fun applyAgreement(sb: StringBuilder, gender: Gender) {
        var end = sb.length
        while (end > 0 && sb[end - 1] == ' ') end--
        if (end == 0 || end == sb.length) return
        var start = end
        while (start > 0 && !sb[start - 1].isWhitespace()) start--
        val word = sb.substring(start, end)
        val adjusted = agreement.adjustPrecedingWord(word, gender)
        if (adjusted != word) sb.replace(start, end, adjusted)
    }

    /** True when nothing but tokens, digits and punctuation remain, so there is nothing to translate. */
    public fun hasTranslatableContent(protectedText: ProtectedText): Boolean =
        TOKEN.replace(protectedText.text, " ").any { it.isLetter() }

    private data class Candidate(
        val range: IntRange,
        val kind: SlotKind,
        val original: String,
        val output: String,
        val gender: Gender?,
    )

    private inline fun String.toIntOrNull(digit: (Char) -> Int): Int? {
        var n = 0L
        for (c in this) {
            val d = digit(c)
            if (d < 0) return null
            n = n * 10 + d
            if (n > Int.MAX_VALUE) return null
        }
        return if (isEmpty()) null else n.toInt()
    }

    public companion object {
        /** Sequences of 4+ digits / `x` masks, e.g. account fragments, OTPs, ids. */
        public val DEFAULT_NUMBER_PATTERN: Regex = Regex("(?<![\\p{L}\\p{N}])[xX\\p{Nd}]{4,}(?![\\p{L}\\p{N}])")

        private fun token(index: Int) = "GLS${index}Q"

        // Tolerates case changes, inner spaces/hyphens/underscores and non-ASCII digits.
        private val TOKEN = Regex(
            "(?<![A-Za-z])G\\s?L\\s?S\\s?[-_]?\\s?(\\p{Nd}+)\\s?Q(?![A-Za-z])",
            RegexOption.IGNORE_CASE,
        )
    }
}
