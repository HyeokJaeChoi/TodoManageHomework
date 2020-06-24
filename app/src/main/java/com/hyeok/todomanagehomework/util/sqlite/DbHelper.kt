package com.hyeok.todomanagehomework.util.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class DbHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
            it.execSQL(DROP_TABLE)
            onCreate(it)
        }
    }

    fun select(tableName: String, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, group: String?, having: String?, sortOrder: String?): Cursor {
        return readableDatabase.query(
            tableName,
            projection,
            selection,
            selectionArgs,
            group,
            having,
            sortOrder
        )
    }

    fun insert(tableName: String, values: ContentValues) {
        writableDatabase.insert(
            tableName,
            null,
            values
        )
    }

    fun delete(tableName: String, selection: String?, selectionArgs: Array<String>?) {
        writableDatabase.delete(
            tableName,
            selection,
            selectionArgs
        )
    }

    fun update(tableName: String, values: ContentValues, selection: String?, selectionArgs: Array<String>?) {
        writableDatabase.update(
            tableName,
            values,
            selection,
            selectionArgs
        )
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "todo.db"

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${TodoContract.TodoEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TodoContract.TodoEntry.TITLE} TEXT," +
                    "${TodoContract.TodoEntry.DATE} TEXT," +
                    "${TodoContract.TodoEntry.START_TIME} TEXT," +
                    "${TodoContract.TodoEntry.END_TIME} TEXT," +
                    "${TodoContract.TodoEntry.LATITUDE} REAL," +
                    "${TodoContract.TodoEntry.LONGITUDE} REAL," +
                    "${TodoContract.TodoEntry.CONTENT} TEXT)"

        private const val DROP_TABLE = "DROP TABLE IF EXISTS ${TodoContract.TodoEntry.TABLE_NAME}"
    }
}