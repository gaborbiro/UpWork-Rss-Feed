package app.gaborbiro.utils

import android.content.Intent

interface Navigator {
    fun getMainActivityIntent(jobId: Long): Intent
    fun getBroadcastIntent(): Intent
}