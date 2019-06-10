package tech.bigfig.roma.components.chat

import android.view.View
import tech.bigfig.roma.entity.Status

interface AdapterListener {
    fun showAccount(id: String?)
    fun showLink(link: String?)
    fun showTag(tag: String?)
    fun showAttachment(v: View?, status: Status, idx: Int)
    fun showMySettings(v: View, status: Status)
    fun showOtherSettings(v: View, status: Status)
}