package tech.bigfig.roma.util.binding

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import timber.log.Timber
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import tech.bigfig.roma.R
import tech.bigfig.roma.entity.Attachment
import tech.bigfig.roma.entity.Status
import tech.bigfig.roma.util.GlideMatrixTransformation
import tech.bigfig.roma.util.RoundedCornersCrop
import tech.bigfig.roma.util.ThemeUtils

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-18.
 */
class ChatBindingAdapter {
    @BindingAdapter(value = ["chatPreview", "cropRadius", "chatPreviewIdx"], requireAll = true)
    fun setImagePreview(view: ImageView, status: Status?, cropRadius: Int, chatPreviewIdx: Int) {
        val link = status?.attachments?.getOrNull(chatPreviewIdx)?.previewUrl
        if (link != null) {
            val focalPoint = status.attachments[chatPreviewIdx].meta?.focus
            val size = status.attachments.size
            val cropTopLeft = chatPreviewIdx == 0
            val cropTopRight = chatPreviewIdx == 1 || chatPreviewIdx == 0 && size == 1
            val cropBottomLeft = chatPreviewIdx == 2 || chatPreviewIdx == 0 && size <3
            val cropBottomRight = chatPreviewIdx == 3 || chatPreviewIdx == 2 && size == 3 || chatPreviewIdx == 0 && size == 1 || chatPreviewIdx == 1 && size == 2
            val mediaPreviewUnloadedId = ThemeUtils.getDrawableId(view.context, R.attr.media_preview_unloaded_drawable,
                    android.R.color.black)
            view.visibility = View.VISIBLE
            var builder = Glide.with(view)
                    .load(link)
                    .placeholder(mediaPreviewUnloadedId)
                    .error(mediaPreviewUnloadedId)
            builder = if (focalPoint != null)
                builder.transform(GlideMatrixTransformation(focalPoint), RoundedCornersCrop(
                        if (cropTopLeft) cropRadius else 0,
                        if (cropTopRight) cropRadius else 0,
                        if (cropBottomLeft) cropRadius else 0,
                        if (cropBottomRight) cropRadius else 0
                ))
            else
                builder.transform(CenterCrop(), RoundedCornersCrop(
                        if (cropTopLeft) cropRadius else 0,
                        if (cropTopRight) cropRadius else 0,
                        if (cropBottomLeft) cropRadius else 0,
                        if (cropBottomRight) cropRadius else 0
                ))

            builder.into(view)
        } else
            view.visibility = View.GONE
    }
    @BindingAdapter(value = ["bitmap"])
    fun setImageBitmap(view: ImageView, bitmap: Bitmap?) {
        view.setImageBitmap(bitmap)
    }
}