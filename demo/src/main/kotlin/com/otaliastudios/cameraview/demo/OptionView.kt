package com.otaliastudios.cameraview.demo

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.CameraView

class OptionView<Value: Any>(context: Context) : LinearLayout(context), AdapterView.OnItemSelectedListener {

    interface Callback {
        fun <T: Any> onValueChanged(option: Option<T>, value: T, name: String): Boolean
    }

    private lateinit var option: Option<Value>
    private lateinit var callback: Callback
    private lateinit var value: Value
    private lateinit var values: List<Value>
    private lateinit var valuesStrings: List<String>

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.option_view, this)
    }

    val spinner = Spinner(context, Spinner.MODE_DROPDOWN).also {
        val content = findViewById<ViewGroup>(R.id.content)
        content.addView(it)
    }

    fun setHasDivider(hasDivider: Boolean) {
        val divider = findViewById<View>(R.id.divider)
        divider.visibility = if (hasDivider) View.VISIBLE else View.GONE
    }

    fun setOption(option: Option<Value>, callback: Callback) {
        this.option = option
        this.callback = callback
        val title = findViewById<TextView>(R.id.title)
        title.text = option.name
    }

    fun onCameraOpened(view: CameraView, options: CameraOptions) {
        values = option.getAll(view, options).toList()
        value = option.get(view)
        valuesStrings = values.map { option.toString(it) }
        if (values.isEmpty()) {
            spinner.onItemSelectedListener = null
            spinner.isEnabled = false
            spinner.alpha = 0.8f
            spinner.adapter = ArrayAdapter(context, R.layout.spinner_text, arrayOf("Not supported."))
            spinner.setSelection(0, false)
        } else {
            spinner.isEnabled = true
            spinner.alpha = 1f
            spinner.adapter = ArrayAdapter(context, R.layout.spinner_text, valuesStrings)
            spinner.setSelection(values.indexOf(value), false)
            spinner.onItemSelectedListener = this
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        if (values[i] != value) {
            Log.e("ControlView", "curr: " + value + " new: " + values[i])
            if (!callback.onValueChanged(option, values[i], valuesStrings[i])) {
                spinner.setSelection(values.indexOf(value)) // Go back.
            } else {
                value = values[i]
            }
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
}
