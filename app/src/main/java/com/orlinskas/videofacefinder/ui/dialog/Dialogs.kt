package com.orlinskas.videofacefinder.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.example.videofacefinder.R
import com.orlinskas.videofacefinder.exception.HandledException

object Dialogs {

    fun exceptionDialog(context: Context, exception: HandledException) =
        MaterialDialog(context).show {
            title(R.string.dialog_error_title)
            message(text = exception.getText(context))
            cancelOnTouchOutside(true)
            cancelable(true)
            positiveButton(res = android.R.string.ok) { it.dismiss() }
        }

    fun exceptionDialog(context: Context, exception: HandledException, retry: () -> Unit) =
        MaterialDialog(context).show {
            title(R.string.dialog_error_title)
            message(text = exception.getText(context))
            cancelOnTouchOutside(true)
            cancelable(true)
            negativeButton(res = R.string.retry) { retry.invoke() }
            positiveButton(res = android.R.string.ok) { it.dismiss() }
        }

    fun simpleDialog(
        context: Context,
        titleRes: Int,
        messageRes: Int? = null,
        message: String? = null,
        positiveTextRes: Int? = null,
        negativeTextRes: Int? = null,
        positiveCallback: (() -> (Unit))? = null,
        negativeCallback: (() -> (Unit))? = null,
        cancellable: Boolean? = null
    ) =
        MaterialDialog(context).show {
            title(res = titleRes)
            messageRes?.let {
                message(res = messageRes)
            }
            message?.let {
                message(text = message)
            }
            if (positiveTextRes == null) {
                positiveButton(res = android.R.string.ok, click = { positiveCallback?.invoke() })
            } else {
                positiveButton(res = positiveTextRes, click = { positiveCallback?.invoke() })
            }
            negativeTextRes?.let {
                negativeButton(res = negativeTextRes, click = { negativeCallback?.invoke() })
            }
            cancellable?.let {
                cancelOnTouchOutside(it)
                cancelable(it)
            }
        }
}
