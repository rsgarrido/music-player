package io.github.rsgarrido.sazanami.ui.equalizer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import io.github.rsgarrido.sazanami.player.equalizer.interchange.SazanamiPresetFile
import io.github.rsgarrido.sazanami.player.equalizer.interchange.SazanamiPresetFileJson
import io.github.rsgarrido.sazanami.player.equalizer.interchange.EqualizerProfileExporter
import io.github.rsgarrido.sazanami.player.equalizer.interchange.EqualizerProfileLimits
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricEqualizerPreset
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricEqualizerState
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class EqualizerProfilePlatformActions(
    val importFromFile: () -> Unit,
    val pasteEqText: () -> Unit,
    val exportCurrentText: () -> Unit,
    val copyCurrentText: () -> Unit,
    val exportCurrentNative: () -> Unit,
    val exportPresetText: (ParametricEqualizerPreset) -> Unit,
    val exportPresetNative: (ParametricEqualizerPreset) -> Unit
)

@Composable
internal fun rememberEqualizerProfilePlatformActions(
    snackbarHostState: SnackbarHostState,
    currentState: ParametricEqualizerState,
    currentName: String,
    onImportText: (String, String?) -> Unit
): EqualizerProfilePlatformActions {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val latestState by rememberUpdatedState(currentState)
    val latestName by rememberUpdatedState(currentName)
    var pendingTextExport by remember {
        mutableStateOf<PendingProfileExport?>(null)
    }
    var pendingNativeExport by remember {
        mutableStateOf<PendingProfileExport?>(null)
    }

    fun show(@StringRes messageRes: Int) {
        scope.launch {
            snackbarHostState.showSnackbar(
                resources.getString(messageRes),
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        readSelectedProfile(context, uri)
                    }
                }.fold(
                    onSuccess = { selected ->
                        onImportText(
                            selected.text,
                            selected.displayName
                        )
                    },
                    onFailure = { error ->
                        Log.w("EqualizerImport", "Could not open EQ profile", error)
                        show(R.string.eq_open_failed)
                    }
                )
            }
        }
    }
    val createTextDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val export = pendingTextExport
        pendingTextExport = null
        if (uri != null && export != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        writeProfile(context, uri, export.content)
                    }
                }.fold(
                    onSuccess = { show(R.string.eq_text_exported) },
                    onFailure = { error ->
                        Log.w("EqualizerExport", "Could not export EQ text", error)
                        show(R.string.eq_text_export_failed)
                    }
                )
            }
        }
    }
    val createNativeDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val export = pendingNativeExport
        pendingNativeExport = null
        if (uri != null && export != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        writeProfile(context, uri, export.content)
                    }
                }.fold(
                    onSuccess = {
                        show(R.string.eq_native_exported)
                    },
                    onFailure = { error ->
                        Log.w("EqualizerExport", "Could not export Sazanami preset", error)
                        show(R.string.eq_native_export_failed)
                    }
                )
            }
        }
    }

    fun nativeFile(
        name: String,
        state: ParametricEqualizerState
    ) = SazanamiPresetFileJson.encode(
        SazanamiPresetFile(
            name = name,
            preampDb = state.preampDb,
            automaticHeadroomEnabled =
                state.automaticHeadroomEnabled,
            filters = state.filters
        )
    )

    return EqualizerProfilePlatformActions(
        importFromFile = {
            openDocument.launch(
                arrayOf(
                    "text/plain",
                    "application/json",
                    "application/octet-stream",
                    "*/*"
                )
            )
        },
        pasteEqText = {
            val clipboard = context.getSystemService(
                ClipboardManager::class.java
            )
            val clip = clipboard.primaryClip
            val description = clipboard.primaryClipDescription
            if (
                clip == null || clip.itemCount == 0 ||
                description == null ||
                !description.hasMimeType("text/*")
            ) {
                show(R.string.eq_clipboard_missing)
            } else {
                val text = clip.getItemAt(0).text?.toString()
                if (text.isNullOrBlank()) {
                    show(R.string.eq_clipboard_empty)
                } else {
                    onImportText(text, resources.getString(R.string.eq_clipboard_source))
                }
            }
        },
        exportCurrentText = {
            val name = sanitizedProfileFilename(latestName)
            pendingTextExport = PendingProfileExport(
                filename = "$name ParametricEQ.txt",
                content =
                    EqualizerProfileExporter.exportText(latestState)
            )
            createTextDocument.launch(
                pendingTextExport!!.filename
            )
        },
        copyCurrentText = {
            val clipboard = context.getSystemService(
                ClipboardManager::class.java
            )
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    resources.getString(R.string.eq_clipboard_label),
                    EqualizerProfileExporter.exportText(latestState)
                )
            )
            show(R.string.eq_text_copied)
        },
        exportCurrentNative = {
            val safeName = if (
                latestName.equals("Flat", true) ||
                latestName.equals("Custom", true)
            ) {
                "Current EQ"
            } else {
                latestName
            }
            pendingNativeExport = PendingProfileExport(
                filename =
                    "${sanitizedProfileFilename(safeName)}.sazeq",
                content = nativeFile(safeName, latestState)
            )
            createNativeDocument.launch(
                pendingNativeExport!!.filename
            )
        },
        exportPresetText = { preset ->
            pendingTextExport = PendingProfileExport(
                filename =
                    "${sanitizedProfileFilename(preset.name)} " +
                        "ParametricEQ.txt",
                content = EqualizerProfileExporter.exportText(
                    ParametricEqualizerState(
                        preampDb = preset.preampDb,
                        automaticHeadroomEnabled =
                            preset.automaticHeadroomEnabled,
                        filters = preset.filters
                    )
                )
            )
            createTextDocument.launch(
                pendingTextExport!!.filename
            )
        },
        exportPresetNative = { preset ->
            pendingNativeExport = PendingProfileExport(
                filename =
                    "${sanitizedProfileFilename(preset.name)}.sazeq",
                content = SazanamiPresetFileJson.encode(
                    SazanamiPresetFileJson.fromPreset(preset)
                )
            )
            createNativeDocument.launch(
                pendingNativeExport!!.filename
            )
        }
    )
}

internal fun sanitizedProfileFilename(name: String): String {
    val sanitized = name.trim()
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        .trimEnd('.', ' ')
        .ifBlank { "Imported EQ" }
    return sanitized.take(100)
}

private fun readSelectedProfile(
    context: Context,
    uri: Uri
): SelectedProfileText {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= EqualizerProfileLimits.MAX_INPUT_BYTES) {
                "EQ profile exceeds the 256 KiB limit."
            }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: throw IllegalArgumentException("Couldn't open selected EQ profile.")
    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val text = decoder.decode(ByteBuffer.wrap(bytes)).toString()
    return SelectedProfileText(
        text = text,
        displayName = queryDisplayName(context, uri)
    )
}

private fun queryDisplayName(
    context: Context,
    uri: Uri
): String? = context.contentResolver.query(
    uri,
    arrayOf(OpenableColumns.DISPLAY_NAME),
    null,
    null,
    null
)?.use { cursor ->
    if (cursor.moveToFirst()) cursor.getString(0) else null
}

private fun writeProfile(
    context: Context,
    uri: Uri,
    content: String
) {
    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
        output.write(content.toByteArray(StandardCharsets.UTF_8))
    } ?: throw IllegalArgumentException("Couldn't create EQ profile.")
}

private data class SelectedProfileText(
    val text: String,
    val displayName: String?
)

private data class PendingProfileExport(
    val filename: String,
    val content: String
)
