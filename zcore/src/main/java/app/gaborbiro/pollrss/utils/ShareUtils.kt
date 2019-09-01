package app.gaborbiro.pollrss.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.share(link: String) {
    if (isPackageInstalled(PUSHBULLET_PACKAGE)) {
        try {
            shareWithPushbullet(link)
        } catch (e: Throwable) {
            e.printStackTrace()
            shareSimple(link)
        }
    } else {
        shareSimple(link)
    }
}

private fun Context.shareWithPushbullet(link: String) {
    Intent(Intent.ACTION_SEND).apply {
        data = Uri.parse(link)
        type = "text/plain"
        putExtra("android.intent.extra.TEXT", link)
        setClassName(
            PUSHBULLET_PACKAGE,
            "com.pushbullet.android.ui.ShareActivity"
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }.also {
        startActivity(it)
    }
}

private fun Context.shareSimple(link: String) {
    Intent(Intent.ACTION_SEND).apply {
        data = Uri.parse(link)
        type = "text/plain"
        putExtra("android.intent.extra.TEXT", link)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }.also {
        val chooserIntent = Intent.createChooser(it, "Share URL")
        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(chooserIntent)
    }
}

const val PUSHBULLET_PACKAGE = "com.pushbullet.android"