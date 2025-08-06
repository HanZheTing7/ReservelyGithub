package com.example.reservely.presentation.features.explore

import android.location.Location
import android.util.Log
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.reservely.R
import com.example.reservely.data.model.Event
import com.example.reservely.presentation.components.formatDuration
import com.example.reservely.presentation.components.formatTimestamp
import com.example.reservely.presentation.features.notification.NotificationsViewModel
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit = {},
    navController: NavController,
    selectedCategories: List<String> = emptyList(),
    selectedPlaces: List<String> = emptyList(),
    nearMe: Boolean = false,
    userLocation: Location? = null
) {

    val today = viewModel.today.value ?: return
    val selectedDate = viewModel.selectedDate.value ?: return
    val currentHour = LocalTime.now(ZoneId.of("Asia/Kuala_Lumpur")).hour

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        WeeklyCalendar(uiState = uiState, onDateSelected = onDateSelected)

        Spacer(modifier = Modifier.height(12.dp))

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = Color.LightGray,
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        HourlyPager(
            selectedHour = viewModel.selectedHour.value,
            selectedDate = selectedDate,
            today = today,
            currentHour = currentHour,
            onHourSelected = { hour ->
                viewModel.onHourSelected(hour)
            }
        )

        SwipeableCardPager(
            viewModel = viewModel,
            navController = navController,
            selectedCategories = selectedCategories,
            selectedPlaces = selectedPlaces,
            nearMe = nearMe,
            userLocation = userLocation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservelyAppBar(viewModel: NotificationsViewModel,
                    onProfileClick: () -> Unit,
                    onFilterClick: () -> Unit,
                    onNotificationClick: () -> Unit,
                    showFilterIcon: Boolean = false) {

    val unreadCount by viewModel.unreadCount.collectAsState()

    LaunchedEffect(unreadCount) {
        Log.d("BadgeRecompose", "Unread count changed: $unreadCount")
    }

    TopAppBar(
        title = {
            Text(
                text = "Reservely",
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            IconButton(onClick = {
                onProfileClick()
            }) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "User Profile"
                )
            }

            if (showFilterIcon) {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }

            IconButton(onClick = onNotificationClick ) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge { Text(unreadCount.toString()) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications"
                    )
                }
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    )
}

@Composable
fun WeeklyCalendar(uiState: CalendarUiState, onDateSelected: (LocalDate) -> Unit) {

    val totalPages = 4 // 1 current + 3 future weeks
    val currentPage = 0 // first page = current week
    val pagerState = rememberPagerState(initialPage = currentPage, pageCount = { totalPages })

    HorizontalPager(state = pagerState,  modifier = Modifier.fillMaxWidth()) { page ->
        val weekOffset = page // current week is 0, next week is 1, up to 3
        val dates = getDatesForWeek(LocalDate.now(), weekOffset)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dates.forEach { date ->
                val isPast = date.isBefore(uiState.today)
                val isSelected = date == uiState.selectedDate

                val textColor = if (isPast) Color.Gray else Color.Black
                val borderModifier = if (!isPast)
                    Modifier.dashedBorder(1.dp, Color.Black, CircleShape)
                else Modifier

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(enabled = !isPast) { onDateSelected(date) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .then(borderModifier)
                    ) {
                        Text(
                            text = date.dayOfWeek.name.take(1),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}


fun Modifier.dashedBorder(strokeWidth: Dp, color: Color, shape: Shape): Modifier =
    drawBehind {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, color = color, style = stroke)
    }

fun getDatesForWeek(today: LocalDate, offset: Int): List<LocalDate> {
    val startOfWeek = today
        .minusDays((today.dayOfWeek.value % 7).toLong())
        .plusWeeks(offset.toLong())
    return (0..6).map { startOfWeek.plusDays(it.toLong()) }
}

@Composable
fun SwipeableCardPager(viewModel: HomeViewModel,
                       navController: NavController,
                       selectedCategories: List<String> = emptyList(),
                       selectedPlaces: List<String> = emptyList(),
                       nearMe: Boolean = false,
                       userLocation: Location? = null) {

    val events by viewModel.events.collectAsState()
    val selectedDate = viewModel.selectedDate.value ?: return
    val selectedHour = viewModel.selectedHour.value
    val malaysiaZoneId = ZoneId.of("Asia/Kuala_Lumpur")
    val participantMap by viewModel.participantMap.collectAsState()
    val now = ZonedDateTime.now(malaysiaZoneId)

    LaunchedEffect(events) {
        viewModel.observeHostNamesFor(events)
    }

    val filteredEvents = events.filter { event ->
        val eventZoned = event.dateTime.toDate().toInstant().atZone(malaysiaZoneId)
        val eventDate = eventZoned.toLocalDate()
        val eventHour = eventZoned.hour

        val isSameDay = eventDate == selectedDate
        val isSameHour = selectedHour == -1 || eventHour == selectedHour
        val isFuture = when {
            selectedDate.isAfter(now.toLocalDate()) -> true
            selectedDate.isBefore(now.toLocalDate()) -> false
            else -> eventZoned.isAfter(now)
        }

        val matchesCategory =
            selectedCategories.isEmpty() || selectedCategories.contains(event.category)

        val matchesPlace = selectedPlaces.isEmpty() || selectedPlaces.any { place ->
            event.address.contains(place, ignoreCase = true)
        }

        val matchesNearMe = if (!nearMe || userLocation == null) true else {
            val geoPoint = event.location
            if (geoPoint != null) {
                val eventLocation = Location("").apply {
                    latitude = geoPoint.latitude
                    longitude = geoPoint.longitude
                }
                val distance = userLocation.distanceTo(eventLocation)
                distance <= 10000 // 10km
            } else {
                false
            }
        }

        val matchesLocation = when {
            selectedPlaces.isNotEmpty() && nearMe -> matchesPlace || matchesNearMe
            selectedPlaces.isNotEmpty() -> matchesPlace
            nearMe -> matchesNearMe
            else -> true
        }

        isSameDay && isSameHour && isFuture && matchesCategory && matchesLocation
    }.sortedBy { it.dateTime.toDate().toInstant() }



    if (events.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (filteredEvents.isEmpty()) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
        val baseText = "No events on ${selectedDate.format(formatter)}"

        val fullText = if (selectedHour != -1)
            "$baseText at ${String.format(Locale.getDefault(), "%02d:00", selectedHour)}"
        else
            baseText

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fullText,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { filteredEvents.size })

    Box(
        modifier = Modifier.fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 16.dp, end = 16.dp)
        ) { page ->

            val hostName = viewModel.hostNameMap.value[filteredEvents[page].hostId]
                ?: filteredEvents[page].hostName
            val participantCount = participantMap[filteredEvents[page].id]?.size ?: 0
            EventCard(
                event = filteredEvents[page],
                participantCount = participantCount,
                hostName = hostName,
                pagerState = pagerState,
                onClick = {
                    navController.navigate("eventDetail/${filteredEvents[page].id}")
                })
        }
    }
}


@Composable
fun EventCard(event: Event, participantCount: Int, hostName: String, pagerState:PagerState = rememberPagerState(pageCount = {1}), onClick:() -> Unit, imageHeightRatio: Float = 0.35f){

    val scrollState = rememberScrollState()

    val isDualPrice = event.isDualPrice ||
            (event.priceMale != null && event.priceFemale != null && event.pricePerPerson == null)


    val imageRes = when(event.category) {
        "Badminton" -> R.drawable.badminton
        "Pickleball" -> R.drawable.pickleball
        else -> R.drawable.placeholder
    }

    LaunchedEffect(pagerState.currentPage) {
        scrollState.scrollTo(0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .scale(0.95f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()
            .verticalScroll(scrollState)) {
            ResponsiveImage(imageRes, heightRatio = imageHeightRatio)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Text(
                    event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "👤 Host: $hostName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "👥 $participantCount / ${event.maxPeople}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "📍 ${event.address}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(alignment = Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🕒 ${formatTimestamp(event.dateTime)}",
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = when {
                            isDualPrice -> {
                                if (event.priceMale != null && event.priceFemale != null) {
                                    "💵 RM %.2f (M) / RM %.2f (F)".format(Locale.US,
                                        event.priceMale, event.priceFemale
                                    )
                                    "💵 Dual Price"
                                } else {
                                    "💵 Price not set"
                                }
                            }
                            event.pricePerPerson != null -> {
                                "💵 RM %.2f".format(Locale.US, event.pricePerPerson)
                            }
                            else -> "💵 Price not set"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.7f)
                    )

                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "⏱ ${formatDuration(event.durationMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun ResponsiveImage(
    drawableRes: Int,
    heightRatio: Float = 0.35f, // 35% of screen height by default
    cornerRadius: Dp = 12.dp
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val imageHeight = screenHeight * heightRatio

    val painter = rememberAsyncImagePainter(drawableRes)

    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(cornerRadius)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun HourlyPager(
    selectedHour: Int,
    selectedDate: LocalDate,
    today: LocalDate,
    currentHour: Int,
    onHourSelected: (Int) -> Unit
) {
    val hours = (0..23).toList()
    val hoursPerPage = 6
    val pages = hours.chunked(hoursPerPage)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })

    //  Auto-scroll to the page that contains the current hour (only for today)
    LaunchedEffect(selectedDate, today, currentHour) {
        if (selectedDate == today) {
            val initialPage = currentHour / hoursPerPage
            pagerState.scrollToPage(initialPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val row = pages[page]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            row.forEach { hour ->
                val isToday = selectedDate == today
                val isPast = isToday && hour < currentHour
                val isSelected = hour == selectedHour

                val shape = RoundedCornerShape(8.dp)

                val textColor = when {
                    isPast -> Color.Gray
                    isSelected -> Color.White
                    else -> Color.Black
                }
                val borderModifier = if(!isPast)
                    Modifier.dashedBorder(1.dp, Color.Black, shape)
                else Modifier

                val clickableModifier = if (!isPast) {
                    Modifier.clickable { onHourSelected(hour) }
                } else Modifier

                val backgroundColor = when {
                    isSelected -> Color.Black
                    else -> Color.Transparent
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(shape)
                        .background(backgroundColor)
                        .then(borderModifier)
                        .then(clickableModifier)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:00", hour),
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    selectedCategories: List<String>,
    onCategoryChange: (List<String>) -> Unit,
    selectedPlaces: List<String>,
    onPlaceChange: (List<String>) -> Unit,
    nearMe: Boolean,
    onNearMeToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Filter Events", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))
            Text("Sport Categories", fontWeight = FontWeight.Bold)
            val categories = listOf("Badminton", "Pickleball")

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = category in selectedCategories,
                        onClick = {
                            val updated = if (category in selectedCategories)
                                selectedCategories - category
                            else
                                selectedCategories + category
                            onCategoryChange(updated)
                        },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Locations", fontWeight = FontWeight.Bold)
            val places = listOf("Kuala Lumpur", "Petaling Jaya", "Subang Jaya") // extend this

            places.forEach { place ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = place in selectedPlaces,
                        onCheckedChange = {
                            val updated = if (place in selectedPlaces)
                                selectedPlaces - place
                            else
                                selectedPlaces + place
                            onPlaceChange(updated)
                        }
                    )
                    Text(place)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = nearMe, onCheckedChange = { onNearMeToggle(it) })
                Text("Near Me")
            }
        }
    }
}


