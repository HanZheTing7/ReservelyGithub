package com.example.reservely.presentation.features.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reservely.R
import com.example.reservely.data.model.OnboardingPage
import com.example.reservely.presentation.features.explore.HomeViewModel
import kotlinx.coroutines.launch

val onboardingPages = listOf(
    OnboardingPage(R.drawable.discover, "Discover Events", "Find sports and activities nearby."),
    OnboardingPage(R.drawable.join, "Join Easily", "Request to join or withdraw with one tap."),
    OnboardingPage(R.drawable.handshake, "Connect & Play", "Meet new friends through sports.")
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val onboardingPage = onboardingPages[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(painterResource(onboardingPage.imageRes), contentDescription = null, modifier = Modifier.size(100.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(onboardingPage.title, style = MaterialTheme.typography.headlineMedium)
                Text(onboardingPage.description, textAlign = TextAlign.Center)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = pagerState.currentPage < onboardingPages.size - 1) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        onFinished()
                    }) {
                    Text("Skip")
                }
            }

            // This Spacer ensures the indicator stays centered
            // when the "Skip" button is invisible.
            if (pagerState.currentPage == onboardingPages.size - 1) {
                Spacer(Modifier.weight(1f))
            }

            OnboardingPagerIndicator(
                pagerState = pagerState, modifier = Modifier.weight(1f)
            )

            Button(onClick = {
                viewModel.completeOnboarding()
                if (pagerState.currentPage == onboardingPages.size - 1) {
                    onFinished()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            }) {
                Text(if (pagerState.currentPage == onboardingPages.lastIndex) "Done" else "Next")
            }
        }
    }
}


@Composable
fun OnboardingPagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(8.dp)
            )
        }
    }
}