package com.noatnoat.chatapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhoneOtpScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (userId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var phoneNumber by remember { mutableStateOf("+84901234567") }
    var otpCode by remember { mutableStateOf("123456") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
                text = "Bảo mật mã hóa đầu-cuối chuẩn Signal",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = uiState) {
                is AuthUiState.Idle, is AuthUiState.Error -> {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Số điện thoại") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.requestOtp(phoneNumber) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gửi mã xác thực OTP")
                    }

                    if (state is AuthUiState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }

                is AuthUiState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đang kết nối backend...")
                }

                is AuthUiState.OtpSent -> {
                    Text(
                        text = "Mã OTP đã gửi tới ${state.phoneNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("Nhập mã OTP (Mặc định: 123456)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.verifyOtp(state.phoneNumber, otpCode) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Xác thực & Tạo khóa E2EE")
                    }

                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Đổi số điện thoại")
                    }
                }

                is AuthUiState.Authenticated -> {
                    Text(
                        text = "Đã đăng nhập thành công!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "User ID: ${state.userId}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "🔒 Khóa E2EE Signal đã được tạo & đồng bộ backend",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onAuthSuccess(state.userId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Vào ứng dụng Chat")
                    }

                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Đăng xuất")
                    }
                }
            }
        }
    }
}
