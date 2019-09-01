package app.gaborbiro.pollrss.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

fun Context.isPackageInstalled(packageName: String): Boolean {
    var found = true
    try {
        packageManager.getPackageInfo(packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        found = false
    }
    return found
}

fun Context.openLink(link: String) {
    Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(link)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }.also {
        startActivity(it)
    }
}