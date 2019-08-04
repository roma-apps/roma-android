package tech.bigfig.roma.pager

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

import tech.bigfig.roma.entity.Attachment
import tech.bigfig.roma.SharedElementTransitionListener
import tech.bigfig.roma.fragment.ViewMediaFragment

import java.lang.IllegalStateException
import java.util.*

class ImagePagerAdapter(
        fragmentManager: FragmentManager,
        private val attachments: List<Attachment>,
        private val initialPosition: Int
) : FragmentStatePagerAdapter(fragmentManager), SharedElementTransitionListener {

    private var primaryItem: ViewMediaFragment? = null

    override fun setPrimaryItem(container: ViewGroup, position: Int, item: Any) {
        super.setPrimaryItem(container, position, item)
        this.primaryItem = item as ViewMediaFragment
    }

    override fun getItem(position: Int): Fragment {
        return if (position >= 0 && position < attachments.size) {
            ViewMediaFragment.newInstance(attachments[position], position == initialPosition)
        } else {
            throw IllegalStateException()
        }
    }

    override fun getCount(): Int {
        return attachments.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return String.format(Locale.getDefault(), "%d/%d", position + 1, attachments.size)
    }

    override fun onTransitionEnd() {
        primaryItem?.onTransitionEnd()
    }
}
