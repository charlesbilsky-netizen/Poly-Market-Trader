package com.polytrader.app.core.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Shared display formatting for money, probabilities and time. */
object Format {

    /** 0.6342 -> "63%" */
    fun percent(probability: Double?): String =
        probability?.let { "${(it.coerceIn(0.0, 1.0) * 100).roundToInt()}%" } ?: "—"

    /** 0.6342 -> "63.4%" */
    fun percentFine(probability: Double?): String =
        probability?.let { String.format(Locale.US, "%.1f%%", it.coerceIn(0.0, 1.0) * 100) } ?: "—"

    /** 0.6342 -> "63.4¢" — prediction-market price in cents. */
    fun cents(price: Double?): String =
        price?.let { String.format(Locale.US, "%.1f¢", it * 100) } ?: "—"

    /** +0.042 -> "+4.2", -0.013 -> "-1.3" (percentage points). */
    fun signedPoints(change: Double?): String {
        change ?: return "—"
        val pts = change * 100
        return String.format(Locale.US, "%+.1f", pts)
    }

    /** 1234567.0 -> "$1.2M" */
    fun usdCompact(amount: Double?): String {
        amount ?: return "—"
        val a = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            a >= 1_000_000_000 -> String.format(Locale.US, "%s$%.1fB", sign, a / 1_000_000_000)
            a >= 1_000_000 -> String.format(Locale.US, "%s$%.1fM", sign, a / 1_000_000)
            a >= 10_000 -> String.format(Locale.US, "%s$%.0fK", sign, a / 1_000)
            a >= 1_000 -> String.format(Locale.US, "%s$%.1fK", sign, a / 1_000)
            else -> String.format(Locale.US, "%s$%.0f", sign, a)
        }
    }

    /** 1234.56 -> "$1,234.56" */
    fun usd(amount: Double?): String {
        amount ?: return "—"
        val sign = if (amount < 0) "-" else ""
        return sign + String.format(Locale.US, "$%,.2f", abs(amount))
    }

    /** Signed P&L: 123.4 -> "+$123.40", -50.0 -> "-$50.00" */
    fun pnl(amount: Double?): String {
        amount ?: return "—"
        val sign = if (amount >= 0) "+" else "-"
        return sign + String.format(Locale.US, "$%,.2f", abs(amount))
    }

    /** 12345.6 -> "12,346 shares" style count. */
    fun shares(count: Double?): String =
        count?.let { String.format(Locale.US, "%,.0f", it) } ?: "—"

    /** Time remaining until [end]; "Ended" when past. */
    fun timeLeft(end: Instant?, now: Instant = Instant.now()): String {
        end ?: return "No end date"
        val d = Duration.between(now, end)
        if (d.isNegative) return "Ended"
        val days = d.toDays()
        return when {
            days >= 365 -> "${days / 365}y ${(days % 365) / 30}mo left"
            days >= 60 -> "${days / 30}mo left"
            days >= 2 -> "${days}d left"
            d.toHours() >= 1 -> "${d.toHours()}h left"
            else -> "${d.toMinutes().coerceAtLeast(1)}m left"
        }
    }

    /** Compact relative timestamp for feeds: "3h ago". */
    fun timeAgo(instant: Instant?, now: Instant = Instant.now()): String {
        instant ?: return ""
        val d = Duration.between(instant, now)
        if (d.isNegative) return "now"
        return when {
            d.toDays() >= 365 -> "${d.toDays() / 365}y ago"
            d.toDays() >= 30 -> "${d.toDays() / 30}mo ago"
            d.toDays() >= 1 -> "${d.toDays()}d ago"
            d.toHours() >= 1 -> "${d.toHours()}h ago"
            d.toMinutes() >= 1 -> "${d.toMinutes()}m ago"
            else -> "just now"
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.US)

    fun date(instant: Instant?): String =
        instant?.let { dateFormatter.format(it.atZone(ZoneId.systemDefault())) } ?: "—"

    fun dateTime(instant: Instant?): String =
        instant?.let { dateTimeFormatter.format(it.atZone(ZoneId.systemDefault())) } ?: "—"

    /** Parses ISO-8601 timestamps that Polymarket returns (tolerant of nulls). */
    fun parseInstant(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso)
        } catch (_: Exception) {
            try {
                java.time.OffsetDateTime.parse(iso).toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Shortens a 0x address for display: 0x1234…abcd */
    fun shortAddress(address: String?): String {
        if (address.isNullOrBlank() || address.length < 10) return address ?: ""
        return "${address.take(6)}…${address.takeLast(4)}"
    }
}
