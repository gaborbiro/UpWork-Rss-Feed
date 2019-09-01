package app.gaborbiro.utils

import android.content.Intent

interface Navigator {
    fun getMainActivityIntent(): Intent
    fun getBroadcastIntent(): Intent
}