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

    /** `&amp;`, `&nbsp;`, `&#8377;` style entities. */
    public val HTML_ENTITIES: Regex = Regex("&(?:[A-Za-z][A-Za-z0-9]{1,31}|#\\d{1,7}|#[xX][0-9A-Fa-f]{1,6});")

    /** Tags and entities: what markup-aware translation keeps intact. */
    public val MARKUP: List<Regex> = listOf(HTML_TAGS, HTML_ENTITIES)

    /** Everything above, for convenience. */
    public val ALL: List<Regex> = listOf(NUMBERS, URLS, EMAILS, HTML_TAGS, HTML_ENTITIES, FORMAT_ARGS)
}
