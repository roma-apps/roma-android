package tech.bigfig.roma.components.chat

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import tech.bigfig.roma.BuildConfig
import tech.bigfig.roma.ComposeActivity
import tech.bigfig.roma.R
import tech.bigfig.roma.db.AccountManager
import tech.bigfig.roma.entity.Attachment
import tech.bigfig.roma.entity.Status
import tech.bigfig.roma.network.MastodonApi
import tech.bigfig.roma.network.ProgressRequestBody
import tech.bigfig.roma.util.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-16.
 */
class ChatViewModel @Inject constructor(
        context: Context,
        private val mastodonApi: MastodonApi,
        accountManager: AccountManager) : AndroidViewModel(context as Application) {
    var cameraUri: Uri? = null
    private var imageToDownsize: Uri? = null
    private val STATUS_IMAGE_SIZE_LIMIT = 8388608 // 8MiB
    private val STATUS_IMAGE_PIXEL_SIZE_LIMIT = 16777216 // 4096^2 Pixels


    private var uriToUpload: Uri? = null
    private var attachment: Attachment? = null

    var imageForCrop: Uri? = null

    private lateinit var statusId: String
    private var url: String? = null
    private val disposables = CompositeDisposable()
    val me = accountManager.activeAccount

    private
    val statusesMutable = MutableLiveData<List<Status>>()
    val statuses: LiveData<List<Status>>
        get() = statusesMutable

    val isSending = ObservableBoolean(false)
    val messageText = ObservableField<String>()

    private var initialStatus: Status? = null

    private val mentions = HashSet<String>()
    private var lastId: String? = null

    var firstVisiblePosition = 0
    var offset = 0

    val isRefreshing = ObservableBoolean(false)
    val isLoading = ObservableBoolean(false)

    val uploadingMediaThumbnail = ObservableField<Bitmap>()
    val uploadingMediaError = ObservableBoolean()
    val uploadingProgress = ObservableInt(0)

    enum class ErrorStatus {
        None,
        IOError,
        Error,
        NoData
    }

    private val sendErrorMutableLiveData = MutableLiveData<ErrorStatus>()
            .apply {
                value = ErrorStatus.None
            }
    val sendErrorLiveData: LiveData<ErrorStatus>
        get() = sendErrorMutableLiveData

    private val errorMutableLiveData = MutableLiveData<ErrorStatus>()
            .apply {
                value = ErrorStatus.None
            }
    val errorLiveData: LiveData<ErrorStatus>
        get() = errorMutableLiveData

    fun setStatus(statusId: String, url: String?) {
        this.statusId = statusId
        this.url = url
        loadData()
    }

    private fun loadData() {
        if (isLoading.get())
            return
        isLoading.set(true)
        errorMutableLiveData.value = ErrorStatus.None
        disposables.add(
                mastodonApi.statusObservable(statusId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { status ->
                                    if (status == null) {
                                        errorMutableLiveData.value = ErrorStatus.NoData
                                        statusesMutable.value = null
                                    } else {
                                        requestAnswers(status)
                                        if (statuses.value.isNullOrEmpty())
                                            pushStatus(status)
                                    }

                                },
                                { error ->
                                    errorMutableLiveData.value = if (error is IOException) ErrorStatus.IOError else ErrorStatus.Error
                                    isRefreshing.set(false)
                                    isLoading.set(false)
                                    statusesMutable.value = null
                                }
                        )
        )
    }

    private fun pushStatus(status: Status) {
        initialStatus = status
        statusesMutable.value = listOf(status)
    }

    private fun requestAnswers(status: Status) {
        disposables.add(
                mastodonApi.statusContextObservable(statusId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { context ->
                                    pushAnswers(status, context.ancestors, context.descendants)
                                    isRefreshing.set(false)
                                    isLoading.set(false)
                                },
                                { error ->
                                    isRefreshing.set(false)
                                    isLoading.set(false)
                                    errorMutableLiveData.value = if (error is IOException) ErrorStatus.IOError else ErrorStatus.Error
                                    statusesMutable.value = null
                                }
                        )
        )
    }

    private fun pushAnswers(status: Status, ancestors: List<Status>, descendants: List<Status>) {
        val data = ArrayList<Status>()
        if (!ancestors.isNullOrEmpty())
            data.addAll(ancestors)
        data.add(status)
        if (!descendants.isNullOrEmpty())
            data.addAll(descendants)
        updateMentions(data)
        data.reverse()
        statusesMutable.postValue(data)
    }

    private fun updateMentions(data: List<Status>) {
        lastId = null
        data.forEach { status ->
            status.mentions.forEach { mention ->
                if (mention.id != me?.accountId)
                    mention.username?.let { userName ->
                        mentions.add(userName)
                    }
            }
            if (lastId == null || status.id > lastId!!)
                lastId = status.id
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun sendMessage() {
        me?.let {
            isSending.set(true)
            val builder = StringBuilder()
            mentions.forEach { mention ->
                if (builder.isNotEmpty())
                    builder.append(' ')
                builder.append('@').append(mention)

            }
            val msg = "$builder ${messageText.get()}"
            disposables.add(mastodonApi.createStatusObservable(
                    "Bearer ${me.accessToken}",
                    me.domain,
                    msg,
                    lastId,
                    null, Status.Visibility.DIRECT.serverString(),
                    false, null, randomAlphanumericString(16))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                isSending.set(false)
                                addStatus(it)
                                messageText.set(null)
                            },
                            {
                                isSending.set(false)
                                sendErrorMutableLiveData
                            }
                    )
            )

        }
    }

    fun sendErrorShown() {
        sendErrorMutableLiveData.value = ErrorStatus.None
    }

    private fun addStatus(status: Status?) {
        if (status != null) {
            statuses.value?.let { statuses ->
                val list = ArrayList<Status>()
                list.add(status)
                list.addAll(statuses)
                statusesMutable.postValue(list)
            }
        }
    }

    fun refresh(isSwipeClick: Boolean = true) {
        if (isSwipeClick)
            isRefreshing.set(true)
        loadData()
    }

    fun uploadMedia(type: ComposeActivity.QueuedMedia.Type, bitmap: Bitmap, uri: Uri, mediaSize: Long) {
        if (uploadingMediaThumbnail.get() == null) {
            uriToUpload = uri
            attachment = null
            uploadingMediaThumbnail.set(bitmap)

            if (type == ComposeActivity.QueuedMedia.Type.IMAGE &&
                    (mediaSize > STATUS_IMAGE_SIZE_LIMIT || getImageSquarePixels(context.contentResolver, uri) > STATUS_IMAGE_PIXEL_SIZE_LIMIT)) {
                try {
                    imageToDownsize = uri
                    downsizeMedia()
                } catch (error: IOException) {
                    uploadingMediaError.set(true)
                }
            } else {

                startUpload()
            }
        }
    }


    fun createNewImageFile(): File {
        // Create an image file name
        val randomId = randomAlphanumericString(12)
        val imageFileName = "Roma_" + randomId + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
    }

    private fun downsizeMedia() {
        imageToDownsize?.let { imageUri ->
            disposables.add(
                    downsizeImage(imageUri, STATUS_IMAGE_SIZE_LIMIT, context.contentResolver, createNewImageFile())
                            .subscribeOn(Schedulers.computation())
                            .subscribe(
                                    { tempFile ->
                                        uriToUpload = FileProvider.getUriForFile(
                                                context,
                                                BuildConfig.APPLICATION_ID + ".fileprovider",
                                                tempFile)
                                        imageToDownsize = null
                                        startUpload()

                                    },
                                    {
                                        uploadingMediaError.set(true)

                                    }
                            )
            )

        }
    }

    fun startUpload() {
        val uri = uriToUpload ?: return
        uploadingMediaError.set(false)
        if (imageToDownsize != null) {
            downsizeMedia()
            return
        }
        if (attachment != null) {
            onUploadSuccess(attachment!!)
            return
        }
        var mimeType = getMimeType(uri, context.contentResolver)
        val map = MimeTypeMap.getSingleton()
        val fileExtension = map.getExtensionFromMimeType(mimeType)
        val filename = String.format("%s_%s_%s.%s",
                context.getString(R.string.app_name),
                Date().time.toString(),
                randomAlphanumericString(10),
                fileExtension)

        val stream: InputStream?

        try {
            stream = context.contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            uploadingMediaError.set(true)
            return
        }


        if (mimeType == null) mimeType = "multipart/form-data"

        uploadingProgress.set(0)

        val fileBody = ProgressRequestBody(stream, getMediaSize(context.contentResolver, uri), MediaType.parse(mimeType),
                ProgressRequestBody.UploadCallback { percentage ->
                    uploadingProgress.set(percentage)
                })

        val body = MultipartBody.Part.createFormData("file", filename, fileBody)

        disposables.add(
                mastodonApi.uploadMediaObservable(body)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.newThread())
                        .subscribe(
                                {
                                    onUploadSuccess(it)
                                },
                                {
                                    uploadingMediaError.set(true)

                                })
        )

    }

    private val context: Context = getApplication()

    private fun onUploadSuccess(attachment: Attachment) {
        this.attachment = attachment
        me?.let {
            val builder = StringBuilder()
            mentions.forEach { mention ->
                if (builder.isNotEmpty())
                    builder.append(' ')
                builder.append('@').append(mention)

            }
            val msg = builder.toString()
            disposables.add(mastodonApi.createStatusObservable(
                    "Bearer ${me.accessToken}",
                    me.domain,
                    msg,
                    lastId,
                    null, Status.Visibility.DIRECT.serverString(),
                    false, listOf(attachment.id), randomAlphanumericString(16))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                uploadingMediaThumbnail.set(null)
                                this@ChatViewModel.attachment = null
                                addStatus(it)
                            },
                            {
                                uploadingMediaError.set(true)
                            }
                    )
            )

        }
    }

    fun cancelUpload() {
        uploadingMediaError.set(false)
        uploadingMediaThumbnail.set(null)
        attachment = null
        uriToUpload = null
    }

    fun deleteStatus(status: Status) {
        val newStatuses = ArrayList<Status>()
        statuses.value?.forEach {
            if (it.id != status.id)
                newStatuses.add(it)
        }
        statusesMutable.value = newStatuses
        disposables.add(
                mastodonApi.deleteStatusObservable(status.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                {
                                    statusesMutable.value?.let {
                                        updateMentions(it)
                                    }
                                    //refresh(false)
                                },
                                {
                                    refresh(false)
                                }
                        )
        )

    }

}