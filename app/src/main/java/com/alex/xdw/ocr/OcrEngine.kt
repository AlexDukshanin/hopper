package com.alex.xdw.ocr

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class OcrResult(
    val fullText: String,
    val lines: List<String>,
) {
    companion object {
        val EMPTY = OcrResult(
            fullText = "",
            lines = emptyList(),
        )
    }
}

class OcrEngine(private val context: Context) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(file: File): OcrResult {
        val image = InputImage.fromFilePath(context, file.toUri())
        return recognize(image)
    }

    suspend fun recognize(bitmap: Bitmap): OcrResult {
        val preparedBitmap = prepareBitmap(bitmap)
        return recognize(InputImage.fromBitmap(preparedBitmap, 0))
    }

    private suspend fun recognize(image: InputImage): OcrResult = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val lines = result.textBlocks
                    .flatMap { block -> block.lines.map { line -> line.text.trim() } }
                    .filter(String::isNotEmpty)

                continuation.resume(
                    OcrResult(
                        fullText = result.text.trim(),
                        lines = lines.ifEmpty {
                            result.text.lines().map(String::trim).filter(String::isNotEmpty)
                        },
                    ),
                )
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
            .addOnCanceledListener {
                continuation.cancel()
            }
    }

    private fun prepareBitmap(bitmap: Bitmap): Bitmap {
        val scaleFactor = when {
            bitmap.width >= 1_400 -> 1f
            bitmap.width >= 900 -> 1.4f
            else -> 2f
        }
        if (scaleFactor == 1f) {
            return bitmap
        }

        val scaledWidth = (bitmap.width * scaleFactor).toInt()
        val scaledHeight = (bitmap.height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
}
