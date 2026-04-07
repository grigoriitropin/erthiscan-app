package io.erthiscan.profile

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.erthiscan.BuildConfig
import io.erthiscan.api.ApiClient
import io.erthiscan.api.GoogleAuthRequest
import io.erthiscan.api.UserProfile
import io.erthiscan.auth.AuthManager
import io.erthiscan.company.page.ConfirmDeleteDialog
import io.erthiscan.company.page.CreateReportScreen
import io.erthiscan.company.page.DeleteChip
import io.erthiscan.company.page.EditChip
import kotlinx.coroutines.launch

enum class ProfileSubScreen { ROOT, REPORTS, CHALLENGES }

@Composable
fun ProfileScreen(
    subScreen: ProfileSubScreen = ProfileSubScreen.ROOT,
    onSubScreenChange: (ProfileSubScreen) -> Unit = {},
    onFullscreenChange: (Boolean) -> Unit = {},
    onCompanyClick: (Int, Int) -> Unit = { _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        if (AuthManager.isLoggedIn) {
            when (subScreen) {
                ProfileSubScreen.REPORTS -> MyReportsScreen(
                    onBack = { onSubScreenChange(ProfileSubScreen.ROOT) },
                    onFullscreenChange = onFullscreenChange,
                    onCompanyClick = onCompanyClick
                )
                ProfileSubScreen.CHALLENGES -> MyChallengesScreen(
                    onBack = { onSubScreenChange(ProfileSubScreen.ROOT) },
                    onFullscreenChange = onFullscreenChange,
                    onCompanyClick = onCompanyClick
                )
                ProfileSubScreen.ROOT -> LoggedInProfile(
                    onShowReports = { onSubScreenChange(ProfileSubScreen.REPORTS) },
                    onShowChallenges = { onSubScreenChange(ProfileSubScreen.CHALLENGES) }
                )
            }
        } else {
            SignInScreen()
        }
    }
}

@Composable
private fun LoggedInProfile(onShowReports: () -> Unit, onShowChallenges: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        try {
            profile = ApiClient.api.getMyProfile()
        } catch (e: Exception) {
            Log.e("ErthiScan", "Failed to load profile", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = profile?.username ?: AuthManager.username ?: "",
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (profile != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${profile!!.reportCount} reports · ${profile!!.challengeCount} challenges",
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
                text = "My Reports",
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
                text = "My Challenges",
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { scope.launch { AuthManager.logout(activity) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.errorContainer,
                contentColor = colorScheme.onErrorContainer
            )
        ) {
            Text(
                text = "Sign Out",
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1.5f))
    }
}

@Composable
private fun MyReportsScreen(
    onBack: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onCompanyClick: (Int, Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var editingReport by remember { mutableStateOf<io.erthiscan.api.UserReportItem?>(null) }
    var deletingReport by remember { mutableStateOf<io.erthiscan.api.UserReportItem?>(null) }

    LaunchedEffect(editingReport) {
        onFullscreenChange(editingReport != null)
    }
    DisposableEffect(Unit) {
        onDispose { onFullscreenChange(false) }
    }

    LaunchedEffect(refreshKey) {
        try {
            profile = ApiClient.api.getMyProfile()
        } catch (e: Exception) {
            Log.e("ErthiScan", "Failed to load reports", e)
        }
    }

    if (editingReport != null) {
        val r = editingReport!!
        CreateReportScreen(
            companyId = r.companyId,
            companyName = r.companyName,
            editReportId = r.id,
            initialText = r.text,
            initialSource = r.sources.firstOrNull() ?: "",
            onBack = { editingReport = null },
            onSubmitted = {
                editingReport = null
                refreshKey++
            }
        )
        return
    }

    if (deletingReport != null) {
        ConfirmDeleteDialog(
            text = "Delete this report? Its challenges will also be removed.",
            onConfirm = {
                val r = deletingReport!!
                deletingReport = null
                scope.launch {
                    try {
                        ApiClient.api.deleteReport(r.id)
                        refreshKey++
                    } catch (e: Exception) {
                        Log.e("ErthiScan", "Failed to delete report", e)
                    }
                }
            },
            onDismiss = { deletingReport = null }
        )
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
    ) {
        Text(
            text = "My Reports",
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (profile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = colorScheme.onSurfaceVariant)
            }
        } else if (profile!!.reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No reports yet", color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profile!!.reports, key = { it.id }) { report ->
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
                            EditChip(onClick = { editingReport = report })
                            DeleteChip(onClick = { deletingReport = report })
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MyChallengesScreen(
    onBack: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onCompanyClick: (Int, Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var editingChallenge by remember { mutableStateOf<io.erthiscan.api.UserChallengeItem?>(null) }
    var deletingChallenge by remember { mutableStateOf<io.erthiscan.api.UserChallengeItem?>(null) }

    LaunchedEffect(editingChallenge) {
        onFullscreenChange(editingChallenge != null)
    }
    DisposableEffect(Unit) {
        onDispose { onFullscreenChange(false) }
    }

    LaunchedEffect(refreshKey) {
        try {
            profile = ApiClient.api.getMyProfile()
        } catch (e: Exception) {
            Log.e("ErthiScan", "Failed to load challenges", e)
        }
    }

    if (editingChallenge != null) {
        val c = editingChallenge!!
        CreateReportScreen(
            companyId = c.companyId,
            companyName = c.companyName,
            editReportId = c.id,
            initialText = c.text,
            initialSource = c.sources.firstOrNull() ?: "",
            onBack = { editingChallenge = null },
            onSubmitted = {
                editingChallenge = null
                refreshKey++
            }
        )
        return
    }

    if (deletingChallenge != null) {
        ConfirmDeleteDialog(
            text = "Delete this challenge?",
            onConfirm = {
                val c = deletingChallenge!!
                deletingChallenge = null
                scope.launch {
                    try {
                        ApiClient.api.deleteReport(c.id)
                        refreshKey++
                    } catch (e: Exception) {
                        Log.e("ErthiScan", "Failed to delete challenge", e)
                    }
                }
            },
            onDismiss = { deletingChallenge = null }
        )
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
    ) {
        Text(
            text = "My Challenges",
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (profile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = colorScheme.onSurfaceVariant)
            }
        } else if (profile!!.challenges.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No challenges yet", color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profile!!.challenges, key = { it.id }) { challenge ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.surfaceContainerHigh)
                            .clickable { onCompanyClick(challenge.companyId, challenge.parentId) }
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
                            EditChip(onClick = { editingChallenge = challenge })
                            DeleteChip(onClick = { deletingChallenge = challenge })
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SignInScreen() {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ErthiScan",
            color = colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in to create reports and vote",
            color = colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        val credentialManager = CredentialManager.create(activity)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        val result = credentialManager.getCredential(activity, request)
                        val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
                        val idToken = googleIdToken.idToken

                        val response = ApiClient.api.authGoogle(GoogleAuthRequest(token = idToken))
                        AuthManager.login(response.accessToken, response.refreshToken, response.userId, response.username, activity)
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
                text = "Sign in with Google",
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
