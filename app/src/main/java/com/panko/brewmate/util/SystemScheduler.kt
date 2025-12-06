package com.panko.brewmate.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SystemScheduler : Scheduler {
    override fun schedule(delayMillis: Long, action: () -> Unit) {
        // Using a global scope for simplicity in this example.
        // In a real app, you might inject a specific CoroutineScope.
        CoroutineScope(Dispatchers.Default).launch {
            delay(delayMillis)
            action()
        }
    }
}