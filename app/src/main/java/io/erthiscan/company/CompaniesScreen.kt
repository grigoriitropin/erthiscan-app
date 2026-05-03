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

/**
 * COMPANIES SCREEN: A paginated, searchable list of all tracked companies.
 * 
 * UI RESPONSIBILITIES:
 * 1. SEARCH: Real-time filtering via the [BasicTextField] connected to [CompaniesViewModel].
 * 2. SORTING: Horizontal chip-based selection for scoring, naming, and activity.
 * 3. PAGINATION: Handled via ViewModel state; UI renders prev/next controls dynamically.
 * 4. FEEDBACK: Integrated [SnackbarHost] for error reporting.
 */
@Composable
fun CompaniesScreen(
    // onCompanyClick: Navigation trigger when a list item is tapped.
    onCompanyClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // contentPadding: Top/Bottom offsets provided by the parent Scaffold.
    contentPadding: PaddingValues = PaddingValues(),
    // ViewModel: Scoped to this screen, handles search and pagination state.
    vm: CompaniesViewModel = hiltViewModel()
) {
    // STATE COLLECTION: Subscribes to the ViewModel's state flow in a lifecycle-aware way.
    val state by vm.state.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    
    // SNACKBAR STATE: Manages the display of transient error messages.
    val snackbarHostState = remember { SnackbarHostState() }
    
    // LIST STATE: Required to programmatically control scroll position.
    val listState = rememberLazyListState()

    // AUTO-SCROLL SIDE EFFECT:
    // When the user changes the search query, we automatically scroll back to the 
    // first item (index 0). This prevents the UI from showing a blank area if the 
    // previous list was scrolled down and the new results are few.
    LaunchedEffect(state.query) {
        if (state.companies.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // ERROR REPORTING SIDE EFFECT:
    // Watches the 'error' field in the state. If an error appears, we show it 
    // in a Snackbar and then immediately tell the ViewModel to dismiss it.
    val context = LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    // ROOT LAYOUT
    Box(modifier = modifier.fillMaxSize().background(colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // STATUS BARS: Avoids overlapping with system icons at the top.
                .windowInsetsPadding(WindowInsets.statusBars)
                // SCAFFOLD PADDING: Prevents content from hiding behind the bottom TabBar.
                .padding(top = contentPadding.calculateTopPadding())
        ) {
            // SCREEN TITLE
            Text(
                text = stringResource(R.string.companies_title),
                color = colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // SEARCH BAR CONTAINER: Custom surface with rounded corners.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // PLACEHOLDER HINT: Rendered manually behind the text field.
                if (state.query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_companies_hint), 
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        fontSize = 16.sp
                    )
                }
                
                // INPUT FIELD: Bare-metal text field for maximum styling control.
                BasicTextField(
                    value = state.query,
                    // DELEGATION: Updates the ViewModel query, which triggers the debounced API call.
                    onValueChange = vm::onQueryChange,
                    textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(8.dp))

            // SORTING SECTION: Horizontal scrollable row of chips.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Iterate through available sort options (label to API-value mapping).
                listOf(
                    stringResource(R.string.sort_reports) to "reports_desc",
                    stringResource(R.string.sort_score_desc) to "score_desc",
                    stringResource(R.string.sort_score_asc) to "score_asc",
                    stringResource(R.string.sort_name_asc) to "name_asc",
                    stringResource(R.string.sort_name_desc) to "name_desc",
                ).forEach { (label, value) ->
                    SortChip(
                        label = label, 
                        value = value, 
                        current = state.sort, 
                        onSelect = { vm.onSortChange(it) } // Resets pagination on change.
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // CONDITIONAL CONTENT RENDERING
            if (state.loading && state.companies.isEmpty()) {
                // INITIAL LOADING STATE
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.loading), color = colorScheme.onSurfaceVariant)
                }
            } else if (!state.loading && state.companies.isEmpty()) {
                // EMPTY RESULTS STATE
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_companies_found), color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            } else {
                // SUCCESS STATE: Main list of companies.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // BOTTOM PADDING: Scaffold inner padding + extra margin.
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                ) {
                    // MAIN LIST ITEMS
                    items(state.companies, key = { it.id }) { company ->
                        CompanyRow(company) { onCompanyClick(company.id) }
                    }

                    // PAGINATION CONTROLS: Rendered as a footer item.
                    if (state.pages > 1) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // PREVIOUS PAGE BUTTON
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
                                
                                // CURRENT POSITION INDICATOR
                                Text(
                                    text = stringResource(R.string.page_indicator, state.page, state.pages), 
                                    color = colorScheme.onSurfaceVariant, 
                                    fontSize = 14.sp
                                )
                                
                                Spacer(Modifier.width(16.dp))
                                
                                // NEXT PAGE BUTTON
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
                    
                    // EXTRA SPACING for the last item.
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        // SNACKBAR HOST: Renders at the bottom, above the TabBar.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}

/**
 * SORT CHIP: Component representing a single sorting criteria.
 */
@Composable
fun SortChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val selected = value == current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            // SELECTION STYLING: High contrast for active chip.
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

/**
 * COMPANY ROW: Displays summary data for a company item in the list.
 */
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
        // CONTENT AREA: Name and Activity Count.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = company.name,
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            // pluralStringResource: Handles grammatically correct "0 reports", "1 report", "5 reports".
            Text(
                text = androidx.compose.ui.res.pluralStringResource(R.plurals.reports_count, company.reportCount, company.reportCount),
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ETHICAL SCORE VISUALIZATION
        if (company.hasReports) {
            // NORMALIZE SCORE: Convert backend range (-100..100) to human-readable (0..100).
            // Formula: ((Value + 100) / 2)
            val displayScore = ((company.ethicalScore.coerceIn(-100f, 100f) + 100f) / 2f).roundToInt()
            
            // COLOR GAUGE:
            val red = Color(0xFFE53935)
            val yellow = Color(0xFFFFB300)
            val green = Color(0xFF43A047)
            
            val t = displayScore / 100f
            
            // LINEAR INTERPOLATION (LERP):
            // Smoothly transitions between colors based on the normalized score value.
            val scoreColor = if (t < 0.5f) {
                // If score is between 0 and 50, interpolate from Red to Yellow.
                lerp(red, yellow, t * 2f)
            } else {
                // If score is between 51 and 100, interpolate from Yellow to Green.
                lerp(yellow, green, (t - 0.5f) * 2f)
            }

            Text(
                text = displayScore.toString(),
                color = scoreColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            // FALLBACK: Display dash if no community reports have been submitted yet.
            Text(
                text = stringResource(R.string.score_dash),
                color = colorScheme.onSurfaceVariant,
                fontSize = 20.sp
            )
        }
    }
}