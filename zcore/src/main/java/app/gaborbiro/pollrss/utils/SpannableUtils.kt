package app.gaborbiro.pollrss.utils

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.DimenRes

private const val CLAMPING_WIDTH_WORD = 10
private const val CLAMPING_WIDTH_LINE = 50

fun TextView.shrinkBetween(
    @DimenRes textSize: Int,
    startOffset: Int,
    endOffset: Int,
    useNearestWhitespace: Boolean = true
): TextView =
    apply {
        this.text.span().apply {
            val (finalStartOffset, finalEndOffset) = if (useNearestWhitespace) {
                Pair(nearestWhitespace(startOffset), nearestWhitespace(this.length - endOffset))
            } else {
                Pair(startOffset, this.length - endOffset)
            }
            setSpan(
                AbsoluteSizeSpan(context.resources.getDimensionPixelSize(textSize), false),
                finalStartOffset,
                finalEndOffset,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }.also {
            text = it
        }
    }

private fun SpannableString.nearestWhitespace(index: Int): Int {
    var nearestWhitespace = indexOf(index, CLAMPING_WIDTH_LINE) { it == '\n' }
    return if (nearestWhitespace >= 0) {
        nearestWhitespace
    } else {
        nearestWhitespace = indexOf(index, CLAMPING_WIDTH_WORD) { it.isWhitespace() }
        if (nearestWhitespace >= 0) {
            nearestWhitespace
        } else {
            index
        }
    }
}

private fun SpannableString.indexOf(
    index: Int,
    seekSize: Int,
    match: (Char) -> Boolean
): Int {
    var nearestIndex = -1
    for (i in 0..seekSize) {
        if (index - i < 0 || index + i >= length) {
            break
        }
        if (match.invoke(get(index - i))) {
            nearestIndex = index - i
            break
        }
        if (match.invoke(get(index + i))) {
            nearestIndex = index + i
            break
        }
    }
    return nearestIndex
}

fun String.bold(vararg subString: String) = span().bold(*subString)

fun SpannableString.bold(vararg subString: String) = apply {
    subString.forEach {
        val start = indexOf(it)
        val end = start + it.length
        setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    }
}

fun TextView.setTextWithLinks(text: String, linkPart: String, runOnClick: () -> Unit) {
    this.setTextWithLinks(text, mapOf(linkPart to runOnClick))
}

fun TextView.setTextWithLinks(
    text: String,
    links: Map<String, () -> Unit>
) {
    this.text = text
    val spannable = this.text.span()

    links.forEach { (link, onClick) ->
        applyLink(this.text.toString(), spannable, link, onClick)
    }

    this.setText(spannable, TextView.BufferType.SPANNABLE)
    this.movementMethod = LinkMovementMethod.getInstance()
}

private fun applyLink(
    text: String,
    spannable: Spannable,
    linkPart: String,
    onClick: () -> Unit
) {
    val startIndex = text.indexOf(linkPart)
    if (startIndex < 0) {
        throw IllegalArgumentException("linkPart must be part of text")
    }
    spannable.setSpan(
        object : ClickableSpan() {
            override fun onClick(widget: View) = onClick()
        },
        startIndex,
        startIndex + linkPart.length,
        Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )
}

private fun String.span() = SpannableString(this)

private fun CharSequence.span() = SpannableString(this)