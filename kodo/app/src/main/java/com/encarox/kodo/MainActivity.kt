package com.encarox.kodo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.encarox.kodo.ui.MainViewModel
import com.encarox.kodo.ui.navigation.KodoNavHost
import com.encarox.kodo.ui.theme.KodoTheme
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.encarox.kodo.data.sms.SmsConsentBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    // Launches the system SMS consent dialog and reads the shared message.
    private val smsConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            mainViewModel.ingestSmsMessage(message)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled implicitly; notifications simply won't post if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        maybeRequestNotificationPermission()

        // Forward SMS consent intents emitted by the receiver.
        lifecycleScope.launch {
            SmsConsentBus.consentIntents.collectLatest { intent ->
                runCatching { smsConsentLauncher.launch(intent) }
            }
        }

        setContent {
            KodoTheme {
                KodoNavHost()
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
