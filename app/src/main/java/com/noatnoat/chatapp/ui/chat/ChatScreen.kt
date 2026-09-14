package com.noatnoat.chatapp.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.noatnoat.chatapp.core.ads.BannerAdView
import com.noatnoat.chatapp.core.database.entity.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var previewImageDialogUrl by remember { mutableStateOf<String?>(null) }

    // Map of messageId -> selected reaction emoji
    val messageReactions = remember { mutableStateMapOf<String, String>() }
    var activeReactionMessage by remember { mutableStateOf<MessageEntity?>(null) }

    var showCreatePollDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendMessage("📷 Attached Image: ${uri.lastPathSegment}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initial = uiState.peerUserId.takeLast(1).ifBlank { "U" }
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = uiState.peerUserId,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "End-to-End Encrypted",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            BannerAdView()

            // Pinned Message Header Banner
            uiState.pinnedMessage?.let { pinnedMsg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📌", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pinned Message",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = pinnedMsg.decryptedText ?: pinnedMsg.ciphertext,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.unpinMessage() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Unpin Message",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Chat Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { msg ->
                    MessageItemBubble(
                        message = msg,
                        reactionEmoji = messageReactions[msg.messageId],
                        onImageClick = { url -> previewImageDialogUrl = url },
                        onLongClick = { activeReactionMessage = msg },
                        onVoteOption = { optIdx -> viewModel.votePoll(msg.messageId, optIdx) }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showCreatePollDialog = true }
                    ) {
                        Text("📊", fontSize = 18.sp)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = !uiState.isSending && inputText.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Create Poll Dialog
    if (showCreatePollDialog) {
        var pollQuestion by remember { mutableStateOf("") }
        var option1 by remember { mutableStateOf("") }
        var option2 by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreatePollDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create Group Poll 📊",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = option1,
                        onValueChange = { option1 = it },
                        label = { Text("Option 1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = option2,
                        onValueChange = { option2 = it },
                        label = { Text("Option 2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCreatePollDialog = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (pollQuestion.isNotBlank() && option1.isNotBlank() && option2.isNotBlank()) {
                                        viewModel.sendPoll(pollQuestion, listOf(option1, option2))
                                        showCreatePollDialog = false
                                    }
                                }
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Create", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Emoji Reaction & Message Actions Picker Dialog
    activeReactionMessage?.let { msg ->
        val isPinned = uiState.pinnedMessage?.messageId == msg.messageId
        Dialog(onDismissRequest = { activeReactionMessage = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "React to Message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("❤️", "👍", "😂", "😮", "😢", "🙏").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        if (messageReactions[msg.messageId] == emoji) {
                                            messageReactions.remove(msg.messageId)
                                        } else {
                                            messageReactions[msg.messageId] = emoji
                                        }
                                        activeReactionMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pin / Unpin Action Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.pinMessage(msg)
                                activeReactionMessage = null
                            },
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("📌", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPinned) "Unpin Message" else "Pin Message to Top",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Image Preview Dialog
    previewImageDialogUrl?.let { imageText ->
        Dialog(onDismissRequest = { previewImageDialogUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Encrypted Image Attachment", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { previewImageDialogUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📷",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = imageText,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemBubble(
    message: MessageEntity,
    reactionEmoji: String? = null,
    onImageClick: (String) -> Unit = {},
    onLongClick: () -> Unit = {},
    onVoteOption: (Int) -> Unit = {}
) {
    val isOutbound = message.isOutbound
    val alignment = if (isOutbound) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleColor = if (isOutbound) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isOutbound) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val textContent = message.decryptedText ?: message.ciphertext
    val isImage = textContent.startsWith("📷") || textContent.contains("Attached Image")
    val isPoll = textContent.startsWith("📊 POLL:")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (isOutbound) Alignment.End else Alignment.Start) {
            Card(
                shape = if (isOutbound) {
                    RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                } else {
                    RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                },
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .combinedClickable(
                        onClick = {
                            if (isImage) onImageClick(textContent)
                        },
                        onLongClick = onLongClick
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text("📷", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Encrypted Media Attachment",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    if (isPoll) {
                        val pollPayload = textContent.substringAfter("📊 POLL: ")
                        val parts = pollPayload.split(" | ")
                        val question = parts.firstOrNull() ?: "Poll"
                        val options = parts.drop(1)

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📊", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = question,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            options.forEachIndexed { index, optStr ->
                                val optName = optStr.substringBeforeLast(" (")
                                val voteCount = optStr.substringAfterLast("(").substringBefore(")").toIntOrNull() ?: 0
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onVoteOption(index) },
                                    color = textColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(optName, fontSize = 14.sp, color = textColor, fontWeight = FontWeight.Medium)
                                        Text("$voteCount votes", fontSize = 12.sp, color = textColor.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = textContent,
                            fontSize = 15.sp,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = timeFormat.format(Date(message.timestamp)),
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Display reaction badge at corner if present
            reactionEmoji?.let { emoji ->
                Box(
                    modifier = Modifier
                        .padding(top = (-6).dp, end = if (isOutbound) 8.dp else 0.dp, start = if (isOutbound) 0.dp else 8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 13.sp)
                }
            }
        }
    }
}
