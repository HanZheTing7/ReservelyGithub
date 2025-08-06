package com.example.reservely.presentation.features.joined_event

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.reservely.presentation.features.explore.EventCard


@Composable
fun JoinedEventsScreen(
    modifier: Modifier = Modifier,
    viewModel: JoinedEventsViewModel = hiltViewModel(),
    navController: NavController,
) {

    // Reload on focus (when screen becomes visible again)
    LaunchedEffect(Unit) {
        viewModel.loadJoinedEvents()
    }

    val upcoming by viewModel.upcomingEvents.collectAsState()
    val past by viewModel.pastEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(upcoming, past) {
        val allEvents = (upcoming + past).map { it.event }
        viewModel.observeHostNamesFor(allEvents)
    }

    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            //  UPCOMING SECTION
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upcoming Events",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                val upcomingPagerState = rememberPagerState(pageCount = { upcoming.size })

                if (upcoming.isNotEmpty()) {
                    HorizontalPager(
                        state = upcomingPagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .height(420.dp)
                    ) { page ->
                        val eventData = upcoming[page]
                        val hostName = viewModel.hostNameMap.collectAsState().value[eventData.event.hostId]
                            ?: eventData.event.hostName
                        EventCard(
                            event = eventData.event,
                            participantCount = eventData.participantCount,
                            hostName = hostName,
                            onClick = {
                                navController.navigate("eventDetail/${eventData.event.id}")
                            },
                            imageHeightRatio = 0.08f
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No upcoming events joined.")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            //  PAST SECTION
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Past Events",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                val pastPagerState = rememberPagerState(pageCount = { past.size })

                if (past.isNotEmpty()) {
                    HorizontalPager(
                        state = pastPagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .height(420.dp)
                    ) { page ->
                        val eventData = past[page]
                        val hostName = viewModel.hostNameMap.collectAsState().value[eventData.event.hostId]
                            ?: eventData.event.hostName
                        EventCard(
                            event = eventData.event,
                            participantCount = eventData.participantCount,
                            hostName = hostName,
                            onClick = {
                                navController.navigate("eventDetail/${eventData.event.id}")
                            },
                            imageHeightRatio = 0.08f
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("You haven't joined any events yet.")
                    }
                }
            }
        }
    }
}





