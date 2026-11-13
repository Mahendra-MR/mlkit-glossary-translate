# mlkit-glossary-translate

A glossary ("keep these words as-is") layer for Google ML Kit on-device translation on Android.

## The problem

`Translator.translate()` from `com.google.mlkit:translate` has no glossary or do-not-translate option.
Domain words that users say in everyday speech come back as formal, "pure" target-language words.
For English → Hindi, `Loan` and `Mandate` become textbook Hindi that few people use in conversation,
and `EMI` or `OTP` may be rewritten or dropped.

## How it works

1. Glossary terms (and, by default, numbers of 4+ digits or `XXXX1234`-style masks) are replaced
   by opaque tokens like `GLS0Q` before the text reaches ML Kit.
2. ML Kit translates the rest of the sentence.
3. Tokens are replaced by your preferred output. Restore is tolerant of ML Kit changing case,
   adding spaces (`gls 0 q`) or converting digits to another script.
4. If a token is lost, the library falls back to a plain ML Kit translation of the original text,
   so the result is never worse than not using the library.

Matching is case-insensitive, respects word boundaries and prefers the longest term
(`Bank Account` beats `Bank`).

## Modules

| Module | Contents |
|---|---|
| `glossary-core` | Pure Kotlin/JVM: `Glossary`, `PlaceholderProtector`, `GlossaryTranslationPipeline`. No Android dependency, fully unit-tested on the JVM. |
| `mlkit-glossary-translate` | Android library: `GlossaryTranslator`, the ML Kit binding. |

## Usage

```kotlin
val glossary = Glossary(
    mapOf(
        "Loan" to "लोन",                 // transliteration
        "EMI" to "EMI",                  // keep the English term
        "Mandate" to "मैंडेट",
        "Bank Account" to "बैंक अकाउंट", // longest match wins over "Bank"
    )
)

val translator = GlossaryTranslator(
    sourceLanguage = TranslateLanguage.ENGLISH,
    targetLanguage = TranslateLanguage.HINDI,
    glossary = glossary,
)

lifecycleScope.launch {
    translator.preloadModel()                       // optional pre-warm
    val hindi: String? = translator.translate("Your Loan EMI of 15000 is due")
    // or: translator.translateResult(text) to get the failure cause
}

translator.glossary = newGlossaryFromRemoteConfig   // swap at runtime
translator.close()                                  // when done
```

`translate` returns `null` on failure (model download, translation). The text being translated is never logged.

Options on `GlossaryTranslator`: `preservePatterns` (regexes copied through unchanged, default 4+ digit
numbers), `fallback` (`PLAIN_TRANSLATION` default or `PARTIAL_RESULT`), `downloadConditions`.

Not published yet; for now include the modules from source (`./gradlew :mlkit-glossary-translate:publishToMavenLocal` for local use).

## Beyond a plain swap

- **Any language pair.** Nothing in the core is Hindi-specific; pass any `TranslateLanguage` codes. The source language should be space-delimited (English, Spanish, ...) because term matching uses word boundaries.
- **Gender agreement.** Mark entries with a gender and pass an agreement helper: `Glossary.build { term("EMI", "किस्त", Gender.FEMININE) }` with `agreement = HindiGenderAgreement()` turns `आपका GLS0Q` into `आपकी किस्त`. Implement `GenderAgreement` for other languages.
- **JSON glossaries** (e.g. from remote config): `GlossaryJson.parse("""{"Loan":"लोन","EMI":{"to":"किस्त","gender":"feminine"}}""")`.
- **Batch and cache:** `translateAll(list)` and `cacheSize = 50`.
- **Sample app** in `sample/` compares plain vs glossary output side by side.

## Status

The pure-Kotlin logic is covered by JVM tests. The instrumented test and sample app exercise the real ML Kit model, but need a device with internet; results on real hardware are not published yet, so token survival for your language pair is worth verifying.

## Limitations

- **Grammar around placeholders.** Hindi inflects verbs and adjectives by noun gender, and postpositions
  can depend on the noun. ML Kit sees only a token, so it cannot know the gender of the word you will
  substitute. `आपका GLS0Q` may need `आपकी` for a feminine noun. Pick glossary terms with that in mind,
  or keep terms whose usual gender matches how ML Kit tends to treat unknown nouns.
- Glossary terms are exact (no stemming): `loans` does not match `loan` unless you add it.
- Token survival depends on the ML Kit model. The restore is tolerant and falls back safely, but a
  fallback means that sentence is translated without your glossary.
- Plain-text only; no HTML/markup handling.

## Development

```bash
./gradlew test
```

## License

Apache License 2.0, see [LICENSE](LICENSE).
