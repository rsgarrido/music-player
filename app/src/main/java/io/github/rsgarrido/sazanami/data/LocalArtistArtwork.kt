package io.github.rsgarrido.sazanami.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import io.github.rsgarrido.sazanami.mediaaccess.FolderArtworkAccessStore
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

private const val MAX_ARTIST_IMAGE_BYTES = 25L * 1024L * 1024L

data class LocalArtistArtwork(val uri: Uri, val revision: String)

/** Only ExternalStorageProvider paths with matching MediaStore volume and full path are provable. */
internal fun matchLocalArtistArtwork(
    treeUri: Uri,
    candidates: List<LocalArtworkCandidate>,
    songs: List<Song>
): Map<String, LocalArtworkCandidate> {
    if (treeUri.authority != "com.android.externalstorage.documents") return emptyMap()
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        ?: return emptyMap()
    if (!documentId.contains(':')) return emptyMap()
    val treeVolume = documentId.substringBefore(':')
    return matchLocalArtistArtwork(treeVolume, documentId.substringAfter(':'), candidates, songs)
}

internal fun matchLocalArtistArtwork(
    treeVolume: String,
    rootPath: String,
    candidates: List<LocalArtworkCandidate>,
    songs: List<Song>
): Map<String, LocalArtworkCandidate> {
    val root = pathSegments(rootPath) ?: return emptyMap()
    val rootKey = root.map { it.lowercase(Locale.ROOT) }
    val artistsByDirectory = mutableMapOf<List<String>, MutableSet<String>>()
    songs.forEach { song ->
        if (!volumeMatches(treeVolume, song.volumeName)) return@forEach
        val path = pathSegments(song.relativePath)?.map { it.lowercase(Locale.ROOT) }
            ?: return@forEach
        if (!path.startsWithSegments(rootKey)) return@forEach
        val artistKey = artistIdentity(song.artist).key
        for (depth in rootKey.size..path.size) {
            artistsByDirectory.getOrPut(path.take(depth)) { mutableSetOf() }.add(artistKey)
        }
    }
    val qualified = mutableMapOf<String, MutableMap<String, LocalArtworkCandidate>>()
    candidates.forEach { candidate ->
        val relative = pathSegments(candidate.relativeDirectory) ?: return@forEach
        val fullDirectory = root + relative
        val directoryName = fullDirectory.lastOrNull() ?: return@forEach
        val identity = artistIdentity(directoryName)
        if (identity.isUnknown) return@forEach
        val owners = artistsByDirectory[fullDirectory.map { it.lowercase(Locale.ROOT) }]
        if (owners != setOf(identity.key)) {
            return@forEach
        }
        qualified.getOrPut(identity.key) { linkedMapOf() }[candidate.uri.toString()] = candidate
    }
    return qualified.mapNotNull { (key, images) ->
        images.values.singleOrNull()?.let { key to it }
    }.toMap()
}

private fun volumeMatches(treeVolume: String, mediaVolume: String): Boolean =
    mediaVolume.isNotBlank() && (treeVolume.equals(mediaVolume, true) ||
        (treeVolume.equals("primary", true) && mediaVolume.equals("external_primary", true)))

private fun pathSegments(path: String): List<String>? {
    val parts = path.replace('\\', '/').split('/').filter(String::isNotBlank)
    if (parts.any { it == "." || it == ".." }) return null
    return parts
}

private fun List<String>.startsWithSegments(prefix: List<String>): Boolean =
    size >= prefix.size && prefix.indices.all { index -> this[index].equals(prefix[index], true) }

/** Content digests stay stable for unchanged images and change even with an unchanged SAF URI. */
internal fun resolveLocalArtistArtwork(
    context: Context,
    treeUri: Uri,
    snapshot: FolderArtworkSnapshot,
    songs: List<Song>
): Map<String, LocalArtistArtwork> = matchLocalArtistArtwork(treeUri, snapshot.artists, songs)
    .mapNotNull { (key, candidate) ->
        if (candidate.sizeBytes != null && candidate.sizeBytes > MAX_ARTIST_IMAGE_BYTES) return@mapNotNull null
        hashArtistImage(context, candidate.uri)?.let { key to LocalArtistArtwork(candidate.uri, it) }
    }.toMap()

internal fun hashArtistImage(context: Context, uri: Uri): String? = runCatching {
    val digest = MessageDigest.getInstance("SHA-256")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_ARTIST_IMAGE_BYTES) return@runCatching null
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}.getOrNull()

/** Regenerable cross-process snapshot; it never represents a manual assignment or backup item. */
internal class LocalArtistArtworkStore(context: Context) {
    private val appContext = context.applicationContext ?: context
    private val preferences = appContext.getSharedPreferences("local_artist_artwork", Context.MODE_PRIVATE)

    fun readActive(): Map<String, LocalArtistArtwork> {
        val tree = FolderArtworkAccessStore(appContext).readState().treeUri ?: return emptyMap()
        return readForTree(tree)
    }

    fun readForTree(tree: Uri?): Map<String, LocalArtistArtwork> {
        tree ?: return emptyMap()
        val granted = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == tree && it.isReadPermission
        }
        if (!granted || preferences.getString("tree", null) != tree.toString()) return emptyMap()
        return runCatching {
            val rows = JSONArray(preferences.getString("entries", "[]"))
            buildMap {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val key = row.getString("key")
                    val uri = Uri.parse(row.getString("uri"))
                    val revision = row.getString("revision")
                    if (key.startsWith("artist_") && uri.scheme == "content" && revision.isNotBlank()) {
                        put(key, LocalArtistArtwork(uri, revision))
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun write(tree: Uri?, artwork: Map<String, LocalArtistArtwork>) {
        if (tree == null) { clear(); return }
        val rows = JSONArray()
        artwork.toSortedMap().forEach { (key, item) ->
            rows.put(JSONObject().put("key", key).put("uri", item.uri.toString())
                .put("revision", item.revision))
        }
        preferences.edit().putString("tree", tree.toString())
            .putString("entries", rows.toString()).apply()
    }

    fun clear() { preferences.edit().remove("tree").remove("entries").apply() }

}
