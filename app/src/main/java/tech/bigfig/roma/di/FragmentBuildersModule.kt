/* Copyright 2018 charlag
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
 * see <http://www.gnu.org/licenses>. */


package tech.bigfig.roma.di

import tech.bigfig.roma.AccountsInListFragment
import tech.bigfig.roma.fragment.*
import tech.bigfig.roma.fragment.preference.AccountPreferencesFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector
import tech.bigfig.roma.components.conversation.ConversationsFragment
import tech.bigfig.roma.components.instancemute.fragment.InstanceListFragment
import tech.bigfig.roma.components.report.fragments.ReportDoneFragment
import tech.bigfig.roma.components.report.fragments.ReportNoteFragment
import tech.bigfig.roma.components.report.fragments.ReportStatusesFragment
import tech.bigfig.roma.components.search.fragments.SearchAccountsFragment
import tech.bigfig.roma.components.search.fragments.SearchHashtagsFragment
import tech.bigfig.roma.components.search.fragments.SearchStatusesFragment
import tech.bigfig.roma.fragment.preference.NotificationPreferencesFragment

/**
 * Created by charlag on 3/24/18.
 */

@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun accountListFragment(): AccountListFragment

    @ContributesAndroidInjector
    abstract fun accountMediaFragment(): AccountMediaFragment

    @ContributesAndroidInjector
    abstract fun viewThreadFragment(): ViewThreadFragment

    @ContributesAndroidInjector
    abstract fun timelineFragment(): TimelineFragment

    @ContributesAndroidInjector
    abstract fun notificationsFragment(): NotificationsFragment

    @ContributesAndroidInjector
    abstract fun searchFragment(): SearchStatusesFragment

    @ContributesAndroidInjector
    abstract fun notificationPreferencesFragment(): NotificationPreferencesFragment

    @ContributesAndroidInjector
    abstract fun accountPreferencesFragment(): AccountPreferencesFragment

    @ContributesAndroidInjector
    abstract fun directMessagesPreferencesFragment(): ConversationsFragment

    @ContributesAndroidInjector
    abstract fun accountInListsFragment(): AccountsInListFragment

    @ContributesAndroidInjector
    abstract fun reportStatusesFragment(): ReportStatusesFragment

    @ContributesAndroidInjector
    abstract fun reportNoteFragment(): ReportNoteFragment

    @ContributesAndroidInjector
    abstract fun reportDoneFragment(): ReportDoneFragment

    @ContributesAndroidInjector
    abstract fun instanceListFragment(): InstanceListFragment

    @ContributesAndroidInjector
    abstract fun searchAccountFragment(): SearchAccountsFragment

    @ContributesAndroidInjector
    abstract fun searchHashtagsFragment(): SearchHashtagsFragment

}
