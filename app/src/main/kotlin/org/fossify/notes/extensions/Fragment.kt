package org.fossify.notes.extensions

import androidx.fragment.app.Fragment
import org.fossify.notes.helpers.Config

val Fragment.config: Config? get() = if (context != null) Config.newInstance(context!!) else null
