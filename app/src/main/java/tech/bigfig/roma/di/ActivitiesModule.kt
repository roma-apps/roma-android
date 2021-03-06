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

import tech.bigfig.roma.*
import tech.bigfig.roma.components.report.ReportActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector
import tech.bigfig.roma.components.chat.di.ChatModule
import tech.bigfig.roma.components.instancemute.InstanceListActivity
import tech.bigfig.roma.components.search.SearchActivity
import tech.bigfig.roma.components.search.fragments.SearchHashtagsFragment

/**
 * Created by charlag on 3/24/18.
 */

@Module(includes = [ChatModule::class])
abstract class ActivitiesModule {

    @ContributesAndroidInjector
    abstract fun contributesBaseActivity(): BaseActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesAccountActivity(): AccountActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesListsActivity(): ListsActivity

    @ContributesAndroidInjector
    abstract fun contributesComposeActivity(): ComposeActivity

    @ContributesAndroidInjector
    abstract fun contributesEditProfileActivity(): EditProfileActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesAccountListActivity(): AccountListActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesModalTimelineActivity(): ModalTimelineActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesViewTagActivity(): ViewTagActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesViewThreadActivity(): ViewThreadActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesFavouritesActivity(): FavouritesActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesSearchActivity(): SearchActivity

    @ContributesAndroidInjector
    abstract fun contributesAboutActivity(): AboutActivity

    @ContributesAndroidInjector
    abstract fun contributesLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    abstract fun contributesSplashActivity(): SplashActivity

    @ContributesAndroidInjector
    abstract fun contributesSavedTootActivity(): SavedTootActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesPreferencesActivity(): PreferencesActivity

    @ContributesAndroidInjector
    abstract fun contributesViewMediaActivity(): ViewMediaActivity

    @ContributesAndroidInjector
    abstract fun contributesLicenseActivity(): LicenseActivity

    @ContributesAndroidInjector
    abstract fun contributesTabPreferenceActivity(): TabPreferenceActivity

    @ContributesAndroidInjector
    abstract fun contributesFiltersActivity(): FiltersActivity

    @ContributesAndroidInjector
    abstract fun contributesPrivacyPolicyActivity(): PrivacyPolicyActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesReportActivity(): ReportActivity

    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributesInstanceListActivity(): InstanceListActivity
}
