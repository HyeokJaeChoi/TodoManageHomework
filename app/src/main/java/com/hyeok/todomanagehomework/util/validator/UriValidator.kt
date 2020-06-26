package com.hyeok.todomanagehomework.util.validator

import android.net.Uri

object UriValidator {

    fun isImageUri(uri: Uri): Boolean {
        val uriString = uri.toString()
        return uriString.contains("content://") && uriString.contains("image")
    }

    fun isVideoUri(uri: Uri): Boolean {
        val uriString = uri.toString()
        return uriString.contains("content://") && uriString.contains("video")
    }

    fun isAudioUri(uri: Uri): Boolean {
        val uriString = uri.toString()
        return uriString.contains("content://") && uriString.contains("audio")
    }

}