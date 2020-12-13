package com.orlinskas.videofacefinder.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout

fun EditText.cursorToEnd() {
    setSelection(this.text.length)
}

fun EditText.onTextChanged(callback: (String?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            callback.invoke(s?.toString())
        }
    })
}

fun TextInputLayout.onTextChanged(callback: (String?) -> Unit) = editText?.onTextChanged(callback)

fun EditText.onImeDoneAction(callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
            callback.invoke()
            true
        } else {
            false
        }
    }
}

fun TextInputLayout.onImeDoneAction(callback: () -> Unit) = editText?.onImeDoneAction(callback)

fun Spinner.onItemSelected(callback: (position: Int?) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            callback.invoke(null)
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            callback.invoke(position)
        }
    }
}

/**
 * Inflates and creates binding for a new view with [layoutId] view is attached to current [ViewGroup]
 */
fun <T : ViewDataBinding> ViewGroup.bindWith(@LayoutRes layoutId: Int): T {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    return DataBindingUtil.inflate(inflater, layoutId, this, false)
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun <T : RecyclerView.ViewHolder> T.getString(@StringRes res: Int): String = itemView.context.getString(
    res
)

fun View.nextVisibility() {
    if (this.visibility == View.VISIBLE) {
        this.visibility = View.GONE
    } else {
        this.visibility = View.VISIBLE
    }
}

fun View.rotate() {
    this.rotation = this.rotation + 180f
}

fun View?.loadBitmapFromView(width: Int, height: Int): Bitmap? {
    return if (this != null) {
        this.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        this.layout(0, 0, this.measuredWidth, this.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            this.measuredWidth,
            this.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        this.background?.draw(canvas)
        this.draw(canvas)
        bitmap
    } else {
        null
    }
}

fun View.fadeGone(visible: Boolean) {
    if (tag == null) {
        tag = true
        visibility = if (visible) View.VISIBLE else View.GONE
    } else {
        animate().cancel()

        if (visible) {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    alpha = 1f
                }
            })
        } else {
            animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    alpha = 1f
                    visibility = View.GONE
                }
            })
        }
    }
}

fun View.fadeGoneFast(visible: Boolean) {
    if (tag == null) {
        tag = true
        visibility = if (visible) View.VISIBLE else View.GONE
    } else {
        animate().cancel()

        if (visible) {
            visibility = View.VISIBLE
            alpha = 0f
            animate().setDuration(300L).alpha(1f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    alpha = 1f
                }
            })
        } else {
            animate().setDuration(1L).alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    alpha = 1f
                    visibility = View.GONE
                }
            })
        }
    }
}

fun View.margin(left: Float? = null, top: Float? = null, right: Float? = null, bottom: Float? = null) {
    layoutParams<ViewGroup.MarginLayoutParams> {
        left?.run { leftMargin = dpToPx(this) }
        top?.run { topMargin = dpToPx(this) }
        right?.run { rightMargin = dpToPx(this) }
        bottom?.run { bottomMargin = dpToPx(this) }
    }
}

inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}

fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)

fun Context.dpToPx(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

//fun ImageView.setImageUrl(url: String, loadCallBack: (Boolean) -> Unit) {
//    val options = RequestOptions()
//        .placeholder(R.drawable.ic_placeholder)
//        .error(R.drawable.ic_placeholder)
//
//    Glide.with(this.context)
//        .setDefaultRequestOptions(options)
//        .load(url)
//        .listener(object : RequestListener<Drawable> {
//            override fun onLoadFailed(
//                e: GlideException?,
//                model: Any?,
//                target: Target<Drawable>?,
//                isFirstResource: Boolean
//            ): Boolean {
//                loadCallBack.invoke(true)
//                return false
//            }
//
//            override fun onResourceReady(
//                resource: Drawable?,
//                model: Any?,
//                target: Target<Drawable>?,
//                dataSource: DataSource?,
//                isFirstResource: Boolean
//            ): Boolean {
//                loadCallBack.invoke(true)
//                return false
//            }
//        })
//        .into(this)
//}
