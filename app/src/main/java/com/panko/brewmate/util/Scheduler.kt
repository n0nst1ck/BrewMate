package com.panko.brewmate.util

interface Scheduler {
    /**
     * Schedules an action to be executed after a specified delay.
     * Implementations can use various methods (e.g., Handlers, Coroutines, Threads)
     * to achieve this without blocking the calling thread.
     */
    fun schedule(delayMillis: Long, action: () -> Unit)
}