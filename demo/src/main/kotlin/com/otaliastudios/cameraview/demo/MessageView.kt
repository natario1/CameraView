package com.otaliastudios.cameraview.demo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class MessageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.option_view, this)
        val content = findViewById<ViewGroup>(R.id.content)
        View.inflate(context, R.layout.spinner_text, content)
    }

    private val message: TextView = findViewById<ViewGroup>(R.id.content).getChildAt(0) as TextView
    private val title: TextView = findViewById(R.id.title)

    fun setMessage(message: String) { this.message.text = message }

    fun setTitle(title: String) { this.title.text = title }

    fun setTitleAndMessage(title: String, message: String) {
        setTitle(title)
        setMessage(message)
    }
}
