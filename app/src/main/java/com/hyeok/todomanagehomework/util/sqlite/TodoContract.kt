package com.hyeok.todomanagehomework.util.sqlite

import android.provider.BaseColumns

object TodoContract {
    object TodoEntry: BaseColumns {
        const val TABLE_NAME = "todo"
        const val TITLE = "title"
        const val DATE = "date"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CONTENT = "content"
        const val MULTIMEDIA_CONTENT = "multimedia_content"
    }
}