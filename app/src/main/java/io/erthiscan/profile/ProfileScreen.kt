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

    LaunchedEffect(Unit) {
        if (ui.auth.isLoggedIn) vm.refresh()
    }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

        Text(
            text = ui.profile?.username ?: ui.auth.username ?: "",
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (ui.profile != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pluralStringResource(R.plurals.reports_and_challenges, ui.profile!!.reportCount, ui.profile!!.reportCount, ui.profile!!.challengeCount),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

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

            if (ui.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.loading), color = colorScheme.onSurfaceVariant)
                }
            } else if (ui.profile?.reports?.isEmpty() != false) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_reports_yet), color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(ui.profile!!.reports, key = { it.id }) { report ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surfaceContainerHigh)
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.loading), color = colorScheme.onSurfaceVariant)
                }
            } else if (ui.profile?.challenges?.isEmpty() != false) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_challenges_yet), color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
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

@Composable
private fun SignInScreen(onGoogleIdToken: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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

        Button(
            onClick = {
                scope.launch {
                    try {
                        val currentActivity = activity ?: return@launch
                        val credentialManager = CredentialManager.create(context)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        val result = credentialManager.getCredential(currentActivity, request)
                        val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
                        val idToken = googleIdToken.idToken
                        onGoogleIdToken(idToken)
                    } catch (e: NoCredentialException) {
                        Log.d("ErthiScan", "No credential found", e)
                    } catch (e: Exception) {
                        Log.e("ErthiScan", "Sign in failed", e)
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