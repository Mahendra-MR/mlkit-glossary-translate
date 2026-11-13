package io.github.mlkitglossary.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GenderAgreementTest {

    private val protector = PlaceholderProtector(agreement = HindiGenderAgreement())

    private val glossary = Glossary.build {
        term("loan", "लोन", Gender.MASCULINE)
        term("EMI", "किस्त", Gender.FEMININE)
        term("OTP", "OTP")
    }

    private fun restored(source: String, translated: String): String {
        val p = protector.protect(source, glossary)
        return protector.restore(translated, p).text
    }

    @Test fun masculineStaysMasculine() {
        assertEquals("आपका लोन", restored("your loan", "आपका GLS0Q"))
    }

    @Test fun feminineFixesPossessive() {
        assertEquals("आपकी किस्त बाकी है", restored("your EMI is due", "आपका GLS0Q बाकी है"))
    }

    @Test fun feminineFixesAdjective() {
        assertEquals("नई किस्त", restored("new EMI", "नया GLS0Q"))
    }

    @Test fun masculineFixesWrongFeminineForm() {
        assertEquals("मेरा लोन", restored("my loan", "मेरी GLS0Q"))
    }

    @Test fun termWithoutGenderIsNotAdjusted() {
        assertEquals("आपका OTP", restored("your OTP", "आपका GLS0Q"))
    }

    @Test fun onlyTheImmediatelyPrecedingWordChanges() {
        assertEquals("आपका नई किस्त", restored("your new EMI", "आपका नया GLS0Q"))
    }

    @Test fun tokenAtStartIsFine() {
        assertEquals("किस्त जमा", restored("EMI paid", "GLS0Q जमा"))
    }

    @Test fun defaultProtectorDoesNotTouchGrammar() {
        val p = PlaceholderProtector().protect("your EMI", glossary)
        assertEquals("आपका किस्त", PlaceholderProtector().restore("आपका GLS0Q", p).text)
    }

    @Test fun builderAndMapConstructorAgree() {
        val a = Glossary(mapOf("loan" to "लोन"))
        val b = Glossary.build { term("loan", "लोन") }
        assertEquals(
            PlaceholderProtector().protect("a loan", a).slots,
            PlaceholderProtector().protect("a loan", b).slots,
        )
    }
}
