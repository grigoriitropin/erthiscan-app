package io.erthiscan.scan

import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanScreen() {
    var isTorchOn by remember { mutableStateOf(false) }
    val vibrator = (LocalContext.current.getSystemService(VibratorManager::class.java))
        .defaultVibrator

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(torchEnabled = isTorchOn) { barcode ->
            Log.d("ErthiScan", "Scanned: $barcode")
        }

        ViewfinderOverlay()

        Text(
            text = "Scan Barcode",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(top = 64.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 80.dp)
                .size(77.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            FilledIconButton(
                onClick = {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, 50))
                    isTorchOn = !isTorchOn
                },
                modifier = Modifier.size(63.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.DarkGray
                )
            ) {
                Crossfade(targetState = isTorchOn) { torchOn ->
                    Icon(
                        imageVector = if (torchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOn,
                        contentDescription = "Toggle flashlight",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
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
