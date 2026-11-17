package com.example.studybuddy.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault())

    fun formatDate(epoch: Long): String {
        if (epoch <= 0) return "Sin fecha"
        return dateFormat.format(Date(epoch))
    }

    fun formatTime(epoch: Long): String {
        if (epoch <= 0) return "--:--"
        return timeFormat.format(Date(epoch))
    }

    fun formatFull(epoch: Long): String {
        if (epoch <= 0) return "Sin fecha"
        return fullFormat.format(Date(epoch))
    }

    fun isOverdue(epoch: Long): Boolean {
        return epoch > 0 && epoch < System.currentTimeMillis()
    }

    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    fun startOfDay(epoch: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epoch
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Inicio (medianoche local) de los últimos [n] días, del más antiguo a hoy. Respeta cambios de horario. */
    fun lastNDayStarts(n: Int, now: Long = System.currentTimeMillis()): List<Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDay(now)
        val result = ArrayList<Long>(n)
        cal.add(Calendar.DAY_OF_MONTH, -(n - 1))
        repeat(n) {
            result.add(startOfDay(cal.timeInMillis))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    /** Agrupa marcas de tiempo por día local; la clave es la medianoche local del día. */
    fun countByLocalDay(timestamps: List<Long>): Map<Long, Int> =
        timestamps.groupingBy { startOfDay(it) }.eachCount()
}
