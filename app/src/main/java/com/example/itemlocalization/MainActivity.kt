package com.example.itemlocalization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
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
import androidx.compose.material3.Button
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
import androidx.core.content.ContextCompat
import com.example.itemlocalization.data.Item
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.util.Locale
import androidx.core.graphics.createBitmap


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItemLocalizationTheme {
                val context = this
                val viewModel: ItemViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )
                val items by viewModel.items.observeAsState(emptyList())

                var showAddScreen by remember { mutableStateOf(false) }

                if (showAddScreen) {
                    AddItemScreen(
                        viewModel = viewModel,
                        context = context,
                        onItemAdded = { showAddScreen = false }
                    )
                } else {
                    MapScreen(
                        items = items,
                        onAddClick = { showAddScreen = true }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    items: List<Item>,
    onAddClick: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(52.2297, 21.0122), 12f)
    }
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
                        snippet = item.description,
                        icon = bitmapDescriptorFromVector(context = LocalContext.current, resId = R.drawable.location)
                    )
                }
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

fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(strAddress, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val location = addresses[0]
            LatLng(location.latitude, location.longitude)
        } else null
    } catch (e: Exception) {
        null
    }
}

fun bitmapDescriptorFromVector(context: Context, resId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, resId)!!
    val bitmap = createBitmap(100, 100)
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}