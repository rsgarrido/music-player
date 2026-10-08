package io.github.rsgarrido.sazanami.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat

internal data class SystemBarPresentation(
    val statusBarsVisible: Boolean,
    val navigationBarsVisible: Boolean,
    val behavior: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean
)

/** Read the owning Activity before a modal window takes focus; never change that Activity. */
@Composable
internal fun rememberPlayerSystemBarPresentation(opening: Any): SystemBarPresentation? {
    val view = LocalView.current
    return remember(opening, view) {
        view.context.findActivity()?.window?.let(::readSystemBarPresentation)
    }
}

internal fun readSystemBarPresentation(window: Window): SystemBarPresentation {
    val view = window.decorView
    val insets = ViewCompat.getRootWindowInsets(view)
    val controller = WindowCompat.getInsetsController(window, view)
    @Suppress("DEPRECATION")
    val legacyVisibility = view.systemUiVisibility
    return SystemBarPresentation(
        statusBarsVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars())
            ?: (legacyVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0),
        navigationBarsVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars())
            ?: (legacyVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0),
        behavior = controller.systemBarsBehavior,
        lightStatusBars = controller.isAppearanceLightStatusBars,
        lightNavigationBars = controller.isAppearanceLightNavigationBars
    )
}

internal fun applySystemBarPresentation(controller: WindowInsetsControllerCompat, presentation: SystemBarPresentation) {
    controller.systemBarsBehavior = presentation.behavior
    controller.isAppearanceLightStatusBars = presentation.lightStatusBars
    controller.isAppearanceLightNavigationBars = presentation.lightNavigationBars
    if (presentation.statusBarsVisible) controller.show(WindowInsetsCompat.Type.statusBars())
    else controller.hide(WindowInsetsCompat.Type.statusBars())
    if (presentation.navigationBarsVisible) controller.show(WindowInsetsCompat.Type.navigationBars())
    else controller.hide(WindowInsetsCompat.Type.navigationBars())
}

/** A Dialog has its own controller. Preserve the source presentation through focus acquisition. */
@Composable
internal fun PreserveDialogSystemBarsEffect(window: Window?, presentation: SystemBarPresentation?) {
    DisposableEffect(window, presentation) {
        if (window == null || presentation == null) {
            onDispose {}
        } else {
            val view = window.decorView
            val controller = WindowCompat.getInsetsController(window, view)
            val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                if (focused) applySystemBarPresentation(controller, presentation)
            }
            val observer = view.viewTreeObserver
            observer.addOnWindowFocusChangeListener(listener)
            applySystemBarPresentation(controller, presentation)
            onDispose {
                if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener)
                else view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
                // This window is being discarded. Do not show bars on the still-mounted player.
            }
        }
    }
}

@Composable
fun ImmersiveSystemBarsEffect(
    isImmersive: Boolean
) {
    val view = LocalView.current

    DisposableEffect(
        isImmersive,
        view
    ) {
        val window = view.context.findActivity()?.window

        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowCompat.getInsetsController(
                window,
                view
            )

            if (isImmersive) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
