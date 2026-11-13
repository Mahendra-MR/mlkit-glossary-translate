# mlkit-glossary-translate

[![CI](https://github.com/Mahendra-MR/mlkit-glossary-translate/actions/workflows/ci.yml/badge.svg)](https://github.com/Mahendra-MR/mlkit-glossary-translate/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

**Glossary, custom dictionary and "do not translate" support for Google ML Kit on-device translation on Android (Kotlin, coroutines).**

Google ML Kit Translation (`com.google.mlkit:translate`) has no glossary, custom dictionary or do-not-translate option.
This library wraps it so you can keep or replace specific words (brand names, `OTP`, `EMI`, `Loan`, product terms),
protect numbers, URLs and HTML tags, and get natural everyday output instead of formal textbook translations,
for example English to Hindi, Hinglish or any of the 59 languages ML Kit supports.

Keywords: ML Kit translate glossary, ML Kit custom terminology, ML Kit do not translate, ML Kit keep word untranslated,
Android offline translation glossary, ML Kit Hindi translation, Hinglish.

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

## Install

Via [JitPack](https://jitpack.io/#Mahendra-MR/mlkit-glossary-translate). In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then in your module:

```kotlin
dependencies {
    implementation("com.github.Mahendra-MR:mlkit-glossary-translate:v0.1.1")
}
```

Requires `minSdk 21`. The JVM-only core is also available as `com.github.Mahendra-MR:glossary-core:v0.1.1`.

## Beyond a plain swap

- **Any language pair.** Nothing in the core is Hindi-specific; pass any `TranslateLanguage` codes. Word-boundary matching is applied for spaced scripts and skipped next to Han, Kana, Thai, Lao, Khmer and Myanmar text.
- **Gender agreement.** Mark entries with a gender and pass an agreement helper: `Glossary.build { term("EMI", "किस्त", Gender.FEMININE) }` with `agreement = HindiGenderAgreement()` turns `आपका GLS0Q` into `आपकी किस्त`. Implement `GenderAgreement` for other languages.
- **JSON glossaries** (e.g. from remote config): `GlossaryJson.parse("""{"Loan":"लोन","EMI":{"to":"किस्त","gender":"feminine"}}""")`.
- **Batch and cache:** `translateAll(list)` and `cacheSize = 50`.
- **Plurals:** `Glossary.build { term("loan", "लोन"); matchPlurals() }`.
- **Preserve patterns:** `preservePatterns = PreservePatterns.ALL` keeps numbers, URLs, e-mails, HTML tags and format args intact.
- **Markup:** `translateMarkup("Your <b>loan</b> is approved")` translates the sentence in context and keeps tags and entities (`&amp;`) intact. If translation reorders tags so they become malformed, it translates each text run between tags separately.
- **Unspaced source scripts:** term matching works for Chinese, Japanese and Thai source text (no word boundary is required next to those scripts), and Latin terms or numbers next to them still match.
- **Sample app** in `sample/` compares plain vs glossary output side by side.

## Limitations

- **Verb agreement after the noun is not fixed.** Only the word immediately before a gendered term is adjusted (`आपका` -> `आपकी`). Verbs and participles later in the sentence (`की गई` vs `किया गया`) are left as ML Kit produced them.
- **Replacements are not inflected.** `matchPlurals` makes `loans` and `loan's` match the `loan` term, but the replacement is inserted unchanged, so target-language plural, case or oblique forms are not generated.
- **Token survival depends on the ML Kit model.** Restore tolerates case, spacing and non-ASCII digits. If a token is lost, the library first retries protecting only numbers and patterns, then falls back to an unprotected translation, so that sentence is translated without your glossary.
- **Markup is handled structurally, not semantically.** `translateMarkup` keeps tags and entities intact and checks the tags are still well-formed (falling back to translating each text run separately), but attributes such as `title="..."` or `alt="..."` are not translated.

## Development

```bash
./gradlew test
```

## License

Apache License 2.0, see [LICENSE](LICENSE).
