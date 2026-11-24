package com.example.quranreelsstudio.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.quranreelsstudio.data.Reciter
import com.example.quranreelsstudio.data.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentAyahIndex: Int = -1,
    val totalAyahs: Int = 0,
    val error: String? = null
)

/**
 * Plays a verse range as a gapless playlist of per-ayah mp3 files
 * streamed from everyayah.com:
 * https://everyayah.com/data/{audioFolder}/{SSSAAA}.mp3
 */
class RecitationPlayer(context: Context) {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val player = ExoPlayer.Builder(context.applicationContext).build()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.value = _state.value.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _state.value = _state.value.copy(currentAyahIndex = player.currentMediaItemIndex)
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    error = "Playback failed — check your internet connection"
                )
            }
        })
    }

    fun playRange(reciter: Reciter, surah: Surah, fromAyah: Int, toAyah: Int) {
        val from = fromAyah.coerceIn(1, surah.verseCount)
        val to = toAyah.coerceIn(from, surah.verseCount)
        if (from > to) return

        player.stop()
        player.clearMediaItems()
        for (ayah in from..to) {
            val url = "https://everyayah.com/data/${reciter.audioFolder}/" +
                String.format(Locale.US, "%03d%03d.mp3", surah.number, ayah)
            player.addMediaItem(MediaItem.fromUri(url))
        }
        _state.value = PlaybackState(totalAyahs = to - from + 1, isBuffering = true)
        player.prepare()
        player.play()
    }

    fun pause() = player.pause()

    fun resume() = player.play()

    /** Jumps the recitation to a verse (0-based index into the played range). */
    fun seekToAyahIndex(index: Int) {
        if (index < 0 || index >= player.mediaItemCount) return
        _state.value = _state.value.copy(currentAyahIndex = index)
        player.seekTo(index, 0L)
        player.play()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _state.value = PlaybackState()
    }

    fun release() = player.release()
}
