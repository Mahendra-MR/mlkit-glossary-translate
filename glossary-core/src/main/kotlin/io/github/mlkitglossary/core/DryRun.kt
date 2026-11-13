package io.github.mlkitglossary.core

public enum class DryRunOutcome {
    /** Nothing in the text matched the glossary or preserve patterns; plain translation. */
    NOTHING_TO_PROTECT,

    /** The text consisted only of protected spans; the engine was not needed. */
    ONLY_PROTECTED_TEXT,

    /** All placeholders survived translation and were restored. */
    CLEAN,

    /** Some placeholders were lost; the restored text was returned anyway ([FallbackStrategy.PARTIAL_RESULT]). */
    PARTIAL_RESULT,

    /** Glossary placeholders were lost; retried protecting only numbers and patterns. */
    RETRIED_WITHOUT_GLOSSARY,

    /** Placeholders were lost and the retry failed; returned the unprotected translation. */
    FELL_BACK_TO_PLAIN,

    /** The engine threw. */
    FAILED,
}

public class DryRunEntry internal constructor(
    public val source: String,
    public val outcome: DryRunOutcome,
    /** Text sent to the engine, with placeholders. Null if nothing was sent. */
    public val protectedText: String?,
    /** What the engine returned for [protectedText]. */
    public val rawTranslation: String?,
    public val output: String?,
    /** Original source spans whose placeholder did not come back. */
    public val missingTerms: List<String>,
    public val unrecognizedTokens: Int,
    public val error: String?,
)

public class DryRunReport internal constructor(public val entries: List<DryRunEntry>) {

    private val protectedEntries get() = entries.filter {
        it.outcome in setOf(
            DryRunOutcome.CLEAN, DryRunOutcome.PARTIAL_RESULT,
            DryRunOutcome.RETRIED_WITHOUT_GLOSSARY, DryRunOutcome.FELL_BACK_TO_PLAIN,
        )
    }

    /** Share of protected sentences whose placeholders all survived (1.0 when none were protected). */
    public val survivalRate: Double
        get() = protectedEntries.let { p -> if (p.isEmpty()) 1.0 else p.count { it.outcome == DryRunOutcome.CLEAN }.toDouble() / p.size }

    public fun count(outcome: DryRunOutcome): Int = entries.count { it.outcome == outcome }

    /** Human-readable report; includes the sentences you passed in, so do not ship it to analytics. */
    public fun format(): String = buildString {
        appendLine("Dry run: ${entries.size} texts, placeholder survival ${(survivalRate * 100).toInt()}%")
        DryRunOutcome.entries.filter { count(it) > 0 }.forEach { appendLine("  ${it.name}: ${count(it)}") }
        entries.forEachIndexed { i, e ->
            appendLine()
            appendLine("#${i + 1} [${e.outcome}]")
            appendLine("  source : ${e.source}")
            e.protectedText?.let { appendLine("  sent   : $it") }
            e.rawTranslation?.let { appendLine("  engine : $it") }
            e.output?.let { appendLine("  output : $it") }
            if (e.missingTerms.isNotEmpty()) appendLine("  lost   : ${e.missingTerms.joinToString(", ")}")
            if (e.unrecognizedTokens > 0) appendLine("  unknown tokens: ${e.unrecognizedTokens}")
            e.error?.let { appendLine("  error  : $it") }
        }
    }
}

internal class Execution(
    val output: String,
    val outcome: DryRunOutcome,
    val protectedText: String?,
    val rawTranslation: String?,
    val missing: List<String>,
    val unrecognizedTokens: Int,
)
