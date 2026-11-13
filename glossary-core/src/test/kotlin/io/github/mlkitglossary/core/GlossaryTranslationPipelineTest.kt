package io.github.mlkitglossary.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryTranslationPipelineTest {

    private class FakeEngine(private val fn: (String) -> String) : TranslationEngine {
        val calls = mutableListOf<String>()
        override suspend fun translate(text: String): String {
            calls += text
            return fn(text)
        }
    }

    private val glossary = Glossary(mapOf("loan" to "लोन", "EMI" to "ईएमआई"))

    @Test fun appliesGlossaryAroundEngine() = runTest {
        val engine = FakeEngine { it.replace("Your", "आपका").replace("is approved", "स्वीकृत है") }
        val out = GlossaryTranslationPipeline(engine, glossary).translate("Your loan is approved")
        assertEquals("आपका लोन स्वीकृत है", out.getOrThrow())
        assertEquals(listOf("Your GLS0Q is approved"), engine.calls)
    }

    @Test fun emptyGlossaryPassesTextStraightThrough() = runTest {
        val engine = FakeEngine { "T:$it" }
        assertEquals("T:hello", GlossaryTranslationPipeline(engine).translate("hello").getOrThrow())
    }

    @Test fun blankTextSkipsEngine() = runTest {
        val engine = FakeEngine { error("must not be called") }
        assertEquals("  ", GlossaryTranslationPipeline(engine, glossary).translate("  ").getOrThrow())
    }

    @Test fun manglingTokenStillRestores() = runTest {
        val engine = FakeEngine { it.replace("GLS0Q", "gls 0 q") }
        val out = GlossaryTranslationPipeline(engine, glossary).translate("Pay loan now")
        assertEquals("Pay लोन now", out.getOrThrow())
    }

    @Test fun droppedTokenFallsBackToPlainTranslation() = runTest {
        val engine = FakeEngine { if (it.contains("GLS")) "dropped" else "plain:$it" }
        val out = GlossaryTranslationPipeline(engine, glossary).translate("Pay loan now")
        assertEquals("plain:Pay loan now", out.getOrThrow())
        assertEquals(2, engine.calls.size)
    }

    @Test fun partialResultStrategyKeepsRestoredText() = runTest {
        val engine = FakeEngine { "भुगतान GLS0Q" }
        val p = GlossaryTranslationPipeline(engine, glossary, fallback = FallbackStrategy.PARTIAL_RESULT)
        assertEquals("भुगतान लोन", p.translate("Pay loan and EMI").getOrThrow())
        assertEquals(1, engine.calls.size)
    }

    @Test fun textThatIsOnlyAGlossaryTermSkipsEngine() = runTest {
        val engine = FakeEngine { error("must not be called") }
        assertEquals("लोन", GlossaryTranslationPipeline(engine, glossary).translate("Loan").getOrThrow())
    }

    @Test fun numbersSurviveTranslation() = runTest {
        val engine = FakeEngine { it.replace("OTP", "ओटीपी") }
        val out = GlossaryTranslationPipeline(engine).translate("OTP 482913 sent to XXXX1234")
        assertEquals("ओटीपी 482913 sent to XXXX1234", out.getOrThrow())
    }

    @Test fun glossaryCanBeSwappedAtRuntime() = runTest {
        val engine = FakeEngine { it }
        val pipeline = GlossaryTranslationPipeline(engine, Glossary.EMPTY)
        assertEquals("Pay loan", pipeline.translate("Pay loan").getOrThrow())
        pipeline.glossary = Glossary(mapOf("loan" to "लोन"))
        assertEquals("Pay लोन", pipeline.translate("Pay loan").getOrThrow())
    }

    @Test fun engineFailureBecomesFailureResult() = runTest {
        val engine = FakeEngine { throw IllegalStateException("boom") }
        val r = GlossaryTranslationPipeline(engine, glossary).translate("Pay loan")
        assertTrue(r.isFailure)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() = runTest {
        val engine = FakeEngine { throw CancellationException("cancelled") }
        GlossaryTranslationPipeline(engine, glossary).translate("Pay loan")
    }
}
