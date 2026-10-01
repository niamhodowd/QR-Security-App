package com.example.qrscanner12


    import androidx.camera.core.ExperimentalGetImage
    import androidx.camera.core.ImageAnalysis
    import androidx.camera.core.ImageProxy
    import com.google.mlkit.vision.barcode.BarcodeScanning
    import com.google.mlkit.vision.common.InputImage

    class QrCodeAnalyzer(
        private val onQrCodeDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        // Initialize the ML Kit Barcode Scanner
        private val scanner = BarcodeScanning.getClient()

        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // Convert CameraX image to ML Kit format
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            // Extract the URL if it exists
                            val url = barcode.url?.url ?: barcode.rawValue
                            if (url != null) {
                                onQrCodeDetected(url)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        // Crucial: Always close the proxy so the next frame can be processed
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
