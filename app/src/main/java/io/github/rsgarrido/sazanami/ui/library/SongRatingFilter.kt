package io.github.rsgarrido.sazanami.ui.library

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey

enum class SongRatingFilter { ALL, RATED, UNRATED }

enum class RatedSongFilter(val exactRating: Int?) {
    ALL(null),
    UNRATED(null),
    FIVE(5),
    FOUR(4),
    THREE(3),
    TWO(2),
    ONE(1);

}

@Composable
internal fun RatedSongFilter.displayLabel(): String = when (this) {
    RatedSongFilter.ALL -> stringResource(R.string.rated_filter_all)
    RatedSongFilter.UNRATED -> stringResource(R.string.rated_filter_unrated)
    else -> requireNotNull(exactRating).let { rating ->
        pluralStringResource(R.plurals.rated_filter_stars, rating, rating)
    }
}

internal fun visibleRatedSongFilters(quickRateActive: Boolean): List<RatedSongFilter> =
    RatedSongFilter.entries.filter { filter ->
        quickRateActive || filter != RatedSongFilter.UNRATED
    }

internal fun normalizeRatedSongFilterForQuickRateMode(
    filter: RatedSongFilter,
    quickRateActive: Boolean
): RatedSongFilter = if (!quickRateActive && filter == RatedSongFilter.UNRATED) {
    RatedSongFilter.ALL
} else {
    filter
}

fun filterSongsForRatedCollection(
    songs: List<Song>,
    filter: RatedSongFilter,
    ratingsByReferenceKey: Map<String, Int>
): List<Song> = when (filter) {
    RatedSongFilter.UNRATED -> emptyList()
    else -> songs.filter { song ->
        val rating = ratingsByReferenceKey[song.membershipKey()]
        rating in 1..5 && (filter.exactRating == null || rating == filter.exactRating)
    }
}

/**
 * Quick Rate turns RATED's All ratings view into a rating workflow over the authoritative Songs
 * catalog. Normal RATED and exact-star filters retain rated-only membership.
 */
internal fun projectSongsForRatedCollection(
    songs: List<Song>,
    filter: RatedSongFilter,
    ratingsByReferenceKey: Map<String, Int>,
    quickRateActive: Boolean
): List<Song> = when {
    quickRateActive && filter == RatedSongFilter.ALL -> songs
    quickRateActive && filter == RatedSongFilter.UNRATED -> songs.filter { song ->
        ratingsByReferenceKey[song.membershipKey()] !in 1..5
    }
    else -> filterSongsForRatedCollection(songs, filter, ratingsByReferenceKey)
}

@StringRes
internal fun ratedCollectionEmptyMessage(
    filter: RatedSongFilter,
    searchQuery: String,
    quickRateActive: Boolean
): Int = when {
    quickRateActive && filter == RatedSongFilter.ALL && searchQuery.isNotBlank() ->
        R.string.rated_empty_search
    searchQuery.isNotBlank() -> R.string.rated_empty_search_rated
    filter != RatedSongFilter.ALL -> R.string.rated_empty_filter
    else -> R.string.rated_empty
}
