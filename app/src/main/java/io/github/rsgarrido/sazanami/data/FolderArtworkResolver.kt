package io.github.rsgarrido.sazanami.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.util.ArrayDeque

internal data class LocalArtworkCandidate(
    val relativeDirectory: String,
    val uri: Uri,
    val sizeBytes: Long?,
    val modifiedMillis: Long?
)

internal data class FolderArtworkSnapshot(
    val covers: Map<String, Uri>,
    val artists: List<LocalArtworkCandidate>,
    val complete: Boolean = true
) {
    companion object { val EMPTY = FolderArtworkSnapshot(emptyMap(), emptyList()) }
}

/** A single SAF walk supplies both existing album covers and local artist candidates. */
internal class FolderArtworkResolver(
    private val context: Context,
    private val treeUri: Uri?,
    private val suppliedSnapshot: FolderArtworkSnapshot? = null
) {
    private val snapshot: FolderArtworkSnapshot by lazy(LazyThreadSafetyMode.NONE) {
        suppliedSnapshot ?: scan(context, treeUri) ?: FolderArtworkSnapshot.EMPTY
    }

    fun resolve(song: Song): Uri? {
        if (treeUri == null || snapshot.covers.isEmpty()) return null
        val relativePath = normalizePath(song.relativePath)
        val folderPath = normalizePath(song.folderPath)
        return snapshot.covers.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (relativeFolder, _) ->
                relativeFolder.isNotBlank() && (
                    relativePath == relativeFolder ||
                        relativePath.endsWith("/$relativeFolder") ||
                        folderPath == relativeFolder ||
                        folderPath.endsWith("/$relativeFolder")
                    )
            }
            ?.value
            ?: snapshot.covers[""]?.takeIf {
                relativePath.isBlank() || !relativePath.contains('/')
            }
    }

    companion object {
        /** Null means the traversal failed; callers must not publish a partial artist index. */
        fun scan(context: Context, treeUri: Uri?): FolderArtworkSnapshot? {
            treeUri ?: return FolderArtworkSnapshot.EMPTY
            val rootDocumentId = runCatching {
                DocumentsContract.getTreeDocumentId(treeUri)
            }.getOrNull() ?: return null
            val covers = linkedMapOf<String, Uri>()
            val artists = linkedMapOf<String, Pair<Int, LocalArtworkCandidate>>()
            var complete = true
            val pending = ArrayDeque<Pair<String, String>>()
            pending.add(rootDocumentId to "")
            while (pending.isNotEmpty()) {
                val (documentId, directory) = pending.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
                val basicColumns = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                val cursor = try {
                    context.contentResolver.query(
                        childrenUri,
                        basicColumns + arrayOf(
                            DocumentsContract.Document.COLUMN_SIZE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                        ), null, null, null
                    ) ?: context.contentResolver.query(childrenUri, basicColumns, null, null, null)
                } catch (_: SecurityException) { return null }
                catch (_: Exception) {
                    try {
                        context.contentResolver.query(childrenUri, basicColumns, null, null, null)
                    } catch (_: Exception) { return null }
                }
                if (cursor == null) {
                    complete = false
                    continue
                }
                try {
                    cursor.use {
                        val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeColumn = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                        val modifiedColumn = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        while (it.moveToNext()) {
                            val childId = it.getString(idColumn) ?: continue
                            val name = it.getString(nameColumn).orEmpty()
                            if (it.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR) {
                                pending.add(childId to listOf(directory, name).filter(String::isNotBlank).joinToString("/"))
                            } else {
                                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                                if (isLikelyAlbumCoverFile(name) && directory !in covers) {
                                    covers[directory] = uri
                                }
                                val priority = artistArtworkFilenamePriority(name)
                                if (priority != null) {
                                    val candidate = LocalArtworkCandidate(
                                        directory, uri,
                                        sizeColumn.takeIf { column -> column >= 0 && !it.isNull(column) }
                                            ?.let(it::getLong),
                                        modifiedColumn.takeIf { column -> column >= 0 && !it.isNull(column) }
                                            ?.let(it::getLong)
                                    )
                                    artists[directory] = preferArtistCandidate(
                                        artists[directory], priority to candidate
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) { return null }
            }
            return FolderArtworkSnapshot(
                covers, if (complete) artists.values.map { it.second } else emptyList(), complete
            )
        }
    }
}

internal fun artistArtworkFilenamePriority(fileName: String): Int? = when (fileName.lowercase()) {
    "artist.jpg" -> 0
    "artist.jpeg" -> 1
    "artist.png" -> 2
    else -> null
}

internal fun preferArtistCandidate(
    previous: Pair<Int, LocalArtworkCandidate>?,
    incoming: Pair<Int, LocalArtworkCandidate>
): Pair<Int, LocalArtworkCandidate> = when {
    previous == null || incoming.first < previous.first -> incoming
    incoming.first > previous.first -> previous
    incoming.second.uri.toString() < previous.second.uri.toString() -> incoming
    else -> previous
}

internal fun isLikelyAlbumCoverFile(fileName: String): Boolean = when (fileName.lowercase()) {
    "cover.jpg", "cover.jpeg", "cover.png",
    "folder.jpg", "folder.jpeg", "folder.png",
    "front.jpg", "front.jpeg", "front.png",
    "album.jpg", "album.jpeg", "album.png" -> true
    else -> false
}

private fun normalizePath(path: String): String = path.replace('\\', '/').trim().trim('/')
