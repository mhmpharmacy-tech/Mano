package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.ui.theme.DarkSage
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "ManoAuth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  trySend(currentUser)
  awaitClose { removeAuthStateListener(listener) }
}

private fun resolveServerClientId(context: Context): String? {
  val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  if (resId == 0) return null
  return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
}

suspend fun attemptAutoSignIn(context: Context, auth: FirebaseAuth): Result<FirebaseUser> {
  val existing = auth.currentUser
  if (existing != null) return Result.success(existing)
  return signInWithGoogleCredentialManager(context, auth)
}

suspend fun signInWithGoogleCredentialManager(context: Context, auth: FirebaseAuth): Result<FirebaseUser> {
  val serverClientId =
    resolveServerClientId(context)
      ?: return Result.failure(IllegalStateException("شناسه اتصال گوگل (default_web_client_id) در پیکربندی برنامه تنظیم نشده است."))

  return try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
    val credentialManager = CredentialManager.create(context)
    val googleOption =
      GetSignInWithGoogleOption.Builder(serverClientId)
        .build()
    val request =
      GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()
    val result = credentialManager.getCredential(context, request)
    val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
    val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
    val authResult = auth.signInWithCredential(firebaseCred).await()
    val signedInUser = authResult.user
    if (signedInUser != null) {
      Result.success(signedInUser)
    } else {
      Result.failure(IllegalStateException("ورود با گوگل تکمیل نشد."))
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: GetCredentialCancellationException) {
    Result.failure(IllegalStateException("ورود با حساب گوگل توسط کاربر لغو شد.", e))
  } catch (e: NoCredentialException) {
    Result.failure(IllegalStateException("حساب گوگل فعالی برای ورود روی این دستگاه یافت نشد.", e))
  } catch (e: Exception) {
    Log.e(TAG, "Google Credential Manager sign-in failed", e)
    Result.failure(e)
  }
}

@Composable
fun CloudSyncAuthCard(
  currentUser: FirebaseUser?,
  titleFa: String,
  descriptionFa: String,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  onSignedOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isBusy by remember { mutableStateOf(false) }
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)

  Surface(
    color =
      if (currentUser != null) manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.55f, darkAlpha = 0.16f)
      else manoCardSurface(),
    shape = RoundedCornerShape(16.dp),
    border =
      BorderStroke(
        1.dp,
        if (currentUser != null) sageAccent.copy(alpha = 0.45f) else manoBorderColor(),
      ),
    modifier = modifier.fillMaxWidth().testTag("cloud_sync_auth_card"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = if (currentUser != null) Icons.Rounded.CloudDone else Icons.Rounded.AccountCircle,
          contentDescription = null,
          tint = if (currentUser != null) sageAccent else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = titleFa,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = if (currentUser != null) sageAccent else MaterialTheme.colorScheme.primary,
        )
      }

      Text(
        text = descriptionFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (currentUser != null) {
        val label = currentUser.email ?: currentUser.displayName ?: currentUser.uid.take(10)
        Text(
          text = "حساب متصل: $label",
          style = MaterialTheme.typography.labelMedium,
          color = sageAccent,
        )
        OutlinedButton(
          enabled = !isBusy,
          onClick = {
            isBusy = true
            scope.launch {
              runCatching {
                Firebase.auth.signOut()
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
              }
              isBusy = false
              onSignedOut()
            }
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("cloud_sign_out_btn"),
        ) {
          Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("خروج از حساب گوگل")
        }
      } else {
        Button(
          enabled = !isBusy,
          onClick = {
            isBusy = true
            scope.launch {
              val res =
                runCatching {
                  if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                  }
                  signInWithGoogleCredentialManager(context, Firebase.auth)
                }.getOrElse { Result.failure(it) }
              isBusy = false
              res.fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { err ->
                  onAuthError(err.localizedMessage ?: "ورود با حساب گوگل انجام نشد.")
                },
              )
            }
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("cloud_google_signin_btn"),
        ) {
          Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(if (isBusy) "در حال اتصال به گوگل..." else "ورود با حساب گوگل (Google Sign-In)")
        }
      }
    }
  }
}
