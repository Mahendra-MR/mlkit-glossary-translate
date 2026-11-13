package io.github.mlkitglossary.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderProtectorTest {

    private val protector = PlaceholderProtector()

    private fun glossary(vararg pairs: Pair<String, String>) = Glossary(mapOf(*pairs))

    private fun roundTrip(text: String, g: Glossary): String {
        val p = protector.protect(text, g)
        return protector.restore(p.text, p).text
    }

    @Test fun emptyGlossaryLeavesTextUntouched() {
        val p = protector.protect("Your loan is approved", Glossary.EMPTY)
        assertEquals("Your loan is approved", p.text)
        assertTrue(p.slots.isEmpty())
    }

    @Test fun emptyInput() {
        assertEquals("", protector.protect("", glossary("a" to "b")).text)
    }

    @Test fun replacesTermWithToken() {
        val p = protector.protect("Your Loan is approved", glossary("loan" to "लोन"))
        assertEquals("Your GLS0Q is approved", p.text)
        assertEquals("लोन", p.slots.single().output)
        assertEquals("Loan", p.slots.single().original)
    }

    @Test fun matchingIsCaseInsensitive() {
        val g = glossary("EMI" to "ईएमआई")
        assertEquals("ईएमआई due, ईएमआई paid, ईएमआई!", roundTrip("emi due, Emi paid, EMI!", g))
    }

    @Test fun wordBoundariesAreRespected() {
        val p = protector.protect("Loans and unloaded and loan", glossary("loan" to "लोन"))
        assertEquals("Loans and unloaded and GLS0Q", p.text)
    }

    @Test fun longestMatchWins() {
        val g = glossary("Bank" to "बैंक", "Bank Account" to "बैंक खाता")
        val p = protector.protect("Verify your Bank Account and Bank", g)
        assertEquals("Verify your GLS0Q and GLS1Q", p.text)
        assertEquals("बैंक खाता", p.slots[0].output)
        assertEquals("बैंक", p.slots[1].output)
    }

    @Test fun multiWordTermMatchesAnyWhitespace() {
        val p = protector.protect("bank   account\nnumber", glossary("bank account" to "खाता"))
        assertEquals("GLS0Q\nnumber", p.text)
    }

    @Test fun repeatedTermsGetSeparateTokensAndAllRestore() {
        val g = glossary("OTP" to "OTP")
        val p = protector.protect("OTP sent. Enter OTP", g)
        assertEquals("GLS0Q sent. Enter GLS1Q", p.text)
        assertEquals("OTP भेजा गया। OTP दर्ज करें", protector.restore("GLS0Q भेजा गया। GLS1Q दर्ज करें", p).text)
    }

    @Test fun adjacentPunctuationStillMatches() {
        val g = glossary("loan" to "लोन")
        assertEquals("(लोन), \"लोन\"; लोन.", roundTrip("(loan), \"loan\"; loan.", g))
    }

    @Test fun termWithSymbolsMatches() {
        val g = glossary("e-KYC" to "ई-केवाईसी", "C++" to "सीपीपी")
        assertEquals("ई-केवाईसी and सीपीपी", roundTrip("e-KYC and C++", g))
    }

    @Test fun regexMetacharactersInTermsAreEscaped() {
        val g = glossary("a.b" to "X")
        assertEquals("aXb X", roundTrip("aXb a.b", g))
        assertEquals("axb", protector.protect("axb", g).text)
    }

    @Test fun blankKeysIgnored() {
        assertTrue(Glossary(mapOf("" to "x", "  " to "y")).isEmpty())
    }

    @Test fun numbersAreProtectedByDefault() {
        val p = protector.protect("Account XXXX1234 and OTP 482913", Glossary.EMPTY)
        assertEquals("Account GLS0Q and OTP GLS1Q", p.text)
        assertEquals(SlotKind.PRESERVED, p.slots[0].kind)
        assertEquals("XXXX1234", p.slots[0].output)
    }

    @Test fun shortNumbersAreNotProtected() {
        assertEquals("EMI 123 due", protector.protect("EMI 123 due", Glossary.EMPTY).text)
    }

    @Test fun glossaryAndNumbersTogether() {
        val g = glossary("EMI" to "ईएमआई")
        val p = protector.protect("EMI of 15000 due", g)
        assertEquals("GLS0Q of GLS1Q due", p.text)
        assertEquals("ईएमआई 15000 देय", protector.restore("GLS0Q GLS1Q देय", p).text)
    }

    @Test fun overlappingGlossaryAndNumberKeepsEarlierLonger() {
        val g = glossary("ref 12345" to "संदर्भ")
        val p = protector.protect("ref 12345", g)
        assertEquals(1, p.slots.size)
        assertEquals(SlotKind.GLOSSARY, p.slots[0].kind)
    }

    @Test fun customPreservePatternsReplaceDefault() {
        val custom = PlaceholderProtector(listOf(Regex("[A-Z]{5}\\d{4}[A-Z]")))
        val p = custom.protect("PAN ABCDE1234F and 123456", Glossary.EMPTY)
        assertEquals("PAN GLS0Q and 123456", p.text)
    }

    @Test fun inputThatAlreadyLooksLikeATokenIsNotProtected() {
        val p = protector.protect("code GLS0Q loan", glossary("loan" to "लोन"))
        assertTrue(p.slots.isEmpty())
        assertEquals("code GLS0Q loan", p.text)
    }

    @Test fun restoreToleratesCaseAndSpacing() {
        val g = glossary("loan" to "लोन")
        val p = protector.protect("loan", g)
        listOf("gls0q", "GLS 0 Q", "Gls0Q", "GLS-0Q", "GLS_0Q", "G L S 0 Q").forEach {
            assertEquals(it, "लोन", protector.restore(it, p).text)
        }
    }

    @Test fun restoreHandlesDevanagariDigitsAndAdjacentScript() {
        val p = protector.protect("loan", glossary("loan" to "लोन"))
        assertEquals("आपका लोनका", protector.restore("आपका GLS०Qका", p).text)
    }

    @Test fun restoreDoesNotConfuseTokenIndexes() {
        val g = Glossary((0..11).associate { "t$it" to "R$it" })
        val text = (0..11).joinToString(" ") { "t$it" }
        assertEquals((0..11).joinToString(" ") { "R$it" }, roundTrip(text, g))
    }

    @Test fun droppedTokenIsReported() {
        val p = protector.protect("loan and emi", glossary("loan" to "लोन", "emi" to "ईएमआई"))
        val r = protector.restore("GLS1Q और", p)
        assertFalse(r.isClean)
        assertEquals(listOf("loan"), r.missing.map { it.original })
    }

    @Test fun duplicatedTokenIsRestoredEverywhere() {
        val p = protector.protect("loan", glossary("loan" to "लोन"))
        val r = protector.restore("GLS0Q GLS0Q", p)
        assertTrue(r.isClean)
        assertEquals("लोन लोन", r.text)
    }

    @Test fun hallucinatedTokenIsReported() {
        val p = protector.protect("loan", glossary("loan" to "लोन"))
        val r = protector.restore("GLS0Q GLS7Q", p)
        assertEquals(1, r.unrecognizedTokens)
        assertFalse(r.isClean)
    }

    @Test fun replacementContainingRegexReplacementCharsIsLiteral() {
        val p = protector.protect("price", glossary("price" to "\$1 \\0"))
        assertEquals("\$1 \\0", protector.restore("GLS0Q", p).text)
    }

    @Test fun onlyTokensMeansNothingToTranslate() {
        val p = protector.protect("EMI 15000", glossary("EMI" to "x"))
        assertFalse(protector.hasTranslatableContent(p))
        assertTrue(protector.hasTranslatableContent(protector.protect("Pay EMI", glossary("EMI" to "x"))))
    }
}
