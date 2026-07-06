package com.example.cdplaya.data

enum class PlayerTheme(
    val id: String,
    val displayName: String
) {
    DEFAULT(
        id = "default",
        displayName = "CDPlaya Default"
    ),

    CLASSIC_WHEEL(
        id = "classic_wheel",
        displayName = "Classic Wheel"
    );

    companion object {
        fun fromId(id: String?): PlayerTheme {
            return values().firstOrNull { playerTheme ->
                playerTheme.id == id
            } ?: DEFAULT
        }
    }
}