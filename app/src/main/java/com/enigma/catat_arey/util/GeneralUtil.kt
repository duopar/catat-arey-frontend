package com.enigma.catat_arey.util

import android.util.Log
import com.enigma.catat_arey.data.network.InventoryLogResponse
import com.enigma.catat_arey.data.network.ProductSaleForecastResponse
import com.enigma.catat_arey.ui.product_detail.ProductForecast
import org.json.JSONObject
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Base64
import java.util.Locale
import java.util.regex.Pattern

object GeneralUtil {
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

    fun isValidPassword(password: String): Boolean {
        val regex = """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[a-zA-Z\d!@#$%^&*]{8,30}$"""
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(password)
        return matcher.matches()
    }

    fun isProperPositiveNumber(input: String): Boolean {
        try {
            return input.isNotBlank() &&
                    input.contains(Regex("^\\d+$")) &&
                    input.toIntOrNull()?.let { it >= 0 } ?: false
        } catch (e: Exception) {
            return false
        }
    }

    fun getUserRole(role: String): AreyUserRole {
        return when (role) {
            "owner" -> AreyUserRole.Owner
            "employee" -> AreyUserRole.Employee
            else -> AreyUserRole.Unknown
        }
    }

    fun getJwtExpiry(token: String): Long {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid JWT format, shouldn't happen.")
            }

            val payload = String(Base64.getUrlDecoder().decode(parts[1]))

            val json = JSONObject(payload)

            if (json.has("exp")) {
                json.getLong("exp")
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    fun createUserTokenString(accessToken: String, refreshToken: String): String {
        return accessToken + AreyConsts.getTokenSeparator() + refreshToken
    }

    fun getUserToken(token: String): UserToken {
        val tokens = token.split(AreyConsts.getTokenSeparator())

        return UserToken(
            accessToken = tokens[0],
            refreshToken = tokens[1]
        )
    }

    fun getDailySum(data: List<InventoryLogResponse>): DailySum {
        val today = LocalDate.now()
        val todaysLogs = data.filter { log ->
            val logDate = Instant.ofEpochSecond(log.createdAt.secondsInEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            logDate == today
        }

        val stockIn = todaysLogs.filter { it.changeType == "stockIn" }.sumOf { it.stockChange }
        val stockOut = todaysLogs.filter { it.changeType == "stockOut" }.sumOf { it.stockChange }

        return DailySum(stockIn, stockOut)
    }

    fun epochLongToFormattedDate(epochSeconds: Long, locale: Locale = Locale.getDefault()): String {
        val instant = Instant.ofEpochSecond(epochSeconds)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss") // 24-hour format

        val dateString = localDateTime.format(dateFormatter)
        val timeString = localDateTime.format(timeFormatter)

        return "$dateString ($timeString)"
    }

    fun createForecastList(data: ProductSaleForecastResponse): List<ProductForecast> {
        if (data.predictedRestockDay == null) return listOf()

        val forecasts = ArrayList<ProductForecast>()

        val today = LocalDate.now()

        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

        val salesMap = mapOf(
            DayOfWeek.MONDAY to data.predictedSales.monday,
            DayOfWeek.TUESDAY to data.predictedSales.tuesday,
            DayOfWeek.WEDNESDAY to data.predictedSales.wednesday,
            DayOfWeek.THURSDAY to data.predictedSales.thursday,
            DayOfWeek.FRIDAY to data.predictedSales.friday,
            DayOfWeek.SATURDAY to data.predictedSales.saturday,
            DayOfWeek.SUNDAY to data.predictedSales.sunday
        )

        val restockDayMap = mapOf(
            "mon" to DayOfWeek.MONDAY,
            "tue" to DayOfWeek.TUESDAY,
            "wed" to DayOfWeek.WEDNESDAY,
            "thu" to DayOfWeek.THURSDAY,
            "fri" to DayOfWeek.FRIDAY,
            "sat" to DayOfWeek.SATURDAY,
            "sun" to DayOfWeek.SUNDAY
        )

        val restockDay = restockDayMap[data.predictedRestockDay.lowercase()]

        for (i in 0..6) {
            val currentDate = today.plusDays(i.toLong())
            val dayOfWeek = currentDate.dayOfWeek

            forecasts.add(
                ProductForecast(
                    day = dayOfWeek.getDisplayName(TextStyle.FULL, Locale("id", "ID")),
                    date = currentDate.format(formatter),
                    predictedSale = salesMap[dayOfWeek] ?: 0,
                    isRestock = dayOfWeek == restockDay,
                    epoch = currentDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                )
            )
        }

        return forecasts
    }

    fun getCurrentEpoch(): Long {
        return System.currentTimeMillis() / 1000
    }

    data class UserToken(
        val accessToken: String,
        val refreshToken: String
    )

    data class DailySum(
        var stockIn: Int,
        var stockOut: Int
    )
}