package tech.bigfig.roma.util.binding

import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import tech.bigfig.roma.R

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-18.
 */
class OtherBindingAdapter {
    @BindingAdapter(value = ["isRefreshing"])
    fun setRefreshing(view: SwipeRefreshLayout, isRefreshing: Boolean) {
        view.isRefreshing = isRefreshing
    }
    @BindingAdapter(value = ["layout_constraintWidth_percent"])
    fun setWidthPercent(view: View, width: Float){
        val layout = view.parent as? ConstraintLayout?:return
        val set = ConstraintSet()
        set.clone(layout)
        set.constrainPercentWidth(view.id,width)
        set.applyTo(layout)
    }
}