package com.example.reservely.presentation.features.create_event

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reservely.data.model.Event
import com.example.reservely.data.model.ParticipantInfo
import com.example.reservely.data.model.SelectedPlace
import com.example.reservely.presentation.components.CreateOrEditEventContent
import com.example.reservely.presentation.components.latLngSaver
import com.example.reservely.presentation.features.profile.ProfileViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun CreateEventScreen(
    placesClient: PlacesClient,
    onEventCreated: (Event) -> Unit
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val hostId = currentUser?.uid.orEmpty()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val userData = profileViewModel.userData.value
    val hostName = userData?.name ?: ""

    // States
    var title by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var dateTime by rememberSaveable { mutableStateOf("") }
    var maxPeople by rememberSaveable { mutableStateOf(1) }
    var pricePerPerson by rememberSaveable { mutableStateOf("") }
    var priceMale by rememberSaveable { mutableStateOf("") }
    var priceFemale by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var joinAsParticipant by rememberSaveable { mutableStateOf(true) }
    var courtBooked by rememberSaveable { mutableStateOf(false) }
    var isDualPrice by rememberSaveable { mutableStateOf(false) }

    var selectedDuration by rememberSaveable { mutableIntStateOf(120) }
    var showDurationPicker by remember { mutableStateOf(false) }

    // Error states
    var titleError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var dateTimeError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var priceMaleError by remember { mutableStateOf(false) }
    var priceFemaleError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }


    var placeLatLng by rememberSaveable(stateSaver = latLngSaver) {
        mutableStateOf<LatLng?>(null)
    }


    CreateOrEditEventContent(
        isEditing = false,
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
        },
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
        buttonText = "Create",
        onSubmit = {
            // Validation
            val hasError = when {
                title.isBlank() -> { titleError = true; true }
                address.isBlank() -> { addressError = true; true }
                dateTime.isBlank() -> { dateTimeError = true; true }
                !isDualPrice && (pricePerPerson.isBlank() || pricePerPerson.toDoubleOrNull() == null) -> { priceError = true; true }
                isDualPrice && (priceMale.isBlank() || priceMale.toDoubleOrNull() == null) -> { priceMaleError = true; true }
                isDualPrice && (priceFemale.isBlank() || priceFemale.toDoubleOrNull() == null) -> { priceFemaleError = true; true }
                description.isBlank() -> { descriptionError = true; true }
                else -> false
            }
            if (hasError) return@CreateOrEditEventContent

            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val timestamp = Timestamp(sdf.parse(dateTime) ?: Date())

                if (placeLatLng == null) {
                    Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
                    return@CreateOrEditEventContent
                }

                val endTime = Calendar.getInstance().apply {
                    time = timestamp.toDate()
                    add(Calendar.MINUTE, selectedDuration)
                }
                val endTimeMillis = endTime.timeInMillis
                val expireAt = Timestamp(Date(endTimeMillis + 14 * 24 * 60 * 60 * 1000)) // 14 days after event ends


                val eventId = FirebaseFirestore.getInstance()
                    .collection("events")
                    .document()
                    .id

                val event = Event(
                    id = eventId,
                    title = title,
                    address = address,
                    dateTime = timestamp,
                    durationMinutes = selectedDuration,
                    maxPeople = maxPeople,
                    pricePerPerson = if (!isDualPrice) pricePerPerson.toDoubleOrNull() else null,
                    priceMale = if (isDualPrice) priceMale.toDoubleOrNull() else null,
                    priceFemale = if (isDualPrice) priceFemale.toDoubleOrNull() else null,
                    isDualPrice = isDualPrice,
                    description = description,
                    category = selectedCategory,
                    hostId = hostId,
                    hostName = hostName,
                    courtBooked = courtBooked,
                    location = placeLatLng.let { it?.let { it1 -> GeoPoint(it1.latitude, it.longitude) } },
                    endTimeMillis = endTimeMillis,
                    chatFrozen = false,
                    expireAt = expireAt
                )

                FirebaseFirestore.getInstance()
                    .collection("events")
                    .document(eventId)
                    .set(event)
                    .addOnSuccessListener { docRef ->
                        if (joinAsParticipant && currentUser != null) {
                            val participant = ParticipantInfo(
                                userId = currentUser.uid,
                                userName = hostName,
                                profileImageUrl = userData?.profileImageUrl ?: "",
                                joinedAt = Timestamp.now()
                            )
                            FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId)
                                .collection("participants")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    if (!doc.exists()) {
                                        FirebaseFirestore.getInstance()
                                            .collection("events")
                                            .document(eventId)
                                            .collection("participants")
                                            .document(currentUser.uid)
                                            .set(participant)
                                    }
                                }
                        }
                        Toast.makeText(context, "Event created", Toast.LENGTH_SHORT).show()
                        onEventCreated(event)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to create event", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}



@Composable
fun AddressAutoCompleteField(
    placesClient: PlacesClient,
    selectedAddress: String,
    onAddressSelected: (SelectedPlace) -> Unit
) {
    var input by remember { mutableStateOf(selectedAddress) }
    var predictions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var shouldFetchPrediction by remember { mutableStateOf(false) }

    LaunchedEffect(input) {
        if (shouldFetchPrediction && input.isNotEmpty()) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(input)
                .setCountries(listOf("MY"))
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    predictions = response.autocompletePredictions
                }
                .addOnFailureListener {
                    predictions = emptyList()
                }
        } else {
            predictions = emptyList()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                shouldFetchPrediction = true
                if (it.isBlank()) predictions = emptyList()
            },
            label = { Text("Select location") }
        )

        if (predictions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(top = 4.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    predictions.take(5).forEach { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(),
                            modifier = Modifier
                                .clickable {
                                    // Fetch place details
                                    val placeId = prediction.placeId
                                    val fields = listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)

                                    val placeRequest = FetchPlaceRequest.builder(placeId, fields).build()
                                    placesClient.fetchPlace(placeRequest)
                                        .addOnSuccessListener { response ->
                                            val place = response.place
                                            val latLng = place.latLng
                                            if (latLng != null) {
                                                input = place.address ?: ""
                                                predictions = emptyList()
                                                shouldFetchPrediction = false
                                                onAddressSelected(SelectedPlace(address = input, latLng = latLng))
                                            }
                                        }
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}




