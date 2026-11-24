package com.example.quranreelsstudio.ui.studio

import android.view.LayoutInflater
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.quranreelsstudio.R
import com.example.quranreelsstudio.data.BackgroundMediaItem
import com.example.quranreelsstudio.data.BackgroundType
import com.example.quranreelsstudio.media.MediaLoader
import com.example.quranreelsstudio.media.MediaThumbs

/**
 * Renders the live background layer of the reel preview:
 * looping muted video (single or a multi-video sequence playlist),
 * photo (optionally Ken Burns motion for AI clips), or a blank color,
 * with a dark scrim derived from the visibility slider.
 */
@Composable
fun BackgroundMediaCanvas(
    backgroundType: BackgroundType,
    item: BackgroundMediaItem?,
    sequenceItems: List<BackgroundMediaItem>,
    useSequence: Boolean,
    blankColor: Long,
    visibility: Int,
    playVideo: Boolean = true,
    modifier: Modifier = Modifier
) {
    when {
        backgroundType == BackgroundType.BLANK_COLOR ->
            BlankColorCanvas(blankColor = blankColor, modifier = modifier)

        backgroundType == BackgroundType.MOTION_VIDEO && useSequence && sequenceItems.isNotEmpty() ->
            VideoBackground(items = sequenceItems, visibility = visibility, playVideo = playVideo, modifier = modifier)

        backgroundType == BackgroundType.MOTION_VIDEO && item != null && item.isVideo ->
            VideoBackground(items = listOf(item), visibility = visibility, playVideo = playVideo, modifier = modifier)

        item != null && !item.isVideo ->
            PhotoBackground(item = item, visibility = visibility, modifier = modifier)

        item != null && item.isVideo ->
            VideoBackground(items = listOf(item), visibility = visibility, playVideo = playVideo, modifier = modifier)

        else ->
            EmptyCanvas(modifier = modifier)
    }
}

@Composable
private fun VideoBackground(items: List<BackgroundMediaItem>, visibility: Int, playVideo: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val sources = items.mapNotNull { item ->
        item.assetPath?.let { "asset:///$it" } ?: item.contentUri ?: item.remoteUrl
    }
    if (sources.isEmpty()) {
        EmptyCanvas(modifier = modifier)
        return
    }
    val playlistKey = sources.joinToString("|")

    // key() rebuilds the whole player when the selection changes,
    // so tapping a video applies it to the preview immediately.
    key(playlistKey) {
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                sources.forEach { addMediaItem(MediaItem.fromUri(it)) }
                repeatMode = if (sources.size > 1) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
                volume = 0f // background video is silent; recitation is the audio
                playWhenReady = playVideo
                prepare()
            }
        }
        var playerView by remember { mutableStateOf<PlayerView?>(null) }

        // Stop decoding while scrolled off-screen — a hidden looping video
        // decoding during scroll was the main cause of the scroll lag.
        LaunchedEffect(playVideo) {
            exoPlayer.playWhenReady = playVideo
        }

        Box(modifier) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.player_texture_view, null, false) as PlayerView)
                        .also { playerView = it }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
            ScrimOverlay(visibility = visibility, modifier = Modifier.matchParentSize())
        }
        DisposableEffect(Unit) {
            onDispose {
                playerView?.player = null
                exoPlayer.release()
            }
        }
    }
}

@Composable
private fun PhotoBackground(item: BackgroundMediaItem, visibility: Int, modifier: Modifier) {
    val context = LocalContext.current
    var bitmap by remember(item.id, item.assetPath, item.contentUri, item.remoteUrl) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(item.id, item.assetPath, item.contentUri, item.remoteUrl) {
        bitmap = null
        bitmap = MediaLoader.loadBitmap(context, item)?.asImageBitmap()
    }

    Box(modifier) {
        val current = bitmap
        if (current != null) {
            if (item.motion) {
                KenBurnsImage(bitmap = current, modifier = Modifier.fillMaxSize())
            } else {
                Image(
                    bitmap = current,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF10222B)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF4FA8D8))
            }
        }
        ScrimOverlay(visibility = visibility, modifier = Modifier.matchParentSize())
    }
}

/** Slow cinematic zoom/pan loop that turns an AI still into a motion clip. */
@Composable
private fun KenBurnsImage(bitmap: ImageBitmap, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "kenburns")
    val scale by transition.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val panX by transition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(17_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "panX"
    )
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = panX.dp.toPx()
        }
    )
}

@Composable
private fun BlankColorCanvas(blankColor: Long, modifier: Modifier) {
    Box(modifier.background(Color(blankColor)))
}

@Composable
private fun EmptyCanvas(modifier: Modifier) {
    Box(modifier.background(Color(0xFF10222B)), contentAlignment = Alignment.Center) {
        Text(
            "Pick a background from the studio controls",
            color = Color(0xFF7FA8BC),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ScrimOverlay(visibility: Int, modifier: Modifier) {
    val scrimAlpha = ((100 - visibility) / 100f) * 0.85f
    Box(modifier.background(Color.Black.copy(alpha = scrimAlpha)))
}

/** Rounded thumbnail for library rows (video frame or downsampled photo). */
@Composable
fun MediaThumb(item: BackgroundMediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var thumb by remember(item.id, item.assetPath, item.contentUri, item.remoteUrl) {
        mutableStateOf<ImageBitmap?>(null)
    }
    LaunchedEffect(item.id, item.assetPath, item.contentUri, item.remoteUrl) {
        thumb = MediaThumbs.get(context, item)?.asImageBitmap()
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10222B))
    ) {
        val current = thumb
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
