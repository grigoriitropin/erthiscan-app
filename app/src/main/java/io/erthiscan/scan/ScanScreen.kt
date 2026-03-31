package io.erthiscan.scan

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview { barcode ->
            Log.d("ErthiScan", "Scanned: $barcode")
        }

        ViewfinderOverlay()
    }
}

@Composable
fun ViewfinderOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameWidth = size.width * 0.7f
        val frameHeight = frameWidth
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f

        val cornerLength = frameWidth * 0.12f
        val cornerRadius = 5.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val color = Color.White

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(color, Offset(left, top + cornerRadius), Offset(left, top + cornerLength), strokeWidth)

        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left + frameWidth - cornerRadius * 2, top),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(left + frameWidth - cornerLength, top), Offset(left + frameWidth - cornerRadius, top), strokeWidth)
        drawLine(color, Offset(left + frameWidth, top + cornerRadius), Offset(left + frameWidth, top + cornerLength), strokeWidth)

        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, top + frameHeight - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(left + cornerRadius, top + frameHeight), Offset(left + cornerLength, top + frameHeight), strokeWidth)
        drawLine(color, Offset(left, top + frameHeight - cornerLength), Offset(left, top + frameHeight - cornerRadius), strokeWidth)

        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left + frameWidth - cornerRadius * 2, top + frameHeight - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(left + frameWidth - cornerLength, top + frameHeight), Offset(left + frameWidth - cornerRadius, top + frameHeight), strokeWidth)
        drawLine(color, Offset(left + frameWidth, top + frameHeight - cornerLength), Offset(left + frameWidth, top + frameHeight - cornerRadius), strokeWidth)
    }
}
