package io.erthiscan.company

import io.erthiscan.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.erthiscan.api.CompanyItem
import kotlin.math.roundToInt

@Composable
fun CompaniesScreen(
    onCompanyClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    vm: CompaniesViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.query) {
        if (state.companies.isNotEmpty()) listState.scrollToItem(0)
    }

    val context = LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = contentPadding.calculateTopPadding())
        ) {
            Text(
                stringResource(R.string.companies_title),
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
                if (state.query.isEmpty()) {
                    Text(stringResource(R.string.search_companies_hint), color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    stringResource(R.string.sort_reports) to "reports_desc",
                    stringResource(R.string.sort_score_desc) to "score_desc",
                    stringResource(R.string.sort_score_asc) to "score_asc",
                    stringResource(R.string.sort_name_asc) to "name_asc",
                    stringResource(R.string.sort_name_desc) to "name_desc",
                ).forEach { (label, value) ->
                    SortChip(label, value, state.sort) { vm.onSortChange(it) }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.loading && state.companies.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.loading), color = colorScheme.onSurfaceVariant)
                }
            } else if (!state.loading && state.companies.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_companies_found), color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                ) {
                    items(state.companies, key = { it.id }) { company ->
                        CompanyRow(company) { onCompanyClick(company.id) }
                    }

                    if (state.pages > 1) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.page > 1) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colorScheme.surfaceContainerHigh)
                                            .clickable { vm.prevPage() }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) { Text(stringResource(R.string.previous_page), color = colorScheme.onSurface, fontSize = 16.sp) }
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.page_indicator, state.page, state.pages), color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                Spacer(Modifier.width(16.dp))
                                if (state.page < state.pages) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colorScheme.surfaceContainerHigh)
                                            .clickable { vm.nextPage() }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) { Text(stringResource(R.string.next_page), color = colorScheme.onSurface, fontSize = 16.sp) }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}

@Composable
fun SortChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
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
fun CompanyRow(company: CompanyItem, onClick: () -> Unit) {
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
                text = androidx.compose.ui.res.pluralStringResource(R.plurals.reports_count, company.reportCount, company.reportCount),
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
                text = stringResource(R.string.score_dash),
                color = colorScheme.onSurfaceVariant,
                fontSize = 20.sp
            )
        }
    }
}