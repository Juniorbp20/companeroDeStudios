package com.example.studybuddy.subjects

import com.example.studybuddy.util.DateUtils
import java.util.Calendar
import java.util.Locale

/** Utilidades puras de horario y cuenta atrás. */
object SubjectTime {

    /** Días naturales entre hoy y la fecha del examen (0 = hoy, negativo = ya pasó). */
    fun daysUntil(now: Long, date: Long): Int {
        val from = Calendar.getInstance().apply { timeInMillis = DateUtils.startOfDay(now) }
        val to = Calendar.getInstance().apply { timeInMillis = DateUtils.startOfDay(date) }
        var days = 0
        if (to.before(from)) {
            while (to.before(from)) { to.add(Calendar.DAY_OF_MONTH, 1); days-- }
        } else {
            while (from.before(to)) { from.add(Calendar.DAY_OF_MONTH, 1); days++ }
        }
        return days
    }

    /** "08:30" a partir de minutos desde medianoche. */
    fun formatMinutes(minutes: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", (minutes / 60) % 24, minutes % 60)

    /** Día de la semana (numeración de Calendar) de una fecha. */
    fun weekdayOf(epoch: Long): Int =
        Calendar.getInstance().apply { timeInMillis = epoch }.get(Calendar.DAY_OF_WEEK)

    /** Orden de lunes a domingo para mostrar el horario semanal. */
    fun mondayFirstIndex(weekday: Int): Int = (weekday + 5) % 7

    /** Urgencia visual de un examen según los días que faltan. */
    enum class Urgency { PAST, TODAY, SOON, UPCOMING }

    fun urgency(daysLeft: Int): Urgency = when {
        daysLeft < 0 -> Urgency.PAST
        daysLeft == 0 -> Urgency.TODAY
        daysLeft <= 3 -> Urgency.SOON
        else -> Urgency.UPCOMING
    }
}
