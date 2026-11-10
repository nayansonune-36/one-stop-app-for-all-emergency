package com.sentinel.app.feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HolidayVillage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
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
    var mapStyleLoaded by remember { mutableStateOf(false) }
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

    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // Initial check for location permission and request if needed
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

    // Listen for GPS location updates via FusedLocationProviderClient with LocationManager fallback
    DisposableEffect(uiState.hasLocationPermission) {
        var fusedLocationClient: FusedLocationProviderClient? = null
        var locationCallback: LocationCallback? = null
        var locationManager: LocationManager? = null
        var locationListener: LocationListener? = null

        if (uiState.hasLocationPermission) {
            // 1. Google Play Services Fused Location Client
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val last = result.lastLocation ?: return
                    viewModel.onLocationUpdated(last.latitude, last.longitude)
                }
            }

            // High accuracy for fast satellite GPS acquisition offline
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .setMinUpdateDistanceMeters(0f)
                .build()

            try {
                // Immediate last known cached location
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.onLocationUpdated(loc.latitude, loc.longitude)
                    }
                }
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            viewModel.onLocationUpdated(loc.latitude, loc.longitude)
                        }
                    }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {}

            // 2. Direct Android LocationManager fallback (100% offline, satellite-native)
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                try {
                    val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val lastPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    val best = lastGps ?: lastNet ?: lastPassive
                    if (best != null) {
                        viewModel.onLocationUpdated(best.latitude, best.longitude)
                    }

                    locationListener = LocationListener { loc ->
                        viewModel.onLocationUpdated(loc.latitude, loc.longitude)
                    }

                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            2000L,
                            0f,
                            locationListener,
                            Looper.getMainLooper()
                        )
                    }
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000L,
                            0f,
                            locationListener,
                            Looper.getMainLooper()
                        )
                    }
                } catch (_: SecurityException) {}
            }
        }

        onDispose {
            locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            locationListener?.let { locationManager?.removeUpdates(it) }
        }
    }

    // Auto-center camera on user location when acquired for the first time
    // Zoom level: 12.0 when near a shelter region; otherwise stay on Guwahati at zoom 12.0 (never all-India zoom)
    LaunchedEffect(uiState.userLocation, maplibreMapInstance, mapStyleLoaded) {
        val userLoc = uiState.userLocation ?: return@LaunchedEffect
        val map = maplibreMapInstance ?: return@LaunchedEffect
        if (!hasCenteredOnUser && mapStyleLoaded) {
            val isNearShelter = uiState.allShelters.any { s ->
                com.sentinel.app.feature.map.util.GeoUtils.calculateDistanceKm(userLoc.first, userLoc.second, s.lat, s.lon) <= 50.0
            }
            if (isNearShelter) {
                hasCenteredOnUser = true
                try {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(userLoc.first, userLoc.second), 12.0),
                        1000
                    )
                } catch (_: Exception) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(userLoc.first, userLoc.second), 12.0))
                }
            }
        }
    }

    // Handle programmatic camera moves (e.g. region chip selection)
    LaunchedEffect(uiState.cameraMoveEvent) {
        val move = uiState.cameraMoveEvent ?: return@LaunchedEffect
        val map = maplibreMapInstance ?: return@LaunchedEffect
        val (coords, zoom) = move
        val (lat, lon) = coords
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom), 800)
        } catch (_: Exception) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom))
        }
        viewModel.onCameraMoveHandled()
    }

    // Update map markers with clustering for overlapping points
    // Gate on mapStyleLoaded to ensure markers are only added after the style is fully loaded.
    LaunchedEffect(mapStyleLoaded, maplibreMapInstance, uiState.filteredShelters, uiState.userLocation) {
        if (!mapStyleLoaded) return@LaunchedEffect
        val map = maplibreMapInstance ?: return@LaunchedEffect

        // Clear existing markers
        activeMarkers.forEach { map.removeMarker(it) }
        userLocationMarker?.let { map.removeMarker(it) }

        // Group filtered shelters by exact coordinate to cluster overlapping points
        val grouped = uiState.filteredShelters.groupBy { Pair(it.lat, it.lon) }
        val newMarkers = mutableListOf<Marker>()

        for ((coords, sheltersAtLoc) in grouped) {
            val (lat, lon) = coords
            val markerOptions: MarkerOptions = if (sheltersAtLoc.size == 1) {
                val shelter = sheltersAtLoc.first()
                val icon = MarkerIconGenerator.createShelterIcon(context, shelter.shelterType, shelter.isApproximate)
                MarkerOptions()
                    .position(LatLng(lat, lon))
                    .title("shelter_${shelter.id}")
                    .snippet(shelter.type)
                    .icon(icon)
            } else {
                // Cluster marker for co-located shelters
                val isApprox = sheltersAtLoc.any { it.isApproximate }
                val clusterIcon = MarkerIconGenerator.createClusterIcon(context, sheltersAtLoc.size, isApprox)
                MarkerOptions()
                    .position(LatLng(lat, lon))
                    .title("cluster_${lat}_${lon}")
                    .snippet("cluster_${sheltersAtLoc.size}")
                    .icon(clusterIcon)
            }

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

    // Remember MapView with explicit lifecycle start & resume so touch gestures,
    // render loop, and gesture detectors are active from initial render.
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            onStart()
            onResume()
        }
    }

    // Bind Android Lifecycle to MapView for foreground/background transitions
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapLibre MapView Host
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        maplibreMapInstance = map

                        // Explicitly enable gestures for panning, zooming, rotation
                        map.uiSettings.apply {
                            isScrollGesturesEnabled = true
                            isZoomGesturesEnabled = true
                            isDoubleTapGesturesEnabled = true
                            isQuickZoomGesturesEnabled = true
                            isRotateGesturesEnabled = true
                            isTiltGesturesEnabled = false
                            isCompassEnabled = false
                            isAttributionEnabled = false
                            isLogoEnabled = false
                        }

                        fun onStyleSuccessfullyLoaded() {
                            mapStyleLoaded = true
                            // Initial camera placement: Guwahati at zoom 12.0 when outside shelter region or unavailable
                            val userLoc = uiState.userLocation
                            val isNearShelter = userLoc != null && uiState.allShelters.any { s ->
                                com.sentinel.app.feature.map.util.GeoUtils.calculateDistanceKm(userLoc.first, userLoc.second, s.lat, s.lon) <= 50.0
                            }
                            val targetCenter = if (isNearShelter && userLoc != null) {
                                hasCenteredOnUser = true
                                LatLng(userLoc.first, userLoc.second)
                            } else {
                                LatLng(26.1445, 91.7362)
                            }
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(targetCenter, 12.0))
                        }

                        fun loadFallbackOfflineStyle() {
                            val fallbackJson = if (uiState.mapStyleJson.isNotBlank()) {
                                uiState.mapStyleJson
                            } else {
                                "{\"version\":8,\"name\":\"SentinelFallback\",\"sources\":{},\"layers\":[{\"id\":\"bg\",\"type\":\"background\",\"paint\":{\"background-color\":\"#0B1220\"}}]}"
                            }
                            try {
                                map.setStyle(Style.Builder().fromJson(fallbackJson)) { _ ->
                                    onStyleSuccessfullyLoaded()
                                }
                            } catch (_: Exception) {
                                mapStyleLoaded = true
                            }
                        }

                        mapView.addOnDidFailLoadingMapListener { _ ->
                            if (!mapStyleLoaded) {
                                loadFallbackOfflineStyle()
                            }
                        }

                        // Primary: OpenFreeMap Liberty style (no API key required)
                        try {
                            map.setStyle(Style.Builder().fromUri(com.sentinel.app.feature.map.data.MapAssetManager.OPENFREEMAP_STYLE_URL)) { _ ->
                                onStyleSuccessfullyLoaded()
                            }
                        } catch (_: Exception) {
                            loadFallbackOfflineStyle()
                        }

                        // Handle marker clicks
                        map.setOnMarkerClickListener { marker ->
                            val title = marker.title ?: return@setOnMarkerClickListener true
                            if (title == "user_location") {
                                return@setOnMarkerClickListener true
                            }

                            if (title.startsWith("cluster_")) {
                                val lat = marker.position.latitude
                                val lon = marker.position.longitude
                                val clusterShelters = uiState.filteredShelters.filter { it.lat == lat && it.lon == lon }
                                viewModel.onClusterSelected(clusterShelters)
                            } else if (title.startsWith("shelter_")) {
                                val shelterId = title.removePrefix("shelter_")
                                val shelter = uiState.allShelters.firstOrNull { it.id == shelterId }
                                if (shelter != null) {
                                    viewModel.onShelterSelected(shelter)
                                }
                            }
                            true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Overlay: Nearest Shelter Card & Region/Type Filters
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
                    maplibreMapInstance?.let { map ->
                        try {
                            map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(shelter.lat, shelter.lon), 14.5),
                                800
                            )
                        } catch (_: Exception) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(shelter.lat, shelter.lon), 14.5))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(spacing.small))

            // State Region Filter Chips
            RegionFilterRow(
                availableStates = uiState.availableStates,
                selectedState = uiState.selectedStateFilter,
                onSelectState = { state -> viewModel.onStateFilterSelected(state) }
            )

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            // Marker Type Filters Row
            TypeFilterRow(
                selectedFilter = uiState.selectedTypeFilter,
                onSelectFilter = { type -> viewModel.onTypeFilterSelected(type) }
            )

            // Location permission rationale banner
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
        }

        // Empty state overlay if registry is empty
        if (uiState.isEmptyRegistry) {
            EmptyRegistryCard(modifier = Modifier.align(Alignment.Center))
        }

        // Floating map controls (Zoom in, Zoom out, Recenter)
        Column(
            modifier = Modifier
                .padding(end = spacing.medium, bottom = 80.dp)
                .align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In (+)
            IconButton(
                onClick = {
                    maplibreMapInstance?.let { map ->
                        try {
                            map.animateCamera(CameraUpdateFactory.zoomIn(), 250)
                        } catch (_: Exception) {
                            map.moveCamera(CameraUpdateFactory.zoomIn())
                        }
                    }
                },
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
                onClick = {
                    maplibreMapInstance?.let { map ->
                        try {
                            map.animateCamera(CameraUpdateFactory.zoomOut(), 250)
                        } catch (_: Exception) {
                            map.moveCamera(CameraUpdateFactory.zoomOut())
                        }
                    }
                },
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

            // Recenter on user button (Always accessible)
            IconButton(
                onClick = {
                    if (!uiState.hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else if (uiState.userLocation != null) {
                        uiState.userLocation?.let { (lat, lon) ->
                            maplibreMapInstance?.let { map ->
                                val target = LatLng(lat, lon)
                                try {
                                    map.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(target, 14.0),
                                        800
                                    )
                                } catch (_: Exception) {
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 14.0))
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.map_locating_you), Toast.LENGTH_SHORT).show()
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
                    contentDescription = stringResource(R.string.settings_perm_location_title),
                    tint = if (uiState.userLocation != null) RedPrimary else TextMuted
                )
            }
        }

        // Co-located Shelters Cluster Bottom Sheet
        uiState.selectedCluster?.let { cluster ->
            ClusterListBottomSheet(
                shelters = cluster,
                isHindi = isHindi,
                onDismiss = { viewModel.onClusterSelected(null) },
                onSelectShelter = { shelter ->
                    viewModel.onShelterSelected(shelter)
                }
            )
        }

        // Shelter Details Bottom Sheet
        uiState.selectedShelter?.let { selected ->
            ShelterDetailBottomSheet(
                shelter = selected,
                isHindi = isHindi,
                onDismiss = { viewModel.onShelterSelected(null) },
                onFocusOnMap = {
                    maplibreMapInstance?.let { map ->
                        try {
                            map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(selected.lat, selected.lon), 14.5),
                                800
                            )
                        } catch (_: Exception) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selected.lat, selected.lon), 14.5))
                        }
                    }
                    viewModel.onShelterSelected(null)
                },
                onCallPhone = { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }
            )
        }

        // OpenFreeMap Attribution
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp),
            color = DarkNavySurface.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 0.dp)
        ) {
            Text(
                text = "OpenFreeMap | © OpenStreetMap contributors",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        text = stringResource(R.string.map_nearest_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionFilterRow(
    availableStates: List<String>,
    selectedState: String?,
    onSelectState: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // "All" state chip
        FilterChip(
            selected = selectedState == null,
            onClick = { onSelectState(null) },
            label = { Text(stringResource(R.string.map_region_all)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
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

        // Dynamic State Chips
        for (stateName in availableStates) {
            FilterChip(
                selected = selectedState.equals(stateName, ignoreCase = true),
                onClick = { onSelectState(stateName) },
                label = { Text(stateName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RedPrimary,
                    selectedLabelColor = TextWhite,
                    containerColor = DarkNavyCard,
                    labelColor = TextWhite
                )
            )
        }
    }
}

@Composable
private fun TypeFilterRow(
    selectedFilter: ShelterType?,
    onSelectFilter: (ShelterType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onSelectFilter(null) },
            label = { Text(stringResource(R.string.shelter_filter_all)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DarkNavySurface,
                selectedLabelColor = TextWhite,
                selectedLeadingIconColor = TextWhite,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )

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
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFF9800),
                selectedLabelColor = DarkNavySurface,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )

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
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RedPrimary,
                selectedLabelColor = TextWhite,
                containerColor = DarkNavyCard,
                labelColor = TextWhite
            )
        )

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
                    modifier = Modifier.size(14.dp)
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
private fun ClusterListBottomSheet(
    shelters: List<Shelter>,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onSelectShelter: (Shelter) -> Unit
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
                .padding(horizontal = spacing.medium, vertical = spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.map_cluster_title, shelters.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.map_sheet_close),
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shelters, key = { it.id }) { shelter ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DividerBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectShelter(shelter)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shelter.localizedName(isHindi),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = shelter.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (shelter.isApproximate) {
                                Spacer(modifier = Modifier.width(spacing.small))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                                ) {
                                    Text(
                                        text = stringResource(R.string.map_badge_approximate),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        // Approximate location badge
                        if (shelter.isApproximate) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = stringResource(R.string.map_badge_approximate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
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

            // State (only if not blank)
            if (shelter.state.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.map_sheet_state_label, shelter.state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            // Capacity (only if not null)
            if (shelter.capacity != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.map_sheet_capacity_format, shelter.capacity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            // Phone / Control Room Label (only if phone is not null)
            if (!shelter.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.small))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shelter.phone,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    if (shelter.isControlRoom) {
                        Spacer(modifier = Modifier.width(spacing.small))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.shelter_label_control_room),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Data Notes (in small text, only if present)
            if (!shelter.dataNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.small))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkNavySurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = stringResource(R.string.map_sheet_notes_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shelter.dataNotes,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }
                }
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

@Composable
private fun MapStatusChip(
    isOnline: Boolean,
    isInsideHomeZone: Boolean,
    homePackStatus: com.sentinel.app.core.maps.HomePackStatus,
    homeZoneName: String
) {
    val (statusText, statusBg, statusColor) = when {
        isInsideHomeZone && homePackStatus is com.sentinel.app.core.maps.HomePackStatus.Downloading -> {
            Triple(
                stringResource(R.string.map_status_downloading, homePackStatus.progressPercent),
                Color(0xFF0284C7).copy(alpha = 0.2f),
                Color(0xFF38BDF8)
            )
        }
        isInsideHomeZone -> {
            Triple(
                stringResource(R.string.map_status_home_ready),
                Color(0xFF10B981).copy(alpha = 0.2f),
                Color(0xFF34D399)
            )
        }
        isOnline -> {
            Triple(
                stringResource(R.string.map_status_online),
                Color(0xFF3B82F6).copy(alpha = 0.2f),
                Color(0xFF60A5FA)
            )
        }
        else -> {
            Triple(
                stringResource(R.string.map_status_offline_coarse),
                Color(0xFFF59E0B).copy(alpha = 0.2f),
                Color(0xFFFBBF24)
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkNavySurface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailedMapUnavailableCard(homeZoneName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkNavySurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Column {
                Text(
                    text = stringResource(R.string.map_offline_unavailable_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.map_offline_unavailable_desc, homeZoneName),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
