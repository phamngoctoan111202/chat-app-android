package com.noatnoat.chatapp.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@Composable
fun DistanceMapDialog(
    senderLat: Double,
    senderLng: Double,
    senderName: String = "Người gửi",
    recipientLat: Double = 10.776889,
    recipientLng: Double = 106.700806,
    recipientName: String = "Vị trí của bạn",
    addressName: String = "Vị trí GPS chia sẻ",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val senderLatLng = remember(senderLat, senderLng) { LatLng(senderLat, senderLng) }
    val recipientLatLng = remember(recipientLat, recipientLng) { LatLng(recipientLat, recipientLng) }

    // Calculate distance between 2 GPS coordinates
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
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 13f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 Bản đồ Khoảng cách GPS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = addressName,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📏 Cách bạn $distanceString",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Google Map View Component
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        cameraPositionState = cameraPositionState
                    ) {
                        // Sender Marker
                        Marker(
                            state = MarkerState(position = senderLatLng),
                            title = senderName,
                            snippet = "Vị trí người gửi"
                        )

                        // Recipient Marker (Current User)
                        Marker(
                            state = MarkerState(position = recipientLatLng),
                            title = recipientName,
                            snippet = "Vị trí của bạn"
                        )

                        // Polyline connecting the 2 markers
                        Polyline(
                            points = listOf(senderLatLng, recipientLatLng),
                            color = Color(0xFF2196F3),
                            width = 10f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Đóng")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val uriStr = "https://www.google.com/maps/dir/?api=1&origin=$recipientLat,$recipientLng&destination=$senderLat,$senderLng"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("🗺️ Chỉ đường Google Maps")
                    }
                }
            }
        }
    }
}
