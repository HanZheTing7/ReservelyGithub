package com.example.reservely.presentation.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.reservely.data.model.SelectedPlace
import com.example.reservely.presentation.features.create_event.AddressAutoCompleteField
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale


@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth(0.8f),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardOptions),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                val desc = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = desc)
                }
            }
        }
    )
}

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun GenderSelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Male", "Female")

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun CloudImagePicker(
    imageUrl: String,
    uploading: Boolean,
    onUploadRequested: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    label: String = "Tap image to change"
) {
    // Use the modern Photo Picker contract. No permissions are needed for this.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onUploadRequested(it) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable {
                    // Launch the Photo Picker, filtering for images only.
                    launcher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (uploading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    onDateTimeSelected: (String) -> Unit
) {
    val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
    val now = ZonedDateTime.now(zoneId)

    val todayStart = now.toLocalDate().atStartOfDay(zoneId)
    val todayMillis = todayStart.toInstant().toEpochMilli()

    val offsetMillis = zoneId.rules.getOffset(now.toInstant()).totalSeconds * 1000L
    val adjustedTodayMillis = todayMillis + offsetMillis
    val maxDateMillis = adjustedTodayMillis + 21 * 24 * 60 * 60 * 1000L



    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = adjustedTodayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in adjustedTodayMillis..maxDateMillis
            }
        }
    )



    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var pickedHour by remember { mutableStateOf(0) }
    var pickedMinute by remember { mutableStateOf(0) }

    Button(onClick = { showDatePicker = true }) {
        Text("Pick Date & Time")
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker && selectedDateMillis != null) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                pickedHour = hour
                pickedMinute = minute

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis!!
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }

                val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(calendar.time)

                onDateTimeSelected(formatted)
            },
            initialHour = pickedHour,
            initialMinute = pickedMinute
        )
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int = 12,
    initialMinute: Int = 0
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
                onDismissRequest()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        title = {
            Text("Select Time")
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}


@Composable
fun SportCategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Badminton", "Pickleball")

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.LightGray.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun DurationPickerDialog(
    onDismiss: () -> Unit,
    onDurationSelected: (Int) -> Unit,
    initialDuration: Int = 120 // default 1 hour
) {
    val durations = remember {
        listOf(
            30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 360, 390, 420, 450, 480, 510
        )
    }

    var selectedIndex by remember { mutableIntStateOf(durations.indexOf(initialDuration).coerceAtLeast(0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDurationSelected(durations[selectedIndex])
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select Duration",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Scrollable duration selector
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(durations) { index, duration ->
                        Text(
                            text = formatDuration(duration),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index }
                                .padding(12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = if (index == selectedIndex) 20.sp else 16.sp,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    )
}

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "$hours hr $mins min"
        hours > 0 -> "$hours hr"
        else -> "$mins min"
    }
}

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

@Composable
fun CreateOrEditEventContent(
    isEditing: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    titleError: Boolean,
    address: String,
    placeLatLng: LatLng?,
    placesClient: PlacesClient,
    onAddressSelected: (SelectedPlace) -> Unit,
    addressError: Boolean,
    dateTime: String,
    onDateTimeChange: (String) -> Unit,
    dateTimeError: Boolean,
    selectedDuration: Int,
    onDurationSelected: (Int) -> Unit,
    showDurationPicker: Boolean,
    onShowDurationPickerChange: (Boolean) -> Unit,
    maxPeople: Int,
    onMaxPeopleChange: (Int) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isDualPrice: Boolean,
    onPricingModeChange: (Boolean) -> Unit,
    pricePerPerson: String,
    onPricePerPersonChange: (String) -> Unit,
    priceError: Boolean,
    priceMale: String,
    onPriceMaleChange: (String) -> Unit,
    priceFemale: String,
    onPriceFemaleChange: (String) -> Unit,
    priceMaleError: Boolean,
    priceFemaleError: Boolean,
    description: String,
    onDescriptionChange: (String) -> Unit,
    descriptionError: Boolean,
    joinAsParticipant: Boolean,
    onJoinAsParticipantChange: (Boolean) -> Unit,
    courtBooked: Boolean,
    onCourtBookedChange: (Boolean) -> Unit,
    buttonText: String,
    onSubmit: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val activeColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(if (isEditing) "Edit Event" else "Create New Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            isError = titleError
        )
        if (titleError) Text("Please enter a title", color = Color.Red, fontSize = 12.sp)

        AddressAutoCompleteField(
            placesClient = placesClient,
            selectedAddress = address,
            onAddressSelected = {
                onAddressSelected(it)
            })
        if (addressError) Text("Please select an address", color = Color.Red, fontSize = 12.sp)

        DateTimePicker { onDateTimeChange(it) }
        if (dateTime.isNotEmpty()) Text("🕒 $dateTime", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (dateTimeError) Text("Please select date & time", color = Color.Red, fontSize = 12.sp)

        Text("Duration: ${formatDuration(selectedDuration)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onShowDurationPickerChange(true) })
        if (showDurationPicker) {
            DurationPickerDialog(
                onDismiss = { onShowDurationPickerChange(false) },
                onDurationSelected = onDurationSelected,
                initialDuration = selectedDuration
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👥 Max People: $maxPeople", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { if (maxPeople > 1) onMaxPeopleChange(maxPeople - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Minus")
            }
            IconButton(onClick = { onMaxPeopleChange(maxPeople + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        SportCategorySelector(selectedCategory, onCategorySelected)

        Text("Pricing Mode", fontWeight = FontWeight.SemiBold)

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPricingModeChange(false) }
                    .padding(vertical = 12.dp)
                    .drawBehind {
                        if (!isDualPrice) {
                            val stroke = 2.dp.toPx()
                            val width = size.width * 0.4f
                            val center = size.width / 2
                            val y = size.height
                            drawLine(
                                activeColor,
                                Offset(center - width / 2, y),
                                Offset(center + width / 2, y),
                                stroke
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Single Price", fontWeight = FontWeight.SemiBold, color = if (!isDualPrice) activeColor else Color.Gray)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPricingModeChange(true) }
                    .padding(vertical = 12.dp)
                    .drawBehind {
                        if (isDualPrice) {
                            val stroke = 2.dp.toPx()
                            val width = size.width * 0.4f
                            val center = size.width / 2
                            val y = size.height
                            drawLine(
                                activeColor,
                                Offset(center - width / 2, y),
                                Offset(center + width / 2, y),
                                stroke
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Dual Price", fontWeight = FontWeight.SemiBold, color = if (isDualPrice) activeColor else Color.Gray)
            }
        }

        if (isDualPrice) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceMale,
                    onValueChange = onPriceMaleChange,
                    label = { Text("M") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp),
                    isError = priceMaleError
                )
                OutlinedTextField(
                    value = priceFemale,
                    onValueChange = onPriceFemaleChange,
                    label = { Text("F") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp),
                    isError = priceFemaleError
                )
            }
        } else {
            OutlinedTextField(
                value = pricePerPerson,
                onValueChange = onPricePerPersonChange,
                label = { Text("Price per Person") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                isError = priceError
            )
        }

        if (priceError) Text("Enter a valid price", color = Color.Red, fontSize = 12.sp)
        if (priceMaleError || priceFemaleError) Text("Enter valid prices for M/F", color = Color.Red, fontSize = 12.sp)

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.heightIn(min = 100.dp),
            isError = descriptionError
        )
        if (descriptionError) Text("Please enter a description", color = Color.Red, fontSize = 12.sp)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onJoinAsParticipantChange(!joinAsParticipant) }) {
            Checkbox(checked = joinAsParticipant, onCheckedChange = onJoinAsParticipantChange)
            Text("Join as Participant", fontWeight = FontWeight.SemiBold)
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCourtBookedChange(!courtBooked) }) {
            Checkbox(checked = courtBooked, onCheckedChange = onCourtBookedChange)
            Text("Court Booked", fontWeight = FontWeight.SemiBold)
        }

        Button(onClick = onSubmit, enabled = title.isNotBlank() && dateTime.isNotBlank(),
            modifier = Modifier
                .padding(
                    bottom = bottomPadding
                )) {
            Text(buttonText)
        }
    }
}

val latLngSaver: Saver<LatLng?, List<Double>> = Saver(
    save = { latLng -> latLng?.let { listOf(it.latitude, it.longitude) } ?: emptyList() },
    restore = { list ->
        if (list.size == 2) LatLng(list[0], list[1]) else null
    }
)

fun getLastKnownLocation(context: Context, onResult: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onResult(null)
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        onResult(location)
    }
}


fun requestNotificationPermissionIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            val activity = context.findActivity()
            if (activity != null) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), 1001)
            }
        }
    }
}

// Extension to get Activity from context
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}






