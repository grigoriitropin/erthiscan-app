package io.erthiscan.company

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.erthiscan.api.ApiClient
import io.erthiscan.api.CompanyItem
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

@Composable
fun CompaniesScreen(onCompanyClick: (Int) -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    var companies by remember { mutableStateOf<List<CompanyItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("reports_desc") }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // Debounce search input
    LaunchedEffect(searchQuery) {
        if (searchQuery != debouncedQuery) {
            kotlinx.coroutines.delay(300)
            debouncedQuery = searchQuery
            currentPage = 1
        }
    }

    // Load data immediately when debounced query, sort, or page changes
    LaunchedEffect(debouncedQuery, sortMode, currentPage) {
        loading = true
        try {
            val response = ApiClient.api.getCompanies(search = debouncedQuery, sort = sortMode, page = currentPage)
            companies = response.items
            totalPages = response.pages
            listState.scrollToItem(0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ErthiScan", "Failed to load companies", e)
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
    ) {
        Text(
            text = "Companies",
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Search companies...",
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortChip("Reports", "reports_desc", sortMode) { sortMode = it; currentPage = 1 }
            SortChip("Score ↓", "score_desc", sortMode) { sortMode = it; currentPage = 1 }
            SortChip("Score ↑", "score_asc", sortMode) { sortMode = it; currentPage = 1 }
            SortChip("A → Z", "name_asc", sortMode) { sortMode = it; currentPage = 1 }
            SortChip("Z → A", "name_desc", sortMode) { sortMode = it; currentPage = 1 }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (loading && companies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(companies, key = { it.id }) { company ->
                CompanyRow(company, onClick = { onCompanyClick(company.id) })
            }

            if (totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentPage > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                                    .clickable { currentPage-- }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("←", color = colorScheme.onSurface, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "$currentPage / $totalPages",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        if (currentPage < totalPages) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                                    .clickable { currentPage++ }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("→", color = colorScheme.onSurface, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SortChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val selected = value == current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh)
            .clickable { onSelect(value) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CompanyRow(company: CompanyItem, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHigh)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = company.name,
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${company.reportCount} reports",
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (company.hasReports) {
            val displayScore = ((company.ethicalScore.coerceIn(-100f, 100f) + 100f) / 2f).roundToInt()
            val red = Color(0xFFE53935)
            val yellow = Color(0xFFFFB300)
            val green = Color(0xFF43A047)
            val t = displayScore / 100f
            val scoreColor = if (t < 0.5f) lerp(red, yellow, t * 2f) else lerp(yellow, green, (t - 0.5f) * 2f)

            Text(
                text = displayScore.toString(),
                color = scoreColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "—",
                color = colorScheme.onSurfaceVariant,
                fontSize = 20.sp
            )
        }
    }
}
