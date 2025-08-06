package com.example.reservely.presentation.features.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.ui.theme.StreamColors
import io.getstream.chat.android.compose.ui.theme.StreamShapes
import io.getstream.chat.android.compose.ui.theme.StreamTypography
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory

@Composable
fun MessageScreen(navController: NavController, channelId: String) {
    val context = LocalContext.current

    ChatTheme(
        colors = StreamColors.defaultColors().copy(
            ownMessagesBackground = Color(0xFFDCF8C6),
            otherMessagesBackground = Color.White,
            inputBackground = Color(0xFFF0F0F0)
        ),
        shapes = StreamShapes.defaultShapes().copy(
            myMessageBubble = RoundedCornerShape(20.dp),
            otherMessageBubble = RoundedCornerShape(20.dp),
            inputField = RoundedCornerShape(12.dp)
        ),
        typography = StreamTypography.defaultTypography().copy(
            body = TextStyle(fontSize = 14.sp),
            bodyBold = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            MessagesScreen(
                viewModelFactory = MessagesViewModelFactory(
                    context = context,
                    channelId = channelId,
                    chatClient = ChatClient.instance(),
                    messageLimit = 30
                ),
                onBackPressed = { navController.popBackStack() }
            )
        }
    }

}
