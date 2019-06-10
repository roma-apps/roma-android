package tech.bigfig.roma.di

import androidx.work.RxWorker
import dagger.android.HasAndroidInjector

/**
 * Created by pandasoft (joelpyska1@gmail.com) on 15/03/2019.
 */
object AndroidWorkerInjection {

    fun inject(worker: RxWorker) {
        val application = worker.applicationContext
        if (application !is HasAndroidInjector) {
            throw RuntimeException(
                    "${application.javaClass.canonicalName} does not implement ${HasAndroidInjector::class.java.canonicalName}")
        }

        val injector = (application as HasAndroidInjector).androidInjector()
        checkNotNull(injector) { "${application.javaClass}.androidInjector() return null" }
        injector.inject(worker)
    }
}