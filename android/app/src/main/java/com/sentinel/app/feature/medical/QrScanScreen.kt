package com.sentinel.app.feature.medical

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.sentinel.app.R
import com.sentinel.app.feature.medical.model.MedicalProfile
import com.sentinel.app.feature.medical.util.MedicalQrParser
import com.sentinel.app.ui.theme.DarkNavyBg
import com.sentinel.app.ui.theme.DarkNavyCard
import com.sentinel.app.ui.theme.DividerBorder
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.RedPrimary
import com.sentinel.app.ui.theme.RedSurface
import com.sentinel.app.ui.theme.TextMuted
import com.sentinel.app.ui.theme.TextWhite
import com.sentinel.app.ui.theme.spacing
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(
    onNavigateBack: () -> Unit,
    onProfileImported: (MedicalProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var scannedProfile by remember { mutableStateOf<MedicalProfile?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        // Top Header
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
                    Text(
                        text = stringResource(R.string.qr_scan_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasCameraPermission && scannedProfile == null) {
                    IconButton(
                        onClick = {
                            val nextTorch = !torchEnabled
                            torchEnabled = nextTorch
                            camera?.cameraControl?.enableTorch(nextTorch)
                        }
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = stringResource(
                                if (torchEnabled) R.string.qr_torch_off else R.string.qr_torch_on
                            ),
                            tint = if (torchEnabled) Color(0xFFFACC15) else TextWhite
                        )
                    }
                }
            }
        }

        if (!hasCameraPermission) {
            // Permission needed state
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
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                Text(
                    text = stringResource(R.string.qr_camera_permission_needed),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(spacing.small))

                Text(
                    text = stringResource(R.string.qr_camera_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.large))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(stringResource(R.string.qr_allow_camera))
                }
            }
        } else if (scannedProfile != null) {
            // Review state for scanned profile
            val profile = scannedProfile!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.medium)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GreenOnline)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GreenOnline.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = GreenOnline,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(spacing.small))
                            Column {
                                Text(
                                    text = stringResource(R.string.qr_found_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.qr_found_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        ProfileDetailRow(stringResource(R.string.medical_field_name), profile.name)
                        ProfileDetailRow(stringResource(R.string.medical_field_age), profile.age)
                        ProfileDetailRow(stringResource(R.string.medical_field_blood), profile.blood)
                        ProfileDetailRow(stringResource(R.string.medical_field_allergies), profile.allergies)
                        ProfileDetailRow(stringResource(R.string.medical_field_conditions), profile.conditions)
                        ProfileDetailRow(stringResource(R.string.medical_field_medications), profile.medications)
                        ProfileDetailRow(stringResource(R.string.medical_field_contact_name), profile.contactName)
                        ProfileDetailRow(stringResource(R.string.medical_field_contact_phone), profile.contactPhone)
                        ProfileDetailRow(stringResource(R.string.medical_field_notes), profile.notes)

                        Spacer(modifier = Modifier.height(spacing.large))

                        Button(
                            onClick = { onProfileImported(profile) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(stringResource(R.string.qr_btn_import))
                        }

                        Spacer(modifier = Modifier.height(spacing.small))

                        OutlinedButton(
                            onClick = {
                                scannedProfile = null
                                scanError = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(stringResource(R.string.qr_btn_scan_again))
                        }
                    }
                }
            }
        } else {
            // Live CameraX Scanner
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CameraPreviewView(
                    onBarcodeScanned = { rawValue ->
                        val parsed = MedicalQrParser.parse(rawValue)
                        if (parsed != null) {
                            scannedProfile = parsed
                            scanError = null
                        } else {
                            scanError = context.getString(R.string.qr_invalid_code)
                        }
                    },
                    onCameraReady = { cam ->
                        camera = cam
                    }
                )

                // Viewfinder Frame Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(2.dp, RedPrimary, RoundedCornerShape(16.dp))
                    )
                }

                // Top Hint
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = spacing.medium)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = spacing.medium, vertical = spacing.small)
                ) {
                    Text(
                        text = stringResource(R.string.qr_scan_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )
                }

                // Error Message if invalid QR was detected
                scanError?.let { err ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(spacing.medium)
                            .background(RedSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, RedPrimary, RoundedCornerShape(8.dp))
                            .padding(spacing.small)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { scanError = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Privacy Notice
            Surface(
                color = DarkNavyCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = stringResource(R.string.qr_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraPreviewView(
    onBarcodeScanned: (String) -> Unit,
    onCameraReady: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var isAnalyzing by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy, isAnalyzing) { rawValue ->
                        onBarcodeScanned(rawValue)
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraReady(cam)
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    isEnabled: Boolean,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null && isEnabled) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val raw = barcode.rawValue
                    if (!raw.isNullOrBlank()) {
                        onSuccess(raw)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
