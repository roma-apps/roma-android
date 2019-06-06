package tech.bigfig.roma.util

import android.graphics.*
import android.os.Build
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.util.Util
import java.nio.ByteBuffer
import java.security.MessageDigest

class RoundedCornersCrop(val topLeftRadius: Int = 0, val topRightRadius: Int = 0, val bottomLeftRadius: Int = 0, val bottomRightRadius: Int = 0) : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return roundedCorners(pool, toTransform)
    }

    override fun equals(other: Any?): Boolean {
        (other as? RoundedCornersCrop)?.let {
            return topLeftRadius == other.topLeftRadius && topRightRadius == other.topRightRadius &&
                    bottomLeftRadius == other.bottomLeftRadius && bottomRightRadius == other.bottomRightRadius
        }
        return false
    }

    override fun hashCode(): Int {
        return Util.hashCode(
                ID.hashCode(),
                Util.hashCode(topLeftRadius)+Util.hashCode(topRightRadius)+Util.hashCode(bottomLeftRadius)+Util.hashCode(bottomRightRadius)
        )
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)

        var radiusData = ByteBuffer.allocate(4).putInt(topLeftRadius).array()
        messageDigest.update(radiusData)
        radiusData = ByteBuffer.allocate(4).putInt(topRightRadius).array()
        messageDigest.update(radiusData)
        radiusData = ByteBuffer.allocate(4).putInt(bottomLeftRadius).array()
        messageDigest.update(radiusData)
        radiusData = ByteBuffer.allocate(4).putInt(bottomRightRadius).array()
        messageDigest.update(radiusData)
    }

    companion object {
        private const val VERSION = 1
        private const val ID = "com.asights.eguide.utils.RoundedCornersCrop.$VERSION"
        private val ID_BYTES: ByteArray = ID.toByteArray(Key.CHARSET)

    }

    private fun roundedCorners(
            pool: BitmapPool, inBitmap: Bitmap): Bitmap {

        // Alpha is required for this transformation.
        val safeConfig = getAlphaSafeConfig(inBitmap)
        val toTransform = getAlphaSafeBitmap(pool, inBitmap)
        val result = pool.get(toTransform.width, toTransform.height, safeConfig)

        result.setHasAlpha(true)

        val shader = BitmapShader(
                toTransform, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
        )
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = shader
        val rect = RectF(0f, 0f, result.width.toFloat(), result.height.toFloat())
        TransformationUtils.getBitmapDrawableLock().lock()
        try {
            val canvas = Canvas(result)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val path = Path()
            path.moveTo(0f,-topLeftRadius.toFloat())
            if (topLeftRadius>0) {
                path.arcTo(RectF(0f,0f,2f*topLeftRadius,2f*topLeftRadius),-180f,90f,false)
            }
            path.lineTo(rect.right-topRightRadius,0f)
            if (topRightRadius>0){
                path.arcTo(RectF(rect.right-2*topRightRadius,0f,rect.right,2f*topRightRadius),-90f,90f,false)
            }
            path.lineTo(rect.right,rect.bottom-bottomRightRadius)
            if (bottomRightRadius>0){
                path.arcTo(RectF(rect.right-2*bottomRightRadius,rect.bottom-2*bottomRightRadius,rect.right,rect.bottom),0f,90f,false)
            }
            path.lineTo(bottomLeftRadius.toFloat(),rect.bottom)
            if (bottomLeftRadius>0){
                path.arcTo(RectF(0f,rect.bottom-2*bottomLeftRadius,2f*bottomLeftRadius,rect.bottom),90f,90f,false)
            }
            path.close()//Given close, last lineto can be removed.
            canvas.drawPath(path,paint)
            clear(canvas)
        } finally {
            TransformationUtils.getBitmapDrawableLock().unlock()
        }

        if (toTransform != inBitmap) {
            pool.put(toTransform)
        }

        return result
    }

    private fun getAlphaSafeConfig(inBitmap: Bitmap): Bitmap.Config {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Avoid short circuiting the sdk check.
            if (Bitmap.Config.RGBA_F16 == inBitmap.config) { // NOPMD
                return Bitmap.Config.RGBA_F16
            }
        }

        return Bitmap.Config.ARGB_8888
    }

    private fun getAlphaSafeBitmap(
            pool: BitmapPool, maybeAlphaSafe: Bitmap
    ): Bitmap {
        val safeConfig = getAlphaSafeConfig(maybeAlphaSafe)
        if (safeConfig == maybeAlphaSafe.config) {
            return maybeAlphaSafe
        }

        val argbBitmap = pool.get(maybeAlphaSafe.width, maybeAlphaSafe.height, safeConfig)
        Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0f /*left*/, 0f /*top*/, null /*paint*/)

        // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
        // when we're finished with it.
        return argbBitmap
    }

    private fun clear(canvas: Canvas) {
        canvas.setBitmap(null)
    }

}
