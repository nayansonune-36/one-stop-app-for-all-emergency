package com.sentinel.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinel.app.R
import com.sentinel.app.ui.theme.BadgeShape
import com.sentinel.app.ui.theme.ButtonShape
import com.sentinel.app.ui.theme.CardShape
import com.sentinel.app.ui.theme.DarkAmber
import com.sentinel.app.ui.theme.DarkAmberBg
import com.sentinel.app.ui.theme.DarkGreen
import com.sentinel.app.ui.theme.DarkGreenBg
import com.sentinel.app.ui.theme.DarkNavy
import com.sentinel.app.ui.theme.RedPrimary
import com.sentinel.app.ui.theme.RedPrimaryDark
import com.sentinel.app.ui.theme.RedPrimaryLight
import com.sentinel.app.ui.theme.RedSoft
import com.sentinel.app.ui.theme.spacing

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onNavigateToBot: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToFirstAid: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshGreetingAndUser()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.holdProgress,
        label = "sos_hold_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.medium, vertical = spacing.medium)
    ) {
        // Top Header: Brand & Status Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(spacing.small))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.brand_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Chip (Online / Offline)
            StatusChip(isOnline = uiState.isOnline)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Greeting Section (Time of day + Name)
        val greetingPrefix = stringResource(uiState.greetingRes)
        val fullGreeting = if (uiState.userName != null) {
            "$greetingPrefix, ${uiState.userName}"
        } else {
            greetingPrefix
        }

        Text(
            text = fullGreeting,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_headline),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Center SOS Control: 3-Second Hold Ring
        SosHoldRingSection(
            progress = animatedProgress,
            onPressStart = { viewModel.startHolding() },
            onPressEnd = { viewModel.stopHolding() },
            onTapForDialog = { viewModel.showConfirmDialog() }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Section: Essential Tools
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_quick_access).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.home_essential_tools),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(spacing.small))

        // 2x2 Grid of Essential Tools Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            ShortcutCard(
                title = stringResource(R.string.home_shortcut_map_title),
                subtitle = stringResource(R.string.home_shortcut_map_sub),
                icon = Icons.Default.Map,
                onClick = onNavigateToMap,
                modifier = Modifier.weight(1f)
            )
            ShortcutCard(
                title = stringResource(R.string.home_shortcut_medical_title),
                subtitle = stringResource(R.string.home_shortcut_medical_sub),
                icon = Icons.Default.ContactPage,
                onClick = onNavigateToMedical,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            val botSubtitle = if (uiState.isOnline) {
                stringResource(R.string.home_shortcut_bot_sub_online)
            } else {
                stringResource(R.string.home_shortcut_bot_sub_offline)
            }
            ShortcutCard(
                title = stringResource(R.string.home_shortcut_bot_title),
                subtitle = botSubtitle,
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = onNavigateToBot,
                modifier = Modifier.weight(1f)
            )
            ShortcutCard(
                title = stringResource(R.string.home_shortcut_aid_title),
                subtitle = stringResource(R.string.home_shortcut_aid_sub, uiState.totalGuidesCount),
                icon = Icons.Default.MedicalServices,
                onClick = onNavigateToFirstAid,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section: First-aid essentials
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_prepare).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_firstaid_essentials),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextButton(
                onClick = onNavigateToFirstAid,
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_view_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = RedPrimary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.small))

        // Horizontal Row of Mini First-Aid Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            MiniAidCard(
                title = stringResource(R.string.aid_topic_cpr),
                icon = Icons.Default.Favorite,
                onClick = onNavigateToFirstAid
            )
            MiniAidCard(
                title = stringResource(R.string.aid_topic_bleeding),
                icon = Icons.Default.LocalHospital,
                onClick = onNavigateToFirstAid
            )
            MiniAidCard(
                title = stringResource(R.string.aid_topic_burns),
                icon = Icons.Default.Whatshot,
                onClick = onNavigateToFirstAid
            )
            MiniAidCard(
                title = stringResource(R.string.aid_topic_choking),
                icon = Icons.Default.Warning,
                onClick = onNavigateToFirstAid
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Honest Disclaimer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Column {
                    Text(
                        text = stringResource(R.string.home_disclaimer_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.home_disclaimer_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirmation Dialog
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                    onClick = {
                        viewModel.dismissConfirmDialog()
                        onNavigateToSos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = ButtonShape
                ) {
                    Text(
                        text = stringResource(R.string.sos_confirm_yes),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissConfirmDialog() },
                    shape = ButtonShape
                ) {
                    Text(
                        text = stringResource(R.string.sos_confirm_no),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CardShape
        )
    }
}

@Composable
private fun StatusChip(isOnline: Boolean) {
    val bgColor = if (isOnline) DarkGreenBg else DarkAmberBg
    val textColor = if (isOnline) DarkGreen else DarkAmber
    val textRes = if (isOnline) R.string.status_online else R.string.status_offline

    Surface(
        shape = RoundedCornerShape(99.dp),
        color = bgColor,
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(textRes).uppercase(),
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            )
        }
    }
}

@Composable
private fun SosHoldRingSection(
    progress: Float,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onTapForDialog: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .drawBehind {
                    // Background track ring
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.5f),
                        radius = size.minDimension / 2 - 8.dp.toPx(),
                        style = Stroke(width = 8.dp.toPx())
                    )
                    // Progress arc (0 to 360 deg)
                    if (progress > 0f) {
                        drawArc(
                            color = RedPrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                            size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Large circular SOS button
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
                                onPressStart()
                                tryAwaitRelease()
                                onPressEnd()
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

        TextButton(onClick = onTapForDialog) {
            Text(
                text = stringResource(R.string.home_sos_tap_hint),
                style = MaterialTheme.typography.labelMedium,
                color = RedPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MiniAidCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .size(width = 110.dp, height = 90.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(BadgeShape)
                    .background(RedSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
