package io.github.mlkitglossary.core

/** Turns spreadsheet-like rows into a [Glossary]; shared by the CSV and Excel loaders. */
internal object GlossaryTable {
    private val SOURCE_HEADERS = setOf("source", "term", "from", "english", "en", "key", "word")
    private val REPLACEMENT_HEADERS = setOf(
        "replacement", "target", "to", "output", "translation", "value", "hindi", "hi",
    )
    private val GENDER_HEADERS = setOf("gender", "g")

    fun toGlossary(rows: List<List<String>>, matchPlurals: Boolean): Glossary {
        var sourceCol = 0
        var replacementCol = 1
        var genderCol = 2
        var start = 0

        val header = rows.firstOrNull()?.map { it.trim().lowercase() }
        if (header != null && header.any { it in SOURCE_HEADERS }) {
            start = 1
            sourceCol = header.indexOfFirst { it in SOURCE_HEADERS }
            replacementCol = header.indexOfFirst { it in REPLACEMENT_HEADERS }.takeIf { it >= 0 }
                ?: (0..header.lastIndex).first { it != sourceCol }
            genderCol = header.indexOfFirst { it in GENDER_HEADERS }
        }

        val entries = LinkedHashMap<String, GlossaryEntry>()
        for (i in start until rows.size) {
            val row = rows[i]
            val source = row.getOrNull(sourceCol)?.trim().orEmpty()
            if (source.isEmpty() && row.all { it.isBlank() }) continue
            if (source.startsWith("#")) continue
            require(source.isNotEmpty()) { "Row ${i + 1}: missing source term" }
            val replacement = row.getOrNull(replacementCol)?.trim().orEmpty()
            require(replacement.isNotEmpty()) { "Row ${i + 1}: missing replacement" }
            val gender = if (genderCol >= 0) Gender.fromLabel(row.getOrNull(genderCol)) else null
            entries[source] = GlossaryEntry(replacement, gender)
        }
        return Glossary.of(entries, matchPlurals)
    }
}
