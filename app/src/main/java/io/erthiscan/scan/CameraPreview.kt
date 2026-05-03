package io.erthiscan.scan

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.guava.await
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * CAMERA PREVIEW: A specialized barcode scanning component.
 * 
 * ARCHITECTURAL ROLE:
 * This component bridges the Android CameraX API with Google ML Kit to provide 
 * a real-time scanning experience. It handles lifecycle management, frame 
 * analysis, and barcode confirmation logic.
 * 
 * KEY FEATURES:
 * 1. LIFECYCLE AWARE: Automatically binds/unbinds from the host lifecycle.
 * 2. NOISE FILTERING: Uses a "Candidate Confirmation" system to prevent false positives.
 * 3. PROXIMITY FOCUS: Prioritizes barcodes closest to the center of the frame.
 * 4. TORCH CONTROL: Exposes hardware flash control for low-light environments.
 */
@Composable
fun CameraPreview(torchEnabled: Boolean = false, onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // EXECUTOR: A dedicated single thread for image analysis to avoid blocking the Main thread.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // ML KIT SCANNER: The underlying engine for barcode detection.
    val scanner = remember { BarcodeScanning.getClient() }

    // SCANNING STATE:
    // candidateValue/Count: Implements a "debouncing" mechanism where a barcode 
    // must be seen N times consecutively before it is considered a valid scan.
    var candidateValue by remember { mutableStateOf<String?>(null) }
    var candidateCount by remember { mutableIntStateOf(0) }
    var confirmedValue by remember { mutableStateOf<String?>(null) }
    
    // CAMERAX STATE:
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // PREVIEW USE CASE: Renders the camera stream to the UI.
    val preview = remember {
        Preview.Builder().build().apply {
            setSurfaceProvider { request ->
                surfaceRequest = request
            }
        }
    }

    // ANALYSIS USE CASE: Processes individual frames for barcodes.
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            // STRATEGY: Drops old frames if the analyzer is busy, ensuring we 
            // always process the most recent visual data.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(analysisExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    // COORDINATES: Used to calculate distance from frame center.
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val centerX = imageProxy.width / 2f
                    val centerY = imageProxy.height / 2f

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            // PROXIMITY LOGIC: 
                            // If multiple barcodes are visible, we pick the one 
                            // nearest to the center of the viewfinder.
                            val closest = barcodes.minByOrNull { barcode ->
                                val box = barcode.boundingBox ?: return@minByOrNull Float.MAX_VALUE
                                val dx = box.centerX() - centerX
                                val dy = box.centerY() - centerY
                                dx * dx + dy * dy
                            }

                            val value = closest?.rawValue
                            if (value != null) {
                                // CONFIRMATION LOGIC:
                                // To ensure accuracy, we require the same barcode 
                                // to be detected in 5 consecutive frames.
                                if (value == candidateValue) {
                                    candidateCount++
                                    if (candidateCount >= 5 && value != confirmedValue) {
                                        confirmedValue = value
                                        onBarcodeScanned(value)
                                    }
                                } else {
                                    // Reset if the scanner sees a different value.
                                    candidateValue = value
                                    candidateCount = 1
                                }
                            }
                        }
                        .addOnCompleteListener {
                            // IMPORTANT: ImageProxy must be closed to free up the frame buffer.
                            imageProxy.close()
                        }
                }
            }
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // INITIALIZATION: Binds the camera to the lifecycle.
    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).await()
        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
        cameraProvider = provider
    }

    // CLEANUP: Ensures resources are released when the component is removed from the composition.
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            imageAnalysis.clearAnalyzer()
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    // REACTIVE TORCH: Responds to external toggle changes (e.g., UI button).
    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // RENDERING: Uses the specialized CameraXViewfinder for optimized surface rendering.
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = Modifier.fillMaxSize()
        )
    }
}
