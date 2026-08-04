package com.example.cdplaya.data.local

import androidx.room.TypeConverter

enum class ListeningSource(val storageValue: String) {
    CDPLAYA("cdplaya"),
    SPOTIFY_IMPORT("spotify_import"),
    LASTFM_IMPORT("lastfm_import");

    companion object {
        fun fromStorageValue(value: String): ListeningSource =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown listening source: $value")
    }
}

enum class ListeningQualificationReason(val storageValue: String) {
    NONE("none"),
    TIME_THRESHOLD("time_threshold"),
    NATURAL_END("natural_end");

    companion object {
        fun fromStorageValue(value: String): ListeningQualificationReason =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown listening qualification reason: $value")
    }
}

enum class ListeningEndReason(val storageValue: String) {
    NATURAL_END("natural_end"),
    TRANSITION("transition"),
    STOPPED("stopped"),
    ERROR("error"),
    UNKNOWN("unknown");

    companion object {
        fun fromStorageValue(value: String): ListeningEndReason =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown listening end reason: $value")
    }
}

class ListeningHistoryTypeConverters {
    @TypeConverter
    fun listeningSourceToString(value: ListeningSource): String = value.storageValue

    @TypeConverter
    fun stringToListeningSource(value: String): ListeningSource =
        ListeningSource.fromStorageValue(value)

    @TypeConverter
    fun qualificationReasonToString(value: ListeningQualificationReason): String =
        value.storageValue

    @TypeConverter
    fun stringToQualificationReason(value: String): ListeningQualificationReason =
        ListeningQualificationReason.fromStorageValue(value)

    @TypeConverter
    fun endReasonToString(value: ListeningEndReason): String = value.storageValue

    @TypeConverter
    fun stringToEndReason(value: String): ListeningEndReason =
        ListeningEndReason.fromStorageValue(value)
}
