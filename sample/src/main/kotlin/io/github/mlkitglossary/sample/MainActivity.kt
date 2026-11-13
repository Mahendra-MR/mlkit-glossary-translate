package io.github.mlkitglossary.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.mlkit.nl.translate.TranslateLanguage
import io.github.mlkitglossary.GlossaryTranslator
import io.github.mlkitglossary.core.Gender
import io.github.mlkitglossary.core.Glossary
import io.github.mlkitglossary.core.HindiGenderAgreement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var translator: GlossaryTranslator

    private val glossary = Glossary.build {
        term("Loan", "लोन", Gender.MASCULINE)
        term("EMI", "EMI", Gender.FEMININE)
        term("OTP", "OTP")
        term("Mandate", "मैंडेट", Gender.MASCULINE)
        term("Bank Account", "बैंक अकाउंट", Gender.MASCULINE)
    }

    private val sentences = listOf(
        "Your Loan is approved",
        "Your EMI of 15000 is due tomorrow",
        "Enter the OTP sent to your mobile",
        "Please verify your Bank Account",
        "The Mandate registration failed, please retry",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        translator = GlossaryTranslator(
            TranslateLanguage.ENGLISH,
            TranslateLanguage.HINDI,
            glossary,
            agreement = HindiGenderAgreement(),
        )
        val output = TextView(this).apply { textSize = 15f }
        val button = Button(this).apply { text = "Translate (plain vs glossary)" }
        val dryRunButton = Button(this).apply { text = "Dry run (placeholder survival)" }
        setContentView(
            ScrollView(this).apply {
                addView(
                    LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(32, 32, 32, 32)
                        addView(button)
                        addView(dryRunButton)
                        addView(output)
                    },
                )
            },
        )
        dryRunButton.setOnClickListener {
            output.text = "Running dry run..."
            scope.launch {
                translator.glossary = glossary
                output.text = translator.dryRun(sentences).format()
            }
        }
        button.setOnClickListener {
            output.text = "Downloading model / translating..."
            scope.launch {
                val sb = StringBuilder()
                for (s in sentences) {
                    translator.glossary = Glossary.EMPTY
                    val plain = translator.translate(s)
                    translator.glossary = glossary
                    val custom = translator.translate(s)
                    sb.append("EN: $s\nPlain: $plain\nGlossary: $custom\n\n")
                }
                output.text = sb.toString()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        translator.close()
        super.onDestroy()
    }
}
