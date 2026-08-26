package com.encarox.kesit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    loading: Boolean,
    error: String?,
    onPick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Kesit", color = Accent, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Kes, önce izle, sonra kaydet.",
            color = InkDim,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        if (loading) {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(12.dp))
            Text("Video okunuyor…", color = InkDim, fontSize = 12.sp)
        } else {
            Button(
                onClick = onPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("Video seç", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent.copy(alpha = 0.14f))
                    .padding(12.dp)
            ) {
                Text(error, color = AccentSoft, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface1)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Feature("Yakınlaştır, milimetrik kes", "Zaman çizgisini büyüt; bir parmak hareketi milisaniyeye iner. Kare kare oynatma tuşları da var.")
            Feature("Kaydetmeden izle", "\"Kesiti izle\" seçili aralığı anında döngüde oynatır. Dosya üretilmez, beklemezsin.")
            Feature("Kalite bozulmaz", "Video yeniden kodlanmaz; hassas modda yalnızca ilk kare grubu dokunulur. Ses aynen kopyalanır.")
            Feature("Orijinal yerinde kalır", "Kesit yeni bir dosya olarak Movies/Kesit klasörüne yazılır. Kaynağa hiç dokunulmaz.")
        }
    }
}

@Composable
private fun Feature(title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Accent)
        )
        Column {
            Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = InkDim, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
