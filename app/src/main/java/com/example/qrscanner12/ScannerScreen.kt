package com.example.qrscanner12



import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.isGranted
import java.util.concurrent.Executors
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.tooling.preview.Preview
import com.example.qrscanner12.ui.theme.QRScanner12Theme





@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(){
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera Scanner Preview", style = MaterialTheme.typography.headlineMedium)
        }
        return
    }

    val cameraPermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    if (cameraPermissionState.status.isGranted) {
        ScannerContent()
    } else {
        PermissionRequestScreen(onGrantClick = { cameraPermissionState.launchPermissionRequest() })
    }
}

@Composable
fun PermissionRequestScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Camera Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We need camera access to scan and validate QR codes for your safety.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission", modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun ScannerContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var detectedUrl by remember { mutableStateOf<String?>(null) }
    var isUrlSafe by remember { mutableStateOf<Boolean?>(null) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor, QrCodeAnalyzer { url ->
                                if (detectedUrl == null) {
                                    detectedUrl = url
                                    checkUrlSafety(context, url) { safe -> isUrlSafe = safe }
                                }
                            })
                        }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        ScannerOverlay()

        detectedUrl?.let { url ->
            ResultDialog(
                url = url,
                isSafe = isUrlSafe,
                onDismiss = {
                    detectedUrl = null
                    isUrlSafe = null
                },
                onVisit = {
                    uriHandler.openUri(url)
                    detectedUrl = null
                    isUrlSafe = null
                }
            )
        }
    }
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val lineYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineY"
    )

    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
        val scannerSize = size.width * 0.7f
        val left = (size.width - scannerSize) / 2
        val top = (size.height - scannerSize) / 2
        val rect = Rect(left, top, left + scannerSize, top + scannerSize)

        // 1. Draw darkened background with a cutout
        val path = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRoundRect(RoundRect(rect, CornerRadius(24.dp.toPx())))
            fillType = PathFillType.EvenOdd
        }
        drawPath(path, Color.Black.copy(alpha = 0.6f))

        // 2. Draw white border around cutout
        drawRoundRect(
            color = Color.White,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(24.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Draw scanning line
        val lineY = top + (scannerSize * lineYProgress)
        drawLine(
            color = Color.Green,
            start = Offset(left + 20.dp.toPx(), lineY),
            end = Offset(left + scannerSize - 20.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun ResultDialog(
    url: String,
    isSafe: Boolean?,
    onDismiss: () -> Unit,
    onVisit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            if (isSafe == false) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
            } else {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
        },
        title = {
            Text(
                if (isSafe == false) "Suspicious Link!" else "QR Code Detected",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (isSafe) {
                    null -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text("Verifying safety...", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    true -> {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "✅ Secure Link",
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    false -> {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "❌ Dangerous Site",
                                color = Color.Red,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onVisit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSafe == false) Color.Red else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isSafe == false) "Proceed Anyway" else "Open Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ScannerScreenPreview() {
    QRScanner12Theme {
        ScannerScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRequestScreenPreview() {
    QRScanner12Theme {
        PermissionRequestScreen(onGrantClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerOverlayPreview() {
    QRScanner12Theme {
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            ScannerOverlay()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResultDialogSafePreview() {
    QRScanner12Theme {
        ResultDialog(
            url = "https://www.google.com",
            isSafe = true,
            onDismiss = {},
            onVisit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResultDialogUnsafePreview() {
    QRScanner12Theme {
        ResultDialog(
            url = "https://malicious-site.com",
            isSafe = false,
            onDismiss = {},
            onVisit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResultDialogLoadingPreview() {
    QRScanner12Theme {
        ResultDialog(
            url = "https://checking-url.com",
            isSafe = null,
            onDismiss = {},
            onVisit = {}
        )
    }
}
