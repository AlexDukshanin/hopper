package com.alex.hopper.storage

import android.content.Context
import java.io.File
import java.util.UUID

class PhotoStorage(context: Context) {
    private val photoDirectory: File = File(context.filesDir, "wagon_photos").apply {
        mkdirs()
    }

    fun createCaptureFile(): File {
        return File(photoDirectory, "wagon_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    }
}
