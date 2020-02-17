package tech.bigfig.roma.util

import android.graphics.Bitmap
import android.graphics.Matrix
import timber.log.Timber
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.util.Util
import tech.bigfig.roma.entity.Attachment
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest

class GlideMatrixTransformation(val focus: Attachment.Focus): BitmapTransformation() {

    private var focalMatrix = Matrix()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)

        messageDigest.update(ByteBuffer.allocate(4).putFloat(focus.x).array())
        messageDigest.update(ByteBuffer.allocate(4).putFloat(focus.y).array())
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        FocalPointUtil.updateFocalPointMatrix(toTransform.width.toFloat(), toTransform.height.toFloat(),
                outWidth.toFloat(), outHeight.toFloat(),
                focus, focalMatrix)
        Timber.d("GlideMatrixTrans","transform: $outWidth-$outHeight")
        return Bitmap.createBitmap(toTransform,0,0,outWidth,outHeight,focalMatrix,true)
    }

    override fun equals(other: Any?): Boolean {
        return other is GlideMatrixTransformation
    }

    override fun hashCode(): Int {
        return Util.hashCode(
                ID.hashCode(),
                Util.hashCode(focus.x)+Util.hashCode(focus.y))
    }


    companion object{
        private const val VERSION = 1
        private const val ID = "tech.bigfig.roma.util.GlideMatrixTransformation.$VERSION"
        private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
    }
}