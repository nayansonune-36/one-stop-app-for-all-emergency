package com.sentinel.app.feature.medical

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.sentinel.app.R
import com.sentinel.app.feature.medical.model.MedicalProfile
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
    modifier: Modifier = Modifier,
    viewModel: QrScanViewModel = viewModel(factory = QrScanViewModel.Factory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
    }

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

                if (uiState.hasCameraPermission && uiState.scannedProfile == null) {
                    IconButton(
                        onClick = {
                            val newTorch = viewModel.toggleTorch()
                            cameraInstance?.cameraControl?.enableTorch(newTorch)
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = stringResource(
                                if (uiState.isTorchOn) R.string.qr_torch_off else R.string.qr_torch_on
                            ),
                            tint = if (uiState.isTorchOn) Color(0xFFFACC15) else TextWhite
                        )
                    }
                }
            }
        }

        if (!uiState.hasCameraPermission) {
            // Permission needed / denied state
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
                    text = if (uiState.permissionRequested) {
                        stringResource(R.string.qr_camera_permission_denied_help)
                    } else {
                        stringResource(R.string.qr_camera_permission_desc)
                    },
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
        } else if (uiState.scannedProfile != null) {
            // Read-only view for scanned profile with clear "Scanned profile" banner
            val profile = uiState.scannedProfile!!
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
                        // Prominent "Scanned Profile" Banner
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GreenOnline.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = GreenOnline,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(spacing.small))
                            Column {
                                Text(
                                    text = stringResource(R.string.qr_scanned_profile_banner),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GreenOnline,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.qr_scanned_profile_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        ProfileDetailRow(stringResource(R.string.medical_field_name), profile.name)
                        ProfileDetailRow(stringResource(R.string.medical_field_age), profile.age)
                        ProfileDetailRow(stringResource(R.string.medical_field_blood), profile.bloodGroup)
                        ProfileDetailRow(stringResource(R.string.medical_field_allergies), profile.allergies)
                        ProfileDetailRow(stringResource(R.string.medical_field_conditions), profile.conditions)
                        ProfileDetailRow(stringResource(R.string.medical_field_medications), profile.medications)
                        ProfileDetailRow(stringResource(R.string.medical_field_contact_name), profile.emergencyContactName)
                        ProfileDetailRow(stringResource(R.string.medical_field_contact_phone), profile.emergencyContactPhone)

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
                            onClick = { viewModel.resetScan() },
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
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreviewView(
                    isTorchOn = uiState.isTorchOn,
                    onBarcodeScanned = { raw ->
                        viewModel.onBarcodeRawDetected(raw)
                    },
                    onCameraReady = { cam ->
                        cameraInstance = cam
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder frame and guide overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(2.dp, RedPrimary, RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                    )

                    Spacer(modifier = Modifier.height(spacing.medium))

                    Text(
                        text = stringResource(R.string.qr_scan_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = spacing.medium, vertical = spacing.small)
                    )

                    uiState.scanErrorRes?.let { errRes ->
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedSurface)
                                .border(1.dp, RedPrimary, RoundedCornerShape(8.dp))
                                .padding(horizontal = spacing.medium, vertical = spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(
                                text = stringResource(errRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = RedPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CameraPreviewView(
    isTorchOn: Boolean,
    onBarcodeScanned: (String) -> Unit,
    onCameraReady: (Camera) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(1280, 720))
                    .build()

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy, onBarcodeScanned)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    camera.cameraControl.enableTorch(isTorchOn)
                    onCameraReady(camera)
                } catch (_: Exception) {
                    // Camera binding failed
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                var found = false
                for (barcode in barcodes) {
                    val raw = barcode.rawValue ?: barcode.displayValue
                    if (!raw.isNullOrBlank()) {
                        onSuccess(raw)
                        found = true
                        break
                    }
                }
                if (!found) {
                    tryZxingFallback(imageProxy, onSuccess)
                }
            }
            .addOnFailureListener {
                tryZxingFallback(imageProxy, onSuccess)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun tryZxingFallback(imageProxy: ImageProxy, onSuccess: (String) -> Unit) {
    try {
        val plane = imageProxy.planes.firstOrNull() ?: return
        val buffer = plane.buffer.duplicate()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height
        val source = PlanarYUVLuminanceSource(
            bytes, width, height, 0, 0, width, height, false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader().apply {
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE
            )
            setHints(hints)
        }
        val result = reader.decodeWithState(binaryBitmap)
        if (!result.text.isNullOrBlank()) {
            onSuccess(result.text)
        }
    } catch (_: Exception) {
        // ZXing frame decode skipped
    }
}
