package com.encarox.kodo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.encarox.kodo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val syncMinutes by viewModel.syncMinutes.collectAsStateWithLifecycle()
    val retention by viewModel.retentionDays.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionCard(title = stringResource(R.string.section_sources)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.gmail_accounts)) },
                    supportingContent = { Text(stringResource(R.string.gmail_accounts_desc)) },
                    modifier = Modifier.clickableRow(onOpenAccounts)
                )
                HorizontalDivider()
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.sms_capture), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.sms_capture_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { viewModel.listenForSms() }) {
                        Icon(Icons.Outlined.Sms, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.listen_sms))
                    }
                }
            }

            SectionCard(title = stringResource(R.string.section_behavior)) {
                SwitchRow(
                    title = stringResource(R.string.notifications),
                    checked = notifications,
                    onCheckedChange = viewModel::setNotifications
                )
                HorizontalDivider()
                StepperRow(
                    title = stringResource(R.string.sync_interval),
                    value = "$syncMinutes ${stringResource(R.string.minutes_short)}",
                    onMinus = { viewModel.setSyncMinutes((syncMinutes - 15).coerceAtLeast(15)) },
                    onPlus = { viewModel.setSyncMinutes((syncMinutes + 15).coerceAtMost(720)) }
                )
                HorizontalDivider()
                StepperRow(
                    title = stringResource(R.string.retention),
                    value = "$retention ${stringResource(R.string.days_short)}",
                    onMinus = { viewModel.setRetentionDays((retention - 1).coerceAtLeast(1)) },
                    onPlus = { viewModel.setRetentionDays((retention + 1).coerceAtMost(90)) }
                )
            }

            Text(
                stringResource(R.string.privacy_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) { Column(content = content) }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperRow(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f))
        OutlinedButton(onClick = onMinus, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(40.dp)) { Text("−") }
        Text(value, Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onPlus, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(40.dp)) { Text("+") }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
