package com.noatnoat.chatapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.noatnoat.chatapp.data.SecureSessionManager
import com.noatnoat.chatapp.ui.auth.AuthUiState
import com.noatnoat.chatapp.ui.auth.AuthViewModel
import com.noatnoat.chatapp.ui.auth.PhoneOtpScreen
import com.noatnoat.chatapp.ui.chat.ChatScreen
import com.noatnoat.chatapp.ui.chat.ChatViewModel

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { SecureSessionManager(context) }
    val authViewModel = remember { AuthViewModel(sessionManager = sessionManager) }
    val chatViewModel = remember { ChatViewModel(sessionManager = sessionManager) }

    val authState by authViewModel.uiState.collectAsState()

    when (authState) {
        is AuthUiState.Authenticated -> {
            ChatScreen(
                viewModel = chatViewModel,
                onLogoutClick = { authViewModel.logout() }
            )
        }
        else -> {
            PhoneOtpScreen(
                viewModel = authViewModel
            )
        }
    }
}
