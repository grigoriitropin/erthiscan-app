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

/**
 * MAIN ACTIVITY: The sole entry point for the Erthiscan UI.
 * 
 * ARCHITECTURAL ROLE:
 * 1. HILT (@AndroidEntryPoint): Injects dependencies into this Activity.
 * 2. NAVIGATION: Orchestrates initial routing based on intents.
 * 3. PERMISSIONS: Hosts the Camera permission gate.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EDGE-TO-EDGE: Enables the app to draw behind system bars.
        enableEdgeToEdge()

        // DEEP LINK PARSING: Extract company ID if launched via App Link.
        val deepLinkCompanyId = parseDeepLinkCompanyId(intent)

        setContent {
            // VIEW MODEL: Manages startup session restoration.
            val vm: StartupViewModel = hiltViewModel()
            val restored by vm.isRestored.collectAsStateWithLifecycle()

            ErthiScanTheme {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (restored) {
                        // START ROUTE: Jump to company view if deep link exists.
                        val startRoute = deepLinkCompanyId?.let {
                            Route.Company(companyId = it)
                        }
                        // PERMISSION GATE: Block UI until camera access is granted.
                        RequireCameraPermissionGate { MainScreen(startRoute = startRoute) }
                    } else {
                        // STARTUP LOADING: Black screen while restoring session.
                        Box(Modifier.fillMaxSize().background(Color.Black))
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // RE-SET INTENT: Required so that deep links opened while activity is running are handled.
        setIntent(intent)
    }
}

/**
 * PARSE DEEP LINK: Extracts '/company/{id}' from 'pjdth.xyz'.
 */
private fun parseDeepLinkCompanyId(intent: Intent?): Int? {
    val uri = intent?.data ?: return null
    if (uri.host != "pjdth.xyz") return null
    val segments = uri.pathSegments
    if (segments.size == 2 && segments[0] == "company") {
        return segments[1].toIntOrNull()
    }
    return null
}

/**
 * REQUIRE CAMERA PERMISSION GATE: Suspends rendering until camera permission is granted.
 */
@Composable
private fun RequireCameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // PERMISSION STATE: Checked synchronously.
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // CONTRACT: Modern approach to request permissions.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    // RESUME OBSERVER: Detect permission changes when returning from system settings.
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // TRIGGER: Auto-launch request if not granted.
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    // RENDER CONTENT: Holds the camera/main content.
    Box(modifier = Modifier.fillMaxSize()) {
        if (granted) content()
    }
}