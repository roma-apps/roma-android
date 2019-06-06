package tech.bigfig.roma.util.extension

import tech.bigfig.roma.entity.Status

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-16.
 */
fun Status.isItemTheSame(status: Status): Boolean {
    return id == status.id
}

fun Status.isContentTheSame(status: Status): Boolean {
    //TODO need to update later
    return content == status.content
}