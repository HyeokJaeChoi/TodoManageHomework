package com.hyeok.todomanagehomework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyeok.todomanagehomework.R
import com.hyeok.todomanagehomework.model.Todo
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.todo_item.view.*

class TodoAdapter(val todoList: MutableList<Todo>, val onClick: (View) -> Unit): RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return todoList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.todo_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(todoList[position], onClick)
    }

    class ViewHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindTo(todo: Todo, onClick: (View) -> Unit) {
            containerView.run {
                todo_date.text = todo.date
                todo_title.text = todo.title
                setOnClickListener(onClick)
            }
        }
    }
}