package com.otaliastudios.cameraview.demo;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class OptionView<Value> extends LinearLayout implements Spinner.OnItemSelectedListener {

    interface Callback {
        <T> boolean onValueChanged(@NonNull Option<T> option, @NonNull T value, @NonNull String name);
    }

    private Value value;
    private ArrayList<Value> values;
    private ArrayList<String> valuesStrings;
    private Option option;
    private Callback callback;
    private Spinner spinner;

    public OptionView(@NonNull Context context) {
        super(context);
        setOrientation(VERTICAL);
        inflate(context, R.layout.option_view, this);
        ViewGroup content = findViewById(R.id.content);
        spinner = new Spinner(context, Spinner.MODE_DROPDOWN);
        content.addView(spinner);
    }

    public void setHasDivider(boolean hasDivider) {
        View divider = findViewById(R.id.divider);
        divider.setVisibility(hasDivider ? View.VISIBLE : View.GONE);
    }

    public void setOption(@NonNull Option<Value> option, @NonNull Callback callback) {
        this.option = option;
        this.callback = callback;
        TextView title = findViewById(R.id.title);
        title.setText(option.getName());
    }

    @SuppressWarnings("all")
    public void onCameraOpened(CameraView view, CameraOptions options) {
        values = new ArrayList(option.getAll(view, options));
        value = (Value) option.get(view);
        valuesStrings = new ArrayList<>();
        for (Value value : values) {
            valuesStrings.add(option.toString(value));
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
        if (!values.get(i).equals(value)) {
            Log.e("ControlView", "curr: " + value + " new: " + values.get(i));
            if (!callback.onValueChanged(option, values.get(i), valuesStrings.get(i))) {
                spinner.setSelection(values.indexOf(value)); // Go back.
            } else {
                value = values.get(i);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}
}
