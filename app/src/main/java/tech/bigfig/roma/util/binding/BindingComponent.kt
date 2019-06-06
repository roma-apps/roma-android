package tech.bigfig.roma.util.binding

import androidx.databinding.DataBindingComponent

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-18.
 */
class BindingComponent: DataBindingComponent {
    override fun getImageBindingAdapter(): ImageBindingAdapter {
        return ImageBindingAdapter()
    }
    override fun getOtherBindingAdapter(): OtherBindingAdapter {
        return OtherBindingAdapter()
    }
    override fun getChatBindingAdapter(): ChatBindingAdapter {
        return ChatBindingAdapter()
    }
}