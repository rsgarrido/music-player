package io.github.rsgarrido.sazanami.ui.settings

import android.content.Context
import android.os.Build
import io.github.rsgarrido.sazanami.R

internal fun Context.installedAppVersion(): Pair<String, Long> = runCatching {
    val info = packageManager.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    info.versionName.orEmpty().ifBlank { getString(R.string.diagnostics_unknown) } to code
}.getOrDefault(getString(R.string.diagnostics_unknown) to 0L)
