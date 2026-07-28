package com.ost.application.explorer.music
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long
)
data class AlbumEntry(val name: String, val albumId: Long, val artist: String, val tracks: List<Track>)
data class ArtistEntry(val name: String, val tracks: List<Track>)
object MusicLibrary {
    private val ALBUM_ART_BASE = Uri.parse("content://media/external/audio/albumart")
    suspend fun loadTracks(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    tracks.add(
                        Track(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            title = cursor.getString(titleCol) ?: "Unknown",
                            artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                            album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Unknown album",
                            albumId = cursor.getLong(albumIdCol),
                            durationMs = cursor.getLong(durationCol),
                            dateAdded = cursor.getLong(dateCol)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        tracks
    }
    fun recentlyAdded(tracks: List<Track>, limit: Int = 25): List<Track> =
        tracks.sortedByDescending { it.dateAdded }.take(limit)
    fun albums(tracks: List<Track>): List<AlbumEntry> =
        tracks.groupBy { it.albumId }
            .map { (albumId, albumTracks) ->
                AlbumEntry(
                    name = albumTracks.first().album,
                    albumId = albumId,
                    artist = albumTracks.first().artist,
                    tracks = albumTracks
                )
            }
            .sortedBy { it.name.lowercase() }
    fun artists(tracks: List<Track>): List<ArtistEntry> =
        tracks.groupBy { it.artist }
            .map { (name, artistTracks) -> ArtistEntry(name, artistTracks) }
            .sortedBy { it.name.lowercase() }
    fun albumArtUri(albumId: Long): Uri = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)
    fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri(albumId))
                .build()
        )
        .build()
}
