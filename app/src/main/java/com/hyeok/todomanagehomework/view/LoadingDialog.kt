package com.hyeok.todomanagehomework.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import com.hyeok.todomanagehomework.R

class LoadingDialog(context: Context): Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutParams = WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.8f
        }
        window?.let {
            it.attributes = layoutParams
            it.setBackgroundDrawableResource(android.R.color.transparent)
        }

        setContentView(R.layout.loading_dialog)
    }

}