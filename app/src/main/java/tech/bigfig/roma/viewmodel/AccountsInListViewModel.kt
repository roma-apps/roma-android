/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>.
 */

package tech.bigfig.roma.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import tech.bigfig.roma.entity.Account
import tech.bigfig.roma.network.MastodonApi
import tech.bigfig.roma.util.Either
import tech.bigfig.roma.util.withoutFirstWhich
import javax.inject.Inject

data class State(val accounts: Either<Throwable, List<Account>>, val searchResult: List<Account>?)

class AccountsInListViewModel @Inject constructor(private val api: MastodonApi) : ViewModel() {

    val state: Observable<State> get() = _state
    private val _state = BehaviorSubject.createDefault(State(Either.Right(listOf()), null))
    private val disposable = CompositeDisposable()

    fun load(listId: String) {
        val state = _state.value!!
        if (state.accounts.isLeft() || state.accounts.asRight().isEmpty()) {
            api.getAccountsInList(listId, 0).subscribe({ accounts ->
                updateState { copy(accounts = Either.Right(accounts)) }
            }, { e ->
                updateState { copy(accounts = Either.Left(e)) }
            }).addTo(disposable)
        }
    }

    fun addAccountToList(listId: String, account: Account) {
        api.addCountToList(listId, listOf(account.id))
                .subscribe({
                    updateState {
                        copy(accounts = accounts.map { it + account })
                    }
                }, {
                    Timber.i(javaClass.simpleName,
                            "Failed to add account to the list: ${account.username}")
                })
                .addTo(disposable)
    }

    fun deleteAccountFromList(listId: String, accountId: String) {
        api.deleteAccountFromList(listId, listOf(accountId))
                .subscribe({
                    updateState {
                        copy(accounts = accounts.map { accounts ->
                            accounts.withoutFirstWhich { it.id == accountId }
                        })
                    }
                }, {
                    Timber.i(javaClass.simpleName, "Failed to remove account from thelist: $accountId")
                })
                .addTo(disposable)
    }

    fun search(query: String) {
        when {
            query.isEmpty() -> updateState { copy(searchResult = null) }
            query.isBlank() -> updateState { copy(searchResult = listOf()) }
            else -> api.searchAccounts(query, null, 10, true)
                    .subscribe({ result ->
                        updateState { copy(searchResult = result) }
                    }, {
                        updateState { copy(searchResult = listOf()) }
                    }).addTo(disposable)
        }
    }

    private inline fun updateState(crossinline fn: State.() -> State) {
        _state.onNext(fn(_state.value!!))
    }
}