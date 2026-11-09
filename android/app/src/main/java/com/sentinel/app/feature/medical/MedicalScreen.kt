package com.sentinel.app.feature.medical

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.sentinel.app.R
import com.sentinel.app.feature.medical.model.MedicalProfile
import com.sentinel.app.feature.medical.util.BiometricHelper
import com.sentinel.app.ui.theme.DarkNavyBg
import com.sentinel.app.ui.theme.DarkNavyCard
import com.sentinel.app.ui.theme.DarkNavySurface
import com.sentinel.app.ui.theme.DividerBorder
import com.sentinel.app.ui.theme.RedPrimary
import com.sentinel.app.ui.theme.RedSurface
import com.sentinel.app.ui.theme.TextMuted
import com.sentinel.app.ui.theme.TextWhite
import com.sentinel.app.ui.theme.spacing

private val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")

@Composable
fun MedicalScreen(
    viewModel: MedicalViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.errorMessageRes, uiState.infoMessageRes) {
        uiState.errorMessageRes?.let { resId ->
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.infoMessageRes?.let { resId ->
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    if (!uiState.isUnlocked) {
        // Locked State UI requiring biometric/device credential
        MedicalLockScreen(
            pinInput = uiState.pinInput,
            onPinChange = { viewModel.updatePinInput(it) },
            onUnlockWithPin = { viewModel.unlockWithPin() },
            onBiometricUnlock = {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = context.getString(R.string.medical_lock_title),
                        subtitle = context.getString(R.string.medical_lock_desc),
                        onSuccess = { viewModel.unlockBiometric() },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    viewModel.unlockBiometric()
                }
            },
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    } else {
        // Unlocked Medical ID Screen
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkNavyBg)
        ) {
            // Header
            Surface(
                color = DarkNavyCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                text = stringResource(R.string.medical_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.medical_saved_on_device),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (uiState.isEditing) {
                                viewModel.saveDraft()
                            } else {
                                viewModel.startEditing()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isEditing) RedPrimary else DarkNavySurface
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = stringResource(if (uiState.isEditing) R.string.medical_btn_save else R.string.medical_btn_edit),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextWhite
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.medium)
            ) {
                // Hero Profile Card
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(spacing.medium))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.profile.name.ifBlank { stringResource(R.string.medical_placeholder_name) },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.medical_offline_available),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        if (uiState.profile.bloodGroup.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RedSurface)
                                    .border(1.dp, RedPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = spacing.small, vertical = 4.dp)
                            ) {
                                Text(
                                    text = uiState.profile.bloodGroup,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = RedPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.small))

                // Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkNavyCard)
                        .padding(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = stringResource(R.string.medical_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                // Profile Fields Form (Edit or View mode)
                MedicalFieldsForm(
                    profile = if (uiState.isEditing) uiState.draftProfile else uiState.profile,
                    isEditing = uiState.isEditing,
                    nameErrorRes = uiState.nameErrorRes,
                    ageErrorRes = uiState.ageErrorRes,
                    phoneErrorRes = uiState.phoneErrorRes,
                    onProfileChange = { updated ->
                        viewModel.updateDraft { updated }
                    }
                )

                if (uiState.isEditing) {
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Button(
                            onClick = { viewModel.saveDraft() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(stringResource(R.string.medical_btn_save))
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelEditing() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.medical_btn_cancel))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Prominent "Show QR" Button
                    Button(
                        onClick = { viewModel.toggleQrDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(
                            text = stringResource(R.string.medical_btn_show_qr_fullscreen),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.small))

                    // QR Scanner shortcut card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToScanner() }
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
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(spacing.medium))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.medical_btn_open_scanner),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.medical_btn_open_scanner_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                        }
                    }
                }
            }
        }
    }

    // Emergency QR Code Dialog / Full-screen modal
    if (uiState.showQrDialog) {
        Dialog(onDismissRequest = { viewModel.toggleQrDialog(false) }) {
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
                            text = stringResource(R.string.qr_modal_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.toggleQrDialog(false) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.qr_modal_close), tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.small))

                    Text(
                        text = stringResource(R.string.qr_modal_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(spacing.medium))

                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        uiState.qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: CircularProgressIndicator(color = RedPrimary)
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    Button(
                        onClick = { viewModel.toggleQrDialog(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.qr_modal_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalLockScreen(
    pinInput: String,
    onPinChange: (String) -> Unit,
    onUnlockWithPin: () -> Unit,
    onBiometricUnlock: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back), tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(spacing.small))
                Column {
                    Text(
                        text = stringResource(R.string.medical_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.medical_protected_on_device),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkNavyCard)
                    .border(1.dp, DividerBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = stringResource(R.string.medical_lock_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Text(
                text = stringResource(R.string.medical_lock_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.large))

            OutlinedTextField(
                value = pinInput,
                onValueChange = onPinChange,
                placeholder = { Text(stringResource(R.string.medical_lock_pin_placeholder), color = TextMuted) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = DividerBorder
                ),
                modifier = Modifier.width(180.dp)
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Button(
                onClick = onUnlockWithPin,
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                enabled = pinInput.length == 4,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(spacing.small))
                Text(stringResource(R.string.medical_lock_btn_unlock))
            }

            Spacer(modifier = Modifier.height(spacing.small))

            OutlinedButton(
                onClick = onBiometricUnlock,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(spacing.small))
                Text(stringResource(R.string.medical_lock_btn_biometric))
            }
        }
    }
}

@Composable
private fun MedicalFieldsForm(
    profile: MedicalProfile,
    isEditing: Boolean,
    nameErrorRes: Int?,
    ageErrorRes: Int?,
    phoneErrorRes: Int?,
    onProfileChange: (MedicalProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FormField(
            label = stringResource(R.string.medical_field_name),
            value = profile.name,
            placeholder = stringResource(R.string.medical_placeholder_name),
            isEditing = isEditing,
            errorMessage = nameErrorRes?.let { stringResource(it) },
            onValueChange = { onProfileChange(profile.copy(name = it)) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                FormField(
                    label = stringResource(R.string.medical_field_age),
                    value = profile.age,
                    placeholder = stringResource(R.string.medical_placeholder_age),
                    isEditing = isEditing,
                    keyboardType = KeyboardType.Number,
                    errorMessage = ageErrorRes?.let { stringResource(it) },
                    onValueChange = { onProfileChange(profile.copy(age = it)) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BloodGroupSelector(
                    selectedGroup = profile.bloodGroup,
                    isEditing = isEditing,
                    onSelected = { onProfileChange(profile.copy(bloodGroup = it)) }
                )
            }
        }

        FormField(
            label = stringResource(R.string.medical_field_allergies),
            value = profile.allergies,
            placeholder = stringResource(R.string.medical_placeholder_none),
            isEditing = isEditing,
            onValueChange = { onProfileChange(profile.copy(allergies = it)) }
        )

        FormField(
            label = stringResource(R.string.medical_field_conditions),
            value = profile.conditions,
            placeholder = stringResource(R.string.medical_placeholder_none),
            isEditing = isEditing,
            onValueChange = { onProfileChange(profile.copy(conditions = it)) }
        )

        FormField(
            label = stringResource(R.string.medical_field_medications),
            value = profile.medications,
            placeholder = stringResource(R.string.medical_placeholder_meds),
            isEditing = isEditing,
            onValueChange = { onProfileChange(profile.copy(medications = it)) }
        )

        FormField(
            label = stringResource(R.string.medical_field_contact_name),
            value = profile.emergencyContactName,
            placeholder = stringResource(R.string.medical_placeholder_name),
            isEditing = isEditing,
            onValueChange = { onProfileChange(profile.copy(emergencyContactName = it)) }
        )

        FormField(
            label = stringResource(R.string.medical_field_contact_phone),
            value = profile.emergencyContactPhone,
            placeholder = stringResource(R.string.medical_field_contact_phone),
            isEditing = isEditing,
            keyboardType = KeyboardType.Phone,
            errorMessage = phoneErrorRes?.let { stringResource(it) },
            onValueChange = { onProfileChange(profile.copy(emergencyContactPhone = it)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodGroupSelector(
    selectedGroup: String,
    isEditing: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.medical_field_blood),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (isEditing) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGroup.ifBlank { stringResource(R.string.medical_placeholder_blood) },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkNavyCard,
                        unfocusedContainerColor = DarkNavyCard,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DividerBorder
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DarkNavyCard)
                ) {
                    BLOOD_GROUPS.forEach { bg ->
                        DropdownMenuItem(
                            text = { Text(bg, color = TextWhite) },
                            onClick = {
                                onSelected(if (bg == "Unknown") "" else bg)
                                expanded = false
                            }
                        )
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedGroup.ifBlank { stringResource(R.string.medical_placeholder_blood) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedGroup.isBlank()) TextMuted else TextWhite,
                    modifier = Modifier.padding(horizontal = spacing.medium, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    isEditing: Boolean,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = TextMuted) },
                singleLine = singleLine,
                minLines = minLines,
                isError = errorMessage != null,
                supportingText = errorMessage?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkNavyCard,
                    unfocusedContainerColor = DarkNavyCard,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = DividerBorder,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) TextMuted else TextWhite,
                    modifier = Modifier.padding(horizontal = spacing.medium, vertical = 12.dp)
                )
            }
        }
    }
}
