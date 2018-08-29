package no.skatteetaten.aurora.utils

import org.springframework.util.StopWatch

val StopWatch.logLine: String
    get() = this.taskInfo.joinToString { "${it.taskName}: ${it.timeMillis}ms" }

fun <T> StopWatch.time(taskName: String, function: () -> T): T {
    this.start(taskName)
    val res = function()
    this.stop()
    return res
}