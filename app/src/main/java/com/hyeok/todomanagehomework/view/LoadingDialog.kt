package com.hyeok.todomanagehomework.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager

class LoadingDialog(context: Context): Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutParams = WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.8f
        }
        window?.attributes = layoutParams
    }

}