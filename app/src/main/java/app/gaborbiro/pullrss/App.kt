package app.gaborbiro.pullrss

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextProvider.appContext = this
    }
}