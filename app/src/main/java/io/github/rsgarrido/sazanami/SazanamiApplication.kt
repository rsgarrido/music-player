package io.github.rsgarrido.sazanami

import android.app.Application
import android.content.res.Configuration
import io.github.rsgarrido.sazanami.localization.AppLocalePresentationRefresh

class SazanamiApplication : Application() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Includes system-language and external per-app-language changes while playback is active.
        AppLocalePresentationRefresh.request(this)
    }
}
