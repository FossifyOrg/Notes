package org.fossify.notes.extensions

import android.text.InputFilter
import android.text.Spanned
import org.fossify.commons.views.MyEditText

fun MyEditText.enforcePlainText() {
    val stripSpans = InputFilter { source, start, end, _, _, _ ->
        val sub = source.subSequence(start, end)
        if (sub is Spanned) sub.toString() else sub
    }
    filters = (filters ?: emptyArray()) + stripSpans
}
