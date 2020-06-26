package com.hyeok.todomanagehomework.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.hyeok.todomanagehomework.R
import com.hyeok.todomanagehomework.adapter.TodoPageAdapter
import com.hyeok.todomanagehomework.model.Todo
import com.hyeok.todomanagehomework.util.sqlite.DbHelper
import com.hyeok.todomanagehomework.util.sqlite.TodoContract
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate

class MainActivity : AppCompatActivity() {

    private val dbHelper by lazy { DbHelper(this) }
    private val todoLists by lazy { mutableListOf<MutableList<Todo>>() }
    private var todoListViewOption = R.id.option_month
    private val currentDate by lazy { LocalDate.now() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTodoList()
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

    private fun initTodoList() {
        var date = currentDate.minusMonths(6)

        for(i in 1..13) {
            val todoList = mutableListOf<Todo>()
            val startDayInMonth = date.year.zeroFormat() + "-" + date.monthValue.zeroFormat() + "-" + 1.zeroFormat()
            val endDayInMonth = date.year.zeroFormat() + "-" + date.monthValue.zeroFormat() + "-" + date.lengthOfMonth().zeroFormat()
            val query = "SELECT * FROM ${TodoContract.TodoEntry.TABLE_NAME} WHERE ${TodoContract.TodoEntry.DATE} BETWEEN '$startDayInMonth' AND '$endDayInMonth' ORDER BY ${TodoContract.TodoEntry.DATE} ASC"

            dbHelper.select(query).run {
                while(moveToNext()) {
                    val title = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.TITLE))
                    val date = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.DATE))
                    val startTime = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.START_TIME))
                    val endTime = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.END_TIME))
                    val latitude = getDouble(getColumnIndexOrThrow(TodoContract.TodoEntry.LATITUDE))
                    val longitude = getDouble(getColumnIndexOrThrow(TodoContract.TodoEntry.LONGITUDE))
                    val content = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.CONTENT))
                    val multimediaContentUri = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.MULTIMEDIA_CONTENT_URI))

                    val todo = Todo(
                        title,
                        date,
                        startTime,
                        endTime,
                        latitude,
                        longitude,
                        content,
                        multimediaContentUri
                    )
                    todoList.add(todo)
                }

                close()
            }

            Log.d(javaClass.simpleName, "$date\n$query\n${todoList.size}\n")
            if(todoList.isNotEmpty()) {
                todoLists.add(todoList)
            }
            else {
                todoLists.add(emptyList<Todo>().toMutableList())
            }
            date = date.plusMonths(1)
        }

        todo_list_slider.run {
            adapter = TodoPageAdapter(todoLists) {

            }
            Handler().postDelayed({
                setCurrentItem(6, false)
                supportActionBar?.title = getString(R.string.todo_sort_option_month)
            }, 100L)
        }
    }

    private fun Int.zeroFormat(): String {
        return String.format("%02d", this)
    }
}