package io.github.mlkitglossary.core

import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Loads a glossary from the first worksheet of an `.xlsx` file, using the same column layout as
 * [GlossaryCsv]. Needs only `java.util.zip`, so it works on Android and the JVM without extra dependencies.
 * Formulas are read as their cached values; dates and rich formatting are not interpreted.
 *
 * The stream is read fully but not closed.
 *
 * @throws IllegalArgumentException if the stream is not a readable workbook or a row is incomplete.
 */
public object GlossaryXlsx {
    private val SI = Regex("<si\\b[^>]*>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
    private val T = Regex("<t\\b[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
    private val ROW = Regex("<row\\b[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
    private val CELL = Regex("<c\\b([^>]*?)(?:/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL)
    private val ATTR_R = Regex("\\br=\"([A-Z]+)(\\d+)\"")
    private val ATTR_T = Regex("\\bt=\"(\\w+)\"")
    private val V = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)

    public fun parse(input: InputStream, matchPlurals: Boolean = false): Glossary {
        val files = readEntries(input)
        val sheetName = files.keys.filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }.minOrNull()
            ?: throw IllegalArgumentException("Not an .xlsx workbook: no worksheet found")
        val shared = files["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        return GlossaryTable.toGlossary(parseSheet(files.getValue(sheetName), shared), matchPlurals)
    }

    private fun readEntries(input: InputStream): Map<String, String> {
        val out = HashMap<String, String>()
        try {
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.startsWith("xl/") && entry.name.endsWith(".xml")) {
                    out[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        } catch (e: java.io.IOException) {
            throw IllegalArgumentException("Not a readable .xlsx file", e)
        }
        return out
    }

    private fun parseSharedStrings(xml: String): List<String> =
        SI.findAll(xml).map { si -> T.findAll(si.groupValues[1]).joinToString("") { unescape(it.groupValues[1]) } }.toList()

    private fun parseSheet(xml: String, shared: List<String>): List<List<String>> {
        val rows = java.util.TreeMap<Int, MutableMap<Int, String>>()
        for (rowMatch in ROW.findAll(xml)) {
            for (cell in CELL.findAll(rowMatch.groupValues[1])) {
                val attrs = cell.groupValues[1]
                val body = cell.groupValues[2]
                val ref = ATTR_R.find(attrs) ?: continue
                val col = columnIndex(ref.groupValues[1])
                val rowNum = ref.groupValues[2].toInt()
                val type = ATTR_T.find(attrs)?.groupValues?.get(1)
                val value = when (type) {
                    "s" -> V.find(body)?.groupValues?.get(1)?.toIntOrNull()?.let { shared.getOrNull(it) }
                    "inlineStr" -> T.findAll(body).joinToString("") { unescape(it.groupValues[1]) }
                    else -> V.find(body)?.groupValues?.get(1)?.let { unescape(it) }
                } ?: continue
                rows.getOrPut(rowNum) { HashMap() }[col] = value
            }
        }
        if (rows.isEmpty()) return emptyList()
        val last = rows.lastKey()
        return (1..last).map { r ->
            val cells = rows[r] ?: return@map emptyList()
            (0..(cells.keys.maxOrNull() ?: -1)).map { cells[it].orEmpty() }
        }
    }

    private fun columnIndex(letters: String): Int = letters.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1

    private fun unescape(s: String): String = s
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}
