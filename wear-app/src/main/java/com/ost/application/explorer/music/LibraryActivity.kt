@file:OptIn(ExperimentalHorologistApi::class)
package com.ost.application.explorer.music
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.ost.application.component.player.OSTMediaPlayerScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TitleCard
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.material3.VolumeScreen
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import com.google.common.util.concurrent.ListenableFuture
import com.ost.application.R
import com.ost.application.explorer.music.MusicLibrary.toMediaItem
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.ThumbnailCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val _tracks = MutableStateFlow<List<Track>?>(null)
    val tracks: StateFlow<List<Track>?> = _tracks.asStateFlow()
    init {
        viewModelScope.launch { _tracks.value = MusicLibrary.loadTracks(getApplication()) }
    }
}
private object LastQueueStore {
    private const val PREFS = "player_prefs"
    fun save(context: Context, tracks: List<Track>, index: Int, positionMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("queue_ids", tracks.joinToString(",") { it.id.toString() })
            putInt("queue_index", index)
            putLong("queue_position", positionMs)
        }
    }
    fun savePosition(context: Context, index: Int, positionMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putInt("queue_index", index)
            putLong("queue_position", positionMs)
        }
    }
    fun load(context: Context, allTracks: List<Track>): Triple<List<Track>, Int, Long>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getString("queue_ids", null)?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: return null
        val byId = allTracks.associateBy { it.id }
        val queue = ids.mapNotNull { byId[it] }
        if (queue.isEmpty()) return null
        val index = prefs.getInt("queue_index", 0).coerceIn(0, queue.lastIndex)
        val position = prefs.getLong("queue_position", 0L).coerceAtLeast(0L)
        return Triple(queue, index, position)
    }
}
class LibraryActivity : ComponentActivity() {
    private var player: Player? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controllerState = mutableStateOf<MediaController?>(null)
    private val musicViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val activePlayer = player ?: throw IllegalStateException("Player not initialized")
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(activePlayer, PlayerRepositoryImpl()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    private val musicViewModel: MusicViewModel by viewModels { musicViewModelFactory }
    private val volumeViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VolumeViewModel::class.java)) {
                val audioRepository = SystemAudioRepository.fromContext(applicationContext)
                val vibrator = applicationContext.getSystemService(VIBRATOR_SERVICE) as Vibrator
                return VolumeViewModel(
                    volumeRepository = audioRepository,
                    audioOutputRepository = audioRepository,
                    vibrator = vibrator,
                    onCleared = { audioRepository.close() }
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    private val volumeViewModel: VolumeViewModel by viewModels { volumeViewModelFactory }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val future = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        ).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                player = controller
                controllerState.value = controller
            }
        }, ContextCompat.getMainExecutor(this))
        setContent {
            OSTToolsTheme {
                val controller = controllerState.value
                LibraryRoot(
                    controller = controller,
                    nowPlaying = {
                        if (controller != null) {
                            NowPlayingPage(controller, musicViewModel, volumeViewModel)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                )
            }
        }
    }
    override fun onStop() {
        super.onStop()
        controllerState.value?.let { controller ->
            if (controller.mediaItemCount > 0) {
                LastQueueStore.savePosition(this, controller.currentMediaItemIndex, controller.currentPosition)
            }
        }
    }
    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        player = null
        super.onDestroy()
    }
}
private fun playQueue(context: Context, controller: MediaController, tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
    if (tracks.isEmpty()) return
    controller.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
    controller.shuffleModeEnabled = shuffle
    controller.prepare()
    controller.play()
    LastQueueStore.save(context, tracks, startIndex, 0L)
}
@Composable
fun LibraryRoot(
    controller: MediaController?,
    nowPlaying: @Composable () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    LaunchedEffect(controller, tracks) {
        val readyController = controller ?: return@LaunchedEffect
        val allTracks = tracks ?: return@LaunchedEffect
        if (readyController.mediaItemCount == 0) {
            LastQueueStore.load(context, allTracks)?.let { (queue, index, position) ->
                readyController.setMediaItems(queue.map { it.toMediaItem() }, index, position)
                readyController.prepare()
            }
        }
    }
    val pagerState = rememberPagerState { 2 }
    AppScaffold(timeText = { TimeText() }) {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(state = pagerState) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    when (page) {
                        0 -> nowPlaying()
                        else -> LibraryPage(controller, tracks)
                    }
                }
            }
        }
    }
}
@Composable
private fun NowPlayingPage(
    controller: MediaController,
    musicViewModel: MusicViewModel,
    volumeViewModel: VolumeViewModel
) {
    val context = LocalContext.current
    var metadata by remember { mutableStateOf(controller.mediaMetadata) }
    var hasItems by remember { mutableStateOf(controller.mediaItemCount > 0) }
    var shuffleOn by remember { mutableStateOf(controller.shuffleModeEnabled) }
    var showVolumeScreen by remember { mutableStateOf(false) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                metadata = mediaMetadata
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                hasItems = controller.mediaItemCount > 0
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleOn = shuffleModeEnabled
            }
        }
        controller.addListener(listener)
        metadata = controller.mediaMetadata
        hasItems = controller.mediaItemCount > 0
        shuffleOn = controller.shuffleModeEnabled
        onDispose { controller.removeListener(listener) }
    }
    var art by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(metadata.artworkUri) {
        art = metadata.artworkUri?.let { ThumbnailCache.loadFromUri(context, "art:$it", it) }
    }
    if (showVolumeScreen) {
        Dialog(onDismissRequest = { showVolumeScreen = false }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                VolumeScreen(volumeViewModel = volumeViewModel)
            }
        }
    }
    ScreenScaffold {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (!hasItems) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.nothing_playing),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                OSTMediaPlayerScreen(
                    playerViewModel = musicViewModel,
                    volumeViewModel = volumeViewModel,
                    artBitmap = art,
                    onVolumeClick = { showVolumeScreen = true },
                    fallbackTitle = metadata.title?.toString(),
                    fallbackSubtitle = metadata.artist?.toString()
                )
            }
        }
    }
}
private sealed class LibDest {
    object Root : LibDest()
    object Recent : LibDest()
    object Artists : LibDest()
    object Albums : LibDest()
    object Songs : LibDest()
    data class AlbumDetail(val album: AlbumEntry) : LibDest()
    data class ArtistDetail(val artist: ArtistEntry) : LibDest()
}
@Composable
private fun LibraryPage(controller: MediaController?, tracks: List<Track>?) {
    val context = LocalContext.current
    var dest by remember { mutableStateOf<LibDest>(LibDest.Root) }
    BackHandler(enabled = dest != LibDest.Root) {
        dest = when (dest) {
            is LibDest.AlbumDetail -> LibDest.Albums
            is LibDest.ArtistDetail -> LibDest.Artists
            else -> LibDest.Root
        }
    }
    if (tracks == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    fun play(queue: List<Track>, index: Int, shuffle: Boolean = false) {
        controller?.let { playQueue(context, it, queue, index, shuffle) }
    }
    when (val current = dest) {
        LibDest.Root -> LibraryRootList(
            trackCount = tracks.size,
            onShuffleAll = { play(tracks.shuffled(), 0) },
            onRecent = { dest = LibDest.Recent },
            onArtists = { dest = LibDest.Artists },
            onAlbums = { dest = LibDest.Albums },
            onSongs = { dest = LibDest.Songs }
        )
        LibDest.Recent -> TrackList(
            title = stringResource(R.string.recently_added),
            tracks = MusicLibrary.recentlyAdded(tracks),
            withLetters = false,
            onPlay = ::play
        )
        LibDest.Songs -> TrackList(
            title = stringResource(R.string.songs),
            tracks = tracks,
            withLetters = true,
            onPlay = ::play
        )
        LibDest.Albums -> GroupList(
            title = stringResource(R.string.albums),
            entries = MusicLibrary.albums(tracks).map { album ->
                GroupRow(album.name, album.artist, album.albumId, album.tracks.size) { dest = LibDest.AlbumDetail(album) }
            }
        )
        LibDest.Artists -> GroupList(
            title = stringResource(R.string.artists),
            entries = MusicLibrary.artists(tracks).map { artist ->
                GroupRow(artist.name, null, artist.tracks.firstOrNull()?.albumId, artist.tracks.size) {
                    dest = LibDest.ArtistDetail(artist)
                }
            }
        )
        is LibDest.AlbumDetail -> TrackList(
            title = current.album.name,
            tracks = current.album.tracks,
            withLetters = false,
            entityActions = true,
            onPlay = ::play
        )
        is LibDest.ArtistDetail -> TrackList(
            title = current.artist.name,
            tracks = current.artist.tracks,
            withLetters = false,
            entityActions = true,
            onPlay = ::play
        )
    }
}
@Composable
private fun LibraryRootList(
    trackCount: Int,
    onShuffleAll: () -> Unit,
    onRecent: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onSongs: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter
        ) {
            item { ListHeader { Text(stringResource(R.string.music)) } }
            item {
                LibraryChip(stringResource(R.string.shuffle_all), stringResource(R.string.tracks_count, trackCount), R.drawable.ic_shuffle_24dp, onShuffleAll)
            }
            item { LibraryChip(stringResource(R.string.recently_added), null, R.drawable.ic_schedule_24dp, onRecent) }
            item { LibraryChip(stringResource(R.string.artists), null, R.drawable.ic_person_24dp, onArtists) }
            item { LibraryChip(stringResource(R.string.albums), null, R.drawable.ic_album_24dp, onAlbums) }
            item { LibraryChip(stringResource(R.string.songs), null, R.drawable.ic_music_24dp, onSongs) }
        }
    }
}
@Composable
private fun LibraryChip(label: String, secondaryLabel: String?, icon: Int, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = secondaryLabel?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ChipDefaults.IconSize)
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
private data class GroupRow(
    val title: String,
    val subtitle: String?,
    val artAlbumId: Long?,
    val trackCount: Int,
    val onClick: () -> Unit
)
@Composable
private fun GroupList(title: String, entries: List<GroupRow>) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter
        ) {
            item { ListHeader { Text(title) } }
            items(entries.size, key = { entries[it].title + it }) { index ->
                val entry = entries[index]
                val position = when {
                    entries.size == 1 -> CardPosition.SINGLE
                    index == 0 -> CardPosition.TOP
                    index == entries.lastIndex -> CardPosition.BOTTOM
                    else -> CardPosition.MIDDLE
                }
                ArtworkAppCard(
                    title = entry.title,
                    summary = entry.subtitle,
                    time = stringResource(R.string.tracks_count, entry.trackCount),
                    albumId = entry.artAlbumId,
                    position = position,
                    onClick = entry.onClick
                )
            }
        }
    }
}
@Composable
private fun TrackList(
    title: String,
    tracks: List<Track>,
    withLetters: Boolean,
    entityActions: Boolean = false,
    onPlay: (queue: List<Track>, index: Int, shuffle: Boolean) -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter
        ) {
            item { ListHeader { Text(title) } }
            if (entityActions) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(onClick = { onPlay(tracks, 0, false) }) {
                            Icon(painterResource(R.drawable.ic_play_24dp), contentDescription = "Play")
                        }
                        FilledTonalIconButton(onClick = { onPlay(tracks.shuffled(), 0, true) }) {
                            Icon(painterResource(R.drawable.ic_shuffle_24dp), contentDescription = "Shuffle")
                        }
                    }
                }
            }
            var lastLetter: Char? = null
            tracks.forEachIndexed { index, track ->
                if (withLetters) {
                    val letter = track.title.firstOrNull()?.uppercaseChar()?.let {
                        if (it.isLetter()) it else '#'
                    } ?: '#'
                    if (letter != lastLetter) {
                        lastLetter = letter
                        item(key = "letter_$letter") {
                            ListHeader { Text(letter.toString(), color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                item(key = "track_${track.id}_$index") {
                    val position = when {
                        tracks.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == tracks.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }
                    ArtworkAppCard(
                        title = track.title,
                        summary = "${track.artist} • ${track.album}",
                        time = formatDuration(track.durationMs),
                        albumId = track.albumId,
                        position = position,
                        onClick = { onPlay(tracks, index, false) }
                    )
                }
            }
        }
    }
}
@Composable
private fun ArtworkAppCard(
    title: String,
    summary: String?,
    time: String,
    albumId: Long?,
    position: CardPosition,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var art by remember(albumId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(albumId) {
        art = albumId?.let {
            ThumbnailCache.loadFromUri(context, "album:$it", MusicLibrary.albumArtUri(it))
        }
    }
    val shape = cardShape(position)
    val currentArt = art
    if (currentArt != null) {
        TitleCard(
            onClick = onClick,
            containerPainter = CardDefaults.containerPainter(
                image = BitmapPainter(currentArt.asImageBitmap()),
                scrim = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.85f),
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.25f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardWithContainerPainterColors(),
            title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            time = { Text(time) },
            subtitle = summary?.let {
                { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        )
    } else {
        TitleCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            time = { Text(time) },
            subtitle = summary?.let {
                { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        )
    }
}
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
private fun cardShape(position: CardPosition): RoundedCornerShape {
    val large = 24.dp
    val small = 4.dp
    return when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        CardPosition.MIDDLE -> RoundedCornerShape(small)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        CardPosition.SINGLE -> RoundedCornerShape(large)
    }
}
