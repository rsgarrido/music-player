package com.example.cdplaya.ui.state

import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.library.LibraryGridColumns
import com.example.cdplaya.ui.library.LibraryViewMode
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.defaultTokens

data class PlayerAppearanceUiState(
    val selectedTheme: PlayerTheme = PlayerTheme.DEFAULT,
    val themeTokens: PlayerThemeTokens = PlayerTheme.DEFAULT.defaultTokens(),
    val modernArtworkTransitionStyle: ModernArtworkTransitionStyle =
        ModernArtworkTransitionStyle.SLIDE,
    val modernSeekbarStyle: ModernSeekbarStyle = ModernSeekbarStyle.CLASSIC_BAR,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val isLoaded: Boolean = false
)

data class LibraryCategoryAppearance(
    val viewMode: LibraryViewMode = LibraryViewMode.LIST,
    val gridColumnCount: Int = LibraryGridColumns.DEFAULT
)

data class LibraryAppearanceUiState(
    val songs: LibraryCategoryAppearance = LibraryCategoryAppearance(),
    val albums: LibraryCategoryAppearance = LibraryCategoryAppearance(),
    val artists: LibraryCategoryAppearance = LibraryCategoryAppearance(),
    val isLoaded: Boolean = false
)
