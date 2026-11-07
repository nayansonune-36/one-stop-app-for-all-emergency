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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun MedicalScreen(
    viewModel: MedicalViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        uiState.errorMessage?.let { code ->
            val msg = when (code) {
                "NAME_REQUIRED" -> context.getString(R.string.medical_name_required)
                "INCORRECT_PIN" -> context.getString(R.string.medical_lock_incorrect_pin)
                else -> code
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.infoMessage?.let { code ->
            val msg = when (code) {
                "SAVED_SUCCESS" -> context.getString(R.string.medical_saved_success)
                "UNLOCKED_SUCCESS" -> context.getString(R.string.medical_unlocked_success)
                "IMPORTED_SUCCESS" -> context.getString(R.string.medical_imported_success)
                else -> code
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    if (!uiState.isUnlocked) {
        // Locked State UI
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
                // Hero Card
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

                        if (uiState.profile.blood.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RedSurface)
                                    .border(1.dp, RedPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = spacing.small, vertical = 4.dp)
                            ) {
                                Text(
                                    text = uiState.profile.blood,
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

                // Profile Fields Form
                MedicalFieldsForm(
                    profile = if (uiState.isEditing) uiState.draftProfile else uiState.profile,
                    isEditing = uiState.isEditing,
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

                    // QR Actions
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

                    Spacer(modifier = Modifier.height(spacing.small))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleQrDialog(true) }
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
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(spacing.medium))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.medical_btn_share_qr),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.medical_btn_share_qr_sub),
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

    // Emergency QR Code Dialog
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
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color.White)
                            .padding(8.dp),
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
    onProfileChange: (MedicalProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FormField(
            label = stringResource(R.string.medical_field_name),
            value = profile.name,
            placeholder = stringResource(R.string.medical_placeholder_name),
            isEditing = isEditing,
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
                    onValueChange = { onProfileChange(profile.copy(age = it)) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormField(
                    label = stringResource(R.string.medical_field_blood),
                    value = profile.blood,
                    placeholder = stringResource(R.string.medical_placeholder_blood),
                    isEditing = isEditing,
                    onValueChange = { onProfileChange(profile.copy(blood = it)) }
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
            value = profile.contactName,
            placeholder = stringResource(R.string.medical_placeholder_name),
            isEditing = isEditing,
            onValueChange = { onProfileChange(profile.copy(contactName = it)) }
        )

        FormField(
            label = stringResource(R.string.medical_field_contact_phone),
            value = profile.contactPhone,
            placeholder = stringResource(R.string.medical_field_contact_phone),
            isEditing = isEditing,
            keyboardType = KeyboardType.Phone,
            onValueChange = { onProfileChange(profile.copy(contactPhone = it)) }
        )

        FormField(
            label = stringResource(R.string.medical_field_notes),
            value = profile.notes,
            placeholder = stringResource(R.string.medical_placeholder_notes),
            isEditing = isEditing,
            singleLine = false,
            minLines = 3,
            onValueChange = { onProfileChange(profile.copy(notes = it)) }
        )
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
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkNavyCard,
                    unfocusedContainerColor = DarkNavyCard,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = DividerBorder
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
