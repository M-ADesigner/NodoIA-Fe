package com.radioauto.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.AppInfo

private val TileShape = RoundedCornerShape(30.dp)

/** Soft two-tone colour wash per tile: every tile gets its own hue, calm and translucent over the aurora. */
@Composable
fun Modifier.tile(hue: Float): Modifier = this
    .clip(TileShape)
    .background(
        Brush.linearGradient(
            listOf(Color.hsv(hue % 360f, 0.38f, 1f, 0.40f), Color.hsv((hue + 30f) % 360f, 0.45f, 0.92f, 0.20f)),
        ),
    )
    .border(1.dp, Color.White.copy(alpha = 0.16f), TileShape)

/** Big, glanceable touch target (well over 48 dp) for the things a driver does most. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hue: Float = 270f,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .tile(hue)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, label, Modifier.size(38.dp), Color.White)
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier, hue: Float = 200f) {
    Column(
        modifier
            .tile(hue)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(app.icon, app.label, Modifier.size(56.dp))
        Text(app.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
