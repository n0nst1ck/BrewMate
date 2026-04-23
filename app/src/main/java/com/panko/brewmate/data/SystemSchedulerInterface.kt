package com.panko.brewmate.data
import com.panko.brewmate.model.ScheduledBrew

interface SystemSchedulerInterface {
    /**
     * Sets an alarm with the Android OS to trigger a broadcast receiver.
     * This is intended for scheduling external events (like a morning coffee).
     */
    fun schedule(brew: ScheduledBrew): Boolean

    /**
     * Cancels an existing scheduled alarm.
     */
    fun cancel(brewId: String): Boolean
}