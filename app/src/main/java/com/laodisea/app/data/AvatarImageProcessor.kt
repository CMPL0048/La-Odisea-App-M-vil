package com.laodisea.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.laodisea.app.domain.AVATAR_MAX_BYTES
import com.laodisea.app.domain.AVATAR_SIZE_PX
import com.laodisea.app.domain.AVATAR_WEBP_QUALITY
import com.laodisea.app.domain.centerSquareCrop
import com.laodisea.app.domain.sampleSizeFor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Error al preparar la foto; su mensaje ya está en español para el jugador. */
class AvatarImageException(message: String) : Exception(message)

/**
 * Convierte la foto elegida en la galería en el avatar que se sube: respeta la rotación EXIF,
 * recorta el cuadrado central, la deja en 512 × 512 y la comprime en WEBP.
 */
@Singleton
class AvatarImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun process(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val decoded = runCatching { decode(uri) }
            .getOrElse { throw AvatarImageException("No se pudo abrir esa imagen. Prueba con otra.") }
        val avatar = cropAndScale(decoded)
        val bytes = ByteArrayOutputStream().use { out ->
            avatar.compress(webpFormat(), AVATAR_WEBP_QUALITY, out)
            out.toByteArray()
        }
        if (bytes.size >= AVATAR_MAX_BYTES) throw AvatarImageException("La foto es muy grande (máximo 1 MB).")
        bytes
    }

    private fun decode(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeWithImageDecoder(uri) else decodeLegacy(uri)

    // ImageDecoder ya aplica la rotación EXIF por su cuenta.
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: Uri): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            // En software para poder recortar y comprimir el bitmap.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
        }

    // API 26–27: BitmapFactory no lee la orientación, así que se aplica a mano desde el EXIF.
    private fun decodeLegacy(uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo decodificar la imagen")
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = exifMatrix(orientation) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun exifMatrix(orientation: Int): Matrix? = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postRotate(90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { postRotate(-90f); postScale(-1f, 1f) }
        else -> null
    }

    private fun cropAndScale(source: Bitmap): Bitmap {
        val crop = centerSquareCrop(source.width, source.height)
        val square = Bitmap.createBitmap(source, crop.left, crop.top, crop.size, crop.size)
        return Bitmap.createScaledBitmap(square, AVATAR_SIZE_PX, AVATAR_SIZE_PX, true)
    }

    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
}
