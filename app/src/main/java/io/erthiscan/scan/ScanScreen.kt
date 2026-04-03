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
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.erthiscan.api.ApiClient
import io.erthiscan.api.ScanBarcodeRequest
import io.erthiscan.api.ScanResponse

@Composable
fun ScanScreen(onViewCompany: (ScanResponse, Int) -> Unit = { _, _ -> }) {
    var isTorchOn by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var scanResult by remember { mutableStateOf<ScanResponse?>(null) }
    val vibrator = (LocalContext.current.getSystemService(VibratorManager::class.java))
        .defaultVibrator

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenMin = min(maxWidth, maxHeight)
        val outerButton = (screenMin * 0.18f).coerceIn(50.dp, 77.dp)
        val innerButton = outerButton * 0.82f
        val iconSize = outerButton * 0.47f

        CameraPreview(torchEnabled = isTorchOn) { barcode ->
            Log.d("ErthiScan", "Scanned: $barcode")
            scannedBarcode = barcode
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

            // Space for tab bar overlay
            Box(modifier = Modifier.weight(0.4f).fillMaxWidth())
        }

        scannedBarcode?.let { barcode ->
            LaunchedEffect(barcode) {
                try {
                    scanResult = ApiClient.api.scanBarcode(ScanBarcodeRequest(barcode))
                } catch (e: Exception) {
                    Log.e("ErthiScan", "API error", e)
                    scanResult = null
                }
            }
        }

        scanResult?.let { result ->
            ProductSheet(
                productName = result.product.name,
                companyName = result.company.name,
                companyId = result.company.id,
                ethicalScore = result.company.ethicalScore,
                hasReports = result.company.reportCount > 0,
                openFactsUrl = result.product.openFactsUrl,
                barcode = result.product.barcode,
                onDismiss = {
                    scannedBarcode = null
                    scanResult = null
                },
                onViewCompany = { companyId -> onViewCompany(result, companyId) }
            )
        }
    }
}

@Composable
fun TabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Companies", "Scan", "Profile")
    val tabWidths = listOf(110.dp, 80.dp, 80.dp)
    val animationProgress by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
    val indicatorOffset = when {
        animationProgress <= 1f -> lerp(0.dp, tabWidths[0], animationProgress)
        else -> tabWidths[0] + lerp(0.dp, tabWidths[1], animationProgress - 1f)
    }
    val indicatorWidth = when {
        animationProgress <= 1f -> lerp(tabWidths[0], tabWidths[1], animationProgress)
        else -> lerp(tabWidths[1], tabWidths[2], animationProgress - 1f)
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colorScheme.primaryContainer)
        )

        Row {
            tabs.forEachIndexed { index, title ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(tabWidths[index])
                        .height(32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabSelected(index) }
                ) {
                    Text(
                        text = title,
                        color = if (selectedTab == index) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
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
