package app.gaborbiro.pollrss

import android.app.Application
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import app.gaborbiro.utils.NavigatorProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log


class App : Application(), LifecycleObserver {

    companion object {
        var appIsInForeground = false
    }

    override fun onCreate() {
        super.onCreate()
        AppContextProvider.appContext = this
        NavigatorProvider.navigator = NavigatorImpl()

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        appIsInForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        appIsInForeground = false
    }
}