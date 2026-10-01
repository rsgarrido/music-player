package io.github.rsgarrido.sazanami.ui.state

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/** Controllers emit meaning and arguments; Compose resolves the current locale. */
sealed interface UiMessage {
    data class Text(@StringRes val resource: Int, val args: List<Any> = emptyList()) : UiMessage
    data class Quantity(@PluralsRes val resource: Int, val count: Int) : UiMessage
    data class Literal(val value: String) : UiMessage
}

@Composable
fun UiMessage.resolve(): String = when (this) {
    is UiMessage.Text -> stringResource(resource, *args.toTypedArray())
    is UiMessage.Quantity -> pluralStringResource(resource, count, count)
    is UiMessage.Literal -> value
}
