package com.nyaai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.nyaai.ui.state.LocalAuthUpdater
import java.util.concurrent.TimeUnit

internal fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onBack: () -> Unit, onLoginSuccess: () -> Unit) {
    val updateAuth = LocalAuthUpdater.current
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var phoneNumber by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            val prefs = context.getSharedPreferences("nyaai_preferences", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("guest_mode", false).apply()
                            updateAuth(true)
                            onLoginSuccess()
                        } else {
                            val err = authResult.exception?.message ?: "Authentication failed"
                            Toast.makeText(context, "Firebase Auth Failed: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Google Sign-In: idToken is missing", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                val hint = when (e.statusCode) {
                    10 -> "\nHint: Release SHA-1 fingerprint must be added to Firebase Console."
                    12500 -> "\nHint: Enable Google provider in Firebase Console > Authentication > Sign-in method."
                    else -> ""
                }
                Toast.makeText(context, "Google Sign-In Failed [${e.statusCode}]: ${e.message}$hint", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            if (result.data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    task.getResult(ApiException::class.java)
                } catch (e: ApiException) {
                    val hint = when (e.statusCode) {
                        10 -> " (Developer Error: Release SHA-1 must be registered in Firebase)"
                        12500 -> " (Google sign-in provider disabled in Firebase)"
                        else -> ""
                    }
                    Toast.makeText(context, "Google Sign-In [${e.statusCode}]$hint", Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult
                }
            }
            Toast.makeText(context, "Google Sign-In canceled", Toast.LENGTH_SHORT).show()
        }
    }

    fun startGoogleSignIn() {
        val defaultWebClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "487753685540-eej3nmg7n7e6r02dsfkj0i5otucpeb7u.apps.googleusercontent.com"
        } catch (_: Exception) {
            "487753685540-eej3nmg7n7e6r02dsfkj0i5otucpeb7u.apps.googleusercontent.com"
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(defaultWebClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    fun sendOtp() {
        if (phoneNumber.length != 10) return
        val hostActivity = context.findActivity()
        if (hostActivity == null) {
            Toast.makeText(context, "Unable to find host activity for phone verification", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(hostActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateAuth(true)
                            onLoginSuccess()
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isLoading = false
                    val msg = e.message ?: ""
                    val customMsg = if (msg.contains("not allowed", ignoreCase = true) || msg.contains("operation-not-allowed", ignoreCase = true)) {
                        "Phone Auth is disabled in Firebase Console. Enable Phone in Authentication > Sign-in method, or tap 'Continue as Guest' below!"
                    } else {
                        "Verification Failed: $msg"
                    }
                    Toast.makeText(context, customMsg, Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    isLoading = false
                    verificationId = id
                    otpSent = true
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        if (otpCode.length != 6) return
        isLoading = true
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            isLoading = false
            if (task.isSuccessful) {
                updateAuth(true)
                onLoginSuccess()
            } else {
                Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(text = "Nyaai", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = colors.primary)
            Text(text = "Your Legal Companion", fontSize = 16.sp, color = colors.onSurfaceVariant)

            Spacer(modifier = Modifier.height(60.dp))

            if (!otpSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 10) phoneNumber = it },
                    label = { Text("Phone Number") },
                    prefix = { Text("+91 ") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { sendOtp() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = phoneNumber.length == 10 && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Get OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = "OTP sent to +91 $phoneNumber", color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    label = { Text("Enter 6-digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { verifyOtp() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = otpCode.length == 6 && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Verify & Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = { otpSent = false }, enabled = !isLoading) {
                    Text("Change Phone Number", color = colors.primary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f))
                Text(" OR ", color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { startGoogleSignIn() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.outline),
                enabled = !isLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFEA4335)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", color = colors.onSurface, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = {
                    val prefs = context.getSharedPreferences("nyaai_preferences", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("guest_mode", true).apply()
                    updateAuth(true)
                    onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    text = "Continue as Guest / Explore App →",
                    color = colors.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
