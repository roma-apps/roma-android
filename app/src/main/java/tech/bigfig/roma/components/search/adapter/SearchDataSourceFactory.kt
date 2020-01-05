/* Copyright 2019 Joel Pyska
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.roma.components.search.adapter

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import tech.bigfig.roma.components.search.SearchType
import tech.bigfig.roma.entity.SearchResults2
import tech.bigfig.roma.network.MastodonApi
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.Executor

class SearchDataSourceFactory<T>(
        private val mastodonApi: MastodonApi,
        private val searchType: SearchType,
        private val searchRequest: String?,
        private val disposables: CompositeDisposable,
        private val retryExecutor: Executor,
        private val cacheData: List<T>? = null,
        private val parser: (SearchResults2?) -> List<T>) : DataSource.Factory<Int, T>() {
    val sourceLiveData = MutableLiveData<SearchDataSource<T>>()
    override fun create(): DataSource<Int, T> {
        val source = SearchDataSource(mastodonApi, searchType, searchRequest, disposables, retryExecutor, cacheData, parser)
        sourceLiveData.postValue(source)
        return source
    }
}