package com.example.reservely.presentation.features.notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reservely.data.model.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(notificationsViewModel: NotificationsViewModel, navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val notifications by notificationsViewModel.notifications.collectAsState()

    var shouldShowPermissionRationale by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notificationsViewModel.markAllAsRead(
            onSuccess = {
                Log.d("NotificationScreen", "ViewModel reports success for markAllAsRead.")
            },
            onError = { exception ->
                Log.e("NotificationScreen", "ViewModel reports error: ${exception.message}")
                // Optional: Show a Toast to the user that it failed
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            when {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission) -> {
                    shouldShowPermissionRationale = true
                }
                else -> {
                    ActivityCompat.requestPermissions(activity, arrayOf(permission), 1001)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Notifications") })
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No notifications yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(notifications) { notification ->
                    NotificationCard(notification)
                }
            }
        }
    }

    //  Show rationale if needed
    if (shouldShowPermissionRationale) {
        AlertDialog(
            onDismissRequest = { shouldShowPermissionRationale = false },
            title = { Text("Notification Permission Required") },
            text = { Text("To get event updates, please allow notification access in system settings.") },
            confirmButton = {
                TextButton(onClick = {
                    shouldShowPermissionRationale = false
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

}


@Composable
fun NotificationCard(notification: Notification) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = notification.title, fontWeight = FontWeight.Bold)
        Text(text = notification.message)
        Text(
            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(notification.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
