/* Copyright 2019 Joel Pyska
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.roma.components.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import tech.bigfig.roma.R
import tech.bigfig.roma.adapter.StatusViewHolder
import tech.bigfig.roma.entity.Status
import tech.bigfig.roma.interfaces.StatusActionListener
import tech.bigfig.roma.viewdata.StatusViewData

class SearchStatusesAdapter(private val useAbsoluteTime: Boolean,
                            private val mediaPreviewEnabled: Boolean,
                            private val showBotOverlay: Boolean,
                            private val animateAvatar: Boolean,
                            private val statusListener: StatusActionListener)
    : PagedListAdapter<Pair<Status, StatusViewData.Concrete>, RecyclerView.ViewHolder>(STATUS_COMPARATOR) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_status, parent, false)
        return StatusViewHolder(view, useAbsoluteTime)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let { item ->
            (holder as? StatusViewHolder)?.setupWithStatus(item.second, statusListener,
                    mediaPreviewEnabled, showBotOverlay, animateAvatar)
        }

    }

    public override fun getItem(position: Int): Pair<Status, StatusViewData.Concrete>? {
        return super.getItem(position)
    }

    companion object {

        val STATUS_COMPARATOR = object : DiffUtil.ItemCallback<Pair<Status, StatusViewData.Concrete>>() {
            override fun areContentsTheSame(oldItem: Pair<Status, StatusViewData.Concrete>, newItem: Pair<Status, StatusViewData.Concrete>): Boolean =
                    oldItem.second.deepEquals(newItem.second)

            override fun areItemsTheSame(oldItem: Pair<Status, StatusViewData.Concrete>, newItem: Pair<Status, StatusViewData.Concrete>): Boolean =
                    oldItem.second.id == newItem.second.id
        }

    }

}