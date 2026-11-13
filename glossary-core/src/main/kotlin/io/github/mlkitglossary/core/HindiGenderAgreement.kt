package io.github.mlkitglossary.core

/**
 * Fixes Hindi possessives and common adjectives directly before a restored noun,
 * e.g. `आपका` + feminine noun -> `आपकी`. Only the word immediately preceding the noun is touched.
 * Extend with [extraPairs] (masculine form to feminine form).
 */
public class HindiGenderAgreement(extraPairs: Map<String, String> = emptyMap()) : GenderAgreement {

    private val toFeminine: Map<String, String>
    private val toMasculine: Map<String, String>

    init {
        val pairs = DEFAULT_PAIRS + extraPairs
        toFeminine = pairs
        toMasculine = pairs.entries.associate { (m, f) -> f to m }
    }

    override fun adjustPrecedingWord(word: String, gender: Gender): String = when (gender) {
        Gender.FEMININE -> toFeminine[word] ?: word
        Gender.MASCULINE -> toMasculine[word] ?: word
    }

    public companion object {
        // Masculine singular -> feminine. "के" (oblique) is intentionally left alone: it is also plural.
        public val DEFAULT_PAIRS: Map<String, String> = mapOf(
            "का" to "की",
            "आपका" to "आपकी",
            "मेरा" to "मेरी",
            "हमारा" to "हमारी",
            "तुम्हारा" to "तुम्हारी",
            "उसका" to "उसकी",
            "इसका" to "इसकी",
            "उनका" to "उनकी",
            "नया" to "नई",
            "पुराना" to "पुरानी",
            "अगला" to "अगली",
            "पिछला" to "पिछली",
            "पूरा" to "पूरी",
            "अधूरा" to "अधूरी",
            "दूसरा" to "दूसरी",
            "पहला" to "पहली",
            "तीसरा" to "तीसरी",
        )
    }
}
