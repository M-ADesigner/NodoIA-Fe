package com.radioauto.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.NavState

/** Next manoeuvre of the running navigation (Waze / Google Maps). Invisible when nobody is navigating. */
@Composable
fun NavCard(onOpen: (packageName: String) -> Unit, modifier: Modifier = Modifier) {
    val nav by NavState.state.collectAsState()
    val info = nav ?: return
    Row(
        modifier
            .glass(RoundedCornerShape(20.dp), strength = 0.5f)
            .clickable { onOpen(info.packageName) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Navigation, "Navegando", Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(info.title.ifEmpty { info.text }, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val detail = listOf(info.text.takeIf { info.title.isNotEmpty() }, info.subText.takeIf { it.isNotEmpty() })
                .filterNotNull().joinToString(" · ")
            if (detail.isNotEmpty()) Text(detail, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
