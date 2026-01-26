package com.panko.brewmate.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SystemScheduler : Scheduler {
    override fun schedule(delayMillis: Long, action: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(delayMillis)
            action()
        }
    }
}