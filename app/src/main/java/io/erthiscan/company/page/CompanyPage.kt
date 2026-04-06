package io.erthiscan.company.page

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.erthiscan.api.ApiClient
import io.erthiscan.api.CompanyDetail
import io.erthiscan.api.ReportItem
import io.erthiscan.api.SubReportItem
import io.erthiscan.api.VoteRequest
import io.erthiscan.auth.AuthManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class CompanyPageScreen { DETAIL, CREATE_REPORT, CREATE_CHALLENGE }

@Composable
fun CompanyPage(companyId: Int, onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var company by remember { mutableStateOf<CompanyDetail?>(null) }
    var screen by remember { mutableStateOf(CompanyPageScreen.DETAIL) }
    var challengeParentId by remember { mutableStateOf<Int?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(companyId, refreshKey) {
        try {
            company = ApiClient.api.getCompany(companyId)
        } catch (e: Exception) {
            Log.e("ErthiScan", "Failed to load company", e)
        }
    }

    when (screen) {
        CompanyPageScreen.CREATE_REPORT -> {
            CreateReportScreen(
                companyId = companyId,
                companyName = company?.name ?: "",
                onBack = { screen = CompanyPageScreen.DETAIL },
                onSubmitted = {
                    screen = CompanyPageScreen.DETAIL
                    refreshKey++
                }
            )
            return
        }
        CompanyPageScreen.CREATE_CHALLENGE -> {
            CreateReportScreen(
                companyId = companyId,
                companyName = company?.name ?: "",
                parentId = challengeParentId,
                onBack = { screen = CompanyPageScreen.DETAIL },
                onSubmitted = {
                    screen = CompanyPageScreen.DETAIL
                    refreshKey++
                }
            )
            return
        }
        CompanyPageScreen.DETAIL -> {}
    }

    val data = company
    if (data == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", color = colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ScoreCard(data.ethicalScore, data.reportCount)
            }

            if (AuthManager.isLoggedIn) {
                item {
                    Button(
                        onClick = { screen = CompanyPageScreen.CREATE_REPORT },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Add Report",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reports",
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
                            text = "No reports yet",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(data.reports, key = { it.id }) { report ->
                    ReportCardWithSubs(
                        report = report,
                        onChallenge = {
                            challengeParentId = report.id
                            screen = CompanyPageScreen.CREATE_CHALLENGE
                        },
                        onVoteUpdated = { refreshKey++ }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
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
            text = "Ethical Score",
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
                text = "—",
                color = colorScheme.onSurfaceVariant,
                fontSize = 36.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$reportCount reports",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ReportCardWithSubs(
    report: ReportItem,
    onChallenge: () -> Unit,
    onVoteUpdated: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope
    val uriHandler = LocalUriHandler.current
    var ethicalCount by remember(report.id) { mutableIntStateOf(report.ethicalCount) }
    var unethicalCount by remember(report.id) { mutableIntStateOf(report.unethicalCount) }
    var userVote by remember(report.id) { mutableStateOf(report.userVote) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = report.author,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
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

        // Vote chips row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ethical chip
            VoteChip(
                label = "Ethical",
                count = ethicalCount,
                color = Color(0xFF43A047),
                isSelected = userVote == 1,
                onClick = {
                    if (!AuthManager.isLoggedIn) return@VoteChip
                    scope.launch {
                        try {
                            val resp = ApiClient.api.vote(report.id, VoteRequest(1))
                            ethicalCount = resp.ethicalCount
                            unethicalCount = resp.unethicalCount
                            userVote = resp.userVote
                            onVoteUpdated()
                        } catch (e: Exception) {
                            Log.e("ErthiScan", "Vote failed", e)
                        }
                    }
                }
            )

            // Unethical chip
            VoteChip(
                label = "Unethical",
                count = unethicalCount,
                color = Color(0xFFE53935),
                isSelected = userVote == -1,
                onClick = {
                    if (!AuthManager.isLoggedIn) return@VoteChip
                    scope.launch {
                        try {
                            val resp = ApiClient.api.vote(report.id, VoteRequest(-1))
                            ethicalCount = resp.ethicalCount
                            unethicalCount = resp.unethicalCount
                            userVote = resp.userVote
                            onVoteUpdated()
                        } catch (e: Exception) {
                            Log.e("ErthiScan", "Vote failed", e)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Challenge button
            if (AuthManager.isLoggedIn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorScheme.surfaceContainerHighest)
                        .clickable { onChallenge() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Challenge",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Sub-reports
        if (report.subReports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (expanded) "Hide challenges (${report.subReports.size})"
                           else "Show challenges (${report.subReports.size})",
                    color = colorScheme.primary,
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    report.subReports.forEach { sub ->
                        SubReportCard(sub)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubReportCard(sub: SubReportItem) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope
    val uriHandler = LocalUriHandler.current
    var trueCount by remember(sub.id) { mutableIntStateOf(sub.trueCount) }
    var falseCount by remember(sub.id) { mutableIntStateOf(sub.falseCount) }
    var userVote by remember(sub.id) { mutableStateOf(sub.userVote) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceContainerHighest)
            .padding(12.dp)
    ) {
        Text(
            text = sub.author,
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
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
                label = "True",
                count = trueCount,
                color = Color(0xFF43A047),
                isSelected = userVote == 1,
                onClick = {
                    if (!AuthManager.isLoggedIn) return@VoteChip
                    scope.launch {
                        try {
                            val resp = ApiClient.api.vote(sub.id, VoteRequest(1))
                            trueCount = resp.ethicalCount
                            falseCount = resp.unethicalCount
                            userVote = resp.userVote
                        } catch (e: Exception) {
                            Log.e("ErthiScan", "Vote failed", e)
                        }
                    }
                }
            )
            VoteChip(
                label = "False",
                count = falseCount,
                color = Color(0xFFE53935),
                isSelected = userVote == -1,
                onClick = {
                    if (!AuthManager.isLoggedIn) return@VoteChip
                    scope.launch {
                        try {
                            val resp = ApiClient.api.vote(sub.id, VoteRequest(-1))
                            trueCount = resp.ethicalCount
                            falseCount = resp.unethicalCount
                            userVote = resp.userVote
                        } catch (e: Exception) {
                            Log.e("ErthiScan", "Vote failed", e)
                        }
                    }
                }
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
