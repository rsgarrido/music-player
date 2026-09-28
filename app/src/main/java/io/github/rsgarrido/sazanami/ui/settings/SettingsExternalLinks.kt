package io.github.rsgarrido.sazanami.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.github.rsgarrido.sazanami.R

internal object SettingsLinks {
    const val REPOSITORY = "https://github.com/rsgarrido/sazanami"
    const val BUG_REPORT = "$REPOSITORY/issues/new?template=bug_report.md"
    const val FEATURE_REQUEST = "$REPOSITORY/issues/new?template=feature_request.md"
}

internal fun openSettingsUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        })
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.settings_help_link_failure, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.settings_help_link_failure, Toast.LENGTH_SHORT).show()
    }
}
