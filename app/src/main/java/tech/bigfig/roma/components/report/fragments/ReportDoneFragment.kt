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

package tech.bigfig.roma.components.report.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import tech.bigfig.roma.components.report.ReportViewModel
import tech.bigfig.roma.components.report.Screen
import kotlinx.android.synthetic.main.fragment_report_done.*
import tech.bigfig.roma.R
import tech.bigfig.roma.di.Injectable
import tech.bigfig.roma.di.ViewModelFactory
import tech.bigfig.roma.util.Loading
import tech.bigfig.roma.util.hide
import tech.bigfig.roma.util.show
import javax.inject.Inject


class ReportDoneFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: ReportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)[ReportViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report_done, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textReported.text = getString(R.string.report_sent_success, viewModel.accountUserName)
        handleClicks()
        subscribeObservables()
    }

    private fun subscribeObservables() {
        viewModel.muteState.observe(viewLifecycleOwner, Observer {
            if (it !is Loading) {
                buttonMute.show()
                progressMute.show()
            } else {
                buttonMute.hide()
                progressMute.hide()
            }

            buttonMute.setText(when {
                it.data == true -> R.string.action_unmute
                else -> R.string.action_mute
            })
        })

        viewModel.blockState.observe(viewLifecycleOwner, Observer {
            if (it !is Loading) {
                buttonBlock.show()
                progressBlock.show()
            }
            else{
                buttonBlock.hide()
                progressBlock.hide()
            }
            buttonBlock.setText(when {
                it.data == true -> R.string.action_unblock
                else -> R.string.action_block
            })
        })

    }

    private fun handleClicks() {
        buttonDone.setOnClickListener {
            viewModel.navigateTo(Screen.Finish)
        }
        buttonBlock.setOnClickListener {
            viewModel.toggleBlock()
        }
        buttonMute.setOnClickListener {
            viewModel.toggleMute()
        }
    }

    companion object {
        fun newInstance() = ReportDoneFragment()
    }

}
