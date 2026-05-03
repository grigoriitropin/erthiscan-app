package io.erthiscan.company.page

import io.erthiscan.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * CREATE REPORT SCREEN: The form used to submit ethical claims or challenges.
 * 
 * DESIGN PHILOSOPHY:
 * This screen is context-aware. It handles three distinct use cases:
 * 1. ADD: Creating a new primary report for a company.
 * 2. CHALLENGE: Creating a nested sub-report against an existing claim.
 * 3. EDIT: Modifying a user's own existing report/challenge.
 * 
 * UI PATTERNS:
 * - Uses [BasicTextField] for high customization of input fields.
 * - Employs [LaunchedEffect] to handle one-time navigation events.
 * - Implements [BackHandler] to ensure clean navigation back to the company profile.
 */
@Composable
fun CreateReportScreen(
    // companyName: Passed from the caller to provide context in the header.
    companyName: String,
    // onBack: Callback to navigate up in the stack.
    onBack: () -> Unit,
    // onSubmitted: Callback to navigate back once the API successfully confirms the report.
    onSubmitted: () -> Unit,
    // ViewModel: Injected by Hilt, scoped to this navigation destination.
    vm: CreateReportViewModel = hiltViewModel(),
) {
    // STATE OBSERVATION: Collects the UI state from the ViewModel in a lifecycle-aware manner.
    val state by vm.state.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    // SIDE EFFECT: NAVIGATION
    // When the 'submitted' flag becomes true in the state, we trigger the callback.
    // This ensures we only navigate away AFTER the backend has confirmed the write.
    LaunchedEffect(state.submitted) { 
        if (state.submitted) onSubmitted() 
    }

    // SYSTEM NAVIGATION: Intercepts the physical back button or gesture.
    BackHandler { onBack() }

    // DYNAMIC TITLE: Determined by the ViewModel's initialization parameters.
    val title = when {
        vm.editReportId != null -> stringResource(R.string.edit_report)
        vm.parentId != null -> stringResource(R.string.challenge_report)
        else -> stringResource(R.string.add_report)
    }

    // ROOT CONTAINER: Applies background and accounts for status/navigation bars.
    Column(
        Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
    ) {
        // HEADER: Displays the action type (Add/Edit/Challenge).
        Text(
            text = title, 
            color = colorScheme.onBackground, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold,
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
        
        // CONTEXT SUB-HEADER: Displays the company name for clarity.
        Text(
            text = companyName, 
            color = colorScheme.onSurfaceVariant, 
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(Modifier.height(16.dp))

        // FORM SECTION: Contains text input and source URL.
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // REPORT TEXT INPUT: Large, multi-line field.
            BasicTextField(
                value = state.text,
                onValueChange = { vm.onText(it) },
                textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
                decorationBox = { inner ->
                    // PLACEHOLDER: Shown only when text is empty.
                    if (state.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.write_report_hint),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )
            
            Spacer(Modifier.height(8.dp))
            
            // CHARACTER COUNTER: Provides visual feedback on length limits.
            Text(
                text = "${state.text.length}/150", 
                color = colorScheme.onSurfaceVariant, 
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
            
            Spacer(Modifier.height(8.dp))

            // SOURCE URL INPUT: Single-line field for evidence links.
            BasicTextField(
                value = state.source,
                onValueChange = { vm.onSource(it) },
                textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
                decorationBox = { inner ->
                    // PLACEHOLDER for source URL.
                    if (state.source.isEmpty()) {
                        Text(
                            text = stringResource(R.string.source_url_hint),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )

            // ERROR DISPLAY: Shows validation or network errors inline.
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it.asComposableString(), 
                    color = colorScheme.error, 
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // SUBMIT BUTTON: Orchestrates the API call.
            Button(
                onClick = { vm.submit() },
                // ENABLED STATE: Disabled during active network requests to prevent double-submits.
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ),
            ) {
                // BUTTON LABEL: Swaps to a loading state during submission.
                Text(
                    text = if (state.submitting) stringResource(R.string.submitting) else stringResource(R.string.submit),
                    fontSize = 16.sp, 
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // BOTTOM SPACING: Ensures the button isn't flushed against the screen edge.
            Spacer(Modifier.height(16.dp))
        }
    }
}