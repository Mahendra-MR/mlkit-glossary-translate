package io.github.mlkitglossary.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DryRunAndAnalysisTest {
    private val glossary = Glossary.build {
        term("loan", "लोन", Gender.MASCULINE)
        term("EMI", "EMI")
        term("mandate", "मैंडेट")
    }

    @Test fun analysisListsMatchesAndUnusedTerms() {
        val a = glossary.analyze(listOf("Pay your loan", "Nothing here", "EMI of 15000"))
        assertEquals(3, a.sentences.size)
        assertEquals(listOf("mandate"), a.unusedTerms)
        assertEquals("Pay your GLS0Q", a.sentences[0].protectedText)
        assertEquals(2, a.sentences[2].matches.size)
        assertTrue(a.format().contains("Never matched: mandate"))
    }

    @Test fun analysisCountsPluralFormsAsUsed() {
        val g = Glossary.build { term("loan", "लोन"); matchPlurals() }
        assertTrue(g.analyze(listOf("two loans")).unusedTerms.isEmpty())
    }

    @Test fun duplicateKeysAreReported() {
        val g = Glossary(mapOf("Loan" to "a", "loan" to "b", "EMI" to "c"))
        assertEquals(listOf("loan"), g.duplicateKeys)
        assertTrue(Glossary(mapOf("Loan" to "a")).duplicateKeys.isEmpty())
        assertTrue(g.analyze(emptyList()).format().contains("Duplicate keys"))
    }

    @Test fun dryRunReportsEachOutcome() = runTest {
        val engine = TranslationEngine { t ->
            when {
                t.contains("now") -> "dropped"
                t.contains("GLS0Q") -> "T:$t"
                else -> "plain:$t"
            }
        }
        val g = Glossary(mapOf("loan" to "लोन", "dropme" to "x"))
        val report = GlossaryTranslationPipeline(engine, g).dryRun(
            listOf("Pay loan", "Hello world", "loan", "Pay dropme now"),
        )
        assertEquals(DryRunOutcome.CLEAN, report.entries[0].outcome)
        assertEquals("T:Pay GLS0Q", report.entries[0].rawTranslation)
        assertEquals("T:Pay लोन", report.entries[0].output)
        assertEquals(DryRunOutcome.NOTHING_TO_PROTECT, report.entries[1].outcome)
        assertEquals(DryRunOutcome.ONLY_PROTECTED_TEXT, report.entries[2].outcome)
        assertEquals(DryRunOutcome.FELL_BACK_TO_PLAIN, report.entries[3].outcome)
        assertEquals(listOf("dropme"), report.entries[3].missingTerms)
        assertEquals(0.5, report.survivalRate, 0.0001)
        assertTrue(report.format().contains("placeholder survival 50%"))
    }

    @Test fun dryRunCapturesFailuresWithoutThrowing() = runTest {
        val engine = TranslationEngine { error("boom") }
        val report = GlossaryTranslationPipeline(engine, glossary).dryRun(listOf("Pay loan"))
        assertEquals(DryRunOutcome.FAILED, report.entries.single().outcome)
        assertEquals("IllegalStateException", report.entries.single().error)
    }

    @Test fun dryRunWithNoProtectedSentencesHasFullSurvival() = runTest {
        val report = GlossaryTranslationPipeline({ it }, glossary).dryRun(listOf("Hello"))
        assertEquals(1.0, report.survivalRate, 0.0)
    }

    @Test fun retryOutcomeIsReported() = runTest {
        val engine = TranslationEngine { t -> if (t.contains("GLS1Q")) "dropped" else "ok $t" }
        val report = GlossaryTranslationPipeline(engine, Glossary(mapOf("loan" to "लोन"))).dryRun(listOf("Pay 15000 loan"))
        assertEquals(DryRunOutcome.RETRIED_WITHOUT_GLOSSARY, report.entries.single().outcome)
    }

    @Test fun genderFromLabel() {
        assertEquals(Gender.FEMININE, Gender.fromLabel(" F "))
        assertEquals(null, Gender.fromLabel("x"))
        assertEquals(null, Gender.fromLabel(null))
    }
}
