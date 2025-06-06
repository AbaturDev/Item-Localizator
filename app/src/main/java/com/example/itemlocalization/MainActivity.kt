@file:Suppress("DEPRECATION")

package com.example.itemlocalization

import android.content.Context
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemlocalization.data.ItemViewModel
import com.example.itemlocalization.ui.theme.ItemLocalizationTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.itemlocalization.data.Item
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.util.Locale
import androidx.core.graphics.createBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager


sealed class Screen {
    object Map : Screen()
    object Add : Screen()
    data class Details(val itemId: Int) : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val startLocation = location?.let { LatLng(it.latitude, it.longitude) }
                ?: LatLng(52.2297, 21.0122)

            setContent {
                ItemLocalizationTheme {
                    AppContent(startLocation = startLocation)
                }
            }
        }
    }
}

@Composable
fun AppContent(startLocation: LatLng) {
    val context = LocalContext.current
    val viewModel: ItemViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    val items by viewModel.items.observeAsState(emptyList())

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) }

    when (val screen = currentScreen) {
        is Screen.Map -> MapScreen(
            items = items,
            onAddClick = { currentScreen = Screen.Add },
            onShowDetails = { item -> currentScreen = Screen.Details(item.id) },
            startLocation = startLocation
        )
        is Screen.Add -> AddItemScreen(
            viewModel = viewModel,
            context = context,
            onItemAdded = { currentScreen = Screen.Map }
        )
        is Screen.Details -> {
            val selectedItem = items.find { it.id == screen.itemId }
            selectedItem?.let {
                ItemDetailsScreen(
                    viewModel = viewModel,
                    item = it,
                    onBack = { currentScreen = Screen.Map }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    items: List<Item>,
    onAddClick: () -> Unit,
    onShowDetails: (Item) -> Unit,
    startLocation: LatLng
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 12f)
    }

    var selectedItem by remember { mutableStateOf<Item?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lokalizator przedmiotów") }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Dodaj przedmiot")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                items.forEach { item ->
                    Marker(
                        state = MarkerState(position = LatLng(item.latitude, item.longitude)),
                        title = item.name,
                        snippet = item.address,
                        icon = bitmapDescriptorFromVector(context = LocalContext.current, resId = R.drawable.custom_marker),
                        onClick = {
                            selectedItem = item
                            true
                        }
                    )
                }
            }
            selectedItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { selectedItem = null },
                    confirmButton = {
                        Button(onClick = {
                            selectedItem?.let { onShowDetails(it) }
                            selectedItem = null
                        }) {
                            Text("Szczegóły")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { selectedItem = null }) {
                            Text("Zamknij")
                        }
                    },
                    title = { Text(
                        text = item.name,
                        textAlign = TextAlign.Center,) },
                    text = { Text("Adres: ${item.address}") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: ItemViewModel,
    context: Context,
    onItemAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Dodaj przedmiot") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adres (np. ulica, miasto)") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Button(
                        onClick = { onItemAdded() },
                    ) {
                        Text("Anuluj")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && address.isNotBlank()) {
                                val latLng = getLocationFromAddress(context, address)
                                if (latLng != null) {
                                    val item = Item(
                                        name = name,
                                        description = description,
                                        address = address,
                                        latitude = latLng.latitude,
                                        longitude = latLng.longitude
                                    )
                                    viewModel.addItem(item)
                                    onItemAdded()
                                    Toast.makeText(context, "Przedmiot dodany", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = "Nie znaleziono lokalizacji dla podanego adresu."
                                }
                            } else {
                                errorMessage = "Wypełnij wymagane pola."
                            }
                        },
                    ) {
                        Text("Dodaj")
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    viewModel: ItemViewModel,
    item: Item,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(item.name) }
    var description by remember { mutableStateOf(item.description) }
    var address by remember { mutableStateOf(item.address) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Usuń \"${item.name}\"", textAlign = TextAlign.Center) },
            text = { Text("Czy na pewno chcesz usunąć ten przedmiot?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(item)
                        showDeleteDialog = false
                        onBack()
                        Toast.makeText(context, "Przedmiot usunięty", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Szczegóły przedmiotu") }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adres") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = "Szerokość geograficzna: ${item.latitude}")
                Text(text = "Długość geograficzna: ${item.longitude}")

                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onBack) {
                        Text("Powrót")
                    }

                    Button(
                        onClick = {
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Usuń", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && address.isNotBlank()) {
                                val latLng = getLocationFromAddress(context, address)
                                if (latLng != null) {
                                    val updatedItem = item.copy(
                                        name = name,
                                        description = description,
                                        address = address,
                                        latitude = latLng.latitude,
                                        longitude = latLng.longitude
                                    )
                                    viewModel.updateItem(updatedItem)
                                    Toast.makeText(context, "Przedmiot zaktualizowany", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = "Nie znaleziono lokalizacji dla podanego adresu."
                                }
                            } else {
                                errorMessage = "Wypełnij wymagane pola."
                            }
                        }
                    ) {
                        Text("Aktualizuj")
                    }
                }
            }
        }
    )
}

fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(strAddress, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val location = addresses[0]
            LatLng(location.latitude, location.longitude)
        } else null
    } catch (_: Exception) {
        null
    }
}

fun bitmapDescriptorFromVector(context: Context, resId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, resId)!!
    val bitmap = createBitmap(120, 120)
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}