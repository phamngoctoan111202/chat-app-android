package com.noatnoat.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.noatnoat.chatapp.theme.ChatAppTheme

import com.noatnoat.chatapp.core.network.logging.AppLogger
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Log FCM Token for testing (Debug Mode Only)
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if (task.isSuccessful) {
        AppLogger.d("FCM_TOKEN", "MY_FCM_TOKEN: ${task.result}")
      }
    }

    enableEdgeToEdge()
    setContent {
      ChatAppTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
