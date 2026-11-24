package com.example.quranreelsstudio.ui.export

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.quranreelsstudio.R
import com.example.quranreelsstudio.data.BackgroundType
import com.example.quranreelsstudio.data.ReelAspectRatio
import com.example.quranreelsstudio.data.Reciter
import com.example.quranreelsstudio.data.Surah
import com.example.quranreelsstudio.export.VideoRenderEngine
import java.io.File

/** Everything the render engine needs, collected from the studio screen. */
data class PendingRender(
    val aspect: ReelAspectRatio,
    val surah: Surah,
    val fromAyah: Int,
    val toAyah: Int,
    val reciter: Reciter,
    val backgroundType: BackgroundType,
    val videoItems: List<com.example.quranreelsstudio.data.BackgroundMediaItem>,
    val photoItem: com.example.quranreelsstudio.data.BackgroundMediaItem?,
    val blankColor: Long,
    val visibility: Int,
    val arabicTexts: List<String>,
    val translationTexts: List<String>?,
    val typeface: android.graphics.Typeface?,
    val subtitleSize: Int = 12
)

/** Stage of the render pipeline, shown on the progress screen. */
data class RenderUiState(
    val running: Boolean = false,
    val label: String = "",
    val percent: Int = 0,
    val error: String? = null,
    val outputPath: String? = null
)

@Composable
fun ExportSettingsScreen(
    pending: PendingRender,
    uiState: RenderUiState,
    onStartRender: (Int) -> Unit,
    onBack: () -> Unit
) {
    var quality by remember { mutableStateOf(1080) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (uiState.running || uiState.error != null) {
            // ---- Progress ----
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.running) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.export_rendering), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.label, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress = { uiState.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${uiState.percent}%", fontSize = 12.sp)
                } else {
                    Text(
                        stringResource(R.string.export_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(uiState.error ?: "Unknown error", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onBack) { Text(stringResource(R.string.export_back_to_studio)) }
                }
            }
        } else {
            // ---- Quality selection ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(stringResource(R.string.export_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.export_description), style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "${pending.surah.englishName} • Verses ${pending.fromAyah}–${pending.toAyah}",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Reciter: ${pending.reciter.nameAr} — ${pending.reciter.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.export_video_quality), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QualityCard("720p", "HD", quality == 720, modifier = Modifier.weight(1f)) { quality = 720 }
                    QualityCard("1080p", "Full HD", quality == 1080, modifier = Modifier.weight(1f)) { quality = 1080 }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onStartRender(quality) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(stringResource(R.string.export_start_rendering), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.export_back_to_studio))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QualityCard(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ExportResultScreen(
    outputPath: String,
    onBackToStudio: () -> Unit
) {
    val context = LocalContext.current
    val file = remember { File(outputPath) }

    // Video player preview
    val exoPlayer = remember(outputPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(outputPath) {
        onDispose { exoPlayer.release() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val result = saveToDevice(context, file)
                    Toast.makeText(
                        context,
                        if (result) context.getString(R.string.result_saved) else "Could not save the video",
                        Toast.LENGTH_LONG
                    ).show()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.result_save), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val uri = fileUri(context, file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.result_share)))
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.result_share), fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.result_share_hint), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Quick launch targets
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick share:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(72.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targets = listOf(
                    "TikTok" to "com.zhiliaoapp.musically",
                    "Instagram" to "com.instagram.android",
                    "YouTube" to "com.google.android.youtube",
                    "X" to "com.twitter.android"
                )
                items(targets.size) { index ->
                    val (label, pkg) = targets[index]
                    QuickShareChip(label = label, onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, fileUri(context, file))
                            setPackage(pkg)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching {
                            context.startActivity(shareIntent)
                        }.onFailure {
                            // App not installed — fall back to the general chooser
                            val generic = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, fileUri(context, file))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(Intent.createChooser(generic, label)) }
                        }
                    })
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val uri = fileUri(context, file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.result_open_with)))
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(R.string.result_open_with))
            }

            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = onBackToStudio) {
                Text(stringResource(R.string.export_back_to_studio))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickShareChip(label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

private fun fileUri(context: android.content.Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

/** Copies the rendered video into the device gallery (Movies/QuranReelsStudio). */
private fun saveToDevice(context: android.content.Context, source: File): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, source.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/QuranReelsStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "QuranReelsStudio")
            dir.mkdirs()
            source.copyTo(File(dir, source.name), overwrite = true)
        }
        true
    } catch (e: Exception) {
        false
    }
}
