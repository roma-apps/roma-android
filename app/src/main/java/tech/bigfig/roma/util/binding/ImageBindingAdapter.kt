package tech.bigfig.roma.util.binding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import tech.bigfig.roma.R

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-18.
 */
class ImageBindingAdapter {
    @BindingAdapter(value = ["avatar"])
    fun setImageRef(view: ImageView, link: String?) {
        if (link != null) {
            Glide.with(view)
                    .load(link)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_default)
                    .into(view)
        } else {
            view.setImageResource(R.drawable.avatar_default)
        }
    }
}