package com.alex.xdw.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

fun formatTimestamp(value: Long): String = dateFormatter.format(Date(value))
