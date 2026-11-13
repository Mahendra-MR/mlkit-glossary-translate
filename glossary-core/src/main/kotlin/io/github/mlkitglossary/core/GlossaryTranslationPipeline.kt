package io.github.mlkitglossary.core

import kotlin.coroutines.cancellation.CancellationException

/** Minimal translation backend; the ML Kit module provides the real implementation. */
public fun interface TranslationEngine {
    public suspend fun translate(text: String): String
}

/** What to do when a protected token does not survive translation. */
public enum class FallbackStrategy {
    /** Translate the original text without any protection (never worse than the plain engine). */
    PLAIN_TRANSLATION,

    /** Return the restored text anyway; missing terms are simply absent. */
    PARTIAL_RESULT,
}

public class GlossaryTranslationPipeline(
    private val engine: TranslationEngine,
    glossary: Glossary = Glossary.EMPTY,
    private val protector: PlaceholderProtector = PlaceholderProtector(),
    private val fallback: FallbackStrategy = FallbackStrategy.PLAIN_TRANSLATION,
) {
    @Volatile
    public var glossary: Glossary = glossary

    public suspend fun translate(text: String): Result<String> {
        if (text.isBlank()) return Result.success(text)
        return try {
            Result.success(run(text))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun run(text: String): String {
        val prepared = protector.protect(text, glossary)
        if (prepared.slots.isEmpty()) return engine.translate(text)

        if (!protector.hasTranslatableContent(prepared)) {
            return protector.restore(prepared.text, prepared).text
        }

        val result = protector.restore(engine.translate(prepared.text), prepared)
        if (result.isClean) return result.text
        return when (fallback) {
            FallbackStrategy.PLAIN_TRANSLATION -> engine.translate(text)
            FallbackStrategy.PARTIAL_RESULT -> result.text
        }
    }
}
