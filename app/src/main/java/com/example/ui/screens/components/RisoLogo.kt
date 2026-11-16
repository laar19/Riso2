package com.example.ui.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun RisoLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Helper function to draw symmetric curvy petals starting from a common bottom joint
        fun drawPetal(
            c1X: Float, c1Y: Float,
            c2X: Float, c2Y: Float,
            endX: Float, endY: Float,
            c3X: Float, c3Y: Float,
            c4X: Float, c4Y: Float,
            color: Color
        ) {
            val path = Path().apply {
                moveTo(w * 0.5f, h * (78f / 108f))
                cubicTo(w * c1X, h * c1Y, w * c2X, h * c2Y, w * endX, h * endY)
                cubicTo(w * c3X, h * c3Y, w * c4X, h * c4Y, w * 0.5f, h * (78f / 108f))
                close()
            }
            drawPath(path = path, color = color)
        }

        // Draw individual lotus layers from back to front
        
        // 1 & 2: Base Leaf Support / Sepals
        drawPetal(46/108f, 84/108f, 38/108f, 82/108f, 32/108f, 76/108f, 40/108f, 74/108f, 48/108f, 76/108f, Color(0xFF6B4226))
        drawPetal(62/108f, 84/108f, 70/108f, 82/108f, 76/108f, 76/108f, 68/108f, 74/108f, 60/108f, 76/108f, Color(0xFF6B4226))

        // 3 & 4: Deep Outer Petals
        drawPetal(20/108f, 78/108f, 14/108f, 62/108f, 24/108f, 50/108f, 30/108f, 53/108f, 38/108f, 60/108f, Color(0xFFA67C52))
        drawPetal(88/108f, 78/108f, 94/108f, 62/108f, 84/108f, 50/108f, 78/108f, 53/108f, 70/108f, 60/108f, Color(0xFFB88E65))

        // 5 & 6: Vibrant Inner Petals
        drawPetal(34/108f, 73/108f, 28/108f, 53/108f, 38/108f, 40/108f, 43/108f, 46/108f, 48/108f, 56/108f, Color(0xFFE07A5F))
        drawPetal(74/108f, 73/108f, 80/108f, 53/108f, 70/108f, 40/108f, 65/108f, 46/108f, 60/108f, 56/108f, Color(0xFFEB8A74))

        // 7: Central Rising Petal (On top, luminous warm coral pastel highlight)
        drawPetal(44/108f, 63/108f, 44/108f, 43/108f, 54/108f, 30/108f, 64/108f, 43/108f, 64/108f, 63/108f, Color(0xFFF9DDD6))

        // 8: Center Spark Highlight at the core
        val sparkPath = Path().apply {
            moveTo(w * (54f / 108f), h * (42f / 108f))
            lineTo(w * (55f / 108f), h * (44f / 108f))
            lineTo(w * (57f / 108f), h * (44.5f / 108f))
            lineTo(w * (55f / 108f), h * (45f / 108f))
            lineTo(w * (54f / 108f), h * (47f / 108f))
            lineTo(w * (53f / 108f), h * (45f / 108f))
            lineTo(w * (51f / 108f), h * (44.5f / 108f))
            lineTo(w * (53f / 108f), h * (44f / 108f))
            close()
        }
        drawPath(path = sparkPath, color = Color.White)
    }
}
