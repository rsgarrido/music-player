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

private data class AlbumGroup(
    val key: String,
    val title: String,
    val artistText: String,
    val songs: List<Song>
)

@Composable
fun AlbumListScreen(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val albums = songs
        .groupBy { song -> song.folderPath }
        .map { entry ->
            val albumSongs = entry.value
            val firstSong = albumSongs.first()

            val artists = albumSongs
                .map { song -> song.artist }
                .distinct()
                .filter { artist -> artist.isNotBlank() }

            AlbumGroup(
                key = entry.key,
                title = firstSong.album.ifBlank { "Unknown Album" },
                artistText = if (artists.size == 1) {
                    artists.first()
                } else {
                    "Various Artists"
                },
                songs = albumSongs
            )
        }
        .sortedBy { album ->
            album.title.lowercase()
        }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { album -> album.key }
        ) { album ->
            val firstSong = album.songs.firstOrNull()

            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = firstSong?.albumArtUri,
                        contentDescription = "Album art for ${album.title}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(text = album.title)
                },
                supportingContent = {
                    Text(
                        text = "${album.artistText} • ${album.songs.size} song(s)"
                    )
                }
            )
        }
    }
}