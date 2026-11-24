package com.bosankus.officeworks.core.ui.theme

import androidx.compose.ui.unit.dp

/** Padding and gaps. Use these rather than raw dp so the layout rhythm can be changed in one place. */
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
}

/** Fixed component sizes. Screens and components read these instead of hardcoding dp. */
object Sizes {
    /** Minimum size for anything tappable (Material and Android accessibility guidance). */
    val touchTarget = 48.dp
    val topBarHeight = 64.dp
    val buttonHeight = 56.dp

    val iconSmall = 16.dp
    val iconMedium = 20.dp

    /** Circle behind an icon, e.g. in the category chips. */

    val border = 1.dp
    val progressStroke = 2.dp
    val badgeCornerRadius = 8.dp

    /** Category chips: drawn smaller than [touchTarget]; the tap area is still padded out to 48dp. */
    val chipHeight = 36.dp
    val chipCornerRadius = 8.dp

    /** Narrowest a product tile may get before the grid drops a column. */
    val productTileMinWidth = 150.dp
}
