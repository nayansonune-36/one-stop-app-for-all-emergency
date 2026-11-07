package com.sentinel.app.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sentinel.app.R
import com.sentinel.app.core.mesh.MeshPermissionHelper
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
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Refresh permissions when returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.onPermissionResult(PermissionGroup.LOCATION, granted)
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
        val granted = results.values.all { it }
        viewModel.onPermissionResult(PermissionGroup.BLUETOOTH, granted)
    }

    val finishOnboarding = {
        viewModel.completeOnboarding()
        onComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.medium))

        // Shield Icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(RedSurface)
                .border(1.dp, RedPrimary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        Text(
            text = stringResource(R.string.onboarding_eyebrow),
            style = MaterialTheme.typography.labelMedium,
            color = RedPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Text(
            text = stringResource(R.string.onboarding_desc),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = spacing.small)
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Permissions List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            PermissionRow(
                icon = Icons.Default.LocationOn,
                text = stringResource(R.string.onboarding_perm_location),
                status = uiState.locationStatus,
                onRequest = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionRow(
                icon = Icons.Default.CameraAlt,
                text = stringResource(R.string.onboarding_perm_camera),
                status = uiState.cameraStatus,
                onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionRow(
                icon = Icons.Default.Mic,
                text = stringResource(R.string.onboarding_perm_mic),
                status = uiState.micStatus,
                onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionRow(
                icon = Icons.Default.Notifications,
                text = stringResource(R.string.onboarding_perm_notifications),
                status = uiState.notificationsStatus,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionRow(
                icon = Icons.Default.Bluetooth,
                text = stringResource(R.string.onboarding_perm_bluetooth),
                status = uiState.bluetoothStatus,
                onRequest = {
                    bluetoothLauncher.launch(
                        MeshPermissionHelper.getRequiredPermissions().toTypedArray()
                    )
                },
                onOpenSettings = { openAppSettings(context) }
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        // Actions
        Button(
            onClick = finishOnboarding,
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_btn_continue),
                style = MaterialTheme.typography.labelLarge,
                color = TextWhite
            )
        }

        Spacer(modifier = Modifier.height(spacing.small))

        OutlinedButton(
            onClick = finishOnboarding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_btn_skip),
                style = MaterialTheme.typography.labelLarge,
                color = TextWhite
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    text: String,
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkNavySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                val statusText = when (status) {
                    PermissionStatus.GRANTED -> stringResource(R.string.onboarding_status_granted)
                    PermissionStatus.DENIED -> stringResource(R.string.onboarding_status_denied)
                    PermissionStatus.PROMPT -> stringResource(R.string.onboarding_status_prompt)
                }
                val statusColor = when (status) {
                    PermissionStatus.GRANTED -> GreenOnline
                    PermissionStatus.DENIED -> RedPrimary
                    PermissionStatus.PROMPT -> TextMuted
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(spacing.small))

            when (status) {
                PermissionStatus.GRANTED -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GreenOnline.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GreenOnline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                PermissionStatus.DENIED -> {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_btn_settings),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                PermissionStatus.PROMPT -> {
                    Button(
                        onClick = onRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_btn_request),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextWhite
                        )
                    }
                }
            }
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
