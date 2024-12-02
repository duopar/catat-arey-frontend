package com.enigma.catat_arey.util

import java.text.NumberFormat
import java.util.Locale

object ProductUtil {
    fun Int.toFormatterCurrency(
        locale: Locale = Locale.getDefault(),
        includeCurrencySymbol: Boolean = false
    ): String {
        val numberFormatter = NumberFormat.getNumberInstance(locale)
        numberFormatter.maximumFractionDigits = 2
        numberFormatter.minimumFractionDigits = 2

        return if (includeCurrencySymbol) {
            NumberFormat.getCurrencyInstance(locale).format(this)
        } else {
            numberFormatter.format(this.toDouble())
        }
    }
}