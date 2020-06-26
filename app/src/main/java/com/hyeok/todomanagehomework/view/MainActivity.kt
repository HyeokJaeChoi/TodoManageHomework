package com.hyeok.todomanagehomework.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.BaseColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.viewpager2.widget.ViewPager2
import com.hyeok.todomanagehomework.R
import com.hyeok.todomanagehomework.adapter.TodoPageAdapter
import com.hyeok.todomanagehomework.model.Todo
import com.hyeok.todomanagehomework.util.sqlite.DbHelper
import com.hyeok.todomanagehomework.util.sqlite.TodoContract
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.WeekFields
import java.util.*

class MainActivity : AppCompatActivity() {

    private val dbHelper by lazy { DbHelper(this) }
    private val todoLists by lazy { mutableListOf<MutableList<Todo>>() }
    private var todoListViewOption = R.id.option_month
    private lateinit var currentDate: LocalDate
    private var currentPagePos = 12
    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(currentPagePos - position > 0) {
                    when(todoListViewOption) {
                        R.id.option_month -> {
                            currentDate = currentDate.minusMonths(1)
                            supportActionBar?.title = currentDate.monthValue.toString() + "월"
                        }
                        R.id.option_week -> {
                            currentDate = currentDate.minusWeeks(1)
                            supportActionBar?.title = currentDate.monthValue.toString() + "월 " + currentDate.get(ChronoField.ALIGNED_WEEK_OF_MONTH) + "째주"
                        }
                        R.id.option_day -> {
                            currentDate = currentDate.minusDays(1)
                            supportActionBar?.title = currentDate.monthValue.toString() + "월 " + currentDate.dayOfMonth.toString() + "일"
                        }
                    }
                }
                else {
                    when(todoListViewOption) {
                        R.id.option_month -> {
                            currentDate = currentDate.plusMonths(1)
                            supportActionBar?.title = currentDate.monthValue.toString() + "월"
                        }
                        R.id.option_week -> {
                            currentDate = currentDate.plusWeeks(1)
                            supportActionBar?.title = currentDate.monthValue.toString() + "월 " + currentDate.get(ChronoField.ALIGNED_WEEK_OF_MONTH) + "째주"
                        }
                        R.id.option_day -> {
                            currentDate = currentDate.plusDays(1)
                            supportActionBar?.title = currentDate.monthValue.toString() + "월 " + currentDate.dayOfMonth.toString() + "일"
                        }
                    }
                }
                currentPagePos = position

                if(position == 2) {
                    addTodoListsToSlider(DATE_BEFORE)
                }
                else if(position == todo_list_slider.adapter?.itemCount?.minus(3)) {
                    addTodoListsToSlider(DATE_AFTER)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTodoList()
        todo_add_btn.setOnClickListener {
            val intent = Intent(this, TodoDetailActivity::class.java)
            startActivityForResult(intent, REQUEST_TODO_DETAIL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_TODO_DETAIL && resultCode == RESULT_OK) {
            initTodoList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            menuInflater.inflate(R.menu.todo_sort_option, menu)

            return true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        todoListViewOption = item.itemId
        initTodoList()

        return super.onOptionsItemSelected(item)
    }

    private fun initTodoList() {
        todo_list_slider.unregisterOnPageChangeCallback(pageChangeCallback)
        todoLists.clear()
        currentPagePos = 12

        if(!this::currentDate.isInitialized) {
            currentDate = LocalDate.now()
        }
        var date = when(todoListViewOption) {
            R.id.option_month -> {
                currentDate.minusMonths(12)
            }
            R.id.option_week -> {
                currentDate.minusWeeks(12)
            }
            R.id.option_day -> {
                currentDate.minusDays(12)
            }
            else -> {
                null
            }
        }

        date?.let {
            for(i in 1..25) {
                val todoList = mutableListOf<Todo>()
                val startDate = when(todoListViewOption) {
                    R.id.option_month -> {
                        date!!.year.zeroFormat() + "-" + date!!.monthValue.zeroFormat() + "-" + 1.zeroFormat()
                    }
                    R.id.option_week -> {
                        val startDateThisWeek = date!!.with(WeekFields.of(Locale.KOREA).dayOfWeek(), 1)
                        startDateThisWeek.year.zeroFormat() + "-" + startDateThisWeek.monthValue.zeroFormat() + "-" + startDateThisWeek.dayOfMonth.zeroFormat()
                    }
                    R.id.option_day -> {
                        date!!.year.zeroFormat() + "-" + date!!.monthValue.zeroFormat() + "-" + date!!.dayOfMonth.zeroFormat()
                    }
                    else -> {
                        ""
                    }
                }
                val endDate = when(todoListViewOption) {
                    R.id.option_month -> {
                        date!!.year.zeroFormat() + "-" + date!!.monthValue.zeroFormat() + "-" + date!!.lengthOfMonth().zeroFormat()
                    }
                    R.id.option_week -> {
                        val endDateThisWeek = date!!.with(WeekFields.of(Locale.KOREA).dayOfWeek(), 7)
                        endDateThisWeek.year.zeroFormat() + "-" + endDateThisWeek.monthValue.zeroFormat() + "-" + endDateThisWeek.dayOfMonth.zeroFormat()
                    }
                    R.id.option_day -> {
                        date!!.year.zeroFormat() + "-" + date!!.monthValue.zeroFormat() + "-" + date!!.dayOfMonth.zeroFormat()
                    }
                    else -> {
                        ""
                    }
                }

                if(startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    val query = "SELECT * FROM ${TodoContract.TodoEntry.TABLE_NAME} WHERE ${TodoContract.TodoEntry.DATE} BETWEEN '$startDate' AND '$endDate' ORDER BY ${TodoContract.TodoEntry.DATE} ASC"
                    Log.d(javaClass.simpleName, "$currentDate $startDate $endDate")

                    dbHelper.select(query).run {
                        while(moveToNext()) {
                            val id = getInt(getColumnIndexOrThrow(BaseColumns._ID))
                            val title = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.TITLE))
                            val date = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.DATE))
                            val startTime = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.START_TIME))
                            val endTime = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.END_TIME))
                            val latitude = getDouble(getColumnIndexOrThrow(TodoContract.TodoEntry.LATITUDE))
                            val longitude = getDouble(getColumnIndexOrThrow(TodoContract.TodoEntry.LONGITUDE))
                            val content = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.CONTENT))
                            val multimediaContentUri = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.MULTIMEDIA_CONTENT_URI))

                            val todo = Todo(
                                id,
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

                    if(todoList.isNotEmpty()) {
                        todoLists.add(todoList)
                    }
                    else {
                        todoLists.add(emptyList<Todo>().toMutableList())
                    }
                }

                when(todoListViewOption) {
                    R.id.option_month -> {
                        date = date!!.plusMonths(1)
                    }
                    R.id.option_week -> {
                        date = date!!.plusWeeks(1)
                    }
                    R.id.option_day -> {
                        date = date!!.plusDays(1)
                    }
                }
            }

            todo_list_slider.run {
                adapter = TodoPageAdapter(todoLists) {
                    val intent = Intent(this@MainActivity, TodoDetailActivity::class.java).apply {
                        putExtra(EXIST_TODO, it)
                    }
                    startActivityForResult(intent, REQUEST_TODO_DETAIL)
                }
                Handler().postDelayed({
                    setCurrentItem(currentPagePos, false)
                    registerOnPageChangeCallback(pageChangeCallback)
                    supportActionBar?.title = when(todoListViewOption) {
                        R.id.option_month -> {
                            currentDate.monthValue.toString() + "월"
                        }
                        R.id.option_week -> {
                            currentDate.monthValue.toString() + "월 " + currentDate.get(ChronoField.ALIGNED_WEEK_OF_MONTH) + "째주"
                        }
                        R.id.option_day -> {
                            currentDate.monthValue.toString() + "월 " + currentDate.dayOfMonth.toString() + "일"
                        }
                        else -> {
                            ""
                        }
                    }
                }, 100L)
            }
        }
    }

    private fun getMoreTodoLists(date: LocalDate): MutableList<MutableList<Todo>> {
        var date = date
        val newTodoLists = mutableListOf<MutableList<Todo>>()

        for(i in 1..6) {
            val todoList = mutableListOf<Todo>()
            val startDate = when(todoListViewOption) {
                R.id.option_month -> {
                    date.year.zeroFormat() + "-" + date.monthValue.zeroFormat() + "-" + 1.zeroFormat()
                }
                R.id.option_week -> {
                    val startDateThisWeek = date.with(WeekFields.of(Locale.KOREA).dayOfWeek(), 1)
                    startDateThisWeek.year.zeroFormat() + "-" + startDateThisWeek.monthValue.zeroFormat() + "-" + startDateThisWeek.dayOfMonth.zeroFormat()
                }
                R.id.option_day -> {
                    date.year.zeroFormat() + "-" + date.monthValue.zeroFormat() + "-" + date.dayOfMonth.zeroFormat()
                }
                else -> {
                    ""
                }
            }
            val endDate = when(todoListViewOption) {
                R.id.option_month -> {
                    date.year.zeroFormat() + "-" + date.monthValue.zeroFormat() + "-" + date.lengthOfMonth().zeroFormat()
                }
                R.id.option_week -> {
                    val endDateThisWeek = date.with(WeekFields.of(Locale.KOREA).dayOfWeek(), 7)
                    endDateThisWeek.year.zeroFormat() + "-" + endDateThisWeek.monthValue.zeroFormat() + "-" + endDateThisWeek.dayOfMonth.zeroFormat()
                }
                R.id.option_day -> {
                    date.year.zeroFormat() + "-" + date.monthValue.zeroFormat() + "-" + date.dayOfMonth.zeroFormat()
                }
                else -> {
                    ""
                }
            }

            if(startDate.isNotEmpty() && endDate.isNotEmpty()) {
                val query = "SELECT * FROM ${TodoContract.TodoEntry.TABLE_NAME} WHERE ${TodoContract.TodoEntry.DATE} BETWEEN '$startDate' AND '$endDate' ORDER BY ${TodoContract.TodoEntry.DATE} ASC"

                dbHelper.select(query).run {
                    while(moveToNext()) {
                        val id = getInt(getColumnIndexOrThrow(BaseColumns._ID))
                        val title = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.TITLE))
                        val date = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.DATE))
                        val startTime = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.START_TIME))
                        val endTime = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.END_TIME))
                        val latitude = getDouble(getColumnIndexOrThrow(TodoContract.TodoEntry.LATITUDE))
                        val longitude = getDouble(getColumnIndexOrThrow(TodoContract.TodoEntry.LONGITUDE))
                        val content = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.CONTENT))
                        val multimediaContentUri = getString(getColumnIndexOrThrow(TodoContract.TodoEntry.MULTIMEDIA_CONTENT_URI))

                        val todo = Todo(
                            id,
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

                if(todoList.isNotEmpty()) {
                    newTodoLists.add(todoList)
                }
                else {
                    newTodoLists.add(emptyList<Todo>().toMutableList())
                }
            }

            date = when(todoListViewOption) {
                R.id.option_month -> {
                    date.plusMonths(1)
                }
                R.id.option_week -> {
                    date.plusWeeks(1)
                }
                R.id.option_day -> {
                    date.plusDays(1)
                }
                else -> {
                    date
                }
            }
        }

        return newTodoLists
    }

    private fun addTodoListsToSlider(addMode: String) {
        todo_list_slider.unregisterOnPageChangeCallback(pageChangeCallback)
        var newTodoLists: MutableList<MutableList<Todo>>
        val date: LocalDate?

        when(addMode) {
            DATE_BEFORE -> {
                date = when(todoListViewOption) {
                    R.id.option_month -> {
                        currentDate.minusMonths(6)
                    }
                    R.id.option_week -> {
                        currentDate.minusWeeks(6)
                    }
                    R.id.option_day -> {
                        currentDate.minusDays(6)
                    }
                    else -> {
                        null
                    }
                }
                date?.let {
                    newTodoLists = getMoreTodoLists(it)
                    (todo_list_slider.adapter as TodoPageAdapter?)?.let {
                        currentPagePos += newTodoLists.size
                        it.todoPages.addAll(0, newTodoLists)
                        it.notifyDataSetChanged()
                    }
                }
            }
            DATE_AFTER -> {
                date = when(todoListViewOption) {
                    R.id.option_month -> {
                        currentDate.plusMonths(1)
                    }
                    R.id.option_week -> {
                        currentDate.plusWeeks(1)
                    }
                    R.id.option_day -> {
                        currentDate.plusDays(1)
                    }
                    else -> {
                        null
                    }
                }
                date?.let {
                    newTodoLists = getMoreTodoLists(it)
                    (todo_list_slider.adapter as TodoPageAdapter?)?.let {
                        it.todoPages.addAll(newTodoLists)
                        it.notifyDataSetChanged()
                    }
                }
            }
        }

        Handler().postDelayed({
            todo_list_slider.setCurrentItem(currentPagePos, false)
            todo_list_slider.registerOnPageChangeCallback(pageChangeCallback)
        }, 100L)
    }

    private fun Int.zeroFormat(): String {
        return String.format("%02d", this)
    }

    companion object {
        const val REQUEST_TODO_DETAIL = 1
        const val DATE_BEFORE = "dateBefore"
        const val DATE_AFTER = "dateAfter"
        const val EXIST_TODO = "existTodo"
    }
}