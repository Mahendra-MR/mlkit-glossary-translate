package io.github.mlkitglossary

import io.github.mlkitglossary.core.Gender
import io.github.mlkitglossary.core.Glossary
import io.github.mlkitglossary.core.GlossaryEntry
import org.json.JSONObject

/**
 * Parses a glossary from JSON, e.g. from Firebase Remote Config or a bundled asset:
 *
 * ```json
 * { "Loan": "लोन", "EMI": { "to": "किस्त", "gender": "feminine" } }
 * ```
 * `gender` is `masculine`/`m` or `feminine`/`f` (case-insensitive) and optional.
 *
 * @throws org.json.JSONException on malformed input
 */
public object GlossaryJson {
    public fun parse(json: String): Glossary {
        val root = JSONObject(json)
        val entries = LinkedHashMap<String, GlossaryEntry>()
        for (key in root.keys()) {
            entries[key] = when (val value = root.get(key)) {
                is JSONObject -> GlossaryEntry(value.getString("to"), parseGender(value.optString("gender")))
                else -> GlossaryEntry(value.toString())
            }
        }
        return Glossary.of(entries)
    }

    private fun parseGender(raw: String): Gender? = when (raw.trim().lowercase()) {
        "masculine", "m" -> Gender.MASCULINE
        "feminine", "f" -> Gender.FEMININE
        else -> null
    }
}
