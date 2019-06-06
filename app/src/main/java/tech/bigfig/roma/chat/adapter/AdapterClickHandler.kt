package tech.bigfig.roma.chat.adapter

import android.view.View
import tech.bigfig.roma.entity.Account
import tech.bigfig.roma.entity.Status


interface AdapterClickHandler {
    fun onAvatarClick(account: Account?)
    fun onAttachClick(v: View?, status: Status, idx: Int )
    fun toggleContent(status: Status)
    fun onMyStatusSettingsClick(view: View, status: Status)
    fun onOtherStatusSettingsClick(view: View, status: Status)
}