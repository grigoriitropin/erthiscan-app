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

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding: PaddingValues ->
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

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ScoreCard(data.ethicalScore, data.reportCount)
                }

                item {
                    Button(
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

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.reports),
                        color = colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                    items(data.reports, key = { it.id }) { report ->
                        ReportCardWithSubs(
                            report = report,
                            onChallenge = { onCreateChallenge(data.name, report.id) },
                            onEdit = { onEditReport(report.id, report.text, report.sources.firstOrNull() ?: "", data.name) },
                            onDelete = { vm.delete(report.id) },
                            onVote = { reportId, value -> vm.vote(reportId, value) },
                            onSubEdit = { sub ->
                                onEditReport(sub.id, sub.text, sub.sources.firstOrNull() ?: "", data.name)
                            },
                            onSubDelete = { sub -> vm.delete(sub.id) },
                            onSubVote = { subId, value -> vm.vote(subId, value) },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

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
                        text = report.createdAt.take(10),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
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

            if (report.subReports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                         text = if (expanded) pluralStringResource(R.plurals.hide_challenges, report.subReports.size, report.subReports.size)
                               else pluralStringResource(R.plurals.show_challenges, report.subReports.size, report.subReports.size),
                        color = colorScheme.primary,
                        fontSize = 12.sp
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        report.subReports.forEach { sub ->
                            SubReportCard(
                                sub = sub,
                                onEdit = { onSubEdit(sub) },
                                onDelete = { onSubDelete(sub) },
                                onVote = { subId, value -> onSubVote(subId, value) },
                            )
                        }
                    }
                }
            }
        }
    }
}

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