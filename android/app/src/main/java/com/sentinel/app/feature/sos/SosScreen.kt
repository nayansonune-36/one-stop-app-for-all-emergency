package com.sentinel.app.feature.sos

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sentinel.app.R
import com.sentinel.app.core.mesh.MeshPermissionHelper
import com.sentinel.app.ui.theme.ButtonShape
import com.sentinel.app.ui.theme.CardShape
import com.sentinel.app.ui.theme.DarkAmber
import com.sentinel.app.ui.theme.DarkAmberBg
import com.sentinel.app.ui.theme.DarkGreen
import com.sentinel.app.ui.theme.DarkGreenBg
import com.sentinel.app.ui.theme.RedPrimary
import com.sentinel.app.ui.theme.RedPrimaryDark
import com.sentinel.app.ui.theme.RedPrimaryLight
import com.sentinel.app.ui.theme.RedSoft
import com.sentinel.app.ui.theme.spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SosScreen(
    viewModel: SosViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsResult()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshHardwareStates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(spacing.small))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { viewModel.toggleDebugPanel() }
                    )
            ) {
                Text(
                    text = stringResource(R.string.screen_sos_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.debug_long_press_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Hardware Disabled Prompts
        if (!uiState.isBluetoothEnabled) {
            HardwareDisabledCard(
                icon = Icons.Default.BluetoothDisabled,
                title = stringResource(R.string.sos_bluetooth_disabled_title),
                desc = stringResource(R.string.sos_bluetooth_disabled_desc),
                buttonText = stringResource(R.string.sos_enable_bluetooth_button),
                onButtonClick = {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(spacing.small))
        }

        if (!uiState.isLocationEnabled) {
            HardwareDisabledCard(
                icon = Icons.Default.LocationOff,
                title = stringResource(R.string.sos_location_disabled_title),
                desc = stringResource(R.string.sos_location_disabled_desc),
                buttonText = stringResource(R.string.sos_enable_location_button),
                onButtonClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(spacing.small))
        }

        // Permission rationale card
        if (!uiState.hasPermissions) {
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = DarkAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(
                            text = stringResource(R.string.permission_required_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = stringResource(R.string.permission_required_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Button(
                        onClick = {
                            permissionLauncher.launch(MeshPermissionHelper.getRequiredPermissions().toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.permission_grant_button),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Main Dashboard: Active SOS State vs Idle State
        if (uiState.isBroadcasting) {
            ActiveSosDashboard(
                uiState = uiState,
                onCancelClick = { viewModel.openCancelDialog() }
            )
        } else {
            IdleSosHoldRing(
                onTriggerConfirmed = { viewModel.triggerSos() },
                onShowConfirm = { viewModel.showConfirmDialog() }
            )
        }

        Spacer(modifier = Modifier.height(spacing.huge))
    }

    // Confirm Dialog
    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.sos_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.sos_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.triggerSos() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = ButtonShape
                ) {
                    Text(
                        text = stringResource(R.string.sos_confirm_yes),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissConfirmDialog() },
                    shape = ButtonShape
                ) {
                    Text(text = stringResource(R.string.sos_confirm_no))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CardShape
        )
    }

    // Cancel Dialog
    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.StopCircle,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.sos_cancel_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.sos_cancel_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelSos() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = ButtonShape
                ) {
                    Text(
                        text = stringResource(R.string.sos_cancel_dialog_confirm),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissCancelDialog() },
                    shape = ButtonShape
                ) {
                    Text(text = stringResource(R.string.sos_cancel_dialog_keep))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CardShape
        )
    }

    // Debug Panel Dialog
    if (uiState.showDebugPanel) {
        SosDebugPanelDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissDebugPanel() }
        )
    }
}

@Composable
private fun ActiveSosDashboard(
    uiState: SosUiState,
    onCancelClick: () -> Unit
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top badges: SOS ACTIVE & Peers Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = RedPrimary,
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sos_active_badge),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (uiState.peersCount > 0) DarkGreenBg else DarkAmberBg,
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (uiState.peersCount > 0) DarkGreen else DarkAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.sos_peers_connected, uiState.peersCount),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (uiState.peersCount > 0) DarkGreen else DarkAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))

            // Timer Block
            Text(
                text = stringResource(R.string.sos_timer_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val minutes = uiState.elapsedSeconds / 60
            val seconds = uiState.elapsedSeconds % 60
            val timerFormatted = String.format("%02d:%02d", minutes, seconds)
            Text(
                text = timerFormatted,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.sos_hops_label, uiState.hops),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.large))

            // Status Row 1: Location Sharing
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Column {
                        Text(
                            text = stringResource(R.string.sos_location_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.locationUnavailable || uiState.lastKnownLat == null) {
                            Text(
                                text = stringResource(R.string.sos_location_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            val coordText = String.format(
                                "%.4f° N, %.4f° E",
                                uiState.lastKnownLat,
                                uiState.lastKnownLon
                            )
                            Text(
                                text = coordText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))

            // Status Row 2: Communication Status
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        when (uiState.statusStep) {
                            SosStatusStep.SEARCHING -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = RedPrimary
                            )
                            SosStatusStep.SENT_TO_DEVICES -> Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = DarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            SosStatusStep.DELIVERED_SERVER -> Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = DarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Column {
                        Text(
                            text = stringResource(R.string.sos_comm_status_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val statusText = when (uiState.statusStep) {
                            SosStatusStep.SEARCHING -> stringResource(R.string.sos_status_searching)
                            SosStatusStep.SENT_TO_DEVICES -> stringResource(
                                R.string.sos_status_sent_to_devices,
                                uiState.sentDeviceCount
                            )
                            SosStatusStep.DELIVERED_SERVER -> stringResource(R.string.sos_status_delivered_server)
                            SosStatusStep.IDLE -> stringResource(R.string.sos_queued_locally)
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))

            // Cancel Button
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.sos_button_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun IdleSosHoldRing(
    onTriggerConfirmed: () -> Unit,
    onShowConfirm: () -> Unit
) {
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val outlineColor = MaterialTheme.colorScheme.outline

    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress,
        label = "sos_idle_hold_progress"
    )

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, outlineColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .drawBehind {
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.5f),
                            radius = size.minDimension / 2 - 8.dp.toPx(),
                            style = Stroke(width = 8.dp.toPx())
                        )
                        if (animatedProgress > 0f) {
                            drawArc(
                                color = RedPrimary,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                                size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .shadow(elevation = 16.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(RedPrimaryLight, RedPrimary, RedPrimaryDark)
                            )
                        )
                        .border(6.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holdJob?.cancel()
                                    holdJob = scope.launch {
                                        val totalSteps = 60
                                        val stepDelay = 3000L / totalSteps
                                        for (step in 1..totalSteps) {
                                            delay(stepDelay)
                                            holdProgress = step.toFloat() / totalSteps
                                        }
                                        onTriggerConfirmed()
                                        holdProgress = 0f
                                    }
                                    tryAwaitRelease()
                                    holdJob?.cancel()
                                    holdProgress = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.nav_sos),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.sos_press_and_hold),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.home_sos_hold_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = onShowConfirm) {
                Text(
                    text = stringResource(R.string.home_sos_tap_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HardwareDisabledCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkAmber.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DarkAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = DarkAmber),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun SosDebugPanelDialog(
    uiState: SosUiState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            Column(modifier = Modifier.padding(spacing.medium)) {
                Text(
                    text = stringResource(R.string.debug_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(spacing.small))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = stringResource(R.string.debug_peers_header) + " (${uiState.peersCount})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState.connectedPeers.isEmpty()) {
                            Text(
                                text = stringResource(R.string.debug_empty_peers),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.connectedPeers.forEach { peer ->
                                Text(
                                    text = "• $peer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.small))
                    }

                    item {
                        Text(
                            text = stringResource(R.string.debug_seen_ids_header, uiState.seenMessageIds.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState.seenMessageIds.isEmpty()) {
                            Text(
                                text = stringResource(R.string.debug_empty_seen),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.seenMessageIds.forEach { id ->
                                Text(
                                    text = "• ${id.take(8)}…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.small))
                    }

                    item {
                        Text(
                            text = stringResource(R.string.debug_message_log_header),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState.packetLogs.isEmpty()) {
                            Text(
                                text = stringResource(R.string.debug_empty_log),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(uiState.packetLogs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.small))

                Button(
                    onClick = onDismiss,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.debug_close_button))
                }
            }
        }
    }
}
