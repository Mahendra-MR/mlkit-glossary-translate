package io.github.mlkitglossary

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.mlkitglossary.core.FallbackStrategy
import io.github.mlkitglossary.core.Glossary
import io.github.mlkitglossary.core.GlossaryTranslationPipeline
import io.github.mlkitglossary.core.PlaceholderProtector
import io.github.mlkitglossary.core.TranslationEngine
import kotlinx.coroutines.tasks.await
import java.io.Closeable
import kotlin.coroutines.cancellation.CancellationException

/**
 * ML Kit on-device translator with a glossary layer.
 *
 * @param sourceLanguage a `TranslateLanguage` code, e.g. `TranslateLanguage.ENGLISH`
 * @param targetLanguage a `TranslateLanguage` code, e.g. `TranslateLanguage.HINDI`
 * @param glossary initial glossary; replace it any time via [glossary].
 * @param preservePatterns spans copied through translation unchanged (default: 4+ digit numbers / masks).
 */
public class GlossaryTranslator(
    sourceLanguage: String,
    targetLanguage: String,
    glossary: Glossary = Glossary.EMPTY,
    preservePatterns: List<Regex> = listOf(PlaceholderProtector.DEFAULT_NUMBER_PATTERN),
    fallback: FallbackStrategy = FallbackStrategy.PLAIN_TRANSLATION,
    private val downloadConditions: DownloadConditions = DownloadConditions.Builder().build(),
) : Closeable {

    private val translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build(),
    )

    private val pipeline = GlossaryTranslationPipeline(
        engine = MlKitEngine(translator, downloadConditions),
        glossary = glossary,
        protector = PlaceholderProtector(preservePatterns),
        fallback = fallback,
    )

    /** Current glossary. Assigning a new one takes effect for subsequent calls. */
    public var glossary: Glossary
        get() = pipeline.glossary
        set(value) {
            pipeline.glossary = value
        }

    /** Downloads the language model if needed. Safe to call repeatedly. Returns false on failure. */
    public suspend fun preloadModel(): Boolean = try {
        translator.downloadModelIfNeeded(downloadConditions).await()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    /** Translates [text], returning null on any failure (model download, translation). */
    public suspend fun translate(text: String): String? = translateResult(text).getOrNull()

    /** Like [translate] but exposes the failure cause. */
    public suspend fun translateResult(text: String): Result<String> = pipeline.translate(text)

    override fun close() {
        translator.close()
    }

    private class MlKitEngine(
        private val translator: Translator,
        private val conditions: DownloadConditions,
    ) : TranslationEngine {
        override suspend fun translate(text: String): String {
            translator.downloadModelIfNeeded(conditions).await()
            return translator.translate(text).await()
        }
    }
}
