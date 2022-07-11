@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.pelmenstar.onealarm.ui.theme.onSuccess
import io.pelmenstar.onealarm.ui.theme.success

enum class SnackbarMessageType(@JvmField internal val id: Char) {
    PLAIN('P'),
    SUCCESS('S'),
    ERROR('E')
}

@Stable
fun createSnackbarMessage(type: SnackbarMessageType, text: String): String {
    return type.id + "R" + text
}

@Stable
fun createSnackbarMessage(type: SnackbarMessageType, @StringRes id: Int): String {
    return String(CharArray(4).also {
        it[0] = type.id
        it[1] = 'C'
        it[2] = id.toChar()
        it[3] = (id shr 16).toChar()
    })
}

@Composable
private inline fun backgroundColorFor(msgType: SnackbarMessageType) = MaterialTheme.colors.let {
    when (msgType) {
        SnackbarMessageType.PLAIN -> it.surface
        SnackbarMessageType.SUCCESS -> it.success
        SnackbarMessageType.ERROR -> it.error
    }
}

@Composable
private inline fun textColorFor(msgType: SnackbarMessageType) = MaterialTheme.colors.let {
    when (msgType) {
        SnackbarMessageType.PLAIN -> it.onSurface
        SnackbarMessageType.SUCCESS -> it.onSuccess
        SnackbarMessageType.ERROR -> it.onError
    }
}

@Stable
private fun getMessageType(text: String): SnackbarMessageType = when (text[0]) {
    'P' -> SnackbarMessageType.PLAIN
    'S' -> SnackbarMessageType.SUCCESS
    'E' -> SnackbarMessageType.ERROR
    else -> throw RuntimeException("Invalid format of message")
}

@Composable
private inline fun getMessageContent(rawMessage: String): String {
    return if(rawMessage[1] == 'R') {
        rawMessage.substring(2)
    } else {
        val low = rawMessage[2].code
        val high = rawMessage[3].code

        val id = low or (high shl 16)

        return stringResource(id)
    }
}

@Composable
fun MessageTypeSnackbar(state: SnackbarHostState) {
    SnackbarHost(
        hostState = state,
        snackbar = { data ->
            val rawMessage = data.message

            val msgType = getMessageType(rawMessage)
            val message = getMessageContent(rawMessage)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp)
            ) {
                Text(
                    modifier = Modifier
                        .background(
                            backgroundColorFor(msgType),
                            CircleShape
                        )
                        .padding(8.dp)
                        .align(Alignment.Center),
                    text = message,
                    color = textColorFor(msgType)
                )
            }
        }
    )
}