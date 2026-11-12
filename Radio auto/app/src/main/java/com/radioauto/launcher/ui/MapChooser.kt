package com.radioauto.launcher.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One thing the driver can navigate with: an installed app (with its own icon) or the built-in map. */
class MapChoice(val id: String, val label: String, val icon: ImageBitmap?, val note: String, val installed: Boolean = true)

/**
 * The Map tab's start screen: which navigator to use. Waze and Google Maps open in a window beside the launcher;
 * the built-in map is shown right here. The last one used is marked.
 */
@Composable
fun MapChooser(choices: List<MapChoice>, lastUsed: String, onPick: (MapChoice) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.background(Brush.radialGradient(listOf(Hud.Mid, Hud.Panel))),
    ) {
        Column(Modifier.fillMaxSize().padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("¿Con qué navegamos?", fontFamily = Hud.Display, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                "Se abre en una ventana al lado y el launcher sigue visible",
                fontSize = 11.sp, color = Hud.Muted, letterSpacing = 1.sp, modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                choices.forEach { ChoiceCard(it, it.id == lastUsed) { onPick(it) } }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Si la radio no permite dos ventanas a la vez, la app se abre a pantalla completa y vuelves con el botón de inicio.",
                fontSize = 10.sp, color = Hud.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}

@Composable
private fun ChoiceCard(choice: MapChoice, last: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier
            .width(200.dp).height(210.dp)
            .clip(shape)
            .background(if (last) Hud.Accent.copy(alpha = 0.14f) else Hud.Mid)
            .border(1.dp, if (last) Hud.Accent else Hud.Line, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Hud.Bg), Alignment.Center) {
            if (choice.icon != null) Image(choice.icon, null, Modifier.size(64.dp))
            else if (!choice.installed) Icon(Icons.Default.Navigation, null, Modifier.size(34.dp), if (choice.id == "com.waze") Color(0xFF33CCFF) else Hud.Accent)
            else Icon(Icons.Default.Map, null, Modifier.size(34.dp), Hud.Accent)
        }
        Text(choice.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(if (!choice.installed) "NO INSTALADA" else if (last) "ÚLTIMA USADA" else choice.note, fontSize = 9.sp, color = if (last) Hud.Accent else Hud.Muted, letterSpacing = 1.5.sp, maxLines = 1)
        Box(
            Modifier.height(34.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Hud.Accent, RoundedCornerShape(4.dp)).padding(horizontal = 16.dp),
            Alignment.Center,
        ) { Text(if (!choice.installed) "INSTALAR" else if (choice.icon == null) "VER AQUÍ" else "ABRIR AQUÍ", fontSize = 10.sp, color = Color.White, letterSpacing = 1.5.sp) }
    }
}
