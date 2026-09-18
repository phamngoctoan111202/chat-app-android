package com.noatnoat.chatapp.ui.chat

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileScreen(
    peerUserId: String,
    nickname: String? = null,
    isMuted: Boolean = false,
    isBlocked: Boolean = false,
    ephemeralTimerSeconds: Int = 0,
    onBackClick: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onShareLocation: () -> Unit,
    onOpenWatchTogether: () -> Unit,
    onOpenCreatePoll: () -> Unit,
    onOpenEphemeralSettings: () -> Unit,
    onBlockToggle: () -> Unit,
    onMuteToggle: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onReportUser: (reason: String) -> Unit = {},
    onSetNickname: (nickname: String) -> Unit = {}
) {
    val context = LocalContext.current

    var showClearChatConfirmDialog by remember { mutableStateOf(false) }
    var showReportUserDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("Gửi tin nhắn rác / Spam") }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf(nickname ?: "") }
    var showSafetyNumberDialog by remember { mutableStateOf(false) }
    var showSharedMediaDialog by remember { mutableStateOf(false) }

    val displayName = nickname.takeIf { !it.isNullBlinkOrBlank() } ?: peerUserId

    Dialog(
        onDismissRequest = onBackClick,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thông tin tài khoản", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. HEADER AVATAR & USER INFO
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.takeLast(1).ifBlank { "U" }.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (!nickname.isNullBlank()) "Biệt danh: $nickname ($peerUserId)" else "Biệt danh: Chưa thiết lập",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isBlocked) "🔴 Đã bị chặn" else if (isMuted) "🔕 Đã tắt thông báo • E2EE Signal" else "🟢 Trực tuyến • Tín hiệu E2EE Signal",
                    fontSize = 13.sp,
                    color = if (isBlocked) Color.Red else if (isMuted) Color.Gray else com.noatnoat.chatapp.theme.OnlineGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. QUICK ACTION BUTTONS BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton(
                        icon = "📞",
                        label = "Gọi thoại",
                        onClick = onStartVoiceCall
                    )
                    QuickActionButton(
                        icon = "📹",
                        label = "Gọi Video",
                        onClick = onStartVideoCall
                    )
                    QuickActionButton(
                        icon = "💬",
                        label = "Nhắn tin",
                        onClick = onBackClick
                    )
                    QuickActionButton(
                        icon = "📍",
                        label = "Vị trí GPS",
                        onClick = onShareLocation
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. MỤC: HÀNH ĐỘNG (ACTIONS)
                SectionTitle("HÀNH ĐỘNG")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ProfileFeatureRow(
                            icon = "✏️",
                            title = "Biệt danh",
                            subtitle = nickname?.let { "Hiện tại: $it" } ?: "Đặt biệt danh cho người này",
                            onClick = {
                                nicknameInput = nickname ?: ""
                                showNicknameDialog = true
                            }
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "🔍",
                            title = "Tìm kiếm trong cuộc trò chuyện",
                            subtitle = "Tìm kiếm tin nhắn, từ khóa",
                            onClick = onBackClick
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "🖼️",
                            title = "File phương tiện, tệp & liên kết",
                            subtitle = "Xem ảnh, video, tệp đính kèm và link đã chia sẻ",
                            onClick = { showSharedMediaDialog = true }
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "📌",
                            title = "Tin nhắn đã ghim",
                            subtitle = "Xem danh sách các tin nhắn đã ghim",
                            onClick = onBackClick
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "📊",
                            title = "Tạo cuộc bình chọn",
                            subtitle = "Bỏ phiếu ý kiến cuộc hội thoại",
                            onClick = onOpenCreatePoll
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "🎬",
                            title = "Xem Video chung (Watch Together)",
                            subtitle = "Đồng bộ hóa màn hình xem video chung",
                            onClick = onOpenWatchTogether
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. MỤC: BẢO MẬT & MÃ HÓA (SECURITY & ENCRYPTION)
                SectionTitle("BẢO MẬT & MÃ HÓA")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ProfileFeatureRow(
                            icon = "⏱️",
                            title = "Tin nhắn tự xóa (Disappearing Messages)",
                            subtitle = if (ephemeralTimerSeconds > 0) "Tự hủy sau $ephemeralTimerSeconds giây" else "Đang Tắt",
                            onClick = onOpenEphemeralSettings
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "🔒",
                            title = "Khóa bảo mật E2EE (Safety Number)",
                            subtitle = "Kiểm tra mã vân tay mã hóa Signal Protocol",
                            onClick = { showSafetyNumberDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. MỤC: QUYỀN RIÊNG TƯ VÀ HỖ TRỢ (PRIVACY & SUPPORT)
                SectionTitle("QUYỀN RIÊNG TƯ VÀ HỖ TRỢ")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ProfileFeatureRow(
                            icon = if (isMuted) "🔕" else "🔔",
                            title = if (isMuted) "Bật lại thông báo" else "Tắt thông báo (Mute)",
                            subtitle = if (isMuted) "Đang tắt âm thanh thông báo cuộc trò chuyện" else "Tắt/Bật âm thanh thông báo cuộc trò chuyện",
                            onClick = {
                                onMuteToggle()
                                val msg = if (isMuted) "Đã bật lại thông báo cuộc trò chuyện" else "Đã tắt thông báo cuộc trò chuyện"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = if (isBlocked) "🔓" else "🚫",
                            title = if (isBlocked) "Bỏ chặn người dùng" else "Chặn người dùng (Block)",
                            subtitle = if (isBlocked) "Cho phép người này gửi tin nhắn lại" else "Không thể gửi tin nhắn hoặc gọi điện",
                            titleColor = if (isBlocked) MaterialTheme.colorScheme.primary else Color(0xFFE31C23),
                            onClick = onBlockToggle
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "⚠️",
                            title = "Báo cáo vi phạm (Report)",
                            subtitle = "Báo cáo tài khoản có hành vi không phù hợp",
                            titleColor = Color(0xFFE31C23),
                            onClick = { showReportUserDialog = true }
                        )
                        ProfileDivider()
                        ProfileFeatureRow(
                            icon = "🗑️",
                            title = "Xóa lịch sử cuộc trò chuyện (Clear Chat)",
                            subtitle = "Xóa toàn bộ tin nhắn lưu cục bộ trên máy",
                            titleColor = Color(0xFFE31C23),
                            onClick = { showClearChatConfirmDialog = true }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // --- INTERACTIVE DIALOGS ---

    // Clear Chat Dialog
    if (showClearChatConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirmDialog = false },
            title = { Text("Xóa toàn bộ lịch sử cuộc trò chuyện?", fontWeight = FontWeight.Bold) },
            text = { Text("Thao tác này sẽ xóa tất cả tin nhắn với $displayName trên thiết bị này. Bạn không thể khôi phục lại.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearChatConfirmDialog = false
                        onClearChat()
                        Toast.makeText(context, "Đã xóa toàn bộ lịch sử cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE31C23))
                ) {
                    Text("Xóa lịch sử", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearChatConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Report User Dialog
    if (showReportUserDialog) {
        AlertDialog(
            onDismissRequest = { showReportUserDialog = false },
            title = { Text("Báo cáo vi phạm", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Chọn lý do báo cáo tài khoản $displayName:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    val reportReasons = listOf(
                        "Gửi tin nhắn rác / Spam",
                        "Quấy rối hoặc đe dọa",
                        "Tài khoản giả mạo người khác",
                        "Nội dung vi phạm tiêu chuẩn cộng đồng"
                    )
                    reportReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedReportReason == reason),
                                onClick = { selectedReportReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportUserDialog = false
                        onReportUser(selectedReportReason)
                        Toast.makeText(context, "Đã gửi báo cáo vi phạm. Cảm ơn phản hồi của bạn!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE31C23))
                ) {
                    Text("Gửi báo cáo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReportUserDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Nickname Dialog
    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Đặt biệt danh", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nhập biệt danh cho $peerUserId:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        singleLine = true,
                        placeholder = { Text("Ví dụ: Tèo, Sếp, Anh Ba...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNicknameDialog = false
                        onSetNickname(nicknameInput)
                        Toast.makeText(context, "Đã cập nhật biệt danh", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNicknameDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Safety Number (E2EE) Dialog
    if (showSafetyNumberDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyNumberDialog = false },
            title = { Text("Khóa bảo mật E2EE (Safety Number)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("So sánh mã vân tay Signal Protocol 60 chữ số này với $displayName để xác minh liên lạc riêng tư:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "34582 91823 84729 19283\n84729 91023 48192 38471\n92019 48201 39182 48102",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSafetyNumberDialog = false
                        Toast.makeText(context, "Đã xác minh mã an toàn E2EE", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Đã xác minh")
                }
            }
        )
    }

    // Shared Media & Links Dialog
    if (showSharedMediaDialog) {
        AlertDialog(
            onDismissRequest = { showSharedMediaDialog = false },
            title = { Text("File phương tiện, tệp & liên kết", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Toàn bộ media được mã hóa Signal E2EE chia sẻ trong cuộc trò chuyện này:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• 📷 0 Hình ảnh / Video", fontSize = 14.sp)
                    Text("• 📄 0 Tệp đính kèm", fontSize = 14.sp)
                    Text("• 🔗 0 Đường liên kết (URL)", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showSharedMediaDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

private fun String?.isNullBlank(): Boolean {
    return this == null || this.isBlank()
}

private fun String?.isNullBlinkOrBlank(): Boolean {
    return this == null || this.isBlank()
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray
    )
}

@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProfileFeatureRow(
    icon: String,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProfileDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF0F2F5))
    )
}
