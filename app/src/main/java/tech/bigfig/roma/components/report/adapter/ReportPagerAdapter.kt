/* Copyright 2019 Joel Pyska
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

package tech.bigfig.roma.components.report.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import tech.bigfig.roma.components.report.fragments.ReportDoneFragment
import tech.bigfig.roma.components.report.fragments.ReportNoteFragment
import tech.bigfig.roma.components.report.fragments.ReportStatusesFragment

class ReportPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> ReportStatusesFragment.newInstance()
            1 -> ReportNoteFragment.newInstance()
            2 -> ReportDoneFragment.newInstance()
            else -> throw IllegalArgumentException("Unknown page index: $position")
        }
    }

    override fun getCount(): Int = 3
}