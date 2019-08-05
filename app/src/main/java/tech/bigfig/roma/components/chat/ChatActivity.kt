package tech.bigfig.roma.components.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import tech.bigfig.roma.components.report.ReportActivity
import com.theartofdev.edmodo.cropper.CropImage
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import com.uber.autodispose.autoDisposable
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import tech.bigfig.roma.*
import tech.bigfig.roma.components.chat.adapter.ChatAdapter
import tech.bigfig.roma.databinding.ActivityChatBinding
import tech.bigfig.roma.di.ViewModelFactory
import tech.bigfig.roma.entity.Attachment
import tech.bigfig.roma.entity.Status
import tech.bigfig.roma.util.*
import tech.bigfig.roma.viewdata.AttachmentViewData
import java.io.File
import java.io.IOException
import javax.inject.Inject

class ChatActivity : BaseActivity(), HasAndroidInjector, ClickHandler, AdapterListener {
    private var downsizeDisposable: Disposable? = null

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: ChatViewModel

    private lateinit var adapter: ChatAdapter

    private lateinit var binding: ActivityChatBinding

    private lateinit var preferences: SharedPreferences

    private var thumbnailViewSize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thumbnailViewSize = resources.getDimensionPixelSize(R.dimen.chat_upload_media_preview_width)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[ChatViewModel::class.java]
        binding.viewModel = viewModel
        binding.clickHandler = this

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_direct_messages)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
        val statusId = intent.getStringExtra(STATUS_ID)
        if (statusId == null) {
            finish()
            return
        }
        viewModel.setStatus(statusId, intent.getStringExtra(URL))
        initStatusesView()
        initRefreshLayout()
        subscribeObservers()
    }

    private fun initRefreshLayout() {
        binding.refreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
        binding.refreshLayout.setColorSchemeResources(R.color.roma_blue)
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ThemeUtils.getColor(this,
                android.R.attr.colorBackground))

    }

    private fun initStatusesView() {
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)
        binding.items.layoutManager = layoutManager
        adapter = ChatAdapter(viewModel.me?.accountId, preferences.getBoolean("absoluteTimeView", false), this)
        binding.items.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePosition == -1 || positionStart == 0 && firstVisiblePosition == 0)
                    binding.items.postDelayed({
                        binding.items.scrollToPosition(positionStart)
                    }, 50)
            }
        })

        binding.items.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateFirstVisible()
                }
            }
        })

        binding.items.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom != oldBottom) {
                binding.items.post {
                    layoutManager.scrollToPositionWithOffset(viewModel.firstVisiblePosition, viewModel.offset)
                }
            }
        }

    }

    private fun updateFirstVisible() {
        (binding.items.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
            viewModel.firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val view = layoutManager.findViewByPosition(viewModel.firstVisiblePosition)
            viewModel.offset = binding.items.height - (view?.bottom ?: 0)
        }
    }

    private fun subscribeObservers() {
        viewModel.statuses.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                binding.items.visibility = View.VISIBLE
                adapter.updateStatuses(it)
            } else {
                binding.items.visibility = View.GONE
            }
        })
        viewModel.errorLiveData.observe(this, Observer { errorStatus ->
            when (errorStatus) {
                ChatViewModel.ErrorStatus.None -> {
                    binding.statusView.visibility = View.GONE
                }
                ChatViewModel.ErrorStatus.IOError -> {
                    binding.statusView.visibility = View.VISIBLE
                    binding.statusView.setup(R.drawable.elephant_offline, R.string.error_network) {
                        viewModel.refresh(false)
                    }
                }
                ChatViewModel.ErrorStatus.Error -> {
                    binding.statusView.visibility = View.VISIBLE
                    binding.statusView.setup(R.drawable.elephant_error, R.string.error_generic) {
                        viewModel.refresh(false)
                    }

                }
                ChatViewModel.ErrorStatus.NoData -> {
                    binding.statusView.visibility = View.VISIBLE
                    binding.statusView.setup(R.drawable.elephant_friend_empty, R.string.message_empty, null)
                }
                else -> {
                    binding.statusView.visibility = View.GONE
                }
            }
        })
        viewModel.sendErrorLiveData.observe(this, Observer { errorStatus ->
            when (errorStatus) {
                ChatViewModel.ErrorStatus.IOError -> {
                    showToast(R.string.error_network)
                    viewModel.sendErrorShown()

                }
                ChatViewModel.ErrorStatus.Error -> {
                    showToast(R.string.error_generic)
                    viewModel.sendErrorShown()
                }
                else -> {
                }
            }

        })
    }

    private fun showToast(errorId: Int) {
        Toast.makeText(this, errorId, Toast.LENGTH_LONG).show()
    }

    override fun onSendClick() {
        viewModel.sendMessage()
    }

    override fun onAttachClick() {
        val menu = PopupMenu(this, binding.buttonAttach)
        menu.inflate(R.menu.choose_media_menu)
        menu.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.pickMedia -> {
                    pickMedia()
                    true
                }
                R.id.takePhoto -> {
                    takePhoto()
                    true
                }
                else -> {
                    false
                }
            }
        }
        menu.show()
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = viewModel.createNewImageFile()
            } catch (ex: IOException) {
                displayTransientError(R.string.error_media_upload_opening)
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                viewModel.cameraUri = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.cameraUri)
                startActivityForResult(intent, MEDIA_TAKE_PHOTO_RESULT)
            }
        }

    }

    override fun onCancelUpload() {
        viewModel.cancelUpload()
    }

    private fun pickMedia() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        } else {
            initiateMediaPicking()
        }
    }

    private fun initiateMediaPicking() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        val mimeTypes = arrayOf("image/*", "video/*")
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(intent, MEDIA_PICK_RESULT)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showAccount(id: String?) {
        id?.let {
            startActivityWithSlideInAnimation(AccountActivity.getIntent(this, id))
        }

    }

    override fun showLink(link: String?) {
        link?.let {
            LinkHelper.openLink(link, this)
        }
    }

    override fun showTag(tag: String?) {
        val intent = Intent(this, ViewTagActivity::class.java)
        intent.putExtra("hashtag", tag)
        startActivityWithSlideInAnimation(intent)
    }

    override fun showAttachment(v: View?, status: Status, idx: Int) {
        when (status.attachments[idx].type) {
            Attachment.Type.IMAGE,
            Attachment.Type.GIFV,
            Attachment.Type.VIDEO -> {
                val intent = ViewMediaActivity.newIntent(this, AttachmentViewData.list(status), idx)
                if (v != null) {
                    val url = status.attachments[idx].url
                    ViewCompat.setTransitionName(v, url)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, url)
                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
            Attachment.Type.UNKNOWN -> {
            }/* Intentionally do nothing. This case is here is to handle when new attachment
                 * types are added to the API before code is added here to handle them. So, the
                 * best fallback is to just show the preview and ignore requests to view them. */

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initiateMediaPicking()
                } else {
                    doErrorDialog(R.string.error_media_upload_permission, R.string.action_retry,
                            View.OnClickListener { pickMedia() })

                }
            }
        }
    }

    private fun doErrorDialog(@StringRes descriptionId: Int, @StringRes actionId: Int,
                              listener: View.OnClickListener) {
        val bar = Snackbar.make(binding.root, getString(descriptionId),
                Snackbar.LENGTH_SHORT)
        bar.setAction(actionId, listener)
        //necessary so snackbar is shown over everything
        bar.view.elevation = resources.getDimensionPixelSize(R.dimen.compose_activity_snackbar_elevation).toFloat()
        bar.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == MEDIA_PICK_RESULT && data != null) {
            val uri = data.data
            if (uri != null && isImageMedia(contentResolver, uri))
                startCropActivity(uri)
            else {
                processMedia(uri)
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == MEDIA_TAKE_PHOTO_RESULT) {
            viewModel.cameraUri?.let { cameraUri ->
                if (viewModel.cameraUri != null && isImageMedia(contentResolver, cameraUri))
                    startCropActivity(cameraUri)
                else {
                    processMedia(cameraUri)
                }
                viewModel.cameraUri = null
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_CANCELED) {
                processMedia(viewModel.imageForCrop)
            } else if (resultCode == Activity.RESULT_OK) {
                if (result != null)
                    processMedia(result.uri)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Snackbar.make(binding.root, R.string.error_media_crop, Snackbar.LENGTH_LONG).show()
                processMedia(viewModel.imageForCrop)
            }
            viewModel.imageForCrop = null
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun processMedia(uri: Uri?) {
        if (uri != null) {
            val mediaSize = getMediaSize(contentResolver, uri)
            pickMedia(uri, mediaSize)
        }
    }

    private fun pickMedia(uri: Uri, mediaSize: Long) {
        if (mediaSize == MEDIA_SIZE_UNKNOWN) {
            displayTransientError(R.string.error_media_upload_opening)
            return
        }

        val contentResolver = contentResolver
        val mimeType = getMimeType(uri, contentResolver)

        if (mimeType != null) {
            when (mimeType.substring(0, mimeType.indexOf('/'))) {
                "video" -> {
                    processPickVideo(uri, mediaSize)
                }
                "image" -> {
                    processPickImage(uri, mediaSize)
                }
                else -> {
                    displayTransientError(R.string.error_media_upload_type)
                }
            }
        } else {
            displayTransientError(R.string.error_media_upload_type)
        }
    }

    private fun processPickImage(uri: Uri, mediaSize: Long) {
        downsizeDisposable?.dispose()
        downsizeDisposable = Single.fromCallable {
            Optional.of(getImageThumbnail(contentResolver, uri, thumbnailViewSize))
        }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturnItem(Optional.empty())
                .autoDisposable(from(this, Lifecycle.Event.ON_DESTROY))
                .subscribe { optionalBitmap ->
                    if (!optionalBitmap.isEmpty()) {
                        viewModel.uploadMedia(ComposeActivity.QueuedMedia.Type.IMAGE, optionalBitmap.value, uri, mediaSize)
                    } else {
                        displayTransientError(R.string.error_media_upload_opening)
                    }
                }
    }

    private fun processPickVideo(uri: Uri, mediaSize: Long) {
        downsizeDisposable?.dispose()
        if (mediaSize > STATUS_VIDEO_SIZE_LIMIT) {
            displayTransientError(R.string.error_image_upload_size)
            return
        }

        downsizeDisposable = Single.fromCallable {
            Optional.of(getVideoThumbnail(this, uri, thumbnailViewSize))
        }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturnItem(Optional.empty())
                .autoDisposable(from(this, Lifecycle.Event.ON_DESTROY))
                .subscribe { optionalBitmap ->
                    if (!optionalBitmap.isEmpty()) {
                        viewModel.uploadMedia(ComposeActivity.QueuedMedia.Type.VIDEO, optionalBitmap.value, uri, mediaSize)
                    } else {
                        displayTransientError(R.string.error_media_upload_opening)
                    }
                }
    }

    private fun displayTransientError(@StringRes stringId: Int) {
        val bar = Snackbar.make(binding.root, stringId, Snackbar.LENGTH_LONG)
        //necessary so snackbar is shown over everything
        bar.view.elevation = resources.getDimensionPixelSize(R.dimen.compose_activity_snackbar_elevation).toFloat()
        bar.show()
    }

    private fun startCropActivity(uri: Uri) {
        viewModel.imageForCrop = uri
        CropImage.activity(uri)
                .setInitialCropWindowPaddingRatio(0f)
                .start(this)
    }

    override fun onTryUploadAgain() {
        viewModel.startUpload()
    }

    override fun showMySettings(v: View, status: Status) {
        PopupMenu(this, v)
                .apply {
                    inflate(R.menu.chat_status_my)
                    setOnMenuItemClickListener {
                        when {
                            it.itemId == R.id.actionStatusDelete -> {
                                viewModel.deleteStatus(status)
                                true
                            }
                            it.itemId == R.id.actionViewMyAccount -> return@setOnMenuItemClickListener viewModel.me?.accountId?.let { myId ->
                                ContextCompat.startActivity(this@ChatActivity, AccountActivity.getIntent(this@ChatActivity, myId),
                                        ActivityOptionsCompat.makeCustomAnimation(this@ChatActivity, R.anim.slide_from_right, R.anim.slide_to_left).toBundle())
                                true
                            } ?: false
                            else -> false
                        }
                    }
                }
                .show()
    }

    override fun showOtherSettings(v: View, status: Status) {
        PopupMenu(this, v)
                .apply {
                    inflate(R.menu.chat_status_other)
                    setOnMenuItemClickListener {
                        when {
                            it.itemId == R.id.actionStatusReport -> {
                                ContextCompat.startActivity(this@ChatActivity, ReportActivity.getIntent(this@ChatActivity, status.account.id, status.account.username, status.id, status.content),
                                        ActivityOptionsCompat.makeCustomAnimation(this@ChatActivity, R.anim.slide_from_right, R.anim.slide_to_left).toBundle())

                                true
                            }
                            it.itemId == R.id.actionViewOtherAccount -> {
                                ContextCompat.startActivity(this@ChatActivity, AccountActivity.getIntent(this@ChatActivity, status.account.id),
                                        ActivityOptionsCompat.makeCustomAnimation(this@ChatActivity, R.anim.slide_from_right, R.anim.slide_to_left).toBundle())
                                true
                            }
                            else -> false
                        }
                    }
                }
                .show()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1
        private const val MEDIA_PICK_RESULT = 1
        private const val MEDIA_TAKE_PHOTO_RESULT = 2
        private const val STATUS_ID = "status.id"
        private const val URL = "url"
        private const val STATUS_VIDEO_SIZE_LIMIT = 41943040 // 40MiB

        @JvmStatic
        fun show(context: Context, statusId: String, url: String?) {
            val intent = Intent(context, ChatActivity::class.java)
                    .apply {
                        putExtra(STATUS_ID, statusId)
                        putExtra(URL, url)
                    }
            ActivityCompat.startActivity(context, intent, ActivityOptionsCompat.makeCustomAnimation(context, R.anim.slide_from_right, R.anim.slide_to_left).toBundle())
        }
    }

    override fun androidInjector() = androidInjector
}
