package com.example.cdplaya.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

private data class ArtistGroup(
    val name: String,
    val songs: List<Song>
)

@Composable
fun ArtistListScreen(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val artists = songs
        .groupBy { song -> song.artist.ifBlank { "Unknown Artist" } }
        .map { entry ->
            ArtistGroup(
                name = entry.key,
                songs = entry.value
            )
        }
        .sortedBy { artist ->
            artist.name.lowercase()
        }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = artists,
            key = { artist -> artist.name }
        ) { artist ->
            val firstSong = artist.songs.firstOrNull()

            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = firstSong?.albumArtUri,
                        contentDescription = "Artwork for ${artist.name}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(text = artist.name)
                },
                supportingContent = {
                    Text(
                        text = "${artist.songs.size} song(s)"
                    )
                }
            )
        }
    }
}