package com.hyeok.todomanagehomework.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Todo (
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val latitude: Double,
    val longitude: Double,
    val content: String,
    val multimediaContentUri: String? = null
): Parcelable