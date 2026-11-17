package de.tanzfit.ui

import android.media.MediaMetadataRetriever
import android.os.Build
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import de.tanzfit.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val thumbCache = HashMap<String, ImageBitmap>()

/** Kleines Vorschaubild: erstes Videobild oder ein Symbol, wenn es kein Video gibt. */
@Composable
fun ExerciseThumb(videoFile: String?, size: Dp = 56.dp) {
    val bitmap by produceState(initialValue = videoFile?.let { thumbCache[it] }, videoFile) {
        value = videoFile?.let { f ->
            thumbCache[f] ?: withContext(Dispatchers.IO) { loadFrame(f) }?.also { thumbCache[f] = it }
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
            Icon(Icons.Default.Movie, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(size / 3))
        } else {
            Icon(
                if (videoFile != null) Icons.Default.Movie else Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun loadFrame(fileName: String): ImageBitmap? = runCatching {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(Repository.videoPath(fileName))
        val frame = if (Build.VERSION.SDK_INT >= 27) {
            r.getScaledFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 240, 240)
        } else {
            r.getFrameAtTime(500_000)
        }
        frame?.asImageBitmap()
    } finally {
        runCatching { r.release() }
    }
}.getOrNull()

/** Spielt ein Video in Endlosschleife und ohne Ton ab (wie ein GIF). */
@Composable
fun LoopingVideo(
    videoFile: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
) {
    val path = Repository.videoPath(videoFile)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(path) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            start()
                        }
                        setOnErrorListener { _, _, _ -> true }
                        setVideoPath(path)
                    }
                },
                update = { v ->
                    if (playing) {
                        if (!v.isPlaying) v.start()
                    } else if (v.isPlaying) {
                        v.pause()
                    }
                },
                onRelease = { v -> v.stopPlayback() },
            )
        }
    }
}

/** Großes Motivations-Popup, schließt sich nach kurzer Zeit selbst. */
@Composable
fun MotivationPopup(text: String, autoCloseMs: Long = 1800, onDismiss: () -> Unit) {
    LaunchedEffect(text) {
        if (autoCloseMs > 0) {
            delay(autoCloseMs)
            onDismiss()
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.clickable { onDismiss() },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = text,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (autoCloseMs <= 0) {
                    Spacer(Modifier.height(12.dp))
                    Text("Tippen zum Schließen", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
