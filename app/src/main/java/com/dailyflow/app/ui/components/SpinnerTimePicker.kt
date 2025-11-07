package com.dailyflow.app.ui.components

import android.view.LayoutInflater
import android.widget.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.dailyflow.app.R
import java.time.LocalTime

@Composable
fun SpinnerTimePicker(
    initialTime: LocalTime = LocalTime.now(),
    onTimeSelected: (LocalTime) -> Unit
) {
    AndroidView(
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(R.layout.spinner_time_picker, null)
            val timePicker = view.findViewById<TimePicker>(R.id.time_picker_spinner)
            timePicker.setIs24HourView(true)
            timePicker
        },
        update = { view ->
            view.hour = initialTime.hour
            view.minute = initialTime.minute
            view.setOnTimeChangedListener { _, hour, minute ->
                onTimeSelected(LocalTime.of(hour, minute))
            }
        }
    )
}
