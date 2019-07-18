package tech.bigfig.roma.components.conversation

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.Config
import androidx.paging.toLiveData
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tech.bigfig.roma.db.AppDatabase
import tech.bigfig.roma.entity.Account
import tech.bigfig.roma.entity.Conversation
import tech.bigfig.roma.entity.Status
import tech.bigfig.roma.network.MastodonApi
import tech.bigfig.roma.util.Listing
import tech.bigfig.roma.util.NetworkState
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Singleton
class ConversationsRepository @Inject constructor(val mastodonApi: MastodonApi, val db: AppDatabase) {

    private val ioExecutor = Executors.newSingleThreadExecutor()

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }

    @MainThread
    fun refresh(accountId: Long, showLoadingIndicator: Boolean): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        if (showLoadingIndicator) {
            networkState.value = NetworkState.LOADING
        }

        mastodonApi.getTimelineDirect(null, null, DEFAULT_PAGE_SIZE).enqueue(
                object : Callback<List<Status>> {
                    override fun onFailure(call: Call<List<Status>>, t: Throwable) {

                        // retrofit calls this on main thread so safe to call set value
                        networkState.value = NetworkState.error(t.message)
                    }

                    override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                        ioExecutor.execute {
                            db.runInTransaction {
                                db.conversationDao().deleteForAccount(accountId)
                                insertResultIntoDb(accountId, statusesToConversations(response.body()))
                            }

                            // since we are in bg thread now, post the result.
                            networkState.postValue(NetworkState.LOADED)
                        }
                    }
                }
        )

        return networkState
    }

    private fun statusesToConversations(body: List<Status>?): List<Conversation> {

        val conversations = HashMap<String, Status>()
        val conversationsRecentFirst = HashMap<Date, Status>()

        body?.reversed()?.iterator()?.forEach { it.pleroma?.conversation_id?.let { id -> conversations[id] = it } }

        conversations.forEach { conversationsRecentFirst[it.value.createdAt] = it.value }

        val conversationList = ArrayList<Conversation>()

        conversationsRecentFirst.toSortedMap(reverseOrder()).forEach {
            conversationList.add(
                    Conversation(
                            id = it.value.pleroma?.conversation_id!!,
                            accounts = emptyList(),
                            lastStatus = it.value,
                            unread = false
                    )
            )
        }

        conversationList.forEach {
            Log.v("SFG", "Conversation -> $it")
        }

        return conversationList
    }

    fun getAccountObjects(mentions: Array<Status.Mention>): List<Account> {
        val accounts = ArrayList<Account>()
        mentions.forEach {
            mastodonApi.account(it.id).execute().body()?.let { it1 -> accounts.add(it1) }
        }
        return accounts
    }

    @MainThread
    fun conversations(accountId: Long): Listing<ConversationEntity> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        val boundaryCallback = ConversationsBoundaryCallback(
                accountId = accountId,
                mastodonApi = mastodonApi,
                handleResponse = this::insertTimelineDirectResultIntoDb,
                ioExecutor = ioExecutor,
                networkPageSize = DEFAULT_PAGE_SIZE)
        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh(accountId, true)
        }

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = db.conversationDao().conversationsForAccount(accountId).toLiveData(
                config = Config(pageSize = DEFAULT_PAGE_SIZE, prefetchDistance = DEFAULT_PAGE_SIZE / 2, enablePlaceholders = false),
                boundaryCallback = boundaryCallback
        )

        return Listing(
                pagedList = livePagedList,
                networkState = boundaryCallback.networkState,
                retry = {
                    boundaryCallback.helper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState
        )
    }

    fun deleteCacheForAccount(accountId: Long) {
        Single.fromCallable {
            db.conversationDao().deleteForAccount(accountId)
        }.subscribeOn(Schedulers.io())
                .subscribe()
    }

    private fun insertTimelineDirectResultIntoDb(accountId: Long, list: List<Status>?) {
        insertResultIntoDb(accountId, statusesToConversations(list))
    }

    private fun insertResultIntoDb(accountId: Long, result: List<Conversation>?) {
        result?.filter { it.lastStatus != null }
                ?.map { it.toEntity(accountId) }
                ?.let { db.conversationDao().insert(it) }

    }
}