package io.github.mlkitglossary.core

/**
 * Loads a glossary from CSV text (Excel/Google Sheets "Save as CSV UTF-8").
 *
 * Columns: `source, replacement, gender` (gender optional: `m`/`f`/`masculine`/`feminine`).
 * An optional header row is detected (`source`/`term`/`english`, `replacement`/`target`/`hindi`, `gender`)
 * and may reorder the columns. Lines starting with `#` and blank lines are ignored.
 * The delimiter (`,` `;` or tab) is detected from the first line unless [delimiter] is given.
 * Quoted fields, doubled quotes and a UTF-8 BOM are handled.
 *
 * @throws IllegalArgumentException if a row has a source without a replacement.
 */
public object GlossaryCsv {
    public fun parse(csv: String, matchPlurals: Boolean = false, delimiter: Char? = null): Glossary {
        val text = csv.removePrefix("\uFEFF")
        val rows = parseRows(text, delimiter ?: detectDelimiter(text))
        return GlossaryTable.toGlossary(rows, matchPlurals)
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: return ','
        var inQuotes = false
        val counts = mutableMapOf(',' to 0, ';' to 0, '\t' to 0)
        for (c in firstLine) {
            if (c == '"') inQuotes = !inQuotes
            else if (!inQuotes && c in counts) counts[c] = counts.getValue(c) + 1
        }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    private fun parseRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        fun endField() { row.add(field.toString()); field.setLength(0) }
        fun endRow() { endField(); rows.add(row); row = mutableListOf() }
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == delimiter -> endField()
                !inQuotes && c == '\r' -> { if (text.getOrNull(i + 1) == '\n') i++; endRow() }
                !inQuotes && c == '\n' -> endRow()
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
