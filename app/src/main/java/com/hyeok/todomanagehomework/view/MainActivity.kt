package com.hyeok.todomanagehomework.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.hyeok.todomanagehomework.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            menuInflater.inflate(R.menu.todo_sort_option, menu)

            return true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.option_month -> {

            }
            R.id.option_week -> {

            }
            R.id.option_day -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }
}