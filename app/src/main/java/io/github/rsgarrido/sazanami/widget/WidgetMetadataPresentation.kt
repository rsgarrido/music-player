package io.github.rsgarrido.sazanami.widget

import android.content.Context
import io.github.rsgarrido.sazanami.R

/** Missing metadata is stored as empty state and resolved in the current render locale. */
internal fun NowPlayingWidgetSnapshot.localizedTitle(context: Context): String =
    title.ifBlank { context.getString(R.string.widget_unknown_title) }

internal fun NowPlayingWidgetSnapshot.localizedArtist(context: Context): String =
    artist.ifBlank { context.getString(R.string.widget_unknown_artist) }
