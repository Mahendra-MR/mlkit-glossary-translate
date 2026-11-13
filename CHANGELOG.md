# Changelog

## 0.1.1
- Publish `glossary-core` so the Android artifact resolves (0.1.0 was missing it).

## 0.1.0
- Glossary with case-insensitive, word-boundary, longest-match-first matching.
- Placeholder protect/restore with tolerant restore and plain-translation fallback.
- Number / mask protection (4+ digits, `XXXX1234`).
- Optional gender agreement (`GenderAgreement`, bundled `HindiGenderAgreement`).
- `translateAll`, in-memory cache, runtime-swappable glossary, JSON glossary loader.
- Sample app and instrumented test (needs a device with network).
- `matchPlurals`, `PreservePatterns`, numbers-only retry before plain fallback.
- `translateMarkup` with tag validation and per-run fallback; term matching for unspaced scripts (CJK, Thai).
- `GlossaryCsv`, `GlossaryXlsx`, `Glossary.analyze`, `dryRun` with outcome report.
