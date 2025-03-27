package com.example.recognition_app.viemodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class SharedViewModel : ViewModel() {
    var bitmap: Bitmap? by mutableStateOf(null)
}
