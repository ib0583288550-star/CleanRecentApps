package com.ybapps.cleanrecentapps

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * Exposes only the three non-sensitive display settings to the LSPosed hook.
 * This avoids deprecated world-readable SharedPreferences on Android 10+.
 */
class SettingsProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "get_settings") return super.call(method, arg, extras)
        val prefs = context?.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            ?: return Bundle()
        return Bundle().apply {
            putInt("x", prefs.getInt("x", 0))
            putInt("y", prefs.getInt("y", 0))
            putInt("size", prefs.getInt("size", 100))
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?): Int = 0
}