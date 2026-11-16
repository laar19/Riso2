package com.example.ui.screens.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun safeCopyText(context: Context, label: String, text: String, toastMsg: String = "Copiado al portapapeles") {
    try {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("RisoScreens", "Error al copiar texto", e)
    }
}

fun safeShareText(context: Context, text: String, chooserTitle: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        android.util.Log.e("RisoScreens", "Error al compartir", e)
        Toast.makeText(context, "No se encontró una aplicación para compartir", Toast.LENGTH_SHORT).show()
    }
}

fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbar_alpha"
    )
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeights = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            val totalElements = state.layoutInfo.totalItemsCount
            val totalHeight = totalElements * elementHeights
            val canvasHeight = size.height

            if (totalElements > 0 && totalHeight > canvasHeight && elementHeights > 0) {
                val scrollbarHeight = (canvasHeight * (canvasHeight / totalHeight)).coerceIn(36.dp.toPx(), canvasHeight)
                val scrollbarOffsetY = (firstVisibleElementIndex.toFloat() / totalElements.toFloat()) * canvasHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(size.width - width.toPx() - 2.dp.toPx(), scrollbarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight),
                    cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2),
                    alpha = alpha
                )
            }
        }
    }
}
