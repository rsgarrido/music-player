package com.example.cdplaya.ui.player.retrorack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song

@Composable
fun RetroRackExpandedPlayer(
    currentSong: Song?,
    onCollapseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF17191D))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RETRO RACK",
            color = Color(0xFF78F06A)
        )
        Text(
            text = currentSong?.title.orEmpty(),
            color = Color(0xFFD6D9CD)
        )
        Button(onClick = onCollapseClick) {
            Text(text = "CLOSE")
        }
    }
}
