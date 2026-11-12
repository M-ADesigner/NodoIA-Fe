package com.radioauto.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.AppInfo
import com.radioauto.launcher.embedded.EmbedMode
import com.radioauto.launcher.embedded.EmbeddedAppState

/** What the container can do; the host decides how (the manager does the real work). */
class EmbeddedAppActions(
    val onOpen: () -> Unit,
    val onExpand: () -> Unit,
    val onBack: () -> Unit,
    val onInstall: () -> Unit,
    val onChange: () -> Unit,
)

/**
 * The panel that stands for an external app next to the launcher: which app, how it is open (beside the launcher or
 * full screen), and the buttons to enlarge it, reopen it, install it or go back. It is honest about the mode: when the
 * device has no shared windows it says so instead of pretending the app is inside.
 */
@Composable
fun EmbeddedAppContainer(
    state: EmbeddedAppState,
    app: AppInfo?,
    actions: EmbeddedAppActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Brush.radialGradient(listOf(Hud.Mid, Hud.Panel))), Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Hud.Bg), Alignment.Center) {
                if (app != null) Image(app.icon, null, Modifier.size(72.dp))
                else if (state is EmbeddedAppState.Loading) CircularProgressIndicator(Modifier.size(32.dp), Hud.Accent)
            }
            Text(app?.label ?: "Aplicación", fontFamily = Hud.Display, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            val (line, color) = when (state) {
                is EmbeddedAppState.Loading -> "Abriendo…" to Hud.Muted
                is EmbeddedAppState.NotInstalled -> "No está instalada en este equipo" to Hud.Hot
                is EmbeddedAppState.Failed -> state.message to Hud.Hot
                is EmbeddedAppState.Opened -> when (state.mode) {
                    EmbedMode.FREEFORM -> "Abierta en una ventana junto al launcher" to Hud.Accent
                    EmbedMode.SPLIT -> "Abierta en pantalla dividida" to Hud.Accent
                    EmbedMode.FULLSCREEN -> "Abierta a pantalla completa: este equipo no permite dos ventanas. Vuelve con el botón de inicio." to Hud.Muted
                }
                EmbeddedAppState.Idle -> "" to Hud.Muted
            }
            Text(line, fontSize = 12.sp, color = color, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (state) {
                    is EmbeddedAppState.NotInstalled -> Action("INSTALAR", actions.onInstall)
                    is EmbeddedAppState.Opened -> {
                        Action("VOLVER AL LAUNCHER", actions.onBack)
                        Action("REABRIR AQUÍ", actions.onOpen)
                        if (state.mode != EmbedMode.FULLSCREEN) Action("AMPLIAR", actions.onExpand)
                    }
                    is EmbeddedAppState.Failed -> Action("REINTENTAR", actions.onOpen)
                    else -> Unit
                }
                Action("CAMBIAR", actions.onChange)
            }
        }
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(40.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Hud.Accent, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        Alignment.Center,
    ) { Text(label, fontSize = 11.sp, color = Color.White, letterSpacing = 1.5.sp, maxLines = 1) }
}
