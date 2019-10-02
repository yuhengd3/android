package org.hackillinois.android.common

import android.content.Context
import org.hackillinois.android.R
import org.hackillinois.android.database.entity.Event
import org.hackillinois.android.notifications.HackIllinoisNotificationManager

class FavoritesManager {
    companion object {
        fun favoriteEvent(context: Context, event: Event?) = event?.let {
            setBoolean(context, it.name, true)
            HackIllinoisNotificationManager.scheduleEventNotification(context, it)
        }

        fun unfavoriteEvent(context: Context, event: Event?) = event?.let {
            setBoolean(context, it.name, false)
            HackIllinoisNotificationManager.cancelEventNotification(context, it)
        }

        fun isFavorited(context: Context, eventName: String) = getBoolean(context, eventName)

        private fun setBoolean(context: Context, key: String, value: Boolean) {
            getFavoritesPrefs(context).edit().apply {
                putBoolean(key, value)
                apply()
            }
        }

        private fun getBoolean(context: Context, key: String) = getFavoritesPrefs(context).getBoolean(key, false)
        private fun getFavoritesPrefs(context: Context) = context.getSharedPreferences(context.getString(R.string.favorites_pref_file_key), Context.MODE_PRIVATE)
    }
}