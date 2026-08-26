package com.encarox.kodo.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.encarox.kodo.R
import com.encarox.kodo.ui.components.KodoLogo

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KodoLogo(size = 120.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        FeatureLine(stringResource(R.string.feature_gmail))
        FeatureLine(stringResource(R.string.feature_sms))
        FeatureLine(stringResource(R.string.feature_private))
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(stringResource(R.string.get_started)) }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("•  ", color = MaterialTheme.colorScheme.secondary)
        Text(text, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
    }
}
