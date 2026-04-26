package io.erthiscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.MaterialTheme
import io.erthiscan.ui.ErthiScanTheme
import io.erthiscan.nav.Route

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkCompanyId = parseDeepLinkCompanyId(intent)

        setContent {
            val vm: StartupViewModel = hiltViewModel()
            val restored by vm.isRestored.collectAsStateWithLifecycle()

            ErthiScanTheme {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (restored) {
                        val startRoute = deepLinkCompanyId?.let {
                            Route.Company(companyId = it)
                        }
                        RequireCameraPermissionGate { MainScreen(startRoute = startRoute) }
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.Black))
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Re-set the intent so deep links opened while activity is running are handled
        setIntent(intent)
    }
}

private fun parseDeepLinkCompanyId(intent: Intent?): Int? {
    val uri = intent?.data ?: return null
    if (uri.host != "pjdth.xyz") return null
    val segments = uri.pathSegments
    if (segments.size == 2 && segments[0] == "company") {
        return segments[1].toIntOrNull()
    }
    return null
}

@Composable
private fun RequireCameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    // This Box holds the camera/main content and should also be full screen
    Box(modifier = Modifier.fillMaxSize()) {
        if (granted) content()
    }
}