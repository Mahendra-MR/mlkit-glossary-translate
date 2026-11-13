package io.github.mlkitglossary

import io.github.mlkitglossary.core.Gender
import io.github.mlkitglossary.core.PlaceholderProtector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONException

class GlossaryJsonTest {
    @Test fun parsesPlainAndGenderedEntries() {
        val g = GlossaryJson.parse("""{"Loan":"लोन","EMI":{"to":"किस्त","gender":"Feminine"}}""")
        val p = PlaceholderProtector().protect("Loan EMI", g)
        assertEquals("लोन", p.slots[0].output)
        assertEquals(null, p.slots[0].gender)
        assertEquals("किस्त", p.slots[1].output)
        assertEquals(Gender.FEMININE, p.slots[1].gender)
    }

    @Test fun emptyObjectGivesEmptyGlossary() = assertTrue(GlossaryJson.parse("{}").isEmpty())

    @Test(expected = JSONException::class)
    fun malformedJsonThrows() {
        GlossaryJson.parse("{not json")
    }
}
