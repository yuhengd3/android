package org.hackillinois.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.hackillinois.android.R;
import org.hackillinois.android.model.Event;

public class FavoritesManager {

    public static void favoriteEvent(Context context, Event event) {
        setBoolean(context, event.getName(), true);
    }

    public static void unfavoriteEvent(Context context, Event event) {
        setBoolean(context, event.getName(), false);
    }

    public static boolean isFavorited(Context context, Event event) {
        return getBoolean(context, event.getName(), false);
    }

    private static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = getFavoritesPrefs(context).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private static boolean getBoolean(Context context, String key, boolean defValue) {
        return getFavoritesPrefs(context).getBoolean(key, defValue);
    }

    private static SharedPreferences getFavoritesPrefs(Context context) {
        return context.getSharedPreferences(context.getString(R.string.favorites_pref_file_key), Context.MODE_PRIVATE);
    }
}
