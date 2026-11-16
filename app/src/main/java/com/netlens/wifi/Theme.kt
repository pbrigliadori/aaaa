package com.netlens.wifi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta inspirada no iOS (dark mode)
val Bg = Color(0xFF000000); val Card = Color(0xFF1C1C1E); val Sep = Color(0xFF38383A)
val Blue = Color(0xFF0A84FF); val Sub = Color(0xFF8E8E93); val Green = Color(0xFF30D158)
val Orange = Color(0xFFFF9F0A); val Red = Color(0xFFFF453A)

@Composable fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = Blue, background = Bg, surface = Bg, onSurface = Color.White,
        onBackground = Color.White, surfaceVariant = Card, outline = Sep), content = content)

@Composable fun LargeTitle(t: String) = Text(t, fontSize = 34.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp))

@Composable fun Group(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    if (title != null) Text(title.uppercase(), color = Sub, fontSize = 13.sp, modifier = Modifier.padding(start = 36.dp, top = 16.dp, bottom = 6.dp))
    else Spacer(Modifier.height(12.dp))
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card), content = content)
}

@Composable fun InfoRow(label: String, value: String, divider: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 16.sp)
        Text(value, color = Sub, fontSize = 16.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 12.dp))
    }
    if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = Sep, thickness = 0.5.dp)
}

fun sigColor(dbm: Int) = when { dbm >= -60 -> Green; dbm >= -75 -> Orange; else -> Red }
fun sigPct(dbm: Int) = ((dbm + 100) / 60f).coerceIn(0.03f, 1f)

@Composable fun PrimaryButton(text: String, onClick: () -> Unit) = Button(onClick, Modifier.padding(16.dp).fillMaxWidth().height(50.dp),
    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
