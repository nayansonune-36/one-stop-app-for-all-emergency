package com.sentinel.app.feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HolidayVillage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sentinel.app.R
import com.sentinel.app.feature.map.model.Shelter
import com.sentinel.app.feature.map.model.ShelterType
import com.sentinel.app.feature.map.util.MarkerIconGenerator
import com.sentinel.app.ui.theme.DarkNavyCard
import com.sentinel.app.ui.theme.DarkNavySurface
import com.sentinel.app.ui.theme.DividerBorder
import com.sentinel.app.ui.theme.RedPrimary
import com.sentinel.app.ui.theme.TextMuted
import com.sentinel.app.ui.theme.TextWhite
import com.sentinel.app.ui.theme.spacing
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var maplibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var activeMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }

    val isHindi = remember {
        val currentLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        currentLocale.language == "hi"
    }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionChanged(granted)
    }

    // Initial check for location permission
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val granted = hasFine || hasCoarse
        viewModel.onLocationPermissionChanged(granted)

        if (!granted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Location updates listener via FusedLocationProviderClient
    DisposableEffect(uiState.hasLocationPermission) {
        var fusedLocationClient: FusedLocationProviderClient? = null
        var locationCallback: LocationCallback? = null

        if (uiState.hasLocationPermission) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.onLocationUpdated(loc.latitude, loc.longitude)
                    }
                }

                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    10000L
                ).setMinUpdateIntervalMillis(5000L).build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { loc ->
                            viewModel.onLocationUpdated(loc.latitude, loc.longitude)
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                viewModel.onLocationPermissionChanged(false)
            }
        }

        onDispose {
            locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        }
    }

    // Update map markers when filteredShelters or userLocation changes
    LaunchedEffect(maplibreMapInstance, uiState.filteredShelters, uiState.userLocation) {
        val map = maplibreMapInstance ?: return@LaunchedEffect

        // Clear existing shelter markers
        activeMarkers.forEach { map.removeMarker(it) }
        userLocationMarker?.let { map.removeMarker(it) }

        // Add shelter markers
        val newMarkers = mutableListOf<Marker>()
        for (shelter in uiState.filteredShelters) {
            val icon = MarkerIconGenerator.createShelterIcon(context, shelter.shelterType)
            val markerOptions = MarkerOptions()
                .position(LatLng(shelter.lat, shelter.lon))
                .title(shelter.id)
                .snippet(shelter.type)
                .icon(icon)

            val added = map.addMarker(markerOptions)
            newMarkers.add(added)
        }
        activeMarkers = newMarkers

        // Add user location dot marker if available
        uiState.userLocation?.let { (lat, lon) ->
            val userIcon = MarkerIconGenerator.createUserLocationIcon(context)
            val userMarkerOptions = MarkerOptions()
                .position(LatLng(lat, lon))
                .title("user_location")
                .icon(userIcon)
            userLocationMarker = map.addMarker(userMarkerOptions)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapLibre MapView Host
        AndroidView(
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                MapView(ctx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        maplibreMapInstance = map

                        // Load strictly offline local style
                        map.setStyle(Style.Builder().fromUri(uiState.mapStyleUrl)) { _ ->
                            // Center camera on Guwahati metropolitan region
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(26.15, 26.15))
                                .target(LatLng(26.15, 91.75))
                                .zoom(11.2)
                                .build()
                        }

                        // Handle marker clicks
                        map.setOnMarkerClickListener { marker ->
                            if (marker.title == "user_location") {
                                return@setOnMarkerClickListener true
                            }
                            val shelter = uiState.allShelters.firstOrNull { it.id == marker.title }
                            if (shelter != null) {
                                viewModel.onShelterSelected(shelter)
                            }
                            true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bind Android Lifecycle to MapView
        val mapViewRef = remember { mutableStateOf<MapView?>(null) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                val mv = mapViewRef.value ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_START -> mv.onStart()
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE -> mv.onPause()
                    Lifecycle.Event.ON_STOP -> mv.onStop()
                    Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Top Overlay: Nearest Shelter Card & Legend / Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
                .align(Alignment.TopCenter)
        ) {
            // Nearest Shelter Floating Card
            NearestShelterCard(
                nearestShelter = uiState.nearestShelter,
                distanceKm = uiState.nearestDistanceKm,
                hasLocationPermission = uiState.hasLocationPermission,
                isHindi = isHindi,
                onCardClick = { shelter ->
                    viewModel.onShelterSelected(shelter)
                    maplibreMapInstance?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(shelter.lat, shelter.lon), 14.5)
                    )
                }
            )

            Spacer(modifier = Modifier.height(spacing.small))

            // Marker Type Filters & Legend Row
            FilterAndLegendRow(
                selectedFilter = uiState.selectedTypeFilter,
                onSelectFilter = { type -> viewModel.onFilterSelected(type) }
            )

            // Permission rationale banner (if location permission is denied)
            if (!uiState.hasLocationPermission) {
                Spacer(modifier = Modifier.height(spacing.small))
                PermissionBanner(
                    onGrantClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            // Offline mode badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkNavySurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerBorder),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = stringResource(R.string.map_offline_mode_active),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Empty state overlay if registry is empty
        if (uiState.isEmptyRegistry) {
            EmptyRegistryCard(modifier = Modifier.align(Alignment.Center))
        }

        // Floating map controls (Zoom in, Zoom out, Recenter) - bottom right
        Column(
            modifier = Modifier
                .padding(end = spacing.medium, bottom = 80.dp)
                .align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In (+)
            IconButton(
                onClick = { maplibreMapInstance?.animateCamera(CameraUpdateFactory.zoomIn()) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Zoom Out (-)
            IconButton(
                onClick = { maplibreMapInstance?.animateCamera(CameraUpdateFactory.zoomOut()) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Recenter on user button (when user location is available)
            if (uiState.userLocation != null) {
                IconButton(
                    onClick = {
                        uiState.userLocation?.let { (lat, lon) ->
                            maplibreMapInstance?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 14.0)
                            )
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = RedPrimary
                    )
                }
            }
        }

        // Shelter Details Bottom Sheet
        uiState.selectedShelter?.let { selected ->
            ShelterDetailBottomSheet(
                shelter = selected,
                isHindi = isHindi,
                onDismiss = { viewModel.onShelterSelected(null) },
                onFocusOnMap = {
                    maplibreMapInstance?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(selected.lat, selected.lon), 14.5)
                    )
                    viewModel.onShelterSelected(null)
                },
                onCallPhone = { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun NearestShelterCard(
    nearestShelter: Shelter?,
    distanceKm: Double?,
    hasLocationPermission: Boolean,
    isHindi: Boolean,
    onCardClick: (Shelter) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = nearestShelter != null) {
                nearestShelter?.let { onCardClick(it) }
            }
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RedPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.map_nearest_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                if (nearestShelter != null && distanceKm != null) {
                    Text(
                        text = nearestShelter.localizedName(isHindi),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.map_distance_km, distanceKm) + " • " + stringResource(R.string.map_tap_to_view),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                } else if (!hasLocationPermission) {
                    Text(
                        text = stringResource(R.string.map_location_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = stringResource(R.string.map_nearest_calculating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterAndLegendRow(
    selectedFilter: ShelterType?,
    onSelectFilter: (ShelterType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // "All" filter chip
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onSelectFilter(null) },
            label = { Text(stringResource(R.string.shelter_filter_all)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RedPrimary,
                selectedLabelColor = TextWhite,
                selectedLeadingIconColor = TextWhite,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )

        // "Shelter" filter chip
        FilterChip(
            selected = selectedFilter == ShelterType.SHELTER,
            onClick = {
                if (selectedFilter == ShelterType.SHELTER) onSelectFilter(null)
                else onSelectFilter(ShelterType.SHELTER)
            },
            label = { Text(stringResource(R.string.shelter_type_shelter)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFF9800),
                selectedLabelColor = DarkNavySurface,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )

        // "Hospital" filter chip
        FilterChip(
            selected = selectedFilter == ShelterType.HOSPITAL,
            onClick = {
                if (selectedFilter == ShelterType.HOSPITAL) onSelectFilter(null)
                else onSelectFilter(ShelterType.HOSPITAL)
            },
            label = { Text(stringResource(R.string.shelter_type_hospital)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RedPrimary,
                selectedLabelColor = TextWhite,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )

        // "Relief Camp" filter chip
        FilterChip(
            selected = selectedFilter == ShelterType.RELIEF_CAMP,
            onClick = {
                if (selectedFilter == ShelterType.RELIEF_CAMP) onSelectFilter(null)
                else onSelectFilter(ShelterType.RELIEF_CAMP)
            },
            label = { Text(stringResource(R.string.shelter_type_relief_camp)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.HolidayVillage,
                    contentDescription = null,
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF00BCD4),
                selectedLabelColor = DarkNavySurface,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )
    }
}

@Composable
private fun PermissionBanner(onGrantClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkNavyCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = stringResource(R.string.map_permission_rationale),
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.map_permission_grant),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
private fun EmptyRegistryCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerBorder),
        modifier = modifier
            .padding(spacing.large)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                text = stringResource(R.string.map_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = stringResource(R.string.map_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelterDetailBottomSheet(
    shelter: Shelter,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onFocusOnMap: () -> Unit,
    onCallPhone: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkNavyCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large, vertical = spacing.medium)
        ) {
            // Header with facility name and type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shelter.localizedName(isHindi),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val (badgeText, badgeBg) = when (shelter.shelterType) {
                        ShelterType.HOSPITAL -> Pair(
                            stringResource(R.string.shelter_type_hospital),
                            RedPrimary
                        )
                        ShelterType.RELIEF_CAMP -> Pair(
                            stringResource(R.string.shelter_type_relief_camp),
                            Color(0xFF00BCD4)
                        )
                        ShelterType.SHELTER -> Pair(
                            stringResource(R.string.shelter_type_shelter),
                            Color(0xFFFF9800)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBg)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeBg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.map_sheet_close),
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Text(
                    text = shelter.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))

            // Capacity (only if not null)
            if (shelter.capacity != null) {
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = stringResource(R.string.map_sheet_capacity_format, shelter.capacity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            // Verified date
            if (shelter.verified.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.map_sheet_verified_format, shelter.verified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            Spacer(modifier = Modifier.height(spacing.large))

            // Action Buttons: Focus on Map & Call (only if phone is not null)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                OutlinedButton(
                    onClick = onFocusOnMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.map_sheet_focus),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!shelter.phone.isNullOrBlank()) {
                    Button(
                        onClick = { onCallPhone(shelter.phone) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                        Text(
                            text = stringResource(R.string.map_sheet_call_button),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}
