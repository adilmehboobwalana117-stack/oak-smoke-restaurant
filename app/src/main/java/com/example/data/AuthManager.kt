package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthManager {
    private const val TAG = "Auth"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private suspend fun signInWithGoogleCredential(cred: Credential): Boolean {
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
            Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            return true
        }
        return false
    }

    private fun buildFriendlyAuthError(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            e is NoCredentialException || raw.contains("NoCredential", ignoreCase = true) || raw.contains("28436") || raw.contains("16:") ->
                "No signed-in Google email account was returned. Add a Google account in Android Settings (or if installed from Google Play Console, add your Play App Signing SHA-1 fingerprint in Firebase Console) and tap Retry."
            raw.contains("network", ignoreCase = true) || raw.contains("timeout", ignoreCase = true) ->
                "Network connection required to sign in. Check internet access and try again."
            raw.contains("DEVELOPER_ERROR", ignoreCase = true) || raw.contains("10:") ->
                "Google Sign-In SHA-1 fingerprint mismatch. Add your Google Play App Signing SHA-1 in Firebase Console -> Project Settings."
            else -> e.localizedMessage ?: "Sign in failed. Please try again."
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val hostContext = context.findActivity() ?: context
                val cred = credentialManager.getCredential(hostContext, request).credential
                if (signInWithGoogleCredential(cred)) {
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    /**
     * Interactive 1-Tap Google Email Sign-In (no verification code needed on devices with a Google account).
     * Uses one credential option per GetCredentialRequest (Android API 36 safety):
     * 1) Primary: GetSignInWithGoogleOption (full interactive Google email chooser)
     * 2) Fallback on NoCredentialException/GetCredentialException: separate GetGoogleIdOption
     *    request with setFilterByAuthorizedAccounts(false) so any Google email on the phone can sign in directly.
     */
    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val activityContext = context.findActivity() ?: run {
            onAuthError("Unable to launch sign-in from this screen. Please reopen the app and try again.")
            return
        }

        val primaryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            )
            .build()

        scope.launch {
            try {
                val primaryCred = try {
                    credentialManager.getCredential(activityContext, primaryRequest).credential
                } catch (cancelled: GetCredentialCancellationException) {
                    throw cancelled
                } catch (primaryErr: GetCredentialException) {
                    Log.w(TAG, "Primary Google Sign-In sheet failed (${primaryErr.javaClass.simpleName}: ${primaryErr.message}); retrying with 1-Tap email picker", primaryErr)
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(
                            GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(clientId)
                                .setAutoSelectEnabled(false)
                                .build()
                        )
                        .build()
                    credentialManager.getCredential(activityContext, fallbackRequest).credential
                }

                if (signInWithGoogleCredential(primaryCred)) {
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned. Please select a Google email account.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(buildFriendlyAuthError(e))
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
