package io.pelmenstar.onealarm.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.dialogContainer(surface: Color, shape: Shape): Modifier =
    shadow(elevation = 2.dp, shape = shape, clip = true)
        .padding(2.dp)
        .background(surface, shape)
        .padding(10.dp)