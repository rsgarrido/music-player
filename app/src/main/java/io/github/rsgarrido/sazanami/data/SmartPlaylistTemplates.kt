package io.github.rsgarrido.sazanami.data

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R

const val SMART_PLAYLIST_GENERATED_REFRESH_INTERVAL_MILLIS = 5L * 24L * 60L * 60L * 1_000L

enum class SmartPlaylistTemplate(
    val key: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val generated: Boolean,
    val membershipMode: String,
    val draft: SmartPlaylistDraft
) {
    HEAVY_ROTATION(
        key = "heavy_rotation",
        nameRes = R.string.smart_template_heavy_rotation,
        descriptionRes = R.string.smart_template_heavy_rotation_description,
        generated = true,
        membershipMode = GeneratedPlaylistMembershipMode.SNAPSHOT,
        draft = SmartPlaylistDraft(
            rules = listOf(
                SmartPlaylistRule(
                    field = SmartPlaylistRuleField.RECENT_PLAY_COUNT,
                    operator = SmartPlaylistOperator.AT_LEAST,
                    values = listOf("1"),
                    parameters = mapOf("days" to "30")
                )
            ),
            sortField = SmartPlaylistSortField.RECENT_PLAY_COUNT,
            sortDirection = SmartPlaylistSortDirection.DESCENDING,
            resultLimit = 50
        )
    ),
    FORGOTTEN_FAVORITES(
        key = "forgotten_favorites",
        nameRes = R.string.smart_template_forgotten_favorites,
        descriptionRes = R.string.smart_template_forgotten_favorites_description,
        generated = true,
        membershipMode = GeneratedPlaylistMembershipMode.SNAPSHOT,
        draft = SmartPlaylistDraft(
            matchMode = SmartPlaylistMatchMode.ALL,
            rules = listOf(
                SmartPlaylistRule(
                    field = SmartPlaylistRuleField.TOTAL_PLAY_COUNT,
                    operator = SmartPlaylistOperator.AT_LEAST,
                    values = listOf("5")
                ),
                SmartPlaylistRule(
                    field = SmartPlaylistRuleField.LAST_PLAYED,
                    operator = SmartPlaylistOperator.MORE_THAN_DAYS_AGO,
                    values = listOf("90")
                )
            ),
            sortField = SmartPlaylistSortField.FORGOTTEN_FAVORITES_RANK,
            sortDirection = SmartPlaylistSortDirection.DESCENDING,
            resultLimit = 50
        )
    ),
    TOP_RATED(
        key = "top_rated",
        nameRes = R.string.smart_template_top_rated,
        descriptionRes = R.string.smart_template_top_rated_description,
        generated = true,
        membershipMode = GeneratedPlaylistMembershipMode.LIVE_DERIVED,
        draft = SmartPlaylistDraft(
            rules = listOf(
                SmartPlaylistRule(
                    field = SmartPlaylistRuleField.RATING,
                    operator = SmartPlaylistOperator.AT_LEAST,
                    values = listOf("4")
                )
            ),
            sortField = SmartPlaylistSortField.RATING,
            sortDirection = SmartPlaylistSortDirection.DESCENDING
        )
    ),
    NEVER_PLAYED(
        key = "never_played",
        nameRes = R.string.smart_template_never_played,
        descriptionRes = R.string.smart_template_never_played_description,
        generated = true,
        membershipMode = GeneratedPlaylistMembershipMode.LIVE_DERIVED,
        draft = SmartPlaylistDraft(
            rules = listOf(
                SmartPlaylistRule(
                    field = SmartPlaylistRuleField.NEVER_PLAYED,
                    operator = SmartPlaylistOperator.IS,
                    values = listOf("true")
                )
            ),
            sortField = SmartPlaylistSortField.TITLE,
            sortDirection = SmartPlaylistSortDirection.ASCENDING
        )
    );

    val refreshPolicy: String
        get() = if (membershipMode == GeneratedPlaylistMembershipMode.SNAPSHOT) {
            GeneratedPlaylistRefreshPolicy.ON_OPEN_IF_STALE
        } else {
            GeneratedPlaylistRefreshPolicy.MANUAL
        }

    val refreshIntervalMillis: Long?
        get() = SMART_PLAYLIST_GENERATED_REFRESH_INTERVAL_MILLIS.takeIf {
            membershipMode == GeneratedPlaylistMembershipMode.SNAPSHOT
        }

    companion object {
        fun fromKey(key: String): SmartPlaylistTemplate? = entries.firstOrNull { it.key == key }
    }
}
