package tech.bigfig.roma.components.search.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import tech.bigfig.roma.AccountActivity
import tech.bigfig.roma.BottomSheetActivity
import tech.bigfig.roma.R
import tech.bigfig.roma.ViewTagActivity
import tech.bigfig.roma.components.search.SearchViewModel
import tech.bigfig.roma.di.Injectable
import tech.bigfig.roma.di.ViewModelFactory
import tech.bigfig.roma.interfaces.LinkListener
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import tech.bigfig.roma.util.*
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

abstract class SearchFragment<T> : Fragment(),
        LinkListener, Injectable, SwipeRefreshLayout.OnRefreshListener {
    private var isSwipeToRefreshEnabled: Boolean = true
    private var snackbarErrorRetry: Snackbar? = null
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    protected lateinit var viewModel: SearchViewModel

    abstract fun createAdapter(): PagedListAdapter<T, *>

    abstract val networkStateRefresh: LiveData<NetworkState>
    abstract val networkState: LiveData<NetworkState>
    abstract val data: LiveData<PagedList<T>>
    protected lateinit var adapter: PagedListAdapter<T, *>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)[SearchViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        setupSwipeRefreshLayout()
        subscribeObservables()
    }

    private fun setupSwipeRefreshLayout() {
        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.setOnRefreshListener(this)
        swipeRefreshLayout.setColorSchemeResources(R.color.roma_blue)
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ThemeUtils.getColor(swipeRefreshLayout.context, android.R.attr.colorBackground))

    }

    private fun subscribeObservables() {
        data.observe(viewLifecycleOwner, Observer {
            if (!swipeRefreshLayout.isEnabled) swipeRefreshLayout.isEnabled = true
            adapter.submitList(it)
        })

        networkStateRefresh.observe(viewLifecycleOwner, Observer {

            searchProgressBar.visible(it == NetworkState.LOADING)

            if (it.status == Status.FAILED)
                showError(it.msg)
            checkNoData()

        })

        networkState.observe(viewLifecycleOwner, Observer {

            progressBarBottom.visible(it == NetworkState.LOADING)

            if (it.status == Status.FAILED)
                showError(it.msg)
        })
    }

    private fun checkNoData() {
        showNoData(adapter.itemCount == 0)
    }

    private fun initAdapter() {
        searchRecyclerView.addItemDecoration(DividerItemDecoration(searchRecyclerView.context, DividerItemDecoration.VERTICAL))
        searchRecyclerView.layoutManager = LinearLayoutManager(searchRecyclerView.context)
        adapter = createAdapter()
        searchRecyclerView.adapter = adapter

    }

    private fun showNoData(isEmpty: Boolean) {
        if (isEmpty && networkStateRefresh.value == NetworkState.LOADED)
            searchNoResultsText.show()
        else
            searchNoResultsText.hide()
    }

    private fun showError(@Suppress("UNUSED_PARAMETER") msg: String?) {
        if (snackbarErrorRetry?.isShown != true) {
            snackbarErrorRetry = Snackbar.make(layoutRoot, R.string.failed_search, Snackbar.LENGTH_INDEFINITE)
            snackbarErrorRetry?.setAction(R.string.action_retry) {
                snackbarErrorRetry = null
                viewModel.retryAllSearches()
            }
            snackbarErrorRetry?.show()
        }
    }

    override fun onViewAccount(id: String) = startActivity(AccountActivity.getIntent(requireContext(), id))

    override fun onViewTag(tag: String) = startActivity(ViewTagActivity.getIntent(requireContext(), tag))

    override fun onViewUrl(url: String) {
        bottomSheetActivity?.viewUrl(url)
    }

    protected val bottomSheetActivity = (activity as? BottomSheetActivity)

    override fun onRefresh() {

        // Dismissed here because the RecyclerView bottomProgressBar is shown as soon as the retry begins.
        swipeRefreshLayout.post {

            swipeRefreshLayout.isRefreshing = false
        }
        viewModel.retryAllSearches()
    }
}
