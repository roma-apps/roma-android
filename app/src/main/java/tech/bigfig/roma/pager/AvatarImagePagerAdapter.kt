package tech.bigfig.roma.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

import java.lang.IllegalStateException

import tech.bigfig.roma.fragment.ViewMediaFragment
import tech.bigfig.roma.SharedElementTransitionListener

class AvatarImagePagerAdapter(fragmentManager: FragmentManager, private val avatarUrl: String) : FragmentPagerAdapter(fragmentManager), SharedElementTransitionListener {

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            ViewMediaFragment.newAvatarInstance(avatarUrl)
        } else {
            throw IllegalStateException()
        }
    }

    override fun getCount() = 1

    override fun onTransitionEnd() {
    }
}
