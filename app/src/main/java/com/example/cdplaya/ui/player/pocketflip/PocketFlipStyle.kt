package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object PocketFlipColors {
    val shell = Color(0xFF982E3B)
    val shellText = Color(0xFFF5C7CB)
    val bezel = Color(0xFF17171B)
    val bezelText = Color(0xFFC7C9C4)
    val display = Color(0xFF263029)
    val artworkWell = Color(0xFF111713)
    val screenText = Color(0xFFE0E7D8)
    val screenTextMuted = Color(0xFF9CA99A)
    val screenAccent = Color(0xFFA5C980)
    val seekInactive = Color(0xFF4A544B)
    val statusOn = Color(0xFF8DD663)
    val statusIdle = Color(0xFF715257)
    val hinge = Color(0xFF6B1E29)
    val hingeCap = Color(0xFF321318)
    val button = Color(0xFF2A2B31)
    val buttonShadow = Color(0xFF541923)
    val buttonIcon = Color(0xFFDADADF)
    val buttonActive = Color(0xFFB6C880)
    val buttonActiveIcon = Color(0xFF20231D)
    val action = Color(0xFF4B1A2E)
    val actionActive = Color(0xFF6D203C)
    val actionIcon = Color(0xFFF7DFE8)
    val utility = Color(0xFF70232F)
    val utilityIcon = Color(0xFFF7D8DB)
}

@Composable
internal fun PocketFlipHinge(compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 18.dp else 22.dp)
            .background(PocketFlipColors.hinge, RoundedCornerShape(50))
            .padding(horizontal = if (compact) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.24f)
                    .height(if (compact) 8.dp else 10.dp)
                    .background(PocketFlipColors.hingeCap, CircleShape)
            )
        }
    }
}
