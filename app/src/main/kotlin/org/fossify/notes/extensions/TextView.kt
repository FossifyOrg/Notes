package org.fossify.notes.extensions

import android.view.inputmethod.EditorInfo
import android.widget.TextView
import org.fossify.commons.extensions.removeBit

fun TextView.maybeRequestIncognito() {
    imeOptions = if (context.config.useIncognitoMode) {
        imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    } else {
        imeOptions.removeBit(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
    }
}
