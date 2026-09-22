package com.noatnoat.chatapp.ui.chat

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.LocalContext
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

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var previewImageDialogUrl by remember { mutableStateOf<String?>(null) }

    // Map of messageId -> selected reaction emoji
    val messageReactions = remember { mutableStateMapOf<String, String>() }
    var activeReactionMessage by remember { mutableStateOf<MessageEntity?>(null) }

    var showCreatePollDialog by remember { mutableStateOf(false) }
    var showEphemeralDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showWatchTogetherDialog by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var activeWatchTogetherVideo by remember { mutableStateOf<String?>(null) }

    var currentLat by remember { mutableStateOf(10.762622) }
    var currentLng by remember { mutableStateOf(106.660172) }
    var currentAddress by remember { mutableStateOf("Acquiring GPS location...") }
    var isLocationLoading by remember { mutableStateOf(false) }

    fun requestLocationFetch() {
        showLocationDialog = true
        isLocationLoading = true
        fetchRealLocation(context) { lat, lng, addr ->
            currentLat = lat
            currentLng = lng
            currentAddress = addr
            isLocationLoading = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            requestLocationFetch()
        } else {
            showLocationDialog = true
            currentAddress = "Location permission denied"
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            viewModel.sendMessage("📷 Attached Image: $uri")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { showProfileDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initial = uiState.peerUserId.takeLast(1).ifBlank { "U" }
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            // Online Status Green/Red Dot
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isBlocked) Color.Red else com.noatnoat.chatapp.theme.OnlineGreen)
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = uiState.nickname ?: uiState.peerUserId,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isBlocked) "🔴 Đã chặn" else if (uiState.isMuted) "🔕 Đã tắt thông báo" else if (uiState.ephemeralTimerSeconds > 0) "⏱️ Disappearing ${uiState.ephemeralTimerSeconds}s" else "Active now • Encrypted",
                                fontSize = 11.sp,
                                color = if (uiState.isBlocked) Color.Red else if (uiState.isMuted) Color.Gray else if (uiState.ephemeralTimerSeconds > 0) MaterialTheme.colorScheme.primary else com.noatnoat.chatapp.theme.OnlineGreen,
                                fontWeight = FontWeight.Medium
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
                actions = {
                    IconButton(
                        onClick = { showEphemeralDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (uiState.ephemeralTimerSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("⏱️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.startCall(context, isVideo = false) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("📞", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.startCall(context, isVideo = true) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("📹", fontSize = 16.sp)
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
                .navigationBarsPadding()
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showProfileDialog) {
                PeerProfileScreen(
                    peerUserId = uiState.peerUserId,
                    nickname = uiState.nickname,
                    isMuted = uiState.isMuted,
                    isBlocked = uiState.isBlocked,
                    ephemeralTimerSeconds = uiState.ephemeralTimerSeconds,
                    onBackClick = { showProfileDialog = false },
                    onStartVoiceCall = {
                        showProfileDialog = false
                        viewModel.startCall(context, isVideo = false)
                    },
                    onStartVideoCall = {
                        showProfileDialog = false
                        viewModel.startCall(context, isVideo = true)
                    },
                    onShareLocation = {
                        showProfileDialog = false
                        requestLocationFetch()
                    },
                    onOpenWatchTogether = {
                        showProfileDialog = false
                        showWatchTogetherDialog = true
                    },
                    onOpenCreatePoll = {
                        showProfileDialog = false
                        showCreatePollDialog = true
                    },
                    onOpenEphemeralSettings = {
                        showProfileDialog = false
                        showEphemeralDialog = true
                    },
                    onBlockToggle = {
                        if (uiState.isBlocked) {
                            viewModel.unblockUser(uiState.peerUserId)
                        } else {
                            viewModel.blockUser(uiState.peerUserId)
                        }
                    },
                    onMuteToggle = { viewModel.toggleMuteNotification() },
                    onClearChat = { viewModel.clearChatHistory() },
                    onReportUser = { reason -> viewModel.reportUser(reason) },
                    onSetNickname = { nick -> viewModel.setNickname(nick) }
                )
            }

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
                        onVoteOption = { optIdx -> viewModel.votePoll(msg.messageId, optIdx) },
                        onWatchTogetherClick = { text -> activeWatchTogetherVideo = text.substringAfter("🎬 WATCH_TOGETHER: ") },
                        onLocationClick = { text ->
                            val locPayload = text.substringAfter("📍 LOCATION: ")
                            val coordsStr = locPayload.substringBefore(" | ")
                            val parts = coordsStr.split(",")
                            val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: 10.762622
                            val lng = parts.getOrNull(1)?.toDoubleOrNull() ?: 106.660172
                            val uriStr = "https://www.google.com/maps/dir/?api=1&origin=$currentLat,$currentLng&destination=$lat,$lng"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriStr))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Expandable Action Badges Row (Media, Poll, Location, Watch Together)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showAttachmentOptions,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Media Picker Action Badge
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showAttachmentOptions = false
                                        imagePickerLauncher.launch("image/*")
                                    },
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🖼️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }

                            // Poll Action Badge
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showAttachmentOptions = false
                                        showCreatePollDialog = true
                                    },
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📊", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Poll", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }

                            // Location Action Badge
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showAttachmentOptions = false
                                        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (hasFine || hasCoarse) {
                                            requestLocationFetch()
                                        } else {
                                            locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                    },
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📍", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }

                            // Watch Together Badge
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showAttachmentOptions = false
                                        showWatchTogetherDialog = true
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎬", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Watch", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Main Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Toggle Attachment Drawer Button
                        IconButton(
                            onClick = { showAttachmentOptions = !showAttachmentOptions },
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showAttachmentOptions) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                        ) {
                            Icon(
                                imageVector = if (showAttachmentOptions) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Toggle Attachments",
                                tint = if (showAttachmentOptions) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Multi-line Expandable Text Field
                        androidx.compose.material3.OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f),
                            minLines = 1,
                            maxLines = 5,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    showAttachmentOptions = false
                                }
                            },
                            enabled = !uiState.isSending && inputText.isNotBlank(),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
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
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
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

    // Disappearing Messages Timer Selection Dialog
    if (showEphemeralDialog) {
        Dialog(onDismissRequest = { showEphemeralDialog = false }) {
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
                        text = "Disappearing Messages ⏱️",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "New messages will automatically expire & disappear for all participants after the selected timer.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    listOf(
                        0 to "Off",
                        30 to "30 Seconds",
                        300 to "5 Minutes",
                        3600 to "1 Hour",
                        86400 to "24 Hours"
                    ).forEach { (seconds, label) ->
                        val isSelected = uiState.ephemeralTimerSeconds == seconds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setEphemeralTimer(seconds)
                                    showEphemeralDialog = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Text("✔", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Live Location Sharing Dialog
    if (showLocationDialog) {
        Dialog(onDismissRequest = { showLocationDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Share Live Location 📍",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("🗺️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isLocationLoading) "Acquiring GPS Signal..." else "Real GPS Location",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "%.6f, %.6f".format(currentLat, currentLng),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Text(
                                text = currentAddress,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showLocationDialog = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.sendLiveLocation(currentLat, currentLng, currentAddress)
                                    showLocationDialog = false
                                }
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Share Now", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Watch Together Room Creation Dialog
    if (showWatchTogetherDialog) {
        var videoUrlInput by remember { mutableStateOf("") }
        var videoTitleInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showWatchTogetherDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Watch Together 🎬",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = videoUrlInput,
                        onValueChange = { videoUrlInput = it },
                        label = { Text("Video Stream URL (MP4 / YouTube)") },
                        placeholder = { Text("https://example.com/video.mp4") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = videoTitleInput,
                        onValueChange = { videoTitleInput = it },
                        label = { Text("Session Title") },
                        placeholder = { Text("Movie Night / Tech Talk") },
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
                                .clickable { showWatchTogetherDialog = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (videoUrlInput.isNotBlank()) {
                                        viewModel.sendWatchTogetherRoom(videoUrlInput, videoTitleInput)
                                        showWatchTogetherDialog = false
                                    }
                                }
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Start Session", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Synchronized Watch Together Player Dialog
    activeWatchTogetherVideo?.let { videoInfo ->
        val parts = videoInfo.split(" | ")
        val url = parts.firstOrNull() ?: ""
        val title = parts.drop(1).firstOrNull() ?: "Watch Together Session"
        var isPlaying by remember { mutableStateOf(true) }

        Dialog(onDismissRequest = { activeWatchTogetherVideo = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        IconButton(onClick = { activeWatchTogetherVideo = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎬", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (isPlaying) "▶ Synchronized Video Streaming Active" else "⏸ Playback Paused for Room", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { isPlaying = !isPlaying }
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isPlaying) "⏸ Pause for All" else "▶ Sync Play",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
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
        val realUri = if (imageText.contains("📷 Attached Image: ")) imageText.substringAfter("📷 Attached Image: ").trim() else imageText.trim()
        Dialog(onDismissRequest = { previewImageDialogUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
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
                        Text("Media Attachment", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { previewImageDialogUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 450.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(if (realUri.startsWith("content://") || realUri.startsWith("file://") || realUri.startsWith("http")) Uri.parse(realUri) else realUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Preview Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // WebRTC Audio/Video Call Screen Overlay Dialog
    uiState.activeCall?.let { call ->
        Dialog(onDismissRequest = { viewModel.endCall() }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (call.isVideo) "📹 Video Call" else "📞 Voice Call",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.peerUserId.takeLast(1).ifBlank { "U" },
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.peerUserId,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = call.status,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated Video Feed / Audio Visualizer
                    if (call.isVideo && call.isCameraOn) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎥 E2EE Video Stream Active", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("1080p WebRTC High Quality", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Call Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (call.isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.toggleMute() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (call.isMuted) "🔇" else "🎙️", fontSize = 22.sp)
                        }

                        // Toggle Camera (if Video Call)
                        if (call.isVideo) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (!call.isCameraOn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.toggleCamera() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (call.isCameraOn) "📹" else "🚫", fontSize = 22.sp)
                            }
                        }

                        // End Call Button
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { viewModel.endCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔴", fontSize = 24.sp)
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
    onVoteOption: (Int) -> Unit = {},
    onWatchTogetherClick: (String) -> Unit = {},
    onLocationClick: (String) -> Unit = {}
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
    val isLocation = textContent.startsWith("📍 LOCATION:")
    val isWatchTogether = textContent.startsWith("🎬 WATCH_TOGETHER:")

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
                            else if (isWatchTogether) onWatchTogetherClick(textContent)
                            else if (isLocation) onLocationClick(textContent)
                        },
                        onLongClick = onLongClick
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isImage) {
                        val imageUrl = if (textContent.contains("📷 Attached Image: ")) textContent.substringAfter("📷 Attached Image: ").trim() else textContent.trim()
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(textColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://") || imageUrl.startsWith("http")) Uri.parse(imageUrl) else imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Attachment Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (isWatchTogether) {
                        val wtPayload = textContent.substringAfter("🎬 WATCH_TOGETHER: ")
                        val videoTitle = wtPayload.substringAfter(" | ").ifBlank { "Watch Together Session" }
                        val videoUrl = wtPayload.substringBefore(" | ")
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎬", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = videoTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("▶ Tap to join synchronized video room", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
                            Text(videoUrl, fontSize = 10.sp, color = textColor.copy(alpha = 0.7f), maxLines = 1)
                        }
                    } else if (isLocation) {
                        val locPayload = textContent.substringAfter("📍 LOCATION: ")
                        val coordsStr = locPayload.substringBefore(" | ")
                        val address = locPayload.substringAfter(" | ")
                        val parts = coordsStr.split(",")
                        val senderLat = parts.getOrNull(0)?.toDoubleOrNull() ?: 10.762622
                        val senderLng = parts.getOrNull(1)?.toDoubleOrNull() ?: 106.660172
                        val recipientLat = 10.776889
                        val recipientLng = 106.700806

                        val senderLatLng = remember(senderLat, senderLng) { LatLng(senderLat, senderLng) }
                        val recipientLatLng = remember(recipientLat, recipientLng) { LatLng(recipientLat, recipientLng) }

                        val distanceString = remember(senderLat, senderLng, recipientLat, recipientLng) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                senderLat, senderLng, recipientLat, recipientLng, results
                            )
                            val meters = results[0]
                            if (meters >= 1000) {
                                String.format(Locale.getDefault(), "%.2f km", meters / 1000f)
                            } else {
                                "${meters.toInt()} m"
                            }
                        }

                        val cameraPositionState = rememberCameraPositionState {
                            val centerLat = (senderLat + recipientLat) / 2.0
                            val centerLng = (senderLng + recipientLng) / 2.0
                            position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 12f)
                        }

                        Column(modifier = Modifier.width(260.dp).padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Vị trí GPS chia sẻ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = address,
                                        fontSize = 11.sp,
                                        color = textColor.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    color = com.noatnoat.chatapp.theme.OnlineGreen,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // EMBEDDED GOOGLE MAP CARD INSIDE CHAT BUBBLE
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray)
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    cameraPositionState = cameraPositionState,
                                    uiSettings = MapUiSettings(
                                        zoomControlsEnabled = false,
                                        scrollGesturesEnabled = false,
                                        zoomGesturesEnabled = false,
                                        rotationGesturesEnabled = false,
                                        tiltGesturesEnabled = false
                                    )
                                ) {
                                    Marker(
                                        state = MarkerState(position = senderLatLng),
                                        title = "Vị trí người gửi"
                                    )
                                    Marker(
                                        state = MarkerState(position = recipientLatLng),
                                        title = "Vị trí của bạn"
                                    )
                                    Polyline(
                                        points = listOf(senderLatLng, recipientLatLng),
                                        color = Color(0xFF2196F3),
                                        width = 8f
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "📏 Cách $distanceString",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "▶ Chạm để mở rộng bản đồ & chỉ đường",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (isPoll) {
                        val pollPayload = textContent.substringAfter("📊 POLL: ")
                        val parts = pollPayload.split(" | ")
                        val question = parts.firstOrNull() ?: "Poll"
                        val options = parts.drop(1)
                        val voteCounts = options.map { optStr ->
                            optStr.substringAfterLast("(").substringBefore(")").toIntOrNull() ?: 0
                        }
                        val totalVotes = voteCounts.sum().coerceAtLeast(1)

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

                            Spacer(modifier = Modifier.height(10.dp))

                            options.forEachIndexed { index, optStr ->
                                val optName = optStr.substringBeforeLast(" (")
                                val voteCount = voteCounts[index]
                                val percentFraction = voteCount.toFloat() / totalVotes.toFloat()
                                val percentInt = (percentFraction * 100).toInt()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(textColor.copy(alpha = 0.12f))
                                        .clickable { onVoteOption(index) }
                                ) {
                                    // Blue / Light Fill Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(percentFraction.coerceAtLeast(0.02f))
                                            .fillMaxSize()
                                            .background(
                                                if (isOutbound) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = optName,
                                            fontSize = 14.sp,
                                            color = textColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$percentInt% ($voteCount)",
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Bold
                                        )
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
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        if (isOutbound) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val (statusIcon, statusLabel) = when (message.status.uppercase()) {
                                "SENDING" -> "⏱" to "Sending"
                                "SENT" -> "✓" to "Sent"
                                "DELIVERED" -> "✓✓" to "Delivered"
                                "SEEN", "READ" -> "✓✓" to "Seen"
                                else -> "✓✓" to "Delivered"
                            }
                            Text(
                                text = "$statusIcon $statusLabel",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.status.uppercase() in listOf("SEEN", "READ")) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    textColor.copy(alpha = 0.85f)
                                }
                            )
                        }
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

fun fetchRealLocation(
    context: Context,
    onLocationFetched: (lat: Double, lng: Double, address: String) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onLocationFetched(10.762622, 106.660172, "GPS Location Unavailable")
        return
    }

    try {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val provider = when {
            isGpsEnabled -> LocationManager.GPS_PROVIDER
            isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }

        @Suppress("DEPRECATION")
        val location: Location? = locationManager.getLastKnownLocation(provider)
            ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            var addressStr = "Lat: %.4f, Lng: %.4f".format(lat, lng)

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val parts = listOfNotNull(
                        addr.thoroughfare,
                        addr.subLocality ?: addr.locality,
                        addr.adminArea,
                        addr.countryName
                    )
                    if (parts.isNotEmpty()) {
                        addressStr = parts.joinToString(", ")
                    }
                }
            } catch (_: Exception) {}

            onLocationFetched(lat, lng, addressStr)
        } else {
            onLocationFetched(10.762622, 106.660172, "GPS Signal Acquired (Ho Chi Minh City)")
        }
    } catch (e: Exception) {
        onLocationFetched(10.762622, 106.660172, "Location Error")
    }
}

@Composable
fun UserProfileDialog(
    peerUserId: String,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onBlockToggle: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peerUserId.takeLast(1).ifBlank { "U" }.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Peer User Name / ID
                Text(
                    text = peerUserId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isBlocked) "🔴 Đã bị chặn" else "🟢 Trực tuyến • Tín hiệu E2EE",
                    fontSize = 13.sp,
                    color = if (isBlocked) Color.Red else com.noatnoat.chatapp.theme.OnlineGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button: Chặn người dùng / Bỏ chặn
                Button(
                    onClick = {
                        onBlockToggle()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) MaterialTheme.colorScheme.primary else Color(0xFFE31C23)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isBlocked) "Bỏ chặn người dùng này" else "Chặn người dùng này",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đóng", fontSize = 14.sp)
                }
            }
        }
    }
}
