package io.github.mlkitglossary.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiLanguageTest {
    private val protector = PlaceholderProtector()
    private val glossary = Glossary(mapOf("loan" to "LOAN", "EMI" to "EMI"))

    private fun restore(source: String, translated: String): String {
        val p = protector.protect(source, glossary)
        return protector.restore(translated, p).text
    }

    @Test fun spanishTarget() = assertEquals("Su LOAN está aprobado", restore("Your loan is approved", "Su GLS0Q está aprobado"))

    @Test fun arabicTargetWithAdjacentScript() = assertEquals("قرضكLOANمعتمد", restore("loan", "قرضكGLS0Qمعتمد"))

    @Test fun japaneseTargetWithoutSpaces() = assertEquals("あなたのLOANは承認されました", restore("Your loan", "あなたのGLS0Qは承認されました"))

    @Test fun chineseTargetWithFullWidthDigits() = assertEquals("您的LOAN", restore("loan", "您的GLS０Q"))

    @Test fun cyrillicSourceTermsMatchCaseInsensitively() {
        val g = Glossary(mapOf("кредит" to "credit"))
        val p = protector.protect("Ваш КРЕДИТ одобрен", g)
        assertEquals("Ваш GLS0Q одобрен", p.text)
    }

    @Test fun customAgreementForAnotherLanguage() {
        val spanish = GenderAgreement { w, g -> if (g == Gender.FEMININE && w == "nuevo") "nueva" else w }
        val pr = PlaceholderProtector(agreement = spanish)
        val gl = Glossary.build { term("EMI", "cuota", Gender.FEMININE) }
        val p = pr.protect("new EMI", gl)
        assertEquals("nueva cuota", pr.restore("nuevo GLS0Q", p).text)
    }
}
