package com.hyeok.todomanagehomework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyeok.todomanagehomework.R
import com.hyeok.todomanagehomework.model.Todo
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.todo_list_page_item.view.*

class TodoPageAdapter(val todoPages: MutableList<MutableList<Todo>>, val onClick: (Todo) -> Unit): RecyclerView.Adapter<TodoPageAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return todoPages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.todo_list_page_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(todoPages[position], onClick)
    }

    class ViewHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindTo(todoPage: MutableList<Todo>, onClick: (Todo) -> Unit) {
            containerView.todo_list.run {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(false)
                adapter = TodoAdapter(todoPage, onClick)
            }
        }
    }
}