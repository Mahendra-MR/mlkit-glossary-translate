package io.github.mlkitglossary.core

/** Grammatical gender of a glossary replacement, used by [GenderAgreement] to fix neighbouring words. */
public enum class Gender {
    MASCULINE,
    FEMININE;

    public companion object {
        /** Parses `masculine`/`m` or `feminine`/`f` (case-insensitive); anything else is null. */
        public fun fromLabel(raw: String?): Gender? = when (raw?.trim()?.lowercase()) {
            "masculine", "m" -> MASCULINE
            "feminine", "f" -> FEMININE
            else -> null
        }
    }
}

/**
 * Adjusts the word that precedes a restored glossary term so it agrees with the term's [Gender].
 * ML Kit only sees a token, so it picks agreement for an unknown noun; this corrects it.
 */
public fun interface GenderAgreement {
    /** Returns [word] adjusted for [gender], or [word] unchanged if it is not a gendered form. */
    public fun adjustPrecedingWord(word: String, gender: Gender): String

    public companion object {
        public val NONE: GenderAgreement = GenderAgreement { word, _ -> word }
    }
}
