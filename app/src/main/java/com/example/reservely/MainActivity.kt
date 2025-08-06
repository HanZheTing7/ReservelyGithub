package com.example.reservely


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.reservely.presentation.theme.ReservelyTheme
import com.example.reservely.presentation.navigation.AppNavigation
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val placesApiKey = BuildConfig.GOOGLE_API_KEY
        val chatApiKey = BuildConfig.CHAT_API_KEY
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, placesApiKey)
        }
        placesClient = Places.createClient(this)

            // 1 - Set up the OfflinePlugin for offline storage
            val offlinePluginFactory = StreamOfflinePluginFactory(appContext = applicationContext)
            val statePluginFactory = StreamStatePluginFactory(config = StatePluginConfig(), appContext = this)

            // 2 - Set up the client for API calls and with the plugin for offline storage
            val client = ChatClient.Builder(chatApiKey, applicationContext)
                .withPlugins(offlinePluginFactory, statePluginFactory)
                .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
                .build()


        setContent {
            ChatTheme{
                ReservelyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(placesClient = placesClient)
                    }
                }
            }
        }
    }
}

