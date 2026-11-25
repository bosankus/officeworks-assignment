package com.bosankus.officeworks.core.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

actual fun formatPrice(minorUnits: Long, currencyCode: String): String = formatPrice(minorUnits, currencyCode, Locale.getDefault())

internal fun formatPrice(minorUnits: Long, currencyCode: String, locale: Locale): String {
    val amount = BigDecimal.valueOf(minorUnits, 2)
    val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
        // Not an ISO code: still show the amount, labelled with whatever the backend sent.
        ?: return "$currencyCode " + NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)
    return NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        // Set explicitly: Android adjusts decimals to the currency, the JVM doesn't (none for JPY, two for AUD).
        currency.defaultFractionDigits.takeIf { it >= 0 }?.let {
            minimumFractionDigits = it
            maximumFractionDigits = it
        }
    }.format(amount)
}
