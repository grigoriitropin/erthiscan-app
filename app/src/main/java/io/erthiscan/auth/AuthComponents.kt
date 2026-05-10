package io.erthiscan.auth

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.erthiscan.BuildConfig
import io.erthiscan.R
import kotlinx.coroutines.launch

/**
 * GOOGLE SIGN IN BUTTON: A standard button that triggers the Credential Manager flow.
 * 
 * DESIGN PATTERN: 
 * This is a stateless component that delegates the result back to the caller. 
 * It encapsulates the complexity of [CredentialManager] and [BuildConfig] lookups.
 */
@Composable
fun GoogleSignInButton(
    onGoogleIdToken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

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

                    onGoogleIdToken(googleIdToken.idToken)
                } catch (e: NoCredentialException) {
                    Log.d("ErthiScan", "No account selected")
                } catch (e: Exception) {
                    Log.e("ErthiScan", "Auth failed", e)
                }
            }
        },
        modifier = modifier,
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

/**
 * SIGN IN SHEET: A bottom sheet or dialog that encourages guest users to log in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInSheet(
    onDismiss: () -> Unit,
    onGoogleIdToken: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sign_in_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(32.dp))
            GoogleSignInButton(
                onGoogleIdToken = onGoogleIdToken,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
