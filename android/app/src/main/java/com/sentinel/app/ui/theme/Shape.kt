package com.sentinel.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SentinelShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Named shapes mapping directly from docs/design/ui-v2/src/index.css
val CardShape = RoundedCornerShape(22.dp)
val ButtonShape = RoundedCornerShape(15.dp)
val IconButtonShape = RoundedCornerShape(13.dp)
val ChipShape = RoundedCornerShape(12.dp)
val BadgeShape = RoundedCornerShape(8.dp)
val PillShape = RoundedCornerShape(99.dp)
