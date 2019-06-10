package tech.bigfig.roma.components.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import tech.bigfig.roma.BR
import tech.bigfig.roma.R
import tech.bigfig.roma.components.chat.AdapterListener
import tech.bigfig.roma.entity.Account
import tech.bigfig.roma.entity.Status
import tech.bigfig.roma.interfaces.BindableViewHolder
import tech.bigfig.roma.interfaces.LinkListener
import tech.bigfig.roma.util.*
import tech.bigfig.roma.util.extension.isContentTheSame
import tech.bigfig.roma.util.extension.isItemTheSame
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-16.
 */
class ChatAdapter(private val myId: String?, useAbsoluteTime: Boolean, private val clickListener: AdapterListener) : RecyclerView.Adapter<BindableViewHolder>(),
        AdapterClickHandler {
    var useAbsoluteTime: Boolean = useAbsoluteTime
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    private val showContent = mutableMapOf<String,ObservableBoolean>()

    private val shortSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val longSdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())

    private enum class ViewType(val value: Int) {
        Me(0),
        Other(1)
    }

    private val statuses = ArrayList<Status>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindableViewHolder {
        return ChatViewHolder(DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                when (viewType) {
                    ViewType.Me.value -> R.layout.item_chat_me
                    else -> R.layout.item_chat_other
                }, parent, false
        )
        )
    }

    override fun getItemCount(): Int = statuses.size

    override fun onBindViewHolder(holder: BindableViewHolder, position: Int) = holder.bind(position)

    private val mentionClickListener = object : LinkListener {
        override fun onViewTag(tag: String?) {
            clickListener.showTag(tag)
        }

        override fun onViewAccount(id: String?) {
            clickListener.showAccount(id)
        }

        override fun onViewUrl(url: String?) {
            clickListener.showLink(url)
        }

    }

    override fun onAvatarClick(account: Account?) {
        clickListener.showAccount(account?.id)
    }

    override fun onAttachClick(v: View?, status: Status, idx: Int) {
        clickListener.showAttachment(v,status,idx)
    }
    override fun toggleContent(status: Status) {
        showContent[status.id]?.let {
            it.set(!it.get())
        }
    }




    private inner class ChatViewHolder(private val binding: ViewDataBinding) : BindableViewHolder(binding.root) {
        override fun bind(position: Int) {
            val status = statuses[position]
            val nextStatus = if (position == statuses.size - 1) null else statuses[position + 1]
            if (!status.attachments.isNullOrEmpty()){
                val observable = showContent[status.id]?: ObservableBoolean(!status.sensitive)
                showContent[status.id] = observable
                binding.setVariable(BR.isShowContent,observable)
            }
            binding.setVariable(BR.status, status)
            binding.setVariable(BR.isShowText,status.attachments.isNullOrEmpty() || !isMentionsOnly(status))
            if (isNeedShowDate(status, nextStatus)) {
                val date = if (useAbsoluteTime) {
                    getAbsoluteTime(status.createdAt, shortSdf, longSdf)
                } else {
                    val then = status.createdAt.time
                    val now = Date().time
                    DateUtils.getRelativeTimeSpanStringForChat(itemView.context, then, if (now < then) then else now)

                }
                binding.setVariable(BR.date, date)
            } else
                binding.setVariable(BR.date, null)
            binding.root.findViewById<TextView>(R.id.content)?.let { content ->
                val emojifiedText = CustomEmojiHelper.emojifyText(status.content, status.emojis, content)
                LinkHelper.setClickableText(content, emojifiedText, status.mentions, mentionClickListener)

            }
            binding.setVariable(BR.clickHandler, this@ChatAdapter)
            binding.executePendingBindings()
        }
    }

    private fun isMentionsOnly(status: Status): Boolean {
        var content = status.content.toString()
        status.mentions.forEach {
            it.username?.let {username->
                content = content.replace("@$username", "", true)
            }
        }
        return content.isBlank()
    }

    private fun isNeedShowDate(status: Status, prevStatus: Status?): Boolean {
        return if (prevStatus == null)
            true
        else if (!isSameDate(status.createdAt, prevStatus.createdAt))
            false
        else
            return Math.abs(status.createdAt.time - prevStatus.createdAt.time) >= MIN_DIFFERENCE_TO_SHOW_TIME

    }

    override fun getItemViewType(position: Int): Int {
        return when (statuses[position].account.id) {
            myId -> ViewType.Me.value
            else -> ViewType.Other.value
        }
    }

    override fun onMyStatusSettingsClick(view: View, status: Status) {
        clickListener.showMySettings(view,status)
    }
    override fun onOtherStatusSettingsClick(view: View, status: Status) {
        clickListener.showOtherSettings(view,status)
    }

    fun updateStatuses(newStatuses: List<Status>) {
        val result = DiffUtil.calculateDiff(DiffCallback(statuses, newStatuses))
        statuses.clear()
        statuses.addAll(newStatuses)
        result.dispatchUpdatesTo(this)
    }

    private class DiffCallback internal constructor(private val oldItems: List<Status>, private val newItems: List<Status>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            return newItems.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].isItemTheSame(newItems[newItemPosition])
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].isContentTheSame(newItems[newItemPosition])
        }
    }

    companion object {
        const val MIN_DIFFERENCE_TO_SHOW_TIME = 5 * 60 * 1000 //5 minutes in milliseconds
    }
}