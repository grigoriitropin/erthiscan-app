package io.erthiscan.company.page

import io.erthiscan.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.erthiscan.api.CompanyDetail
import io.erthiscan.api.ReportItem
import io.erthiscan.api.SubReportItem
import kotlin.math.roundToInt

/**
 * COMPANY PAGE: The detailed profile view for a specific company.
 * 
 * ARCHITECTURAL ROLE:
 * This screen serves as the primary consumption point for ethical data. It renders 
 * a hierarchical list of reports (claims) and their nested sub-reports (challenges).
 * 
 * KEY FEATURES:
 * 1. DYNAMIC SCORING: Displays a normalized 0-100 score with a color-coded gauge.
 * 2. NESTED REPORTING: Supports parent reports and expandable "Challenge" sub-lists.
 * 3. DISPUTE DETECTION: Visually flags reports that have been heavily challenged.
 * 4. AUTHENTICATED ACTIONS: Delegates voting, editing, and deletion to the [CompanyPageViewModel].
 */
@Composable
fun CompanyPage(
    onBack: () -> Unit,
    onCreateReport: (String) -> Unit,
    onCreateChallenge: (String, Int) -> Unit,
    onEditReport: (Int, String, String, String) -> Unit,
    vm: CompanyPageViewModel = hiltViewModel(),
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by vm.state.collectAsStateWithLifecycle()
    val data = state.company
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ERROR OBSERVATION: Shows transient network/logic errors via Snackbar.
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // SYSTEM BARS: Ensures content correctly accounts for the status bar.
        contentWindowInsets = WindowInsets.systemBars
    ) { padding: PaddingValues ->
        // LOADING / FAILURE STATE
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(if (state.loading) stringResource(R.string.loading) else stringResource(R.string.failed_to_load), color = colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            // HEADER: Company Name
            Text(
                text = data.name,
                color = colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // MAIN SCROLLABLE CONTENT: Uses LazyColumn for performance with large report lists.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up all available space between header and system bars.
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Consistent spacing between cards.
            ) {
                // SECTION: OVERVIEW
                // Renders the visual score gauge and total report count.
                item {
                    ScoreCard(data.ethicalScore, data.reportCount)
                }

                // SECTION: USER ACTIONS
                // Provides a prominent button to start the report submission flow.
                item {
                    Button(
                        // CALLBACK: Triggered on click, passes current company name to the form 
                        // to provide context for the user during report writing.
                        onClick = { onCreateReport(data.name) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.add_report),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // SECTION HEADER: "Reports"
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.reports),
                        color = colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // CONDITIONAL LIST RENDERING: 
                // If the list is empty, we show a friendly placeholder card instead of a blank screen.
                if (data.reports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_reports_yet),
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // RENDERING INDIVIDUAL REPORTS:
                    // Using 'key' optimization to ensure Smooth scrolling and efficient re-compositions 
                    // when items are voted on or deleted.
                    items(data.reports, key = { it.id }) { report ->
                        ReportCardWithSubs(
                            report = report,
                            // CHALLENGE CALLBACK: Navigates to CreateReportScreen with a parentId reference.
                            onChallenge = { onCreateChallenge(data.name, report.id) },
                            // EDIT CALLBACK: 
                            // 1. Extracts the primary source URL (firstOrNull) to pre-fill the form.
                            // 2. Passes the report's text and company name to allow full context during editing.
                            onEdit = { onEditReport(report.id, report.text, report.sources.firstOrNull() ?: "", data.name) },
                            // DELETE CALLBACK: Triggers the ViewModel's destructive API call.
                            onDelete = { vm.delete(report.id) },
                            // VOTE CALLBACK: Delegates the integer value (1 or -1) to the ViewModel.
                            onVote = { reportId, value -> vm.vote(reportId, value) },
                            // SUB-REPORT (CHALLENGE) EDIT:
                            // Similar to primary reports, we pre-fill the form with the specific sub-report data.
                            onSubEdit = { sub ->
                                onEditReport(sub.id, sub.text, sub.sources.firstOrNull() ?: "", data.name)
                            },
                            // SUB-REPORT DELETE: Targeted deletion of a nested challenge.
                            onSubDelete = { sub -> vm.delete(sub.id) },
                            // SUB-REPORT VOTE: Reactive voting on a specific challenge.
                            onSubVote = { subId, value -> vm.vote(subId, value) },
                        )
                    }
                }

                // BOTTOM PADDING: Ensures the last item isn't clipped by the screen edge.
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

        }
    }
}

/**
 * SCORE CARD: Visualizes the company's ethical standing.
 * Maps the -100..100 raw score to a 0..100 display range.
 */
@Composable
private fun ScoreCard(ethicalScore: Float, reportCount: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val hasReports = reportCount > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHigh)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ethical_score),
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (hasReports) {
            val displayScore = ((ethicalScore.coerceIn(-100f, 100f) + 100f) / 2f).roundToInt()
            val red = Color(0xFFE53935)
            val yellow = Color(0xFFFFB300)
            val green = Color(0xFF43A047)
            val t = displayScore / 100f
            // COLOR INTERPOLATION: Shifts from Red to Yellow (0-50) and Yellow to Green (51-100).
            val scoreColor = if (t < 0.5f) lerp(red, yellow, t * 2f) else lerp(yellow, green, (t - 0.5f) * 2f)

            Text(
                text = displayScore.toString(),
                color = scoreColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = stringResource(R.string.score_dash),
                color = colorScheme.onSurfaceVariant,
                fontSize = 36.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pluralStringResource(R.plurals.reports_count, reportCount, reportCount),
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

/**
 * REPORT CARD WITH SUBS: Renders a primary claim and its nested challenges.
 * 
 * DISPUTE LOGIC:
 * A report is flagged with a red border if its total positive vote sum is 
 * significantly challenged by valid (voted 'True') counter-claims.
 * Heuristic: Disputed if (Challenges true_count sum) >= 70% of (Report vote_sum).
 */
@Composable
private fun ReportCardWithSubs(
    report: ReportItem,
    onChallenge: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVote: (Int, Int) -> Unit,
    onSubEdit: (SubReportItem) -> Unit,
    onSubDelete: (SubReportItem) -> Unit,
    onSubVote: (Int, Int) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // DISPUTE CALCULATION
    val voteSum = report.ethicalCount - report.unethicalCount
    val challengePenalty = report.subReports.sumOf { maxOf(0, it.trueCount - it.falseCount) }
    val isDisputed = voteSum > 0 && challengePenalty >= 0.7 * voteSum

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            text = stringResource(R.string.delete_report_confirmation),
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHigh)
    ) {
        // DISPUTE INDICATOR: A vertical red stripe on the left edge.
        if (isDisputed) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color(0xFFE53935))
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.author,
                        color = colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = report.createdAt.take(10), // Shows only YYYY-MM-DD
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                // EDIT/DELETE: Only rendered if the user has permission (handled by logic in the ViewModel 
                // but the UI must provide the callbacks).
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditChip(onClick = onEdit)
                    DeleteChip(onClick = { showDeleteDialog = true })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.text,
                color = colorScheme.onSurface,
                fontSize = 14.sp
            )

            // SOURCES: Clickable underlines that open external browser links.
            if (report.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                report.sources.forEach { source ->
                    val uri = if (source.startsWith("http://") || source.startsWith("https://")) source else "https://$source"
                    Text(
                        text = source,
                        color = colorScheme.primary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            try { uriHandler.openUri(uri) } catch (_: Exception) {}
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VOTING BAR
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoteChip(
                    label = stringResource(R.string.ethical),
                    count = report.ethicalCount,
                    color = Color(0xFF43A047),
                    isSelected = report.userVote == 1,
                    onClick = { onVote(report.id, 1) }
                )

                VoteChip(
                    label = stringResource(R.string.unethical),
                    count = report.unethicalCount,
                    color = Color(0xFFE53935),
                    isSelected = report.userVote == -1,
                    onClick = { onVote(report.id, -1) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // CHALLENGE BUTTON: Navigation trigger to CreateReportScreen with parentId.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onChallenge() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.challenge),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // CHALLENGES SUB-LIST: Logic for rendering counter-claims (sub-reports).
            // We only render this section if the backend actually returned child reports.
            if (report.subReports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // TOGGLE BUTTON: A clickable area to expand/collapse the challenge list.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp)) // Rounds the click ripple for better aesthetics.
                        .clickable { expanded = !expanded } // Reactively toggles the visibility state.
                        .padding(vertical = 4.dp)
                ) {
                    // DYNAMIC LABEL: Swaps text based on 'expanded' state and uses plural resources 
                    // to handle "1 challenge" vs "5 challenges" correctly across locales.
                    Text(
                         text = if (expanded) {
                             pluralStringResource(R.plurals.hide_challenges, report.subReports.size, report.subReports.size)
                         } else {
                             pluralStringResource(R.plurals.show_challenges, report.subReports.size, report.subReports.size)
                         },
                        color = colorScheme.primary, // Highlights the interactive element.
                        fontSize = 12.sp
                    )
                }

                // ANIMATED TRANSITION: Ensures the nested list doesn't "pop" in, 
                // providing a much smoother user experience during expansion.
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // NESTED ITERATION: Renders each individual challenge.
                        report.subReports.forEach { sub ->
                            SubReportCard(
                                sub = sub,
                                // NESTED EDIT: Triggers the same top-level edit flow, pre-filling 
                                // the form with sub-report specific data.
                                onEdit = { onSubEdit(sub) },
                                // NESTED DELETE: Triggers targeted deletion for this child report.
                                onDelete = { onSubDelete(sub) },
                                // NESTED VOTE: Allows users to verify or debunk the challenge claim.
                                onVote = { subId, value -> onSubVote(subId, value) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SUB REPORT CARD: A simplified card for nested challenges.
 */
@Composable
private fun SubReportCard(
    sub: SubReportItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVote: (Int, Int) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            text = stringResource(R.string.delete_challenge_confirmation),
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceContainerHighest)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sub.author,
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditChip(onClick = onEdit)
                DeleteChip(onClick = { showDeleteDialog = true })
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sub.text,
            color = colorScheme.onSurface,
            fontSize = 13.sp
        )

        if (sub.sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            sub.sources.forEach { source ->
                val uri = if (source.startsWith("http://") || source.startsWith("https://")) source else "https://$source"
                Text(
                    text = source,
                    color = colorScheme.primary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        try { uriHandler.openUri(uri) } catch (_: Exception) {}
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SUB-REPORT VOTING: Simplified labels (True/False) instead of Ethical/Unethical.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoteChip(
                 label = stringResource(R.string.true_label),
                    count = sub.trueCount,
                color = Color(0xFF43A047),
                    isSelected = sub.userVote == 1,
                onClick = { onVote(sub.id, 1) }
            )
            VoteChip(
                 label = stringResource(R.string.false_label),
                    count = sub.falseCount,
                color = Color(0xFFE53935),
                    isSelected = sub.userVote == -1,
                onClick = { onVote(sub.id, -1) }
            )
        }
    }
}

/**
 * VOTE CHIP: Reusable component for voting buttons with selection state colors.
 */
@Composable
private fun VoteChip(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = if (isSelected) color.copy(alpha = 0.25f) else colorScheme.surfaceContainerHighest
    val textColor = if (isSelected) color else colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label $count",
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
internal fun EditChip(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surfaceContainerHighest)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.edit),
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
internal fun DeleteChip(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.errorContainer)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.delete),
            color = colorScheme.onErrorContainer,
            fontSize = 11.sp
        )
    }
}

@Composable
internal fun ConfirmDeleteDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete)) },
        text = { Text(text) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}