package com.james.anvil

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatThreadLocal = ThreadLocal.withInitial {
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
}

fun formatDate(timestamp: Long): String {
    return dateFormatThreadLocal.get()!!.format(Date(timestamp))
}
