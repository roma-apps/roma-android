package tech.bigfig.roma.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tech.bigfig.roma.R
import tech.bigfig.roma.interfaces.LinkListener

class HashtagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val hashtag: TextView = itemView.findViewById(R.id.hashtag)

    fun setup(tag: String, listener: LinkListener) {
        hashtag.text = String.format("#%s", tag)
        hashtag.setOnClickListener { listener.onViewTag(tag) }
    }
}