package com.encarox.kodo.extract

import com.encarox.kodo.data.extract.CodeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CodeExtractorTest {

    @Test fun extractsSixDigitEnglish() {
        val r = CodeExtractor.extract("Your verification code is 483920. Do not share it.")
        assertNotNull(r)
        assertEquals("483920", r!!.code)
    }

    @Test fun extractsTurkishCode() {
        val r = CodeExtractor.extract("Instagram doğrulama kodunuz: 550129")
        assertNotNull(r)
        assertEquals("550129", r!!.code)
        assertEquals("Instagram", r.service)
    }

    @Test fun extractsGroupedCode() {
        val r = CodeExtractor.extract("G-123 456 is your Google verification code")
        assertNotNull(r)
        assertEquals("123456", r!!.code)
        assertEquals("Google", r.service)
    }

    @Test fun ignoresPriceAndOrder() {
        val r = CodeExtractor.extract("Your order 100234 total is 149 TL. Thanks!")
        assertNull(r)
    }

    @Test fun ignoresYear() {
        val r = CodeExtractor.extract("Happy new year 2026 from our team")
        assertNull(r)
    }

    @Test fun bareCodeAccepted() {
        val r = CodeExtractor.extract("902413")
        assertNotNull(r)
        assertEquals("902413", r!!.code)
    }

    @Test fun repeatedDigitsRejected() {
        val r = CodeExtractor.extract("Your code is 000000")
        assertNull(r)
    }
}
