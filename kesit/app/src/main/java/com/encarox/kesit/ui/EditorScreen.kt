package com.encarox.kesit.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.encarox.kesit.media.TrimMode
import com.encarox.kesit.media.VideoInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun EditorScreen(
    info: VideoInfo,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(info.uri) { EditorController(context, info) }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    // Oynatma cizgisini akici tutmak icin duzenli ornekleme.
    LaunchedEffect(controller) {
        while (true) {
            controller.syncPlayhead()
            delay(33)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Ink)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = info.displayName,
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "${info.displayWidth}x${info.displayHeight} · " +
                        "${info.frameRate.toInt()} fps · ${timecode(info.durationMs, millis = false)}",
                    color = InkDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }

        PlayerSurface(
            controller = controller,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 150.dp)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
        )

        SelectionReadout(controller)

        Spacer(Modifier.height(10.dp))

        TrimTimeline(
            durationMs = controller.durationMs,
            startMs = controller.startMs,
            endMs = controller.endMs,
            playheadMs = controller.playheadMs,
            viewStartMs = controller.viewStartMs,
            viewEndMs = controller.viewEndMs,
            filmstrip = controller.filmstrip,
            aspectRatio = info.aspectRatio,
            onSelectionChange = controller::onSelectionChange,
            onScrub = controller::scrubTo,
            onWindowChange = controller::onWindowChange,
            onDragStateChange = controller::onDragStateChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        ZoomRow(controller)

        Spacer(Modifier.height(8.dp))

        NudgeRow(controller)

        Spacer(Modifier.height(10.dp))

        QualityRow(controller)

        Spacer(Modifier.height(10.dp))

        ActionRow(
            controller = controller,
            onExport = { scope.launch { controller.export() } },
        )

        Spacer(Modifier.height(14.dp))
    }

    when (val state = controller.exportState) {
        is ExportState.Working -> ExportProgressDialog(state.progress)
        is ExportState.Done -> ExportDoneDialog(
            state = state,
            onDismiss = controller::dismissExport,
            onOpen = { uri -> context.viewVideo(uri) },
            onShare = { uri -> context.shareVideo(uri) },
        )
        is ExportState.Failed -> AlertDialog(
            onDismissRequest = controller::dismissExport,
            confirmButton = {
                TextButton(onClick = controller::dismissExport) { Text("Tamam") }
            },
            title = { Text("Kesilemedi") },
            text = { Text(state.message) },
            containerColor = Surface2,
        )
        ExportState.Idle -> Unit
    }
}

@OptIn(markerClass = [UnstableApi::class])
@Composable
private fun PlayerSurface(controller: EditorController, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    player = controller.player
                }
            },
            onRelease = { it.player = null },
        )

        // Ortadaki oynat/durdur dokunma hedefi
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = if (controller.playing) 0.0f else 0.45f))
                .clickable { controller.togglePlay() },
            contentAlignment = Alignment.Center,
        ) {
            if (!controller.playing) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Oynat",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        if (controller.previewing) {
            Text(
                text = "ÖNİZLEME · DÖNGÜ",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Accent)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun SelectionReadout(controller: EditorController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeChip(
            label = "BAŞLANGIÇ",
            value = timecode(controller.startMs),
            active = controller.activeHandle == DragTarget.START,
            modifier = Modifier.weight(1f),
            onClick = { controller.selectHandle(DragTarget.START) },
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = durationLabel(controller.selectionMs),
                color = Accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "≈ ${sizeLabel(controller.info.estimatedBytesFor(controller.selectionMs))}",
                color = InkDim,
                fontSize = 10.sp,
            )
        }
        TimeChip(
            label = "BİTİŞ",
            value = timecode(controller.endMs),
            active = controller.activeHandle == DragTarget.END,
            modifier = Modifier.weight(1f),
            onClick = { controller.selectHandle(DragTarget.END) },
        )
    }
}

@Composable
private fun TimeChip(
    label: String,
    value: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Accent.copy(alpha = 0.18f) else Surface2)
            .border(
                width = 1.dp,
                color = if (active) Accent else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (active) Accent else InkDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = Ink,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ZoomRow(controller: EditorController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallAction("−", Modifier.width(46.dp)) { controller.zoom(0.5f) }
        SmallAction("+", Modifier.width(46.dp)) { controller.zoom(2f) }
        Text(
            text = "×${"%.1f".format(controller.zoomFactor)}",
            color = InkDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(46.dp),
            textAlign = TextAlign.Center,
        )
        SmallAction("Seçime", Modifier.weight(1f)) { controller.zoomToSelection() }
        SmallAction("Tümü", Modifier.weight(1f)) { controller.zoomAll() }
    }
}

@Composable
private fun NudgeRow(controller: EditorController) {
    val frame = controller.info.frameDurationMsRounded
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SmallAction("−1sn", Modifier.weight(1f)) { controller.nudge(-1000) }
            SmallAction("−100", Modifier.weight(1f)) { controller.nudge(-100) }
            SmallAction("−1◧", Modifier.weight(1f)) { controller.nudge(-frame) }
            SmallAction("+1◧", Modifier.weight(1f)) { controller.nudge(frame) }
            SmallAction("+100", Modifier.weight(1f)) { controller.nudge(100) }
            SmallAction("+1sn", Modifier.weight(1f)) { controller.nudge(1000) }
        }
        Text(
            text = "Aktif tutamaç: ${if (controller.activeHandle == DragTarget.END) "bitiş" else "başlangıç"} · " +
                "1 kare = $frame ms",
            color = InkDim,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun QualityRow(controller: EditorController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModeChip(
            title = "Hassas",
            subtitle = "tam kare",
            selected = controller.mode == TrimMode.PRECISE,
            modifier = Modifier.weight(1f),
        ) { controller.mode = TrimMode.PRECISE }
        ModeChip(
            title = "Kayıpsız",
            subtitle = "anlık, anahtar kare",
            selected = controller.mode == TrimMode.LOSSLESS,
            modifier = Modifier.weight(1f),
        ) { controller.mode = TrimMode.LOSSLESS }
    }
}

@Composable
private fun ModeChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Accent.copy(alpha = 0.16f) else Surface2)
            .border(1.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
    ) {
        Text(title, color = if (selected) Accent else Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = InkDim, fontSize = 10.sp)
    }
}

@Composable
private fun ActionRow(controller: EditorController, onExport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { controller.togglePreview() },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = if (controller.previewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(if (controller.previewing) "Durdur" else "Kesiti izle", fontSize = 14.sp)
        }
        Button(
            onClick = onExport,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text("Kaydet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExportProgressDialog(progress: Float) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Kesiliyor") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Accent,
                )
                Spacer(Modifier.height(8.dp))
                Text("%${percent(progress)}", color = InkDim, fontSize = 12.sp)
            }
        },
        containerColor = Surface2,
    )
}

@Composable
private fun ExportDoneDialog(
    state: ExportState.Done,
    onDismiss: () -> Unit,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    val result = state.result
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onOpen(result.uri) }) { Text("Oynat", color = Accent) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onShare(result.uri) }) { Text("Paylaş", color = InkDim) }
                TextButton(onClick = onDismiss) { Text("Kapat", color = InkDim) }
            }
        },
        title = { Text("Kaydedildi") },
        text = {
            Column {
                Text(result.displayName, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Movies/Kesit · ${durationLabel(result.durationMs)} · " +
                        "${sizeLabel(result.sizeBytes)} · ${elapsedLabel(result.elapsedMs)}",
                    color = InkDim,
                    fontSize = 12.sp,
                )
                if (result.startShiftMs > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Kayıpsız modda başlangıç en yakın anahtar kareye " +
                            "${result.startShiftMs} ms geri alındı.",
                        color = AccentSoft,
                        fontSize = 11.sp,
                    )
                }
                result.note?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = InkDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Kaynak video olduğu gibi duruyor; aynı videodan kesmeye devam edebilirsin.",
                    color = InkDim,
                    fontSize = 11.sp,
                )
            }
        },
        containerColor = Surface2,
    )
}

@Composable
private fun SmallAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Ink, fontSize = 12.sp, maxLines = 1)
    }
}

private fun android.content.Context.viewVideo(uri: Uri) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }
}

private fun android.content.Context.shareVideo(uri: Uri) {
    runCatching {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Kesiti paylaş",
            )
        )
    }
}
