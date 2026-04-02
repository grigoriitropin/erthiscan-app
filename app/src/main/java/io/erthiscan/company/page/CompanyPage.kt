package io.erthiscan.company.page

import android.util.Log
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.erthiscan.api.ApiClient
import io.erthiscan.api.CompanyDetail
import io.erthiscan.api.ReportItem
import kotlin.math.roundToInt

@Composable
fun CompanyPage(companyId: Int, onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var company by remember { mutableStateOf<CompanyDetail?>(null) }

    LaunchedEffect(companyId) {
        try {
            company = ApiClient.api.getCompany(companyId)
        } catch (e: Exception) {
            Log.e("ErthiScan", "Failed to load company", e)
        }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = colorScheme.onBackground,
                fontSize = 24.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = data.name,
                color = colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

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
                    ReportCard(report)
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
private fun ReportCard(report: ReportItem) {
    val colorScheme = MaterialTheme.colorScheme
    val voteColor = when {
        report.voteSum > 0 -> Color(0xFF43A047)
        report.voteSum < 0 -> Color(0xFFE53935)
        else -> colorScheme.onSurfaceVariant
    }

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
            Text(
                text = if (report.voteSum > 0) "+${report.voteSum}" else report.voteSum.toString(),
                color = voteColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
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
                Text(
                    text = source,
                    color = colorScheme.primary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
