package io.github.mlkitglossary

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.translate.TranslateLanguage
import io.github.mlkitglossary.core.Gender
import io.github.mlkitglossary.core.Glossary
import io.github.mlkitglossary.core.HindiGenderAgreement
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Runs against the real ML Kit model; needs network on first run to download English->Hindi. */
class GlossaryTranslatorInstrumentedTest {

    private val glossary = Glossary.build {
        term("Loan", "लोन", Gender.MASCULINE)
        term("EMI", "EMI", Gender.FEMININE)
        term("OTP", "OTP")
        term("Mandate", "मैंडेट", Gender.MASCULINE)
        term("Bank Account", "बैंक अकाउंट", Gender.MASCULINE)
    }

    private lateinit var translator: GlossaryTranslator

    @Before fun setUp() {
        MlKitContext.initializeIfNeeded(InstrumentationRegistry.getInstrumentation().targetContext)
        translator = GlossaryTranslator(
            TranslateLanguage.ENGLISH,
            TranslateLanguage.HINDI,
            glossary,
            agreement = HindiGenderAgreement(),
        )
        val r = runBlocking { translator.translateResult("Hello world") }
        Log.i("GlossaryTest", "warmup: $r " + Log.getStackTraceString(r.exceptionOrNull()))
        assertTrue("model unavailable: ${r.exceptionOrNull()}", r.isSuccess)
    }

    @After fun tearDown() {
        if (::translator.isInitialized) translator.close()
    }

    private val sentences = listOf(
        "Your Loan is approved",
        "Your EMI of 15000 is due tomorrow",
        "Enter the OTP sent to your mobile",
        "Please verify your Bank Account",
        "The Mandate registration failed, please retry",
        "Loan amount XXXX1234 will be credited to your Bank Account",
    )

    @Test fun glossaryTermsAppearAndNoTokenLeaks() = runBlocking {
        for (s in sentences) {
            val plain = translator.also { it.glossary = Glossary.EMPTY }.translate(s)
            translator.glossary = glossary
            val withGlossary = translator.translate(s)
            Log.i("GlossaryTest", "EN : $s")
            Log.i("GlossaryTest", "PLAIN: $plain")
            Log.i("GlossaryTest", "GLOSS: $withGlossary")
            assertNotNull(withGlossary)
            assertFalse("token leaked: $withGlossary", withGlossary!!.contains("GLS", ignoreCase = true))
        }
    }

    @Test fun specificTermsAreUsed() = runBlocking {
        assertTrue(translator.translate("Your Loan is approved")!!.contains("लोन"))
        assertTrue(translator.translate("Enter the OTP sent to your mobile")!!.contains("OTP"))
        assertTrue(translator.translate("Your EMI of 15000 is due tomorrow")!!.contains("15000"))
    }
}
