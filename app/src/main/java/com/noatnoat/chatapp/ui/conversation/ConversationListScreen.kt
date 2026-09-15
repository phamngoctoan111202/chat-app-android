package com.noatnoat.chatapp.ui.conversation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noatnoat.chatapp.core.ads.BannerAdView
import com.noatnoat.chatapp.core.database.entity.ConversationEntity
import com.noatnoat.chatapp.core.network.websocket.WsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationViewModel,
    onConversationClick: (peerUserId: String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }
    var newPhoneInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Chats",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val connText = when (uiState.connectionState) {
                            is WsState.Connected -> "🟢 Online"
                            is WsState.Connecting -> "🟡 Connecting..."
                            else -> "🔴 Offline"
                        }
                        Text(
                            text = connText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    // Signal Style Profile Avatar Button (Opens Settings)
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        val initial = uiState.currentUserId.takeLast(1).ifBlank { "U" }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            BannerAdView()

            if (uiState.conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active conversations.\nTap + to start a new chat!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.conversations) { conv ->
                        ConversationItemRow(
                            conversation = conv,
                            onClick = { onConversationClick(conv.peerUserId) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Chat", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPhoneInput,
                    onValueChange = { newPhoneInput = it },
                    label = { Text("Phone Number (+84...)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhoneInput.isNotBlank()) {
                            viewModel.startNewConversation(newPhoneInput)
                            val peerId = "user_" + newPhoneInput.takeLast(6)
                            showNewChatDialog = false
                            newPhoneInput = ""
                            onConversationClick(peerId)
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Start Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ConversationItemRow(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val initial = conversation.peerPhoneNumber.takeLast(1).ifBlank { "U" }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.peerPhoneNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = timeFormat.format(Date(conversation.lastTimestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessageText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
