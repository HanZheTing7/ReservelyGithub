package com.example.reservely.presentation.features.event_detail

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reservely.R
import com.example.reservely.data.model.Event
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.reservely.presentation.features.chat.ChatHelper
import com.example.reservely.presentation.features.chat.ChatHelperViewModel
import com.example.reservely.data.model.JoinRequest
import com.example.reservely.data.model.ParticipantInfo
import com.example.reservely.data.model.PlusOneRequest
import com.example.reservely.presentation.components.formatDuration
import com.example.reservely.presentation.components.formatTimestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    navController: NavController,
    event: Event,
    currentUserId: String,
    onEditClick: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
    chatHelper: ChatHelper = hiltViewModel<ChatHelperViewModel>().chatHelper
) {
    val context = LocalContext.current

    val isLoadingUserStatus by viewModel.isLoadingUserStatus.collectAsState()

    val isPastEvent = remember(event) {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Kuala_Lumpur"))
        val eventDateTime = event.dateTime.toDate().toInstant().atZone(ZoneId.of("Asia/Kuala_Lumpur"))
        eventDateTime.isBefore(now)
    }

    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"

    val joinRequests by viewModel.joinRequests.collectAsState()
    val withdrawals by viewModel.withdrawRequests.collectAsState()
    val waitlist by viewModel.waitlistRequests.collectAsState()
    val participants by viewModel.participants.collectAsState()

    val hasJoined by viewModel.hasJoined.collectAsState()
    val hasRequested by viewModel.hasRequested.collectAsState()
    val hasWithdrawRequested by viewModel.hasWithdrawRequested.collectAsState()
    val isWaitlisted by viewModel.isWaitlisted.collectAsState()

    val plusOneRequests by viewModel.plusOneRequests.collectAsState()
    val plusOneWaitlist by viewModel.plusOneWaitlist.collectAsState()

    val hostName by viewModel.hostName.collectAsState()

    var showPlusOneDialog by remember { mutableStateOf(false) }
    var showHostPlusOneDialog by remember { mutableStateOf(false) }

    var selectedUser by remember { mutableStateOf<JoinRequest?>(null) }
    var showUnifiedSheet by remember { mutableStateOf(false) }
    var accumulatedDrag by remember { mutableStateOf(0f) }

    val swipeThreshold = 50f
    val isDualPrice = event.isDualPrice ||
            (event.priceMale != null && event.priceFemale != null && event.pricePerPerson == null)

    LaunchedEffect(event.id, currentUserId) {
        viewModel.loadJoinRequests(event.id)
        viewModel.loadWithdrawalRequests(event.id)
        viewModel.loadWaitlistRequests(event.id)
        viewModel.checkIfEventIsFull(event)
        viewModel.checkUserStatus(event.id, currentUserId)
        viewModel.loadParticipants(event.id)
        viewModel.loadPlusOneRequests(event.id)
    }

    LaunchedEffect(event.hostId) {
        viewModel.observeHostName(event.hostId, fallback = event.hostName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
                    onDragEnd = {
                        if (accumulatedDrag < -swipeThreshold) {
                            showUnifiedSheet = true
                        }
                        accumulatedDrag = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val imageHeight = screenHeight * 0.4f
            val imageRes = when (event.category) {
                "Badminton" -> R.drawable.badminton
                "Pickleball" -> R.drawable.pickleball
                else -> R.drawable.placeholder
            }

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
                            onDragEnd = {
                                if (accumulatedDrag < -swipeThreshold) {
                                    showUnifiedSheet = true
                                }
                                accumulatedDrag = 0f
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("👤 Host: ${hostName ?: event.hostName}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text("📍 ${event.address}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("🕒 ${formatTimestamp(event.dateTime)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("⏱ Duration: ${formatDuration(event.durationMinutes)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("👥 Max: ${event.maxPeople}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                Text(
                    text = when {
                        isDualPrice -> {
                            if (event.priceMale != null && event.priceFemale != null) {
                                "💵 RM %.2f (M) || RM %.2f (F)".format(Locale.US, event.priceMale!!, event.priceFemale!!)
                            } else "💵 Price not set"
                        }
                        event.pricePerPerson != null -> "💵 RM %.2f".format(Locale.US, event.pricePerPerson!!)
                        else -> "💵 Price not set"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (event.courtBooked) {
                    Text("✅ Court is Booked", color = Color.Green, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("❌ Court not Booked", color = Color.Red)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(event.description, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(24.dp))

                if (!isPastEvent) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (event.hostId == currentUserId) {
                            Button(onClick = { showHostPlusOneDialog = true }) {
                                Text("+1 Add Friend (Host)")
                            }

                            Button(onClick = onEditClick, modifier = Modifier.padding(end = 20.dp)) {
                                Text("Edit Event")
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                                if (isLoadingUserStatus) {
                                    CircularProgressIndicator()
                                } else {
                                    when {
                                        hasJoined -> {

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = {}, enabled = false) {
                                                    Text("✅ Confirmed")
                                                }

                                                if (hasWithdrawRequested) {
                                                    Button(onClick = {
                                                        viewModel.cancelWithdrawal(
                                                            event.id,
                                                            currentUserId
                                                        )
                                                    }) {
                                                        Text("Cancel Withdrawal")
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            viewModel.requestWithdrawal(
                                                                event,
                                                                currentUserId
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color.Red
                                                        )
                                                    ) { Text("Withdraw") }
                                                }
                                            }
                                        }

                                        hasRequested -> {
                                            Button(
                                                onClick = {
                                                    viewModel.toggleJoinRequest(
                                                        event,
                                                        currentUserId,
                                                        userName
                                                    ) { _, _ -> }
                                                }
                                            ) {
                                                Text("⏳ Requested (Tap to Cancel)")
                                            }

                                            Button(onClick = { showPlusOneDialog = true }) {
                                                Text("+1 Request for Friend")
                                            }
                                        }

                                        isWaitlisted -> {
                                            Button(onClick = {
                                                viewModel.toggleJoinRequest(
                                                    event,
                                                    currentUserId,
                                                    userName
                                                ) { _, msg ->
                                                    Toast.makeText(
                                                        context,
                                                        msg ?: "",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }) {
                                                Text("⏳ Waitlisted (Tap to Cancel)")
                                            }
                                        }

                                        else -> {
                                            Button(
                                                onClick = {
                                                    viewModel.toggleJoinRequest(
                                                        event,
                                                        currentUserId,
                                                        userName
                                                    ) { _, _ -> }
                                                }
                                            ) {
                                                Text("➕ Request to Join")
                                            }

                                            Button(onClick = { showPlusOneDialog = true }) {
                                                Text("+1 Request for Friend")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .clickable { showUnifiedSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )
                        Text("Swipe up to view participants", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showUnifiedSheet) {
        UnifiedBottomSheet(
            participants = participants,
            maxPeople = event.maxPeople,
            isHost = currentUserId == event.hostId,
            joinRequests = joinRequests,
            withdrawalRequests = withdrawals,
            waitlist = waitlist,
            plusOneRequests = plusOneRequests,
            plusOneWaitlist = plusOneWaitlist,
            onAcceptJoin = { viewModel.acceptJoinRequest(event, it) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
            onRejectJoin = { viewModel.removeJoinRequest(event.id, it.userId) },
            onApprovePlusOne = { plusOneReq, isWaitlist ->
                viewModel.approvePlusOneRequest(event, plusOneReq, isWaitlist) { _, msg ->
                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                }
            },
            onRejectPlusOne = { plusOneReq, isWaitlist ->
                viewModel.rejectPlusOneRequest(event.id, plusOneReq, isWaitlist) { _, msg ->
                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                }
            },
            onAcceptWithdraw = { viewModel.acceptWithdrawalRequest(event, it.userId) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
            onRejectWithdraw = { viewModel.rejectWithdrawalRequest(event, it.userId) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
            onAcceptWaitlist = { viewModel.acceptWaitlistRequest(event, it) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
            onRejectWaitlist = {
                FirebaseFirestore.getInstance()
                    .collection("events")
                    .document(event.id)
                    .collection("waitlist")
                    .document(it.userId)
                    .delete()
            },
            onKickParticipant = { participant ->
                viewModel.kickParticipant(event, participant.userId) { success, message ->
                    if (!success) {
                        Toast.makeText(context, message ?: "Failed to kick", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showUnifiedSheet = false },
            onUserClick = { selectedUser = it },
            isPastEvent = isPastEvent,
            eventId = event.id
        )
    }

    if (selectedUser != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedUser = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = selectedUser!!.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Name: ${selectedUser!!.userName}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (currentUserId == null) {
                            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        chatHelper.createDirectMessageChannel(
                            currentUserId = currentUserId,
                            targetUserId = selectedUser!!.userId
                        ) { channel, error ->
                            if (channel != null) {
                                selectedUser = null // Close the sheet
                                navController.navigate("messages?cid=${Uri.encode(channel.cid)}")
                            } else {
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                ) {
                    Text("Chat")
                }
            }
        }
    }


    if (showHostPlusOneDialog) {
        AddPlusOneDialog(
            onDismiss = { showHostPlusOneDialog = false },
            onSubmit = { friendName ->
                viewModel.addHostPlusOne(event, friendName) { success, msg ->
                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                }
                showHostPlusOneDialog = false
            },
            currentCount = participants.size,
            maxPeople = event.maxPeople
        )
    }

    if (showPlusOneDialog) {
        RequestPlusOneDialog(
            onDismiss = { showPlusOneDialog = false },
            onSubmit = { friendName ->
                viewModel.submitPlusOneRequest(event, currentUserId, userName, friendName) { success, msg ->
                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                }
                showPlusOneDialog = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedBottomSheet(
    participants: List<ParticipantInfo>,
    maxPeople: Int,
    isHost: Boolean,
    joinRequests: List<JoinRequest>,
    withdrawalRequests: List<JoinRequest>,
    waitlist: List<JoinRequest>,
    onAcceptJoin: (JoinRequest) -> Unit,
    onRejectJoin: (JoinRequest) -> Unit,
    onAcceptWithdraw: (JoinRequest) -> Unit,
    onRejectWithdraw: (JoinRequest) -> Unit,
    onAcceptWaitlist: (JoinRequest) -> Unit,
    onRejectWaitlist: (JoinRequest) -> Unit,
    onKickParticipant: (ParticipantInfo) -> Unit,
    plusOneRequests: List<PlusOneRequest>,
    plusOneWaitlist: List<PlusOneRequest>,
    onApprovePlusOne: (PlusOneRequest, isWaitlist: Boolean) -> Unit,
    onRejectPlusOne: (PlusOneRequest, isWaitlist: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onUserClick: (JoinRequest) -> Unit,
    isPastEvent: Boolean,
    eventId: String
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /** Participants Section **/
            item {
                Text(
                    "Participants (${participants.size} / $maxPeople)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
            }

            if (participants.isEmpty()) {
                item {
                    Text("No participants yet.")
                }
            } else {
                items(participants) { participant ->
                    var showDialog by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUserClick(
                                    JoinRequest(
                                        userId = participant.userId,
                                        userName = participant.userName,
                                        profileImageUrl = participant.profileImageUrl,
                                        age = participant.age,
                                        gender = participant.gender
                                    )
                                )
                            }
                            .padding(end = 4.dp)
                    ) {
                        AsyncImage(
                            model = participant.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = participant.userName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (isHost && !isPastEvent) {
                            IconButton(
                                onClick = { showDialog = true }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Kick", tint = Color.Red)
                            }
                        }
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Remove Participant") },
                            text = { Text("Are you sure you want to remove ${participant.userName}?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDialog = false
                                    onKickParticipant(participant)
                                }) {
                                    Text("Kick", color = Color.Red)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }

            }

            /** Host-only Sections **/
            if (isHost && !isPastEvent) {

                /** Join Requests (with +1 merged) **/
                item { Spacer(modifier = Modifier.height(12.dp)); Text("Join Requests", fontWeight = FontWeight.Bold) }

                if (joinRequests.isEmpty() && plusOneRequests.isEmpty()) {
                    item { Text("No join requests.") }
                } else {
                    items(joinRequests) { req ->
                        RequestUserCard(req, { onAcceptJoin(req) }, { onRejectJoin(req) }, { onUserClick(req) })
                    }

                    items(plusOneRequests) { plusOne ->
                        val joinRequestAddOne = JoinRequest(
                            userId = "${plusOne.requesterId}_plusOne_${plusOne.id}",
                            userName = "${plusOne.friendName} (+1 of ${plusOne.requesterName})",
                            profileImageUrl = plusOne.requesterProfileImage,
                            age = 0,
                            gender = ""
                        )

                        RequestUserCard(
                            request = joinRequestAddOne,
                            onAccept = { onApprovePlusOne(plusOne, false) },
                            onReject = { onRejectPlusOne(plusOne,false) },
                            onUserClick = { /* Optional: Show requester info if you want */ }
                        )
                    }

                }

                /** Withdrawal Requests **/
                item { Spacer(modifier = Modifier.height(12.dp)); Text("Withdrawal Requests", fontWeight = FontWeight.Bold) }

                if (withdrawalRequests.isEmpty()) {
                    item { Text("No withdrawal requests.") }
                } else {
                    items(withdrawalRequests) { req ->
                        RequestUserCard(req, { onAcceptWithdraw(req) }, { onRejectWithdraw(req) }, { onUserClick(req) })
                    }
                }

                /** Waitlist (with +1 waitlist merged) **/
                item { Spacer(modifier = Modifier.height(12.dp)); Text("Waitlist", fontWeight = FontWeight.Bold) }

                if (waitlist.isEmpty() && plusOneWaitlist.isEmpty()) {
                    item { Text("No waitlist entries.") }
                } else {
                    items(waitlist) { req ->
                        RequestUserCard(req, { onAcceptWaitlist(req) }, { onRejectWaitlist(req) }, { onUserClick(req) })
                    }

                    items(plusOneWaitlist) { plusOne ->
                        val joinRequestPlusOne = JoinRequest(
                            userId = "${plusOne.requesterId}_plusOne_${plusOne.id}",
                            userName = "${plusOne.friendName} (+1 of ${plusOne.requesterName})",
                            profileImageUrl = plusOne.requesterProfileImage,
                            age = 0,
                            gender = ""
                        )

                        RequestUserCard(
                            request = joinRequestPlusOne,
                            onAccept = { onApprovePlusOne(plusOne, true) },
                            onReject = { onRejectPlusOne(plusOne, true) },
                            onUserClick = { /* Optional: Show requester info */ }
                        )
                    }

                }
            }
        }
        }
    }


@Composable
fun RequestUserCard(
    request: JoinRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.clickable { onUserClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = request.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(request.userName, fontWeight = FontWeight.SemiBold)
            }

            Row {
                IconButton(onClick = onAccept) {
                    Icon(Icons.Default.Check, contentDescription = "Accept")
                }
                IconButton(onClick = onReject) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }
            }
        }
    }
}

@Composable
fun RequestPlusOneDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var friendName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Request +1") },
        text = {
            Column {
                Text("Enter your friend's name:")
                OutlinedTextField(
                    value = friendName,
                    onValueChange = { friendName = it },
                    label = { Text("Friend's Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (friendName.isNotBlank()) onSubmit(friendName) },
                enabled = friendName.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddPlusOneDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    currentCount: Int,
    maxPeople: Int
) {
    var friendName by remember { mutableStateOf("") }
    val isFull = currentCount >= maxPeople

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add +1 as Host") },
        text = {
            Column {
                if (isFull) {
                    Text("Event is full. Cannot add more participants.", color = Color.Red)
                } else {
                    Text("Enter your friend's name:")
                    OutlinedTextField(
                        value = friendName,
                        onValueChange = { friendName = it },
                        label = { Text("Friend's Name") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (friendName.isNotBlank() && !isFull) onSubmit(friendName) },
                enabled = friendName.isNotBlank() && !isFull
            ) {
                Text("Add +1")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


