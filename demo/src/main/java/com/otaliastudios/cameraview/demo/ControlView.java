package com.otaliastudios.cameraview.demo;


import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import java.util.ArrayList;
import java.util.Collection;

public class ControlView<Value> extends LinearLayout implements Spinner.OnItemSelectedListener {

    interface Callback {
        boolean onValueChanged(Control control, Object value, String name);
    }

    private Value value;
    private ArrayList<Value> values;
    private ArrayList<String> valuesStrings;
    private Control control;
    private Callback callback;
    private Spinner spinner;

    public ControlView(Context context, Control control, Callback callback) {
        super(context);
        this.control = control;
        this.callback = callback;
        setOrientation(VERTICAL);

        inflate(context, R.layout.control_view, this);
        TextView title = findViewById(R.id.title);
        title.setText(control.getName());
        View divider = findViewById(R.id.divider);
        divider.setVisibility(control.isSectionLast() ? View.VISIBLE : View.GONE);

        ViewGroup content = findViewById(R.id.content);
        spinner = new Spinner(context, Spinner.MODE_DROPDOWN);
        content.addView(spinner);
    }

    public Value getValue() {
        return value;
    }

    @SuppressWarnings("all")
    public void onCameraOpened(CameraView view) {
        values = new ArrayList(control.getValues(view));
        value = (Value) control.getCurrentValue(view);
        valuesStrings = new ArrayList<>();
        for (Value value : values) {
            valuesStrings.add(stringify(value));
        }

        if (values.isEmpty()) {
            spinner.setOnItemSelectedListener(null);
            spinner.setEnabled(false);
            spinner.setAlpha(0.8f);
            spinner.setAdapter(new ArrayAdapter(getContext(),
                    R.layout.spinner_text, new String[]{ "Not supported." }));
            spinner.setSelection(0, false);
        } else {
            spinner.setEnabled(true);
            spinner.setAlpha(1f);
            spinner.setAdapter(new ArrayAdapter(getContext(),
                    R.layout.spinner_text, valuesStrings));
            spinner.setSelection(values.indexOf(value), false);
            spinner.setOnItemSelectedListener(this);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (values.get(i) != value) {
            Log.e("ControlView", "curr: " + value + " new: " + values.get(i));
            if (!callback.onValueChanged(control, values.get(i), valuesStrings.get(i))) {
                spinner.setSelection(values.indexOf(value)); // Go back.
            } else {
                value = values.get(i);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    private String stringify(Value value) {
        if (value instanceof Integer) {
            if ((Integer) value == ViewGroup.LayoutParams.MATCH_PARENT) return "match parent";
            if ((Integer) value == ViewGroup.LayoutParams.WRAP_CONTENT) return "wrap content";
        }
        return String.valueOf(value).replace("_", " ").toLowerCase();
    }
}
