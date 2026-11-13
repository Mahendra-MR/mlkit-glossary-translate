package io.github.mlkitglossary.core

public enum class SlotKind {
    /** A glossary term replaced by its preferred output. */
    GLOSSARY,

    /** Text (numbers, ids...) that must come out of translation unchanged. */
    PRESERVED,
}

/** One protected span of the source text. [output] is what replaces its token after translation. */
public data class Slot(
    val index: Int,
    val kind: SlotKind,
    val original: String,
    val output: String,
    val gender: Gender? = null,
    /** The token was padded with a space on this side because it touched a letter or digit. */
    val padBefore: Boolean = false,
    val padAfter: Boolean = false,
)

public class ProtectedText internal constructor(
    /** Text to hand to the translation engine, with tokens in place of protected spans. */
    public val text: String,
    public val slots: List<Slot>,
)

public class RestoreResult internal constructor(
    public val text: String,
    /** Slots whose token did not survive translation. */
    public val missing: List<Slot>,
    /** Token-shaped strings in the output that do not map to any slot. */
    public val unrecognizedTokens: Int,
) {
    public val isClean: Boolean get() = missing.isEmpty() && unrecognizedTokens == 0
}
