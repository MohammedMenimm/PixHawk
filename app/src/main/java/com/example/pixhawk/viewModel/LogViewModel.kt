package com.example.pixhawk.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class LogViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs
    fun addLog(log: String) {
        viewModelScope.launch {
            _logs.value += log
        }
    }
}
