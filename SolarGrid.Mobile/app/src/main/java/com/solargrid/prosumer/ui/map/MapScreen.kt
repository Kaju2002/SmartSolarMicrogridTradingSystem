/*
 * File: MapScreen.kt
 * Description: Google Map with nearby station markers
 */
package com.solargrid.prosumer.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue
import com.solargrid.prosumer.ui.theme.SignInButtonText
import kotlin.math.round

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onBookStation: (stationId: String, stationName: String) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(DEMO_LAT, DEMO_LNG), 12f)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun requestDeviceLocation() {
        if (!hasLocationPermission()) {
            viewModel.onLocationResolved(DEMO_LAT, DEMO_LNG, isDemo = true)
            return
        }
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token,
        ).addOnSuccessListener { location ->
            if (location != null) {
                viewModel.onLocationResolved(location.latitude, location.longitude, isDemo = false)
            } else {
                viewModel.onLocationResolved(DEMO_LAT, DEMO_LNG, isDemo = true)
                Toast.makeText(
                    context,
                    context.getString(R.string.map_using_demo),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }.addOnFailureListener {
            viewModel.onLocationResolved(DEMO_LAT, DEMO_LNG, isDemo = true)
            Toast.makeText(
                context,
                context.getString(R.string.map_using_demo),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestDeviceLocation()
        } else {
            viewModel.onLocationResolved(DEMO_LAT, DEMO_LNG, isDemo = true)
            Toast.makeText(
                context,
                context.getString(R.string.map_using_demo),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission()) {
            requestDeviceLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(state.userLat, state.userLng, state.stations) {
        val target = state.stations.firstOrNull()?.let { s ->
            val lat = s.latitude
            val lng = s.longitude
            if (lat != null && lng != null) LatLng(lat, lng) else null
        } ?: LatLng(state.userLat, state.userLng)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 12f)
    }

    val selected = state.stations.firstOrNull { it.stationId == state.selectedStationId }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission() && !state.usingDemoLocation,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
            ),
        ) {
            state.stations.forEach { station ->
                val lat = station.latitude
                val lng = station.longitude
                if (lat != null && lng != null) {
                    val id = station.stationId.orEmpty()
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = station.stationName ?: "Station",
                        snippet = station.distanceKm?.let {
                            String.format("%.1f km away", it)
                        },
                        onClick = {
                            viewModel.selectStation(id)
                            false
                        },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            MapIconButton(
                onClick = {
                    if (hasLocationPermission()) {
                        requestDeviceLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = stringResource(R.string.map_my_location),
                    tint = PrimaryBlue,
                )
            }
            Spacer(Modifier.height(8.dp))
            MapIconButton(onClick = viewModel::refresh) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.home_refresh),
                        tint = PrimaryBlue,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (!state.error.isNullOrBlank()) {
                Text(
                    text = state.error.orEmpty(),
                    color = Color(0xFFB71C1C),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.usingDemoLocation) {
                Text(
                    text = stringResource(R.string.map_demo_banner),
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(10.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            if (selected != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(16.dp),
                ) {
                    Text(
                        text = selected.stationName ?: "Station",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF111111),
                    )
                    val dist = selected.distanceKm
                    if (dist != null) {
                        Text(
                            text = stringResource(
                                R.string.map_distance_km,
                                (round(dist * 10) / 10.0),
                            ),
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onBookStation(
                                selected.stationId.orEmpty(),
                                selected.stationName.orEmpty(),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = SignInButtonText,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.map_book_here),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
    ) {
        content()
    }
}
