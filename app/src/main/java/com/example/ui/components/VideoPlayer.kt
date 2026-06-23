package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    videoResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    isMuted: Boolean = false,
    reloadTrigger: Int = 0,
    onPlayerCreated: (ExoPlayer?) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var reconnectTrigger by remember { mutableStateOf(0) }
    var reconnectAttempts by remember { mutableStateOf(0) }
    val maxReconnectAttempts = 5
    var isRetrying by remember { mutableStateOf(false) }
    var reconnectStatusMessage by remember { mutableStateOf<String?>(null) }

    // Instantiate high-quality Media3 ExoPlayer instance
    val exoplayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Call callback when player changes or is disposed
    DisposableEffect(exoplayer) {
        onPlayerCreated(exoplayer)
        onDispose {
            onPlayerCreated(null)
        }
    }

    // Reset retry states when the main playlist or stream URL changes
    LaunchedEffect(url) {
        reconnectAttempts = 0
        isRetrying = false
        reconnectStatusMessage = null
    }

    // Handle updates when the media source URL changes, reload is triggered, or automatic reconnect is scheduled
    LaunchedEffect(url, reloadTrigger, reconnectTrigger) {
        if (url.isNotBlank()) {
            try {
                val mediaItem = MediaItem.fromUri(url)
                exoplayer.stop()
                exoplayer.clearMediaItems()
                exoplayer.setMediaItem(mediaItem)
                exoplayer.prepare()
                exoplayer.play()
            } catch (e: Exception) {
                onError("Unable to initialize stream source: ${e.localizedMessage}")
            }
        }
    }

    // Capture playback failures and relay to state
    DisposableEffect(exoplayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++
                    isRetrying = true
                    reconnectStatusMessage = "Stream connection lost. Reconnecting... (Attempt $reconnectAttempts of $maxReconnectAttempts)"
                    onError("Playback interrupted; initiating automatic reconnection sequence.")
                } else {
                    isRetrying = false
                    reconnectStatusMessage = null
                    onError("Stream offline or format unsupported: ${error.localizedMessage}")
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    reconnectAttempts = 0
                    isRetrying = false
                    reconnectStatusMessage = null
                }
            }
        }
        exoplayer.addListener(listener)
        onDispose {
            exoplayer.removeListener(listener)
        }
    }

    // Auto-reconnect retry delay loop worker
    LaunchedEffect(isRetrying, reconnectAttempts) {
        if (isRetrying && reconnectAttempts > 0) {
            kotlinx.coroutines.delay(4000) // Delay 4 seconds before trying to establish network stream connection
            if (isRetrying) {
                reconnectTrigger++
            }
        }
    }

    // Deallocate the hardware resource when player view exits
    DisposableEffect(Unit) {
        onDispose {
            exoplayer.release()
        }
    }

    // Handle mute state changes dynamically
    LaunchedEffect(isMuted) {
        exoplayer.volume = if (isMuted) 0f else 1f
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoplayer
                    useController = true
                    setResizeMode(videoResizeMode)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = exoplayer
                playerView.setResizeMode(videoResizeMode)
            }
        )

        if (isRetrying && reconnectStatusMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00FF66),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = reconnectStatusMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}
