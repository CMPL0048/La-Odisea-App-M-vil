package com.laodisea.app.ui.profile

import android.content.Context
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Resultado de abrir el escáner de QR de Google Play. */
sealed interface QrScanResult {
    data class Scanned(val text: String) : QrScanResult
    data object Cancelled : QrScanResult

    /** No hay escáner (sin Play Services, sin cámara o módulo sin descargar): se ofrece escribir el código. */
    data class Unavailable(val message: String) : QrScanResult
}

/**
 * Abre el escáner de Google (solo QR). La cámara la maneja Google Play Services, así que la app
 * no pide el permiso CAMERA.
 */
suspend fun scanLinkQr(context: Context): QrScanResult = suspendCancellableCoroutine { cont ->
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = runCatching { GmsBarcodeScanning.getClient(context, options) }.getOrElse {
        cont.resume(QrScanResult.Unavailable(SCANNER_UNAVAILABLE))
        return@suspendCancellableCoroutine
    }
    scanner.startScan()
        .addOnSuccessListener { barcode ->
            val text = barcode.rawValue
            cont.resume(if (text.isNullOrBlank()) QrScanResult.Cancelled else QrScanResult.Scanned(text))
        }
        .addOnCanceledListener { cont.resume(QrScanResult.Cancelled) }
        .addOnFailureListener { e ->
            val cancelled = (e as? MlKitException)?.errorCode == MlKitException.CODE_SCANNER_CANCELLED
            cont.resume(if (cancelled) QrScanResult.Cancelled else QrScanResult.Unavailable(SCANNER_UNAVAILABLE))
        }
}

private const val SCANNER_UNAVAILABLE =
    "No se pudo abrir el escáner en este teléfono. Escribe el código que aparece debajo del QR en el juego."
