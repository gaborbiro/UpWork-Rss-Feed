package app.gaborbiro.pollrss.utils

import android.content.Context
import android.content.pm.PackageManager

fun Context.isPackageInstalled(packageName: String): Boolean {
    var found = true
    try {
        packageManager.getPackageInfo(packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        found = false
    }
    return found
}