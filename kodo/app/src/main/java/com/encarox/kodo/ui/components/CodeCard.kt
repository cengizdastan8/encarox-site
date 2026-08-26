package com.encarox.kodo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.encarox.kodo.data.model.CodeItem
import com.encarox.kodo.data.model.CodeSource
import com.encarox.kodo.ui.theme.CodeTextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CodeCard(
    item: CodeItem,
    onCopy: (CodeItem) -> Unit,
    onClick: (CodeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        if (item.isRead) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "card"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        color = container,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (item.source == CodeSource.GMAIL) Icons.Outlined.Email
                else Icons.Outlined.Sms
                Icon(icon, contentDescription = null, tint = accent)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.code,
                    style = CodeTextStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildString {
                    append(item.service ?: item.sender)
                    item.account?.let { append("  •  $it") }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1
                )
                Text(
                    text = relativeTime(item.receivedAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Normal
                )
            }

            IconButton(onClick = { onCopy(item) }) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = accent
                )
            }
        }
    }
}

private fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(millis))
    }
}
