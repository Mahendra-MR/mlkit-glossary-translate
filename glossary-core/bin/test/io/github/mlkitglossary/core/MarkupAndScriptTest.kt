package io.github.mlkitglossary.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupAndScriptTest {

    private val protector = PlaceholderProtector()

    // ---- unspaced source languages ----

    @Test fun chineseTermMatchesInsideRun() {
        val g = Glossary(mapOf("贷款" to "loan"))
        val p = protector.protect("您的贷款已批准", g)
        assertEquals("您的GLS0Q已批准".replace("GLS0Q", " GLS0Q "), p.text)
        assertEquals("loan", p.slots.single().output)
    }

    @Test fun japaneseKatakanaTermMatchesInsideCompound() {
        val g = Glossary(mapOf("ローン" to "लोन"))
        assertEquals(1, protector.protect("ローン契約を確認", g).slots.size)
    }

    @Test fun thaiTermMatches() {
        val g = Glossary(mapOf("สินเชื่อ" to "loan"))
        assertEquals(1, protector.protect("สินเชื่อของคุณได้รับอนุมัติ", g).slots.size)
    }

    @Test fun latinTermNextToCjkStillMatches() {
        val g = Glossary(mapOf("EMI" to "EMI"))
        assertEquals(1, protector.protect("使用EMI支付", g).slots.size)
    }

    @Test fun latinWordBoundariesStillHold() {
        val g = Glossary(mapOf("EMI" to "EMI"))
        assertEquals(0, protector.protect("REMIND", g).slots.size)
    }

    @Test fun numbersNextToCjkArePreserved() {
        val p = protector.protect("金额15000元", Glossary.EMPTY)
        assertEquals("15000", p.slots.single().original)
    }

    @Test fun cjkTermRoundTrips() {
        val g = Glossary(mapOf("贷款" to "loan"))
        val p = protector.protect("您的贷款已批准", g)
        assertEquals("您的loan已批准", protector.restore("您的 GLS0Q 已批准", p).text)
    }

    // ---- markup ----

    @Test fun validatorAcceptsSameStructure() {
        assertTrue(MarkupValidator.preservesStructure("a <b>x</b> c", "ए <b>एक्स</b> सी"))
    }

    @Test fun validatorRejectsSwappedTags() {
        assertFalse(MarkupValidator.preservesStructure("a <b>x</b> c", "ए </b>एक्स<b> सी"))
    }

    @Test fun validatorRejectsDroppedTag() {
        assertFalse(MarkupValidator.preservesStructure("a <b>x</b> c", "ए <b>एक्स सी"))
    }

    @Test fun validatorIgnoresVoidTags() {
        assertTrue(MarkupValidator.preservesStructure("a<br>b", "ए<br/>बी"))
    }

    @Test fun markupTranslatedInContextWhenTagsSurvive() = runTest {
        val engine = TranslationEngine { it.replace("Your", "आपका").replace("is approved", "स्वीकृत है") }
        val g = Glossary(mapOf("loan" to "लोन"))
        val out = GlossaryTranslationPipeline(engine, g).translateMarkup("Your <b>loan</b> is approved")
        assertEquals("आपका <b>लोन</b> स्वीकृत है", out.getOrThrow())
    }

    @Test fun markupFallsBackToRunsWhenTagsGetSwapped() = runTest {
        val calls = mutableListOf<String>()
        val engine = TranslationEngine { t ->
            calls += t
            if (t.contains("GLS")) {
                t.replace("GLS0Q", "TMP").replace("GLS1Q", "GLS0Q").replace("TMP", "GLS1Q")
            } else {
                "T(${t})"
            }
        }
        val out = GlossaryTranslationPipeline(engine).translateMarkup("Pay <b>now</b> please")
        assertEquals("T(Pay) <b>T(now)</b> T(please)", out.getOrThrow())
    }

    @Test fun entitiesArePreserved() = runTest {
        val engine = TranslationEngine { it.replace("Terms", "शर्तें") }
        val out = GlossaryTranslationPipeline(engine).translateMarkup("Terms &amp; conditions")
        assertEquals("शर्तें &amp; conditions", out.getOrThrow())
    }

    @Test fun plainTextWithoutMarkupGoesThroughNormalPath() = runTest {
        val engine = TranslationEngine { "T:$it" }
        assertEquals("T:hello", GlossaryTranslationPipeline(engine).translateMarkup("hello").getOrThrow())
    }

    @Test fun markupOnlyTextNeedsNoEngine() = runTest {
        val engine = TranslationEngine { error("must not be called") }
        assertEquals("<br>", GlossaryTranslationPipeline(engine).translateMarkup("<br>").getOrThrow())
    }
}
