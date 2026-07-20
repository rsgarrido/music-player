package com.example.cdplaya.ui.library

import android.R
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
fun SongGroupDetailScreen(
    title: String,
    subtitle: String,
    artworkUri: Uri?,
    songs: List<Song>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    showAlbumName: Boolean,
    showTrackNumbers: Boolean,
    onBackClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    favoriteSongKeys: Set<String>,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onAddAllToPlaylistClick: () -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artworkUri,
                contentDescription = "Artwork for $title",
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_media_play),
                placeholder = painterResource(R.drawable.ic_media_play)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPlayAllClick,
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play"
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(text = "Play")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onShuffleAllClick,
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle"
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(text = "Shuffle")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onAddAllToPlaylistClick,
                        enabled = songs.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Add to playlist")
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Text(
                text = "No songs match your search.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            SongList(
                songs = songs,
                currentSongId = currentSongId,
                recentlyAddedSongIds = recentlyAddedSongIds,
                showAlbumName = showAlbumName,
                showTrackNumbers = showTrackNumbers,
                onSongClick = onSongClick,
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                favoriteSongKeys = favoriteSongKeys,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onEditSongTagsClick = onEditSongTagsClick,
                bottomContentPadding = bottomContentPadding,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
