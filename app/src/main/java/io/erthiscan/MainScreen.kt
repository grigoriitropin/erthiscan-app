package io.erthiscan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.erthiscan.company.CompaniesScreen
import io.erthiscan.company.page.CompanyPage
import io.erthiscan.company.page.CreateReportScreen
import io.erthiscan.nav.Route
import io.erthiscan.profile.ProfileScreen
import io.erthiscan.scan.ScanScreen
import io.erthiscan.scan.TabBar
import io.erthiscan.api.ScanResponse
import io.erthiscan.util.vibrateShort
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MainScreen(startRoute: Route? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(startRoute) {
        if (startRoute != null) {
            navController.navigate(startRoute)
        }
    }

    val showBottomBar = currentDestination?.let { dest ->
        dest.hasRoute<Route.Companies>() || 
        dest.hasRoute<Route.Scan>() || 
        dest.hasRoute<Route.Profile>()
    } ?: true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val selected = when {
                        currentDestination?.hasRoute<Route.Companies>() == true -> 0
                        currentDestination?.hasRoute<Route.Scan>() == true -> 1
                        currentDestination?.hasRoute<Route.Profile>() == true -> 2
                        else -> 1
                    }
                    
                    TabBar(selected) { idx ->
                        context.vibrateShort()
                        val target = when (idx) { 
                            0 -> Route.Companies 
                            1 -> Route.Scan 
                            else -> Route.Profile 
                        }
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
        // We set contentWindowInsets to zero to avoid Scaffold applying double padding internally
        // We will handle safeDrawing via the screens individually
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        // We MUST NOT apply padding(innerPadding) to the NavHost container if we want full screen camera
        // Instead, we pass the padding values to the screens
        // Disable transitions when Scan is involved (camera surface lingers),
        // otherwise keep a short fade for a smooth nav feel.
        fun NavBackStackEntry.isScan(): Boolean = destination.hasRoute<Route.Scan>()
        NavHost(
            navController = navController,
            startDestination = Route.Scan,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (initialState.isScan() || targetState.isScan()) EnterTransition.None
                else fadeIn(tween(180))
            },
            exitTransition = {
                if (initialState.isScan() || targetState.isScan()) ExitTransition.None
                else fadeOut(tween(180))
            },
            popEnterTransition = {
                if (initialState.isScan() || targetState.isScan()) EnterTransition.None
                else fadeIn(tween(180))
            },
            popExitTransition = {
                if (initialState.isScan() || targetState.isScan()) ExitTransition.None
                else fadeOut(tween(180))
            },
        ) {
            composable<Route.Companies> {
                CompaniesScreen(
                    onCompanyClick = { id -> navController.navigate(Route.Company(id)) },
                    contentPadding = innerPadding,
                )
            }
            composable<Route.Scan> {
                ScanScreen(
                    onViewCompany = { _, id -> navController.navigate(Route.Company(id)) },
                    innerPadding = innerPadding // Pass padding so buttons stay above TabBar
                )
            }
            composable<Route.Profile> {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ProfileScreen(
                        onShowReports = { navController.navigate(Route.ProfileReports) },
                        onShowChallenges = { navController.navigate(Route.ProfileChallenges) },
                        onCompanyClick = { companyId, reportId ->
                            navController.navigate(Route.Company(companyId, reportId))
                        },
                        modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                    )
                }
            }
            composable<Route.ProfileReports> {
                io.erthiscan.profile.MyReportsScreen(
                    onBack = { navController.popBackStack() },
                    onCompanyClick = { companyId, reportId ->
                        navController.navigate(Route.Company(companyId, reportId))
                    },
                    onEdit = { r -> navController.navigate(Route.CreateReport(
                        companyId = r.companyId, companyName = r.companyName, editReportId = r.id, initialText = r.text, initialSource = r.sources.firstOrNull().orEmpty())) },
                )
            }
            composable<Route.ProfileChallenges> {
                io.erthiscan.profile.MyChallengesScreen(
                    onBack = { navController.popBackStack() },
                    onCompanyClick = { companyId, reportId ->
                        navController.navigate(Route.Company(companyId, reportId))
                    },
                    onEdit = { c -> navController.navigate(Route.CreateReport(
                        companyId = c.companyId, companyName = c.companyName, editReportId = c.id, initialText = c.text, initialSource = c.sources.firstOrNull().orEmpty())) },
                )
            }
            composable<Route.Company> { entry ->
                val r = entry.toRoute<Route.Company>()
                CompanyPage(
                    onBack = { navController.popBackStack() },
                    onCreateReport = { companyName ->
                        navController.navigate(Route.CreateReport(companyId = r.companyId, companyName = companyName))
                    },
                    onCreateChallenge = { companyName, parentId ->
                        navController.navigate(Route.CreateReport(companyId = r.companyId, companyName = companyName, parentId = parentId))
                    },
                    onEditReport = { reportId, text, source, companyName ->
                        navController.navigate(Route.CreateReport(
                            companyId = r.companyId, companyName = companyName, editReportId = reportId, initialText = text, initialSource = source))
                    },
                )
            }
            composable<Route.CreateReport> { entry ->
                val r = entry.toRoute<Route.CreateReport>()
                CreateReportScreen(
                    companyName = r.companyName,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() },
                )
            }
        }
    }
}