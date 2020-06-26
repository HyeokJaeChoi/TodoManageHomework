package com.hyeok.todomanagehomework.util.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

object DocumentUriConverter {

    fun getBitmapFromContentUri(context: Context, contentUri: Uri): Bitmap? {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(contentUri, "r")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        val bitmap = if(fileDescriptor != null) {
            BitmapFactory.decodeFileDescriptor(fileDescriptor)
        }
        else {
            null
        }

        parcelFileDescriptor?.close()
        return bitmap
    }

}