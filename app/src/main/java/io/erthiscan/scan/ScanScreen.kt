package io.erthiscan.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.lerp
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

@Composable
fun ScanScreen() {
    var isTorchOn by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val vibrator = (LocalContext.current.getSystemService(VibratorManager::class.java))
        .defaultVibrator

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenMin = min(maxWidth, maxHeight)
        val outerButton = (screenMin * 0.18f).coerceIn(50.dp, 77.dp)
        val innerButton = outerButton * 0.82f
        val iconSize = outerButton * 0.47f

        CameraPreview(torchEnabled = isTorchOn) { barcode ->
            Log.d("ErthiScan", "Scanned: $barcode")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Scan Barcode",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }

            BoxWithConstraints(
                modifier = Modifier.weight(3f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val frameSize = (min(maxWidth, maxHeight) * 0.8f).coerceAtMost(400.dp)
                ViewfinderOverlay(
                    modifier = Modifier.size(frameSize)
                )
            }

            Box(
                modifier = Modifier.weight(0.8f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(outerButton)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    FilledIconButton(
                        onClick = {
                            vibrator.vibrate(VibrationEffect.createOneShot(30, 50))
                            isTorchOn = !isTorchOn
                        },
                        modifier = Modifier.size(innerButton),
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
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.weight(0.4f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TabBar(selectedTab = selectedTab, onTabSelected = {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, 50))
                    selectedTab = it
                })
            }
        }
    }
}

@Composable
fun TabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Scan", "Companies")
    val tabWidths = listOf(80.dp, 110.dp)
    val animationProgress by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
    val indicatorOffset = lerp(0.dp, tabWidths[0], animationProgress)
    val indicatorWidth = lerp(tabWidths[0], tabWidths[1], animationProgress)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Row {
            tabs.forEachIndexed { index, title ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(tabWidths[index])
                        .height(32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTabSelected(index) }
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val frameWidth = size.width
        val frameHeight = size.height

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
            topLeft = Offset(0f, 0f),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(cornerRadius, 0f), Offset(cornerLength, 0f), strokeWidth)
        drawLine(color, Offset(0f, cornerRadius), Offset(0f, cornerLength), strokeWidth)

        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(frameWidth - cornerRadius * 2, 0f),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(frameWidth - cornerLength, 0f), Offset(frameWidth - cornerRadius, 0f), strokeWidth)
        drawLine(color, Offset(frameWidth, cornerRadius), Offset(frameWidth, cornerLength), strokeWidth)

        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(0f, frameHeight - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(cornerRadius, frameHeight), Offset(cornerLength, frameHeight), strokeWidth)
        drawLine(color, Offset(0f, frameHeight - cornerLength), Offset(0f, frameHeight - cornerRadius), strokeWidth)

        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(frameWidth - cornerRadius * 2, frameHeight - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = stroke
        )
        drawLine(color, Offset(frameWidth - cornerLength, frameHeight), Offset(frameWidth - cornerRadius, frameHeight), strokeWidth)
        drawLine(color, Offset(frameWidth, frameHeight - cornerLength), Offset(frameWidth, frameHeight - cornerRadius), strokeWidth)
    }
}
