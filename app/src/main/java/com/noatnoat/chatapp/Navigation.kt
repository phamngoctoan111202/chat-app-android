package com.noatnoat.chatapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noatnoat.chatapp.data.SecureSessionManager
import com.noatnoat.chatapp.ui.auth.AuthViewModel
import com.noatnoat.chatapp.ui.auth.PhoneOtpScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { SecureSessionManager(context) }
    val authViewModel = remember { AuthViewModel(sessionManager = sessionManager) }

    PhoneOtpScreen(
        viewModel = authViewModel,
        onAuthSuccess = { userId ->
            // Successfully logged in and keys uploaded to backend
        }
    )
}
