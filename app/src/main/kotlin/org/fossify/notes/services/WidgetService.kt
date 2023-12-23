package org.fossify.notes.services

import android.content.Intent
import android.widget.RemoteViewsService
import org.fossify.notes.adapters.WidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = WidgetAdapter(applicationContext, intent)
}
