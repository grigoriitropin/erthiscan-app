package io.erthiscan.scan

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.erthiscan.R

/**
 * PRODUCT NOT FOUND SHEET: A fallback UI for unrecognized barcodes.
 * 
 * ARCHITECTURAL ROLE:
 * When the ErthiScan backend fails to identify a barcode (404), this sheet 
 * is displayed to guide the user towards external contribution.
 * 
 * KEY FEATURES:
 * 1. DATA TRANSPARENCY: Displays the raw barcode value that wasn't found.
 * 2. CROWDSOURCING LOOP: Encourages users to add the product to Open Food Facts (OFF), 
 *    which serves as one of our data sources.
 * 3. DEEP LINKING: Attempts to open the Google Play Store directly to the OFF app 
 *    with a web fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductNotFoundSheet(
    barcode: String,
    onDismiss: () -> Unit
) {
    // STATE: Standard M3 bottom sheet state with non-persistent behavior.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER SECTION: Informs the user of the missing data.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.product_not_found),
                    color = colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                // DISPLAY: Shows the barcode so the user can verify they scanned correctly.
                Text(
                    text = barcode,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // EXPLANATION SECTION: Contextualizes why OFF is the recommended platform.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.help_build_database),
                    color = colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.open_food_facts_explanation),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CTA BUTTON: External navigation logic.
            Button(
                onClick = {
                    val pkg = "org.openfoodfacts.scanner"
                    try {
                        // MARKET SCHEME: Attempts to open the Play Store app.
                        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
                    } catch (_: Exception) {
                        // FALLBACK: Opens the browser if Play Store isn't available or fails.
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri()))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.add_on_open_food_facts),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
