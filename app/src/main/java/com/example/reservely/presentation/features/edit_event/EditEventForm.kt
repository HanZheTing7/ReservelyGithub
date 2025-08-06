package com.example.reservely.presentation.features.edit_event

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reservely.data.model.Event
import com.example.reservely.presentation.components.CreateOrEditEventContent
import com.example.reservely.presentation.components.formatTimestamp
import com.example.reservely.presentation.components.latLngSaver
import com.example.reservely.presentation.features.profile.ProfileViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EditEventForm(placesClient: PlacesClient, event: Event, onEventUpdated: () -> Unit, viewModel: EditEventViewModel = hiltViewModel()) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(event.title) }
    var address by rememberSaveable { mutableStateOf(event.address) }
    var dateTime by rememberSaveable { mutableStateOf(formatTimestamp(event.dateTime)) }
    var maxPeople by rememberSaveable { mutableStateOf(event.maxPeople) }
    var description by rememberSaveable { mutableStateOf(event.description) }
    var selectedCategory by rememberSaveable { mutableStateOf(event.category) }
    var joinAsParticipant by rememberSaveable { mutableStateOf(true) }
    var courtBooked by rememberSaveable { mutableStateOf(event.courtBooked) }

    var selectedDuration by rememberSaveable { mutableStateOf(event.durationMinutes) }
    var showDurationPicker by remember { mutableStateOf(false) }

    val inferredDualPrice = event.isDualPrice || (event.priceMale != null && event.priceFemale != null && event.pricePerPerson == null)
    var isDualPrice by rememberSaveable { mutableStateOf(inferredDualPrice) }

    var priceMale by rememberSaveable {
        mutableStateOf(event.priceMale?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var priceFemale by rememberSaveable {
        mutableStateOf(event.priceFemale?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }

    var pricePerPerson by rememberSaveable {
        mutableStateOf(event.pricePerPerson?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }

    var placeLatLng by rememberSaveable(stateSaver = latLngSaver) { mutableStateOf(event.location?.let { LatLng(it.latitude, it.longitude) }) }


    val profileViewModel: ProfileViewModel = viewModel()
    val userData = profileViewModel.userData.value
    val hostName = userData?.name ?: ""

    // Error flags
    var titleError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var dateTimeError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var priceMaleError by remember { mutableStateOf(false) }
    var priceFemaleError by remember { mutableStateOf(false) }

    LaunchedEffect(event.id) {
        viewModel.checkJoinAsParticipant(event.id)
    }
    joinAsParticipant = viewModel.joinAsParticipant

    //  Reusable for create and edit event screen
    CreateOrEditEventContent(
        isEditing = true,
        title = title,
        onTitleChange = { title = it; titleError = false },
        titleError = titleError,
        address = address,
        placeLatLng = placeLatLng,
        placesClient = placesClient,
        onAddressSelected = {
            address = it.address
            placeLatLng = it.latLng
            addressError = false
        }
        ,
        addressError = addressError,
        dateTime = dateTime,
        onDateTimeChange = { dateTime = it; dateTimeError = false },
        dateTimeError = dateTimeError,
        selectedDuration = selectedDuration,
        onDurationSelected = { selectedDuration = it },
        showDurationPicker = showDurationPicker,
        onShowDurationPickerChange = { showDurationPicker = it },
        maxPeople = maxPeople,
        onMaxPeopleChange = { maxPeople = it },
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it },
        isDualPrice = isDualPrice,
        onPricingModeChange = { isDualPrice = it },
        pricePerPerson = pricePerPerson,
        onPricePerPersonChange = { pricePerPerson = it; priceError = false },
        priceError = priceError,
        priceMale = priceMale,
        onPriceMaleChange = { priceMale = it; priceMaleError = false },
        priceFemale = priceFemale,
        onPriceFemaleChange = { priceFemale = it; priceFemaleError = false },
        priceMaleError = priceMaleError,
        priceFemaleError = priceFemaleError,
        description = description,
        onDescriptionChange = { description = it; descriptionError = false },
        descriptionError = descriptionError,
        joinAsParticipant = joinAsParticipant,
        onJoinAsParticipantChange = { joinAsParticipant = it },
        courtBooked = courtBooked,
        onCourtBookedChange = { courtBooked = it },
        buttonText = "Save Changes",
        onSubmit = {
            val hasError = when {
                title.isBlank() -> { titleError = true; true }
                address.isBlank() -> { addressError = true; true }
                dateTime.isBlank() -> { dateTimeError = true; true }
                !isDualPrice && pricePerPerson.toDoubleOrNull() == null -> { priceError = true; true }
                isDualPrice && priceMale.toDoubleOrNull() == null -> { priceMaleError = true; true }
                isDualPrice && priceFemale.toDoubleOrNull() == null -> { priceFemaleError = true; true }
                description.isBlank() -> { descriptionError = true; true }
                else -> false
            }

            if (hasError) return@CreateOrEditEventContent

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val timestamp = Timestamp(sdf.parse(dateTime) ?: Date())

            val endTime = Calendar.getInstance().apply {
                time = timestamp.toDate()
                add(Calendar.MINUTE, selectedDuration)
            }
            val endTimeMillis = endTime.timeInMillis

            val expireAt = Timestamp(Date(endTimeMillis + 14 * 24 * 60 * 60 * 1000)) // 14 days after event ends

            val updatedEvent = event.copy(
                title = title,
                address = address,
                dateTime = timestamp,
                durationMinutes = selectedDuration,
                maxPeople = maxPeople,
                pricePerPerson = if (isDualPrice) null else pricePerPerson.toDoubleOrNull(),
                priceMale = if (isDualPrice) priceMale.toDoubleOrNull() else null,
                priceFemale = if (isDualPrice) priceFemale.toDoubleOrNull() else null,
                isDualPrice = isDualPrice,
                description = description,
                category = selectedCategory,
                courtBooked = courtBooked,
                expireAt = expireAt
            )

            if (userData != null) {
                viewModel.updateEvent(
                    context = context,
                    event = event,
                    updatedEvent = updatedEvent,
                    joinAsParticipant = joinAsParticipant,
                    hostName = hostName,
                    userData = userData,
                    onSuccess = onEventUpdated
                )
            }
        },
        bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
    )
}
