package com.noatnoat.chatapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UnifiedAuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (userId: String) -> Unit = {},
    onNavigateToDebugLogs: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Phone, 1: Email

    // Form states
    var phoneNumber by remember { mutableStateOf("+84901234567") }
    var otpCode by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ChatApp E2EE",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Signal Protocol End-to-End Encryption",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main State Machine
            when (val state = uiState) {
                is AuthUiState.Authenticated -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Authentication Successful!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "User ID: ${state.userId}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "🔒 Signal E2EE keys generated & synced",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onAuthSuccess(state.userId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enter Chat Application")
                        }

                        TextButton(onClick = { viewModel.logout() }) {
                            Text("Sign Out")
                        }
                    }
                }

                is AuthUiState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Authenticating & Generating E2EE Keys...")
                }

                else -> {
                    // Tab Selector
                    TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("📱 Phone OTP") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("✉️ Email") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedTab == 0) {
                        // TAB 0: PHONE OTP
                        if (state is AuthUiState.OtpSent) {
                            Text(
                                text = "OTP code sent to ${state.phoneNumber}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text("Enter 6-digit SMS OTP Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.verifyOtp(state.phoneNumber, otpCode) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Verify & Login")
                            }

                            TextButton(onClick = { viewModel.logout() }) {
                                Text("Change Phone Number")
                            }
                        } else {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number (+84...)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.requestOtp(phoneNumber) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send OTP Verification Code")
                            }
                        }
                    } else {
                        // TAB 1: EMAIL & PASSWORD (WITH OTP VERIFICATION)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text("6-digit Email OTP Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.sendEmailOtp(email) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send Email OTP Verification Code")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isRegisterMode) {
                                    viewModel.registerWithEmail(email, password, otpCode)
                                } else {
                                    viewModel.loginWithEmail(email, password)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isRegisterMode) "Verify OTP & Register" else "Login with Email")
                        }

                        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                            Text(
                                if (isRegisterMode) "Already verified? Login" else "Don't have an account? Register with Email OTP"
                            )
                        }
                    }

                    // Error Message Display
                    if (state is AuthUiState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR  ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GOOGLE SIGN-IN BUTTON
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithFirebaseToken(
                                idToken = "google_id_token_credential",
                                phoneNumber = "+84901234567"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🌐 Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // PASSKEYS BIOMETRIC BUTTON
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithFirebaseToken(
                                idToken = "passkey_biometric_credential",
                                phoneNumber = "passkey_user"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔑 Sign in with Passkeys / Biometrics", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // DEBUG LOG DIAGNOSTICS LINK
                    TextButton(onClick = onNavigateToDebugLogs) {
                        Text("🛠️ View System Diagnostics & Debug Logs", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
