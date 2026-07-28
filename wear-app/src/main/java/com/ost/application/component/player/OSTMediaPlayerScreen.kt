package com.ost.application.component.player

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.material3.components.actions.SettingsButton
import com.google.android.horologist.audio.ui.material3.components.actions.VolumeButtonWithBadge
import com.google.android.horologist.audio.ui.material3.components.toAudioOutputUi
import com.google.android.horologist.images.coil.CoilPaintable
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.components.background.ArtworkImageBackground
import com.google.android.horologist.media.ui.material3.components.background.ColorBackground
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerViewModel
import com.google.android.horologist.media.ui.state.model.MediaUiModel

@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    onFavoriteClick: (() -> Unit)? = null
) {
    var faved by remember { mutableStateOf(false) }
    SettingsButton(
        modifier = modifier,
        onClick = {
            faved = !faved
            onFavoriteClick?.invoke()
        },
        imageVector = if (faved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = androidx.compose.ui.res.stringResource(com.ost.application.R.string.favorite)
    )
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun UampSettingsButtons(
    volumeViewModel: VolumeViewModel,
    onVolumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val volumeUiState by volumeViewModel.volumeUiState.collectAsStateWithLifecycle()
    val audioOutput by volumeViewModel.audioOutput.collectAsStateWithLifecycle()

    val windowInfo = LocalWindowInfo.current
    val horizontalPadding = remember(windowInfo.containerSize) {
        (windowInfo.containerSize.width / 10f).dp
    }
    val bottomPadding = remember(windowInfo.containerSize) {
        (windowInfo.containerSize.height / 100f).dp
    }

    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            VolumeButtonWithBadge(
                onOutputClick = onVolumeClick,
                audioOutputUi = audioOutput.toAudioOutputUi(),
                volumeUiState = volumeUiState,
                enabled = enabled
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            FavoriteButton()
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun OSTMediaPlayerScreen(
    playerViewModel: PlayerViewModel,
    volumeViewModel: VolumeViewModel,
    artBitmap: Bitmap?,
    onVolumeClick: () -> Unit,
    fallbackTitle: String? = null,
    fallbackSubtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val adaptiveColorScheme = rememberAdaptiveColorScheme(artBitmap)

    MaterialTheme(colorScheme = adaptiveColorScheme) {
        PlayerScreen(
            modifier = modifier.fillMaxSize(),
            background = { playerUiState ->
                val readyModel = playerUiState.media as? MediaUiModel.Ready
                val artworkColor = readyModel?.artworkColor
                if (artworkColor != null) {
                    ColorBackground(
                        color = artworkColor,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (artBitmap != null) {
                    val paintable = remember(artBitmap) {
                        CoilPaintable(artBitmap)
                    }
                    ArtworkImageBackground(
                        artwork = paintable,
                        colorScheme = MaterialTheme.colorScheme,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ArtworkImageBackground(
                        artwork = readyModel?.artwork,
                        colorScheme = MaterialTheme.colorScheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            playerViewModel = playerViewModel,
            volumeViewModel = volumeViewModel,
            mediaDisplay = { playerUiState ->
                AnimatedMediaInfoDisplay(
                    media = playerUiState.media,
                    loading = !playerUiState.connected || playerUiState.media is MediaUiModel.Loading
                )
            },
            buttons = { playerUiState ->
                UampSettingsButtons(
                    volumeViewModel = volumeViewModel,
                    onVolumeClick = onVolumeClick,
                    enabled = playerUiState.connected && playerUiState.media != null
                )
            },
            controlButtons = { playerUiController, playerUiState ->
                AnimatedMediaControlButtons(
                    onPlayButtonClick = { playerUiController.play() },
                    onPauseButtonClick = { playerUiController.pause() },
                    playPauseButtonEnabled = playerUiState.playPauseEnabled,
                    playing = playerUiState.playing,
                    onSeekToPreviousButtonClick = { playerUiController.skipToPreviousMedia() },
                    onSeekToPreviousRepeatableClick = { playerUiController.seekBack() },
                    seekToPreviousButtonEnabled = playerUiState.seekToPreviousEnabled,
                    onSeekToNextButtonClick = { playerUiController.skipToNextMedia() },
                    onSeekToNextRepeatableClick = { playerUiController.seekForward() },
                    seekToNextButtonEnabled = playerUiState.seekToNextEnabled,
                    trackPositionUiModel = playerUiState.trackPositionUiModel
                )
            }
        )
    }
}
