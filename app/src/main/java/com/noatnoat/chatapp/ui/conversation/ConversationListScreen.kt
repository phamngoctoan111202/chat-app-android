package com.noatnoat.chatapp.ui.conversation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noatnoat.chatapp.core.database.entity.ConversationEntity
import com.noatnoat.chatapp.core.network.websocket.WsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noatnoat.chatapp.core.network.logging.AppLogger

@Composable
fun ConversationListScreen(
    viewModel: ConversationViewModel,
    onConversationClick: (peerUserId: String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    
    val totalUnread = uiState.conversations.sumOf { it.unreadCount }

    val TAG = "FLOW_CONVERSATION"

    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Xác nhận xóa đoạn chat?") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ lịch sử trò chuyện với ${conversationToDelete?.peerPhoneNumber}? Mọi tin nhắn mã hóa E2EE sẽ bị xóa khỏi máy và không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationToDelete?.let { conv ->
                            viewModel.deleteConversation(conv)
                            Toast.makeText(context, "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                        }
                        conversationToDelete = null
                    }
                ) {
                    Text("Xóa", color = Color(0xFFE31C23), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
    if (isSearchDialogOpen) {
        UserSearchDialog(
            viewModel = viewModel,
            onDismiss = { isSearchDialogOpen = false },
            onUserSelected = { peerId ->
                isSearchDialogOpen = false
                viewModel.startNewConversation(peerId)
                onConversationClick(peerId)
            }
        )
    }

    Scaffold(
        bottomBar = {
            HomeBottomNavigation(
                totalUnread = totalUnread,
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == 3) {
                        onSettingsClick()
                    } else {
                        selectedTab = tab
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // 1. HEADER (Đã bỏ chữ "Đoạn chat")
            HomeHeader(
                onSettingsClick = onSettingsClick,
                onNewChatClick = { isSearchDialogOpen = true }
            )
            
            // 2. CONNECTION STATUS BANNER
            when (val state = uiState.connectionState) {
                is WsState.Connecting -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFFBE6))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đang kết nối máy chủ...",
                            color = Color(0xFFD48806),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is WsState.Error -> {
                    AppLogger.d(TAG, "Trạng thái lỗi WebSocket: ${state.throwable.message}")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF0F0))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mất kết nối máy chủ - Đang tự động kết nối lại...",
                            color = Color(0xFFE31C23),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {}
            }
            
            when (selectedTab) {
                0 -> {
                    // 3. SEARCH BAR (Nhấp vào mở Dialog tìm kiếm)
                    HomeSearchBar(
                        query = searchInput,
                        onClick = { isSearchDialogOpen = true },
                        onQueryChange = { searchInput = it },
                        onSearch = { isSearchDialogOpen = true }
                    )
                    
                    val filteredConversations = uiState.conversations.filter {
                        searchInput.isBlank() || it.peerPhoneNumber.contains(searchInput, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        // 5. CHAT LIST
                        if (filteredConversations.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Chưa có đoạn chat nào.",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredConversations, key = { it.conversationId }) { conv ->
                                ConversationItemRow(
                                    conversation = conv,
                                    onClick = { onConversationClick(conv.peerUserId) },
                                    onDeleteRequest = { conversationToDelete = conv }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Chưa có liên hệ nào.", color = Color.Gray, fontSize = 16.sp)
                    }
                }
                2 -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Không có thông báo mới.", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    onSettingsClick: () -> Unit,
    onNewChatClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F2F5))
                    .clickable { onNewChatClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Tạo tin nhắn mới",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F2F5))
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    query: String,
    onClick: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onSearch: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(50)) // Pill shape
            .background(Color(0xFFF0F2F5))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (query.isEmpty()) "Tìm kiếm" else query,
                color = if (query.isEmpty()) Color.Gray else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun UserSearchDialog(
    viewModel: ConversationViewModel,
    onDismiss: () -> Unit,
    onUserSelected: (peerUserId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // SEARCH DIALOG HEADER & INPUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF0F2F5))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { text ->
                                searchQuery = text
                                viewModel.searchUsers(text)
                            },
                            textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        val peerId = if (searchQuery.startsWith("user_")) searchQuery else "user_" + searchQuery.takeLast(6)
                                        onUserSelected(peerId)
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Tìm kiếm người nhắn tin mới...",
                                        color = Color.Gray,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.searchUsers("")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Xóa", tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DYNAMIC SEARCH RESULTS LIST
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (searchQuery.isNotBlank() && uiState.searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "❌ Không tìm thấy người dùng phù hợp",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE31C23),
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Vui lòng kiểm tra lại số điện thoại hoặc tên tài khoản đã đăng ký.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                items(uiState.searchResults) { userResult ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUserSelected(userResult.userId)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE4E6EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userResult.username.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userResult.username,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            if (userResult.phoneNumber.isNotBlank()) {
                                Text(
                                    text = userResult.phoneNumber,
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Text(
                            text = "Nhắn tin",
                            color = Color(0xFF0080FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationItemRow(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val isUnread = conversation.unreadCount > 0
    val textColor = if (isUnread) Color.Black else Color.Gray
    val fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDeleteRequest()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFEDED))
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa cuộc trò chuyện",
                        tint = Color(0xFFE31C23),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xóa", color = Color(0xFFE31C23), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onDeleteRequest
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = conversation.peerPhoneNumber.takeLast(1).ifBlank { "U" }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E6EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.uppercase(),
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Text(
                        text = formatHomeTime(conversation.lastTimestamp),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = fontWeight
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessageText,
                        fontSize = 14.sp,
                        color = textColor,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0080FF))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeBottomNavigation(
    totalUnread: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                BadgedBox(
                    badge = { 
                        if (totalUnread > 0) {
                            Badge { Text(totalUnread.toString()) } 
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Chats")
                }
            },
            label = { Text("Đoạn chat") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0080FF),
                selectedTextColor = Color(0xFF0080FF),
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "People") },
            label = { Text("Mọi người") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0080FF),
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                BadgedBox(
                    badge = { Badge { Text("") } }
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications")
                }
            },
            label = { Text("Thông báo") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0080FF),
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") },
            label = { Text("Menu") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0080FF),
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}


fun formatHomeTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = Calendar.getInstance()
    val timeToCheck = Calendar.getInstance().apply { timeInMillis = timestamp }

    val daysDiff = (now.timeInMillis - timeToCheck.timeInMillis) / (1000 * 60 * 60 * 24)

    return if (now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } else if (daysDiff < 7) {
        val dayOfWeek = timeToCheck.get(Calendar.DAY_OF_WEEK)
        when (dayOfWeek) {
            Calendar.SUNDAY -> "CN"
            Calendar.MONDAY -> "T2"
            Calendar.TUESDAY -> "T3"
            Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY -> "T5"
            Calendar.FRIDAY -> "T6"
            Calendar.SATURDAY -> "T7"
            else -> ""
        }
    } else {
        SimpleDateFormat("d/M", Locale.getDefault()).format(Date(timestamp))
    }
}
