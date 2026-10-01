package io.github.rsgarrido.sazanami.localization

import android.content.Context
import android.util.Log
import io.github.rsgarrido.sazanami.widget.invalidateNowPlayingWidgetPresentations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** Invalidation only: the locale itself is always owned by Android/AndroidX. */
internal object AppLocalePresentationRefresh {
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = requests.asSharedFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun request(context: Context) {
        requests.tryEmit(Unit)
        val appContext = context.applicationContext
        scope.launch {
            try {
                invalidateNowPlayingWidgetPresentations(appContext)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("SazanamiAppLocales", "Widget language refresh failed", error)
            }
        }
    }
}
