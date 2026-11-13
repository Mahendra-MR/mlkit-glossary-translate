package io.github.mlkitglossary.core

import kotlin.coroutines.cancellation.CancellationException

/** Minimal translation backend; the ML Kit module provides the real implementation. */
public fun interface TranslationEngine {
    public suspend fun translate(text: String): String
}

/** What to do when a protected token does not survive translation. */
public enum class FallbackStrategy {
    /**
     * Retry protecting only numbers/patterns (so figures stay correct), then, if that also fails,
     * translate the original text unprotected. Never worse than the plain engine.
     */
    PLAIN_TRANSLATION,

    /** Return the restored text anyway; missing terms are simply absent. */
    PARTIAL_RESULT,
}

/**
 * @param cacheSize number of translations to remember (0 disables). The cache is cleared
 * whenever [glossary] is assigned.
 */
public class GlossaryTranslationPipeline(
    private val engine: TranslationEngine,
    glossary: Glossary = Glossary.EMPTY,
    private val protector: PlaceholderProtector = PlaceholderProtector(),
    private val fallback: FallbackStrategy = FallbackStrategy.PLAIN_TRANSLATION,
    cacheSize: Int = 0,
) {
    private val cache: MutableMap<String, String>? = if (cacheSize > 0) {
        object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) = size > cacheSize
        }
    } else {
        null
    }

    @Volatile
    private var current: Glossary = glossary

    public var glossary: Glossary
        get() = current
        set(value) {
            current = value
            cache?.let { synchronized(it) { it.clear() } }
        }

    public suspend fun translate(text: String): Result<String> {
        if (text.isBlank()) return Result.success(text)
        cache?.let { c -> synchronized(c) { c[text] }?.let { return Result.success(it) } }
        val snapshot = current
        return try {
            val out = run(text, snapshot)
            // A glossary swapped mid-flight must not poison the cache with stale output.
            cache?.let { c -> synchronized(c) { if (snapshot === current) c[text] = out } }
            Result.success(out)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translates text that contains HTML tags/entities. Tags are kept as tokens so the whole sentence
     * is translated in context; if the result no longer has well-formed tags (translation reordered
     * them), each text run between tags is translated on its own and the tags are kept in place.
     */
    public suspend fun translateMarkup(text: String): Result<String> {
        if (text.isBlank()) return Result.success(text)
        return try {
            Result.success(runMarkup(text, current))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runMarkup(text: String, glossary: Glossary): String {
        val markup = protector.withExtraPatterns(PreservePatterns.MARKUP)
        val prepared = markup.protect(text, glossary)
        if (prepared.slots.none { it.original.startsWith("<") || it.original.startsWith("&") }) {
            return run(text, glossary)
        }
        if (markup.hasTranslatableContent(prepared)) {
            val result = markup.restore(engine.translate(prepared.text), prepared)
            if (result.isClean && MarkupValidator.preservesStructure(text, result.text)) return result.text
        } else {
            return markup.restore(prepared.text, prepared).text
        }
        return translateRuns(text, glossary)
    }

    private suspend fun translateRuns(text: String, glossary: Glossary): String {
        val sb = StringBuilder()
        var cursor = 0
        suspend fun runOf(start: Int, end: Int) {
            val segment = text.substring(start, end)
            if (segment.isBlank()) {
                sb.append(segment)
                return
            }
            val lead = segment.takeWhile { it.isWhitespace() }
            val trail = segment.takeLastWhile { it.isWhitespace() }
            sb.append(lead).append(run(segment.trim(), glossary)).append(trail)
        }
        for (m in PreservePatterns.MARKUP.flatMap { it.findAll(text).toList() }.sortedBy { it.range.first }) {
            if (m.range.first < cursor) continue
            runOf(cursor, m.range.first)
            sb.append(m.value)
            cursor = m.range.last + 1
        }
        runOf(cursor, text.length)
        return sb.toString()
    }


    /** Translates each text independently; one failure does not affect the others. */
    public suspend fun translateAll(texts: List<String>): List<Result<String>> = texts.map { translate(it) }

    private suspend fun run(text: String, glossary: Glossary): String {
        val prepared = protector.protect(text, glossary)
        if (prepared.slots.isEmpty()) return engine.translate(text)

        if (!protector.hasTranslatableContent(prepared)) {
            return protector.restore(prepared.text, prepared).text
        }

        val result = protector.restore(engine.translate(prepared.text), prepared)
        if (result.isClean) return result.text
        if (fallback == FallbackStrategy.PARTIAL_RESULT) return result.text

        val hasGlossarySlot = prepared.slots.any { it.kind == SlotKind.GLOSSARY }
        if (hasGlossarySlot) {
            val numbersOnly = protector.protect(text, glossary, setOf(SlotKind.PRESERVED))
            if (numbersOnly.slots.isNotEmpty()) {
                val retry = protector.restore(engine.translate(numbersOnly.text), numbersOnly)
                if (retry.isClean) return retry.text
            }
        }
        return engine.translate(text)
    }
}
