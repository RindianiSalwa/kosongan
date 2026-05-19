package com.dapurandia.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.*

object FileUtils {

    // Fungsi ini copy file dari Uri ke file temp di cache, lalu return file tersebut
    fun getFileFromUri(context: Context, uri: Uri): File? {
        try {
            val fileName = getFileName(context, uri)
            val tempFile = File.createTempFile(fileName, null, context.cacheDir)
            tempFile.deleteOnExit()

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "temp_file"
    }
}
