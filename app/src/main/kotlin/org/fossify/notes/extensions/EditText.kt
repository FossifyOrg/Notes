package org.fossify.notes.extensions

import android.text.InputFilter
import android.text.Spanned
import org.fossify.commons.views.MyEditText

fun MyEditText.enforcePlainText() {
    val stripSpans = InputFilter { source, start, end, _, _, _ ->
        if (source !is Spanned) return@InputFilter null
        val hasRealStyle = source.getSpans(start, end, Any::class.java)
            .any { span ->
                (source.getSpanFlags(span) and Spanned.SPAN_COMPOSING) == 0
            }

        if (hasRealStyle) source.subSequence(start, end).toString() else null
    }
    filters = (filters ?: emptyArray()) + stripSpans
}
