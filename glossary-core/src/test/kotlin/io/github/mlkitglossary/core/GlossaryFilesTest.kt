package io.github.mlkitglossary.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GlossaryFilesTest {
    private val protector = PlaceholderProtector()

    private fun outputFor(g: Glossary, term: String) = protector.protect(term, g).slots.singleOrNull()

    @Test fun csvWithoutHeader() {
        val g = GlossaryCsv.parse("Loan,लोन\nEMI,किस्त,f\n")
        assertEquals("लोन", outputFor(g, "Loan")!!.output)
        assertEquals(Gender.FEMININE, outputFor(g, "EMI")!!.gender)
        assertNull(outputFor(g, "Loan")!!.gender)
    }

    @Test fun csvWithHeaderAndReorderedColumns() {
        val g = GlossaryCsv.parse("gender,Hindi,English\nm,लोन,Loan\nf,किस्त,EMI\n")
        assertEquals("लोन", outputFor(g, "Loan")!!.output)
        assertEquals(Gender.MASCULINE, outputFor(g, "Loan")!!.gender)
        assertEquals(Gender.FEMININE, outputFor(g, "EMI")!!.gender)
    }

    @Test fun csvQuotedFieldsCommasQuotesAndNewlines() {
        val g = GlossaryCsv.parse("\"Bank, Account\",\"बैंक \"\"खाता\"\"\"\n\"Multi\nLine\",x\n")
        assertEquals("बैंक \"खाता\"", outputFor(g, "Bank, Account")!!.output)
        assertEquals("x", outputFor(g, "Multi Line")!!.output)
    }

    @Test fun csvSemicolonAndTabDelimitersAreDetected() {
        assertEquals("लोन", outputFor(GlossaryCsv.parse("source;replacement\nLoan;लोन"), "Loan")!!.output)
        assertEquals("लोन", outputFor(GlossaryCsv.parse("Loan\tलोन"), "Loan")!!.output)
    }

    @Test fun csvBomCrLfBlankAndCommentLines() {
        val g = GlossaryCsv.parse("﻿# my terms\r\nLoan,लोन\r\n\r\nOTP,OTP\r\n")
        assertEquals(2, g.size)
        assertEquals("लोन", outputFor(g, "Loan")!!.output)
    }

    @Test fun csvMatchPluralsFlagIsApplied() {
        assertEquals(1, protector.protect("loans", GlossaryCsv.parse("loan,लोन", matchPlurals = true)).slots.size)
    }

    @Test fun csvMissingReplacementReportsRowNumberOnly() {
        val e = runCatching { GlossaryCsv.parse("Loan,लोन\nEMI,\n") }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
        assertEquals("Row 2: missing replacement", e!!.message)
    }

    @Test fun emptyCsvIsEmptyGlossary() = assertTrue(GlossaryCsv.parse("").isEmpty())

    // ---- xlsx ----

    private fun xlsx(sharedStrings: String?, sheet: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("[Content_Types].xml")); z.write("<Types/>".toByteArray()); z.closeEntry()
            if (sharedStrings != null) {
                z.putNextEntry(ZipEntry("xl/sharedStrings.xml")); z.write(sharedStrings.toByteArray()); z.closeEntry()
            }
            z.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml")); z.write(sheet.toByteArray()); z.closeEntry()
        }
        return out.toByteArray()
    }

    @Test fun xlsxSharedStringsHeaderGenderAndRichText() {
        val shared = "<sst><si><t>English</t></si><si><t>Hindi</t></si><si><t>Gender</t></si>" +
            "<si><t>Loan</t></si><si><r><t>लो</t></r><r><t>न</t></r></si><si><t>m</t></si>" +
            "<si><t>Bank &amp; Co</t></si><si><t xml:space=\"preserve\">बैंक</t></si></sst>"
        val sheet = "<worksheet><sheetData>" +
            "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c><c r=\"C1\" t=\"s\"><v>2</v></c></row>" +
            "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>3</v></c><c r=\"B2\" t=\"s\"><v>4</v></c><c r=\"C2\" t=\"s\"><v>5</v></c></row>" +
            "<row r=\"4\"><c r=\"A4\" t=\"s\"><v>6</v></c><c r=\"B4\" t=\"s\"><v>7</v></c></row>" +
            "</sheetData></worksheet>"
        val g = GlossaryXlsx.parse(ByteArrayInputStream(xlsx(shared, sheet)))
        assertEquals(2, g.size)
        assertEquals("लोन", outputFor(g, "Loan")!!.output)
        assertEquals(Gender.MASCULINE, outputFor(g, "Loan")!!.gender)
        assertEquals("बैंक", outputFor(g, "Bank & Co")!!.output)
    }

    @Test fun xlsxInlineStringsAndNumbers() {
        val sheet = "<worksheet><sheetData><row r=\"1\">" +
            "<c r=\"A1\" t=\"inlineStr\"><is><t>code</t></is></c><c r=\"B1\"><v>5</v></c></row></sheetData></worksheet>"
        val g = GlossaryXlsx.parse(ByteArrayInputStream(xlsx(null, sheet)))
        assertEquals("5", outputFor(g, "code")!!.output)
    }

    @Test fun xlsxWithColumnsBeyondZ() {
        val sheet = "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c>" +
            "<c r=\"B1\" t=\"inlineStr\"><is><t>b</t></is></c></row></sheetData></worksheet>"
        assertEquals(1, GlossaryXlsx.parse(ByteArrayInputStream(xlsx(null, sheet))).size)
    }

    @Test fun notAZipThrowsIllegalArgument() {
        val e = runCatching { GlossaryXlsx.parse(ByteArrayInputStream("plain text".toByteArray())) }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
    }
}
