package io.github.mlkitglossary.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitationFixesTest {

    private val protector = PlaceholderProtector()

    @Test fun pluralsDoNotMatchByDefault() {
        val g = Glossary(mapOf("loan" to "लोन"))
        assertEquals("loans", protector.protect("loans", g).text)
    }

    @Test fun pluralsAndPossessivesMatchWhenEnabled() {
        val g = Glossary.build { term("loan", "लोन"); term("box", "डिब्बा"); matchPlurals() }
        val p = protector.protect("loans, loan's, Loans, boxes", g)
        assertEquals("GLS0Q, GLS1Q, GLS2Q, GLS3Q", p.text)
        assertEquals(listOf("लोन", "लोन", "लोन", "डिब्बा"), p.slots.map { it.output })
    }

    @Test fun pluralMatchingStillRespectsWordBoundaries() {
        val g = Glossary.build { term("loan", "लोन"); matchPlurals() }
        assertEquals("loaner loanss", protector.protect("loaner loanss", g).text)
    }

    @Test fun exactLongerTermStillBeatsPluralOfShorter() {
        val g = Glossary.build { term("loan", "लोन"); term("loans", "ऋण"); matchPlurals() }
        val p = protector.protect("loans", g)
        assertEquals("ऋण", p.slots.single().output)
    }

    @Test fun urlsEmailsTagsAndFormatArgsArePreserved() {
        val p = PlaceholderProtector(PreservePatterns.ALL)
        val src = "Hi %1\$s, see https://a.example/x?y=1 or mail me@x.com <b>now</b> {name} %d"
        val prot = p.protect(src, Glossary.EMPTY)
        assertEquals("Hi GLS0Q, see GLS1Q or mail GLS2Q GLS3Q now GLS4Q GLS5Q GLS6Q", prot.text)
        assertEquals(src, p.restore(prot.text, prot).text)
    }

    @Test fun formatArgsDoNotMatchPlainPercent() {
        val p = PlaceholderProtector(listOf(PreservePatterns.FORMAT_ARGS))
        assertEquals("100% sure", p.protect("100% sure", Glossary.EMPTY).text)
    }

    @Test fun lostGlossaryTokenRetriesWithNumbersOnlyBeforePlain() = runTest {
        val calls = mutableListOf<String>()
        val engine = TranslationEngine { t ->
            calls += t
            when {
                t.contains("GLS1Q") -> "dropped"
                t.contains("GLS0Q") -> "भुगतान GLS0Q"
                else -> "plain:$t"
            }
        }
        val g = Glossary(mapOf("loan" to "लोन"))
        val out = GlossaryTranslationPipeline(engine, g).translate("Pay 15000 loan")
        assertTrue(out.isSuccess)
        assertEquals("भुगतान 15000", out.getOrThrow())
        assertEquals(2, calls.size)
        assertEquals("Pay GLS0Q loan", calls[1])
    }

    @Test fun fallsBackToPlainWhenNumbersOnlyRetryAlsoFails() = runTest {
        val engine = TranslationEngine { t -> if (t.contains("GLS")) "dropped" else "plain:$t" }
        val g = Glossary(mapOf("loan" to "लोन"))
        assertEquals("plain:Pay 15000 loan", GlossaryTranslationPipeline(engine, g).translate("Pay 15000 loan").getOrThrow())
    }

    @Test fun glossaryOnlyTextGoesStraightToPlainFallback() = runTest {
        val engine = TranslationEngine { t -> if (t.contains("GLS")) "dropped" else "plain:$t" }
        val g = Glossary(mapOf("loan" to "लोन"))
        assertEquals("plain:Pay loan", GlossaryTranslationPipeline(engine, g).translate("Pay loan").getOrThrow())
    }
}
