package tech.bigfig.roma.interfaces

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-16.
 */
open class BindableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    open fun bind(position: Int) {}
}