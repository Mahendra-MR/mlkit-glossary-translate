package io.github.mlkitglossary.core

/**
 * Ready-made regexes for [PlaceholderProtector] `preservePatterns`: spans that must pass through
 * translation untouched. Combine them, e.g. `listOf(NUMBERS) + FORMAT_ARGS + URLS`.
 */
public object PreservePatterns {
    /** 4+ digit numbers and `XXXX1234`-style masks. */
    public val NUMBERS: Regex = PlaceholderProtector.DEFAULT_NUMBER_PATTERN

    public val URLS: Regex = Regex("https?://[^\\s<>\"')]+")

    public val EMAILS: Regex = Regex("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+")

    /** HTML/XML tags such as `<b>`, `</b>`, `<a href="x">`. */
    public val HTML_TAGS: Regex = Regex("</?[A-Za-z][^<>]*>")

    /** Android/Java format args (`%s`, `%1\$d`, `%.2f`) and `{name}` / `{{name}}` / `{0}` placeholders. */
    public val FORMAT_ARGS: Regex = Regex("%(?:\\d+\\$)?[-#+0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]|\\{\\{?[\\w.]+}}?")

    /** Everything above, for convenience. */
    public val ALL: List<Regex> = listOf(NUMBERS, URLS, EMAILS, HTML_TAGS, FORMAT_ARGS)
}
