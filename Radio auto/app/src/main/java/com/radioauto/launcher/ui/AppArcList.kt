package com.radioauto.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.AppInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val ROW = 60.dp
private val THUMB = 36.dp
private const val HANDLE_INSET = 0.2f

/** Distance of a row from the middle of the list: 0 in the centre, -1 at the top edge, +1 at the bottom edge. */
private fun LazyListState.offsetFromCentre(index: Int): Float? {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
    return ((item.offset + item.size / 2f) - (info.viewportStartOffset + half)) / half
}

/**
 * Apps in a rounded purple panel. Each row has the icon on the left and the name centred in the panel;
 * the icons bend into an arc (the middle one sticks out the most) while the names stay straight.
 * The bend is a graphics-layer transform, so scrolling costs no re-layout. The round handle on the right
 * shows where you are and can be dragged. Favourites come first; long press pins or unpins an app.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppArcList(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val rowPx = with(density) { (ROW + 1.dp).toPx() }
    val thumbPx = with(density) { THUMB.toPx() }
    val trackHeight = remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(percent = 28)

    // Deep version of the accent colour, darker toward the bottom, like the purple panel of the reference.
    val deep = lerp(c.primary, Color.Black, 0.55f)
    val panel = Brush.verticalGradient(listOf(deep.copy(alpha = 0.95f), lerp(deep, Color.Black, 0.6f).copy(alpha = 0.95f)))

    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(panel)
            .border(1.dp, c.onSurface.copy(alpha = 0.18f), shape)
            .onSizeChanged { trackHeight.floatValue = it.height.toFloat() }
    ) {
        val iconStart = maxWidth * 0.14f
        val maxShift = with(density) { (maxWidth * 0.075f).toPx() }

        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 14.dp)) {
            itemsIndexed(apps, key = { _, app -> app.packageName }, contentType = { _, _ -> "app" }) { index, app ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ROW)
                        .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) })
                ) {
                    Image(
                        app.icon, null,
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = iconStart)
                            .size(40.dp)
                            .graphicsLayer {
                                // Read inside the layer block: only this icon's layer is redone while scrolling.
                                state.offsetFromCentre(index)?.let { t -> translationX = -t * t * maxShift }
                            },
                    )
                    Text(
                        app.label,
                        Modifier.align(Alignment.Center).padding(horizontal = 80.dp),
                        fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = c.onSurface.copy(alpha = 0.12f))
            }
        }

        // Scroll handle: follows the list position, and dragging it scrolls the list.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 14.dp)
                .size(THUMB)
                .graphicsLayer {
                    // Travels only along the straight part of the right edge, clear of the rounded corners.
                    val top = trackHeight.floatValue * HANDLE_INSET
                    val travel = trackHeight.floatValue * (1 - 2 * HANDLE_INSET) - thumbPx
                    val info = state.layoutInfo
                    val first = info.visibleItemsInfo.firstOrNull()
                    val total = info.totalItemsCount
                    translationY = if (first != null && total > info.visibleItemsInfo.size) {
                        val position = first.index + (-first.offset) / max(1f, first.size.toFloat())
                        val fraction = (position / (total - info.visibleItemsInfo.size)).coerceIn(0f, 1f)
                        top + fraction * travel
                    } else top
                }
                .clip(CircleShape)
                .background(c.onSurface.copy(alpha = 0.35f))
                .border(1.5.dp, c.onSurface.copy(alpha = 0.7f), CircleShape)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        // Convert thumb travel into list travel.
                        val travel = trackHeight.floatValue * (1 - 2 * HANDLE_INSET) - thumbPx
                        val ratio = (apps.size * rowPx / max(1f, travel)).coerceAtLeast(1f)
                        scope.launch { state.scrollBy(delta * ratio) }
                    },
                ),
        )
    }
}
