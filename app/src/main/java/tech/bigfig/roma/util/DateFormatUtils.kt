package tech.bigfig.roma.util

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-18.
 */

fun isSameDate(date1: Date, date2: Date): Boolean {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return dateFormat.format(date1) == dateFormat.format(date2)
}

fun getAbsoluteTime(createdAt: Date?, shortSdf: DateFormat, longSdf:DateFormat): String {
    return if (createdAt != null) {
        if (DateUtils.isToday(createdAt.time)) {
            shortSdf.format(createdAt)
        } else {
            longSdf.format(createdAt)
        }
    } else {
        "??:??:??"
    }
}
