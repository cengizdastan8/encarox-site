package com.encarox.kesit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.encarox.kesit.media.VideoInfo
import com.encarox.kesit.ui.EditorScreen
import com.encarox.kesit.ui.HomeScreen
import com.encarox.kesit.ui.KesitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLegacyWritePermissionIfNeeded()
        val incoming = intent?.videoUri()
        setContent {
            KesitTheme {
                KesitApp(initialUri = incoming)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Android 9 ve oncesinde Movies/Kesit klasorune yazabilmek icin izin gerekiyor.
     * Android 10+ surumlerde MediaStore sayesinde hicbir izin istemiyoruz.
     */
    private fun requestLegacyWritePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 42
            )
        }
    }
}

private fun Intent.videoUri(): Uri? = when (action) {
    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
    Intent.ACTION_VIEW -> data
    else -> null
}

@Composable
private fun KesitApp(initialUri: Uri?) {
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf(initialUri) }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> if (picked != null) sourceUri = picked }

    LaunchedEffect(sourceUri) {
        val uri = sourceUri
        if (uri == null) {
            info = null
            return@LaunchedEffect
        }
        loading = true
        error = null
        val probed = withContext(Dispatchers.IO) {
            runCatching { VideoInfo.probe(context, uri) }
        }
        loading = false
        probed
            .onSuccess { probe ->
                if (probe.durationMs <= 0L || probe.videoMime == null) {
                    error = "Bu dosya okunamadı ya da içinde video izi yok."
                    sourceUri = null
                } else {
                    info = probe
                }
            }
            .onFailure {
                error = it.message ?: "Video açılamadı."
                sourceUri = null
            }
    }

    val current = info
    if (current != null) {
        EditorScreen(
            info = current,
            onBack = {
                info = null
                sourceUri = null
            },
        )
    } else {
        HomeScreen(
            loading = loading,
            error = error,
            onPick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
        )
    }
}
