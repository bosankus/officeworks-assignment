package com.bosankus.officeworks.core.ui.format

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceFormatTest {

    private val australia = Locale.forLanguageTag("en-AU")

    @Test
    fun `AUD in Australia uses a plain dollar sign and separators`() {
        assertEquals("\$109.95", formatPrice(10_995, "AUD", australia))
        assertEquals("\$1,099.00", formatPrice(109_900, "AUD", australia))
        assertEquals("\$0.05", formatPrice(5, "AUD", australia))
    }

    @Test
    fun `AUD outside Australia is told apart from US dollars`() {
        assertEquals("A\$109.95", formatPrice(10_995, "AUD", Locale.US))
        assertEquals("\$109.95", formatPrice(10_995, "USD", Locale.US))
    }

    @Test
    fun `a currency without cents drops the decimals`() {
        assertEquals("¥1,200", formatPrice(120_000, "JPY", Locale.US))
    }

    @Test
    fun `an unknown currency code still shows the amount`() {
        assertEquals("ABC 5.00", formatPrice(500, "ABC", australia))
    }
}
