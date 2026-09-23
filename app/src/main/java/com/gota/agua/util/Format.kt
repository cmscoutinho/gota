package com.gota.agua.util

import java.text.NumberFormat
import java.util.Locale

val PtBr: Locale = Locale.forLanguageTag("pt-BR")

fun formatMl(ml: Int): String = NumberFormat.getIntegerInstance(PtBr).format(ml) + " ml"

fun formatLiters(ml: Int): String = String.format(PtBr, "%.1f L", ml / 1000f)

fun formatMinutesOfDay(minutes: Int): String =
    String.format(PtBr, "%02d:%02d", minutes / 60, minutes % 60)

fun formatInterval(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}
