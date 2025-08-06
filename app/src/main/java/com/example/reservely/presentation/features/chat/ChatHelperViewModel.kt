package com.example.reservely.presentation.features.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatHelperViewModel @Inject constructor(
    val chatHelper: ChatHelper
) : ViewModel()
