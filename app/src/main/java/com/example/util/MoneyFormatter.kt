package com.example.util

import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToLong

object MoneyFormatter {

  private val symbols =
    DecimalFormatSymbols(Locale.US).apply {
      groupingSeparator = ','
      decimalSeparator = '.'
    }

  fun formatCents(cents: Long, currencySymbol: String): String {
    val isNegative = cents < 0L
    val absCents = kotlin.math.abs(cents)
    val whole = absCents / 100L
    val rem = absCents % 100L
    val formattedWhole = DecimalFormat("#,##0", symbols).format(whole)
    val prefix = if (isNegative) "-$currencySymbol" else currencySymbol
    return if (rem == 0L) {
      "$prefix$formattedWhole"
    } else {
      val remStr = rem.toString().padStart(2, '0')
      "$prefix$formattedWhole.$remStr"
    }
  }

  fun parseAmountToCents(input: String): Long? {
    val normalized = input.trim().replace(",", ".").replace(" ", "")
    if (normalized.isEmpty()) return null
    val parsed = normalized.toDoubleOrNull() ?: return null
    if (parsed.isNaN() || parsed.isInfinite() || parsed <= 0.0) return null
    val cents = (parsed * 100.0).roundToLong()
    return if (cents in 1L..100_000_000_000L) cents else null
  }

  fun centsToInputString(cents: Long): String {
    if (cents <= 0L) return ""
    val whole = cents / 100L
    val rem = cents % 100L
    return if (rem == 0L) whole.toString() else "$whole.${rem.toString().padStart(2, '0')}"
  }

  fun formatTimestamp(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "Ahora"
    val formatter = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("es-ES"))
    return formatter.format(date)
  }
}
