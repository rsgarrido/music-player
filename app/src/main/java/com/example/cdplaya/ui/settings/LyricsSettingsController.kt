package com.example.cdplaya.ui.settings

import android.content.Context
import android.net.Uri
import com.example.cdplaya.lyrics.AndroidLyricsFolderAccess
import com.example.cdplaya.lyrics.LocalLyricsRepository
import com.example.cdplaya.lyrics.LocalLyricsServices
import com.example.cdplaya.lyrics.LyricsRoot
import com.example.cdplaya.lyrics.LyricsRootIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LyricsFolderUiState(
    val roots: List<LyricsFolderUiItem> = emptyList(),
    val indexedFileCount: Int = 0,
    val isScanning: Boolean = false,
    val message: String? = null
)

data class LyricsFolderUiItem(
    val root: LyricsRoot,
    val hasPersistedAccess: Boolean
)

class LyricsSettingsController private constructor(
    private val repository: LocalLyricsRepository,
    private val folderAccess: AndroidLyricsFolderAccess
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(LyricsFolderUiState())
    val state: StateFlow<LyricsFolderUiState> = _state.asStateFlow()

    init {
        scope.launch {
            repository.roots.collectLatest { roots ->
                _state.value = _state.value.copy(
                    roots = roots.map { root ->
                        LyricsFolderUiItem(
                            root = root,
                            hasPersistedAccess = folderAccess.hasReadAccess(root.uri)
                        )
                    }
                )
            }
        }
    }

    fun addRoot(uri: Uri) {
        launchScan {
            val root = withContext(Dispatchers.IO) {
                folderAccess.retainReadAccess(uri).getOrElse {
                    throw LyricsFolderAccessException()
                }
            }
            repository.addRoot(root).snapshot
        }
    }

    fun removeRoot(rootUri: String) {
        launchScan { repository.removeRoot(rootUri).snapshot }
    }

    fun rescan() {
        launchScan { repository.refreshIndex().snapshot }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun launchScan(block: suspend () -> com.example.cdplaya.lyrics.LyricsIndexSnapshot) {
        if (_state.value.isScanning) return
        _state.value = _state.value.copy(isScanning = true, message = null)
        scope.launch {
            runCatching { block() }.fold(
                onSuccess = { snapshot ->
                    val permissionCount = snapshot.issues.count {
                        it.kind == LyricsRootIssue.Kind.PERMISSION_LOST
                    }
                    val failureCount = snapshot.issues.size - permissionCount
                    val message = when {
                        permissionCount > 0 ->
                            "$permissionCount lyrics folder permission(s) are missing."
                        failureCount > 0 ->
                            "$failureCount lyrics folder(s) could not be scanned."
                        else -> null
                    }
                    _state.value = _state.value.copy(
                        roots = repository.roots.value.map { root ->
                            LyricsFolderUiItem(
                                root = root,
                                hasPersistedAccess = folderAccess.hasReadAccess(root.uri)
                            )
                        },
                        indexedFileCount = snapshot.files.size,
                        isScanning = false,
                        message = message
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isScanning = false,
                        message = if (error is LyricsFolderAccessException) {
                            "CDPlaya could not retain access to that folder."
                        } else {
                            "Lyrics folders could not be scanned."
                        }
                    )
                }
            )
        }
    }

    companion object {
        @Volatile
        private var instance: LyricsSettingsController? = null

        fun shared(context: Context): LyricsSettingsController =
            instance ?: synchronized(this) {
                instance ?: LocalLyricsServices.shared(context).let { services ->
                    LyricsSettingsController(
                        repository = services.repository,
                        folderAccess = services.folderAccess
                    )
                }.also { instance = it }
            }
    }
}

private class LyricsFolderAccessException : Exception()
