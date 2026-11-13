package io.github.mlkitglossary.core

/**
 * Word-boundary building blocks. Scripts written without spaces between words (Han, Kana, Thai...)
 * are excluded from "word character", so a term next to such text still matches and a term made of
 * such characters needs no boundary on that edge.
 */
internal object WordChars {
    private val UNSPACED_SCRIPTS = setOf(
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.THAI,
        Character.UnicodeScript.LAO,
        Character.UnicodeScript.KHMER,
        Character.UnicodeScript.MYANMAR,
    )

    /** Regex char class of characters that glue into a word (letters/digits/underscore, spaced scripts only). */
    const val CLASS = "[\\p{L}\\p{N}_&&[^\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsThai}\\p{IsLao}\\p{IsKhmer}\\p{IsMyanmar}]]"

    fun isBoundaryChar(c: Char): Boolean =
        (c.isLetterOrDigit() || c == '_') && Character.UnicodeScript.of(c.code) !in UNSPACED_SCRIPTS
}
