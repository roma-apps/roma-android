package tech.bigfig.roma.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.reactivex.Single
import java.io.*
import java.lang.RuntimeException

fun downsizeImage(uri: Uri, sizeLimit:Int, contentResolver: ContentResolver, tempFile: File): Single<File> {
    return Single.fromCallable {
        var inputStream: InputStream?
        inputStream = contentResolver.openInputStream(uri)

        // Initially, just get the image dimensions.
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)
        IOUtils.closeQuietly(inputStream)
        // Get EXIF data, for orientation info.
        val orientation = getImageOrientation(uri, contentResolver)
        /* Unfortunately, there isn't a determined worst case compression ratio for image
             * formats. So, the only way to tell if they're too big is to compress them and
             * test, and keep trying at smaller sizes. The initial estimate should be good for
             * many cases, so it should only iterate once, but the loop is used to be absolutely
             * sure it gets downsized to below the limit. */
        var scaledImageSize = 1024
        do {
            val stream: OutputStream
            stream = FileOutputStream(tempFile)

            inputStream = contentResolver.openInputStream(uri)

            options.inSampleSize = calculateInSampleSize(options, scaledImageSize, scaledImageSize)
            options.inJustDecodeBounds = false
            val scaledBitmap: Bitmap = try{
                BitmapFactory.decodeStream(inputStream, null, options)
            } finally {
                IOUtils.closeQuietly(inputStream)
            } ?: throw RuntimeException("Failed to decode stream")
            val reorientedBitmap = reorientBitmap(scaledBitmap, orientation)
            if (reorientedBitmap == null) {
                scaledBitmap.recycle()
                throw RuntimeException("Failed to reorient stream")
            }
            val format: Bitmap.CompressFormat
            /* It's not likely the user will give transparent images over the upload limit, but
                 * if they do, make sure the transparency is retained. */
            format = if (!reorientedBitmap!!.hasAlpha()) {
                Bitmap.CompressFormat.JPEG
            } else {
                Bitmap.CompressFormat.PNG
            }
            reorientedBitmap.compress(format, 85, stream)
            reorientedBitmap.recycle()
            scaledImageSize /= 2
        } while (tempFile.length() > sizeLimit)
        tempFile
    }
}