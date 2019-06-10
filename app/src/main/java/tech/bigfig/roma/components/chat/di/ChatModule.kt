package tech.bigfig.roma.components.chat.di

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import tech.bigfig.roma.components.chat.ChatActivity
import tech.bigfig.roma.components.chat.ChatViewModel
import tech.bigfig.roma.di.ViewModelKey

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 2019-05-16.
 */
@Module
abstract class ChatModule {
    @ContributesAndroidInjector
    abstract fun contributesChatActivity(): ChatActivity

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    internal abstract fun bindsChatActivityViewModel(viewModel: ChatViewModel): ViewModel
}