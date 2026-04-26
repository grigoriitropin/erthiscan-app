package io.erthiscan.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.erthiscan.R
import io.erthiscan.api.ScanResponse
import io.erthiscan.util.vibrateShort

@Composable
fun ScanScreen(
    onViewCompany: (ScanResponse, Int) -> Unit = { _, _ -> },
    innerPadding: PaddingValues = PaddingValues(0.dp),
    vm: ScanViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(torchEnabled = state.torch) { barcode ->
            vm.onBarcode(barcode)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .consumeWindowInsets(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.scan_barcode),
                    color = Color.White,
                    fontSize = 20.sp
                )
            }

            BoxWithConstraints(
                modifier = Modifier.weight(3f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val frameSize = (min(maxWidth, maxHeight) * 0.8f).coerceAtMost(400.dp)
                ViewfinderOverlay(modifier = Modifier.size(frameSize))
            }

            Box(
                modifier = Modifier.weight(1.2f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val windowInfo = LocalWindowInfo.current
                val density = LocalDensity.current
                val screenMin = with(density) {
                    min(windowInfo.containerSize.width.toDp(), windowInfo.containerSize.height.toDp())
                }
                val outerButton = (screenMin * 0.18f).coerceIn(50.dp, 77.dp)
                val innerButton = outerButton * 0.82f
                val iconSize = outerButton * 0.47f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(outerButton)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    FilledIconButton(
                        onClick = {
                            context.vibrateShort()
                            vm.toggleTorch()
                        },
                        modifier = Modifier.size(innerButton),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.DarkGray
                        )
                    ) {
                        Crossfade(targetState = state.torch, label = "torch") { torchOn ->
                            Icon(
                                imageVector = if (torchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOn,
                                contentDescription = stringResource(R.string.toggle_flashlight),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(innerPadding)
        )

        state.notFoundBarcode?.let { barcode ->
            ProductNotFoundSheet(
                barcode = barcode,
                onDismiss = vm::dismissNotFound
            )
        }

        state.result?.let { result ->
            ProductSheet(
                productName = result.product.name,
                companyName = result.company.name,
                companyId = result.company.id,
                ethicalScore = result.company.ethicalScore,
                hasReports = result.company.reportCount > 0,
                openFactsUrl = result.product.openFactsUrl,
                onDismiss = vm::dismissResult,
                onViewCompany = { companyId -> onViewCompany(result, companyId) }
            )
        }
    }
}

@Composable
fun TabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        stringResource(R.string.tab_companies),
        stringResource(R.string.tab_scan),
        stringResource(R.string.tab_profile),
    )
    val tabWidth = 100.dp

    val indicatorOffset by animateDpAsState(
        targetValue = tabWidth * selectedTab,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "TabIndicator"
    )
    val density = LocalDensity.current

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(with(density) { indicatorOffset.roundToPx() }, 0) }
                .width(tabWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colorScheme.primaryContainer)
        )

        Row {
            tabs.forEachIndexed { index, title ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(tabWidth)
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
