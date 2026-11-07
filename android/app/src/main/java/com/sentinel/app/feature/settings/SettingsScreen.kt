package com.sentinel.app.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sentinel.app.R
import com.sentinel.app.core.mesh.MeshPermissionHelper
import com.sentinel.app.feature.onboarding.PermissionGroup
import com.sentinel.app.feature.onboarding.PermissionStatus
import com.sentinel.app.ui.theme.DarkNavyBg
import com.sentinel.app.ui.theme.DarkNavyCard
import com.sentinel.app.ui.theme.DarkNavySurface
import com.sentinel.app.ui.theme.DividerBorder
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.RedPrimary
import com.sentinel.app.ui.theme.RedSurface
import com.sentinel.app.ui.theme.TextMuted
import com.sentinel.app.ui.theme.TextWhite
import com.sentinel.app.ui.theme.spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToMedical: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msgKey ->
            val text = when (msgKey) {
                "LANGUAGE_SAVED" -> context.getString(R.string.settings_toast_language_saved)
                "PIN_SAVED" -> context.getString(R.string.settings_toast_pin_saved)
                "PERMISSIONS_REFRESHED" -> context.getString(R.string.settings_toast_permissions_refreshed)
                else -> msgKey
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionResult(PermissionGroup.LOCATION, results.values.any { it })
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(PermissionGroup.CAMERA, granted)
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(PermissionGroup.MICROPHONE, granted)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(PermissionGroup.NOTIFICATIONS, granted)
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionResult(PermissionGroup.BLUETOOTH, results.values.all { it })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        // Header
        Surface(color = DarkNavyCard, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_back),
                        tint = TextWhite
                    )
                }
                Spacer(modifier = Modifier.width(spacing.small))
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Scrollable Settings
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Profile Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(spacing.medium))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.userName ?: stringResource(R.string.medical_placeholder_name),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_profile_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToMedical,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(stringResource(R.string.medical_btn_edit))
                    }
                }
            }

            // General Section
            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                // Language Selection Row
                SettingItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_desc_short)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        FilterChip(
                            selected = uiState.currentLanguage == "en",
                            onClick = { viewModel.switchLanguage("en") },
                            label = { Text("English") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedPrimary,
                                selectedLabelColor = TextWhite
                            )
                        )
                        FilterChip(
                            selected = uiState.currentLanguage == "hi",
                            onClick = { viewModel.switchLanguage("hi") },
                            label = { Text("हिन्दी") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedPrimary,
                                selectedLabelColor = TextWhite
                            )
                        )
                    }
                }

                // Safety Notifications Row
                SettingItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = stringResource(R.string.settings_notifications_desc)
                ) {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = RedPrimary
                        )
                    )
                }
            }

            // Security & Medical ID Section
            SettingsSection(title = stringResource(R.string.settings_section_security)) {
                // Lock Medical ID Row
                SettingItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_lock_title),
                    subtitle = stringResource(R.string.settings_lock_desc)
                ) {
                    Switch(
                        checked = uiState.isMedicalLocked,
                        onCheckedChange = { viewModel.toggleMedicalLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = RedPrimary
                        )
                    )
                }

                // Change PIN Row
                SettingItemClickable(
                    icon = Icons.Default.Pin,
                    title = stringResource(R.string.settings_pin_title),
                    subtitle = stringResource(
                        if (uiState.hasPinSet) R.string.settings_pin_set else R.string.settings_pin_not_set
                    ),
                    onClick = { viewModel.openPinDialog() }
                )
            }

            // Permissions Section
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_permissions),
                        style = MaterialTheme.typography.labelMedium,
                        color = RedPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            viewModel.refreshPermissions()
                            Toast.makeText(context, context.getString(R.string.settings_toast_permissions_refreshed), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = spacing.small)) {
                        PermissionRowItem(
                            icon = Icons.Default.LocationOn,
                            title = stringResource(R.string.settings_perm_location_title),
                            desc = stringResource(R.string.settings_perm_location_desc),
                            status = uiState.locationStatus,
                            onManage = {
                                if (uiState.locationStatus == PermissionStatus.DENIED) {
                                    openAppSettings(context)
                                } else {
                                    locationLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            }
                        )

                        PermissionRowItem(
                            icon = Icons.Default.CameraAlt,
                            title = stringResource(R.string.settings_perm_camera_title),
                            desc = stringResource(R.string.settings_perm_camera_desc),
                            status = uiState.cameraStatus,
                            onManage = {
                                if (uiState.cameraStatus == PermissionStatus.DENIED) {
                                    openAppSettings(context)
                                } else {
                                    cameraLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )

                        PermissionRowItem(
                            icon = Icons.Default.Mic,
                            title = stringResource(R.string.settings_perm_mic_title),
                            desc = stringResource(R.string.settings_perm_mic_desc),
                            status = uiState.micStatus,
                            onManage = {
                                if (uiState.micStatus == PermissionStatus.DENIED) {
                                    openAppSettings(context)
                                } else {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )

                        PermissionRowItem(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.settings_perm_notif_title),
                            desc = stringResource(R.string.settings_perm_notif_desc),
                            status = uiState.notificationsStatus,
                            onManage = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (uiState.notificationsStatus == PermissionStatus.DENIED) {
                                        openAppSettings(context)
                                    } else {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                        )

                        PermissionRowItem(
                            icon = Icons.Default.Bluetooth,
                            title = stringResource(R.string.settings_perm_bluetooth_title),
                            desc = stringResource(R.string.settings_perm_bluetooth_desc),
                            status = uiState.bluetoothStatus,
                            onManage = {
                                if (uiState.bluetoothStatus == PermissionStatus.DENIED) {
                                    openAppSettings(context)
                                } else {
                                    bluetoothLauncher.launch(
                                        MeshPermissionHelper.getRequiredPermissions().toTypedArray()
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Information Section
            SettingsSection(title = stringResource(R.string.settings_section_info)) {
                SettingItemClickable(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = "S.E.N.T.I.N.E.L. v1.0.0",
                    onClick = {
                        Toast.makeText(context, "S.E.N.T.I.N.E.L. v1.0.0 (Offline-First Emergency Response)", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingItemClickable(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.settings_privacy_title),
                    subtitle = stringResource(R.string.settings_privacy_desc),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.settings_privacy_desc), Toast.LENGTH_SHORT).show()
                    }
                )

                SettingItemClickable(
                    icon = Icons.Default.Warning,
                    title = stringResource(R.string.settings_disclaimer_title),
                    subtitle = stringResource(R.string.settings_disclaimer_desc),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.home_disclaimer_body), Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // PIN Setup Dialog
    if (uiState.isPinDialogOpen) {
        Dialog(onDismissRequest = { viewModel.closePinDialog() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.small)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_pin_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.closePinDialog() }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.small))

                    Text(
                        text = stringResource(R.string.settings_pin_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(spacing.medium))

                    OutlinedTextField(
                        value = uiState.pinInput,
                        onValueChange = { viewModel.updatePinInput(it) },
                        placeholder = { Text("••••", color = TextMuted) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = DividerBorder
                        ),
                        modifier = Modifier.width(160.dp)
                    )

                    Spacer(modifier = Modifier.height(spacing.large))

                    Button(
                        onClick = { viewModel.savePin() },
                        enabled = uiState.pinInput.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_pin_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = RedPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = spacing.small)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkNavySurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextWhite, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        action()
    }
}

@Composable
private fun SettingItemClickable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkNavySurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextWhite, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    desc: String,
    status: PermissionStatus,
    onManage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkNavySurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextWhite, fontWeight = FontWeight.SemiBold)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            val (statusText, statusColor) = when (status) {
                PermissionStatus.GRANTED -> Pair(stringResource(R.string.onboarding_status_granted), GreenOnline)
                PermissionStatus.DENIED -> Pair(stringResource(R.string.onboarding_status_denied), RedPrimary)
                PermissionStatus.PROMPT -> Pair(stringResource(R.string.onboarding_status_prompt), TextMuted)
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(spacing.small))

        OutlinedButton(
            onClick = onManage,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = stringResource(
                    if (status == PermissionStatus.DENIED) R.string.settings_perm_settings else R.string.settings_perm_manage
                ),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
