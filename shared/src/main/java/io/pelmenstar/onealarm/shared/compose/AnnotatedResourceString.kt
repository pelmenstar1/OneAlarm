package io.pelmenstar.onealarm.shared.compose

import android.content.res.Resources
import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import io.pelmenstar.onealarm.shared.hasFlag

/**
 * Retrives a string from given resource and convert the string to Jetpack's [AnnotatedString]
 */
@Composable
fun annotatedStingResource(@StringRes id: Int): AnnotatedString {
    val text = stringResource(id)

    val native = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
    val nativeSpans = native.getSpans<StyleSpan>(0, native.length)

    val jetpackSpans = nativeSpans.map {
        val start = native.getSpanStart(it)
        val end = native.getSpanEnd(it)

        var fontWeight: FontWeight? = null
        var fontStyle: FontStyle? = null

        if(it.style.hasFlag(Typeface.BOLD)) {
            fontWeight = FontWeight.Bold
        }

        if(it.style.hasFlag(Typeface.ITALIC)) {
            fontStyle = FontStyle.Italic
        }

        val style = SpanStyle(fontWeight = fontWeight, fontStyle = fontStyle)

        AnnotatedString.Range(style, start, end)
    }

    return AnnotatedString(native.toString(), jetpackSpans)
}

@Composable
@ReadOnlyComposable
private fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}