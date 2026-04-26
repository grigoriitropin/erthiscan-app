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

@Composable
fun CameraPreview(torchEnabled: Boolean = false, onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    var candidateValue by remember { mutableStateOf<String?>(null) }
    var candidateCount by remember { mutableIntStateOf(0) }
    var confirmedValue by remember { mutableStateOf<String?>(null) }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val preview = remember {
        Preview.Builder().build().apply {
            setSurfaceProvider { request ->
                surfaceRequest = request
            }
        }
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(analysisExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val centerX = imageProxy.width / 2f
                    val centerY = imageProxy.height / 2f

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val closest = barcodes.minByOrNull { barcode ->
                                val box = barcode.boundingBox ?: return@minByOrNull Float.MAX_VALUE
                                val dx = box.centerX() - centerX
                                val dy = box.centerY() - centerY
                                dx * dx + dy * dy
                            }

                            val value = closest?.rawValue
                            if (value != null) {
                                if (value == candidateValue) {
                                    candidateCount++
                                    if (candidateCount >= 5 && value != confirmedValue) {
                                        confirmedValue = value
                                        onBarcodeScanned(value)
                                    }
                                } else {
                                    candidateValue = value
                                    candidateCount = 1
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

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

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            imageAnalysis.clearAnalyzer()
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = Modifier.fillMaxSize()
        )
    }
}
