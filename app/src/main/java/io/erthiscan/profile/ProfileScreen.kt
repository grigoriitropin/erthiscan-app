package io.erthiscan.profile

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import io.erthiscan.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.erthiscan.BuildConfig
import io.erthiscan.company.page.ConfirmDeleteDialog
import io.erthiscan.company.page.DeleteChip
import io.erthiscan.company.page.EditChip

/**
 * PROFILE SCREEN
 * 
 * ARCHITECTURAL ROLE:
 * This screen serves as the user's personal hub. It handles the duality of:
 * 1. UNAUTHENTICATED STATE: Prompting for Google Sign-In.
 * 2. AUTHENTICATED STATE: Showing contributions, stats, and account settings.
 */
@Composable
fun ProfileScreen(
    onShowReports: () -> Unit,
    onShowChallenges: () -> Unit,
    onCompanyClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    vm: ProfileViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // DATA SYNC: Automatically fetches the latest user profile and contribution counts 
    // whenever the screen is accessed while logged in.
    LaunchedEffect(Unit) {
        if (ui.auth.isLoggedIn) vm.refresh()
    }

    // ERROR OBSERVATION: Displays transient network or authentication failures.
    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // INSET HANDLING: Uses custom padding calculation to avoid overlapping with bottom navigation.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = padding.calculateBottomPadding()
                )
                .consumeWindowInsets(padding)
        ) {
            // STATE-DRIVEN UI: Swaps the entire screen content based on login status.
            if (ui.auth.isLoggedIn) {
                LoggedInProfile(
                    onShowReports = onShowReports,
                    onShowChallenges = onShowChallenges,
                    onLogout = { vm.logout() },
                    ui = ui
                )
            } else {
                SignInScreen(onGoogleIdToken = { token -> vm.signInGoogle(token) })
            }
        }
    }
}

/**
 * LOGGED IN PROFILE: The main dashboard for authenticated users.
 */
@Composable
private fun LoggedInProfile(
    onShowReports: () -> Unit,
    onShowChallenges: () -> Unit,
    onLogout: () -> Unit,
    ui: ProfileUiState
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // IDENTITY: Displays the username from the latest profile sync or cached auth state.
        Text(
            text = ui.profile?.username ?: ui.auth.username ?: "",
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // CONTRIBUTION STATS: Shows total impact using plural resources for localization.
        if (ui.profile != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pluralStringResource(R.plurals.reports_and_challenges, ui.profile!!.reportCount, ui.profile!!.reportCount, ui.profile!!.challengeCount),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // NAVIGATION: Links to detailed history views.
        Button(
            onClick = onShowReports,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                text = stringResource(R.string.my_reports),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onShowChallenges,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                text = stringResource(R.string.my_challenges),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SIGNOUT: Uses a high-contrast 'Error' color scheme to indicate a destructive action.
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.errorContainer,
                contentColor = colorScheme.onErrorContainer
            )
        ) {
            Text(
                text = stringResource(R.string.sign_out),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1.5f))
    }
}

/**
 * MY REPORTS SCREEN: A historical list of every primary claim made by the user.
 * Provides deep-linking back to the specific company page and in-place editing.
 */
@Composable
fun MyReportsScreen(
    onBack: () -> Unit,
    onCompanyClick: (Int, Int) -> Unit,
    onEdit: (io.erthiscan.api.UserReportItem) -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val ui by vm.ui.collectAsStateWithLifecycle()
    var deletingReport by remember { mutableStateOf<io.erthiscan.api.UserReportItem?>(null) }

    LaunchedEffect(Unit) { vm.refresh() }

    // DELETE CONFIRMATION: Prevents accidental removal of contributions.
    if (deletingReport != null) {
        ConfirmDeleteDialog(
            text = stringResource(R.string.delete_report_confirmation),
            onConfirm = {
                val r = deletingReport!!
                deletingReport = null
                vm.deleteReport(r.id)
            },
            onDismiss = { deletingReport = null }
        )
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Text(
                text = stringResource(R.string.my_reports),
                color = colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // LOADING & EMPTY STATES
            if (ui.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.loading), color = colorScheme.onSurfaceVariant)
                }
            } else if (ui.profile?.reports?.isEmpty() != false) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_reports_yet), color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(ui.profile!!.reports, key = { it.id }) { report ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                // DEEP LINK: Navigates to CompanyPage and auto-scrolls to this report.
                                .clickable { onCompanyClick(report.companyId, report.id) }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.companyName,
                                    color = colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                // VOTE FEEDBACK: Colors the score green (positive) or red (negative).
                                val voteColor = when {
                                    report.voteSum > 0 -> Color(0xFF43A047)
                                    report.voteSum < 0 -> Color(0xFFE53935)
                                    else -> colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = if (report.voteSum > 0) "+${report.voteSum}" else report.voteSum.toString(),
                                    color = voteColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = report.text,
                                color = colorScheme.onSurface,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // ACTIONS: Reuses chips from the main Company view for consistency.
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EditChip(onClick = { onEdit(report) })
                                DeleteChip(onClick = { deletingReport = report })
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * MY CHALLENGES SCREEN: Similar to reports, but tracks nested counter-claims.
 */
@Composable
fun MyChallengesScreen(
    onBack: () -> Unit,
    onCompanyClick: (Int, Int) -> Unit,
    onEdit: (io.erthiscan.api.UserChallengeItem) -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val ui by vm.ui.collectAsStateWithLifecycle()
    var deletingChallenge by remember { mutableStateOf<io.erthiscan.api.UserChallengeItem?>(null) }

    LaunchedEffect(Unit) { vm.refresh() }

    if (deletingChallenge != null) {
        ConfirmDeleteDialog(
            text = stringResource(R.string.delete_challenge_confirmation),
            onConfirm = {
                val c = deletingChallenge!!
                deletingChallenge = null
                vm.deleteReport(c.id)
            },
            onDismiss = { deletingChallenge = null }
        )
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Text(
                text = stringResource(R.string.my_challenges),
                color = colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (ui.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.loading), color = colorScheme.onSurfaceVariant)
                }
            } else if (ui.profile?.challenges?.isEmpty() != false) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_challenges_yet), color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(ui.profile!!.challenges, key = { it.id }) { challenge ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .clickable { onCompanyClick(challenge.companyId, challenge.id) }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = challenge.companyName,
                                    color = colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val voteColor = when {
                                    challenge.voteSum > 0 -> Color(0xFF43A047)
                                    challenge.voteSum < 0 -> Color(0xFFE53935)
                                    else -> colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = if (challenge.voteSum > 0) "+${challenge.voteSum}" else challenge.voteSum.toString(),
                                    color = voteColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = challenge.text,
                                color = colorScheme.onSurface,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EditChip(onClick = { onEdit(challenge) })
                                DeleteChip(onClick = { deletingChallenge = challenge })
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * SIGN IN SCREEN: Powered by Android's modern [CredentialManager].
 * It facilitates a seamless Google One Tap login experience.
 */
@Composable
private fun SignInScreen(onGoogleIdToken: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.sign_in_subtitle),
            color = colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // AUTH TRIGGER: Launches the system BottomSheet for Google Account selection.
        Button(
            onClick = {
                scope.launch {
                    try {
                        val currentActivity = activity ?: return@launch
                        val credentialManager = CredentialManager.create(context)

                        // CONFIG: Uses the server's Client ID to verify the token backend-side.
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false) // Allows selecting any account.
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        // EXECUTION: Suspends until the user selects an account or cancels.
                        val result = credentialManager.getCredential(currentActivity, request)
                        val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)

                        // CALLBACK: Passes the JWT idToken to the ViewModel for backend verification.
                        onGoogleIdToken(googleIdToken.idToken)
                    } catch (e: NoCredentialException) {
                        Log.d("ErthiScan", "No account selected by user", e)
                    } catch (e: Exception) {
                        Log.e("ErthiScan", "Credential Manager failed", e)
                    }
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
                text = stringResource(R.string.sign_in_google),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}