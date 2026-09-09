package com.noatnoat.chatapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.noatnoat.chatapp.data.SecureSessionManager
import com.noatnoat.chatapp.ui.auth.AuthUiState
import com.noatnoat.chatapp.ui.auth.AuthViewModel
import com.noatnoat.chatapp.ui.auth.PhoneOtpScreen
import com.noatnoat.chatapp.ui.chat.ChatScreen
import com.noatnoat.chatapp.ui.chat.ChatViewModel
import com.noatnoat.chatapp.ui.conversation.ConversationListScreen
import com.noatnoat.chatapp.ui.conversation.ConversationViewModel
import com.noatnoat.chatapp.ui.debug.DebugLogScreen
import com.noatnoat.chatapp.ui.debug.DebugLogViewModel

sealed interface Screen {
    object Auth : Screen
    object ConversationList : Screen
    data class Chat(val peerUserId: String) : Screen
    object DebugLog : Screen
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { SecureSessionManager(context) }
    val authViewModel = remember { AuthViewModel(sessionManager = sessionManager) }
    val conversationViewModel = remember { ConversationViewModel(sessionManager = sessionManager) }
    val chatViewModel = remember { ChatViewModel(sessionManager = sessionManager) }
    val debugLogViewModel = remember { DebugLogViewModel(sessionManager = sessionManager) }

    val authState by authViewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.ConversationList) }

    when (authState) {
        is AuthUiState.Authenticated -> {
            when (currentScreen) {
                is Screen.ConversationList -> {
                    ConversationListScreen(
                        viewModel = conversationViewModel,
                        onConversationClick = { peerUserId ->
                            currentScreen = Screen.Chat(peerUserId)
                        },
                        onLogoutClick = {
                            authViewModel.logout()
                        }
                    )
                }
                is Screen.Chat -> {
                    ChatScreen(
                        viewModel = chatViewModel,
                        onLogoutClick = {
                            currentScreen = Screen.ConversationList
                        }
                    )
                }
                is Screen.DebugLog -> {
                    DebugLogScreen(
                        viewModel = debugLogViewModel,
                        onBackClick = {
                            currentScreen = Screen.ConversationList
                        }
                    )
                }
                else -> {
                    ConversationListScreen(
                        viewModel = conversationViewModel,
                        onConversationClick = { peerUserId ->
                            currentScreen = Screen.Chat(peerUserId)
                        },
                        onLogoutClick = {
                            authViewModel.logout()
                        }
                    )
                }
            }
        }
        else -> {
            PhoneOtpScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    currentScreen = Screen.ConversationList
                }
            )
        }
    }
}
