package ch.lauzhand.helpinghand.model

/**
 * A date stamp from epoch milliseconds, as dd.MM.yyyy (the Swiss way). The date is in UTC, because there is no
 * time-zone library in the shared code; near midnight it can differ by one day from the local date.
 */
fun formatDate(millis: Long): String {
    // days since 1970-01-01 -> civil date (Howard Hinnant's algorithm)
    val z = millis.floorDiv(86_400_000L) + 719_468L
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yoe + era * 400 + if (month <= 2) 1 else 0
    return "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year"
}
