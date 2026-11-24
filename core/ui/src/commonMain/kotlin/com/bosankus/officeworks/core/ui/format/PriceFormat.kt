package com.bosankus.officeworks.core.ui.format

/**
 * A price for the device's locale, using the currency the backend sent: AUD reads "$109.95" in
 * Australia and "A$109.95" elsewhere. [minorUnits] are hundredths, as in `Money.cents`.
 */
expect fun formatPrice(minorUnits: Long, currencyCode: String): String
