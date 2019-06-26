/* Copyright 2019 Conny Duck
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.roma.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.emoji.text.EmojiCompat
import androidx.recyclerview.widget.RecyclerView
import tech.bigfig.roma.viewdata.PollOptionViewData
import tech.bigfig.roma.viewdata.calculatePercent
import tech.bigfig.roma.R
import tech.bigfig.roma.entity.Emoji
import tech.bigfig.roma.util.CustomEmojiHelper
import tech.bigfig.roma.util.HtmlUtils
import tech.bigfig.roma.util.visible

class PollAdapter: RecyclerView.Adapter<PollViewHolder>() {

    private var pollOptions: List<PollOptionViewData> = emptyList()
    private var voteCount: Int = 0
    private var mode = RESULT
    private var emojis: List<Emoji> = emptyList()

    fun setup(options: List<PollOptionViewData>, voteCount: Int, emojis: List<Emoji>, mode: Int) {
        this.pollOptions = options
        this.voteCount = voteCount
        this.emojis = emojis
        this.mode = mode
        notifyDataSetChanged()
    }

    fun getSelected() : List<Int> {
        return pollOptions.filter { it.selected }
                .map { pollOptions.indexOf(it) }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        return PollViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_poll, parent, false))
    }

    override fun getItemCount(): Int {
        return pollOptions.size
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {

        val option = pollOptions[position]

        holder.resultTextView.visible(mode == RESULT)
        holder.radioButton.visible(mode == SINGLE)
        holder.checkBox.visible(mode == MULTIPLE)

        when(mode) {
            RESULT -> {
                val percent = calculatePercent(option.votesCount, voteCount)

                val pollOptionText = holder.resultTextView.context.getString(R.string.poll_option_format, percent, option.title)

                val emojifiedPollOptionText = CustomEmojiHelper.emojifyText(HtmlUtils.fromHtml(pollOptionText), emojis, holder.resultTextView)
                holder.resultTextView.text =  EmojiCompat.get().process(emojifiedPollOptionText)

                val level = percent * 100

                holder.resultTextView.background.level = level

            }
            SINGLE -> {
                val emojifiedPollOptionText = CustomEmojiHelper.emojifyString(option.title, emojis, holder.radioButton)
                holder.radioButton.text = EmojiCompat.get().process(emojifiedPollOptionText)
                holder.radioButton.isChecked = option.selected
                holder.radioButton.setOnClickListener {
                    pollOptions.forEachIndexed { index, pollOption ->
                        pollOption.selected = index == holder.adapterPosition
                        notifyItemChanged(index)
                    }
                }
            }
            MULTIPLE -> {
                val emojifiedPollOptionText = CustomEmojiHelper.emojifyString(option.title, emojis, holder.checkBox)
                holder.checkBox.text = EmojiCompat.get().process(emojifiedPollOptionText)
                holder.checkBox.isChecked = option.selected
                holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                    pollOptions[holder.adapterPosition].selected = isChecked
                }
            }
        }

    }

    companion object {
        const val RESULT = 0
        const val SINGLE = 1
        const val MULTIPLE = 2
    }
}



class PollViewHolder(view: View): RecyclerView.ViewHolder(view) {

    val resultTextView: TextView = view.findViewById(R.id.status_poll_option_result)
    val radioButton: RadioButton = view.findViewById(R.id.status_poll_radio_button)
    val checkBox: CheckBox = view.findViewById(R.id.status_poll_checkbox)

}