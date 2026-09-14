package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppFolder
import com.example.data.model.CustomWidgetId
import com.example.data.model.GestureType
import com.example.data.model.LauncherAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "launcher_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorites")
        private val KEY_HIDDEN = stringSetPreferencesKey("hidden")
        private val KEY_ENABLED_WIDGETS = stringSetPreferencesKey("enabled_widgets")
        private val KEY_DAILY_INTENTION = stringPreferencesKey("daily_intention")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_CLOCK_24H = booleanPreferencesKey("clock_24h")
        private val KEY_AUTO_OPEN_SINGLE = booleanPreferencesKey("auto_open_single")
        private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")

        private val KEY_GESTURE_SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
        private val KEY_GESTURE_SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
        private val KEY_GESTURE_SWIPE_LEFT = stringPreferencesKey("gesture_swipe_left")
        private val KEY_GESTURE_SWIPE_RIGHT = stringPreferencesKey("gesture_swipe_right")
        private val KEY_GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        private val KEY_GESTURE_LONG_PRESS = stringPreferencesKey("gesture_long_press")

        private val KEY_FOLDERS = stringPreferencesKey("folders_json")
        private val KEY_ICON_STYLE = stringPreferencesKey("icon_style")
        private val KEY_RECENT_APPS = stringPreferencesKey("recent_apps_json")
        private val KEY_HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
        private val KEY_SYSTEM_APP_WIDGET_IDS = stringPreferencesKey("system_app_widget_ids")
    }

    val hideStatusBarFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HIDE_STATUS_BAR] ?: false
    }

    val systemAppWidgetIdsFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SYSTEM_APP_WIDGET_IDS]
        if (raw.isNullOrBlank()) emptyList()
        else {
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<Int>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getInt(i))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITES] ?: emptySet()
    }

    val hiddenFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_HIDDEN] ?: emptySet()
    }

    val enabledWidgetsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_WIDGETS] ?: setOf(
            CustomWidgetId.CLOCK_DATE.name,
            CustomWidgetId.BATTERY.name,
            CustomWidgetId.DAILY_INTENTION.name
        )
    }

    val dailyIntentionFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_INTENTION] ?: "Concéntrate en lo esencial hoy."
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "amoled" // amoled, monochrome_light, monochrome_gray
    }

    val clock24hFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CLOCK_24H] ?: true
    }

    val autoOpenSingleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_OPEN_SINGLE] ?: false
    }

    val textSizeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TEXT_SIZE] ?: "medium" // small, medium, large
    }

    val iconStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ICON_STYLE] ?: "monochrome" // monochrome, custom_badge, system, none
    }

    val foldersFlow: Flow<List<AppFolder>> = context.dataStore.data.map { prefs ->
        val rawJson = prefs[KEY_FOLDERS]
        if (rawJson.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(rawJson)
                val list = mutableListOf<AppFolder>()
                for (i in 0 until array.length()) {
                    list.add(AppFolder.fromJson(array.getJSONObject(i)))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val recentAppsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val rawJson = prefs[KEY_RECENT_APPS]
        if (rawJson.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(rawJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val gesturesFlow: Flow<Map<GestureType, LauncherAction>> = context.dataStore.data.map { prefs ->
        mapOf(
            GestureType.SWIPE_UP to parseAction(prefs[KEY_GESTURE_SWIPE_UP], LauncherAction.OPEN_DRAWER),
            GestureType.SWIPE_DOWN to parseAction(prefs[KEY_GESTURE_SWIPE_DOWN], LauncherAction.OPEN_NOTIFICATIONS),
            GestureType.SWIPE_LEFT to parseAction(prefs[KEY_GESTURE_SWIPE_LEFT], LauncherAction.OPEN_CAMERA),
            GestureType.SWIPE_RIGHT to parseAction(prefs[KEY_GESTURE_SWIPE_RIGHT], LauncherAction.OPEN_SETTINGS),
            GestureType.DOUBLE_TAP to parseAction(prefs[KEY_GESTURE_DOUBLE_TAP], LauncherAction.TOGGLE_FLASHLIGHT),
            GestureType.LONG_PRESS to parseAction(prefs[KEY_GESTURE_LONG_PRESS], LauncherAction.OPEN_SETTINGS)
        )
    }

    private fun parseAction(value: String?, default: LauncherAction): LauncherAction {
        if (value == null) return default
        return try {
            LauncherAction.valueOf(value)
        } catch (_: Exception) {
            default
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[KEY_FAVORITES] = current
        }
    }

    suspend fun setFavorites(packageNames: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAVORITES] = packageNames
        }
    }

    suspend fun toggleHidden(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[KEY_HIDDEN] = current
        }
    }

    suspend fun toggleWidget(widgetId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_ENABLED_WIDGETS] ?: setOf(
                CustomWidgetId.CLOCK_DATE.name,
                CustomWidgetId.BATTERY.name,
                CustomWidgetId.DAILY_INTENTION.name
            )).toMutableSet()
            if (enabled) {
                current.add(widgetId)
            } else {
                current.remove(widgetId)
            }
            prefs[KEY_ENABLED_WIDGETS] = current
        }
    }

    suspend fun setDailyIntention(text: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_INTENTION] = text
        }
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = theme
        }
    }

    suspend fun setClock24h(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLOCK_24H] = enabled
        }
    }

    suspend fun setAutoOpenSingle(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_OPEN_SINGLE] = enabled
        }
    }

    suspend fun setTextSize(size: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEXT_SIZE] = size
        }
    }

    suspend fun setIconStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ICON_STYLE] = style
        }
    }

    suspend fun setHideStatusBar(hide: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HIDE_STATUS_BAR] = hide
        }
    }

    suspend fun addSystemAppWidget(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_SYSTEM_APP_WIDGET_IDS]
            val list = mutableListOf<Int>()
            if (!raw.isNullOrBlank()) {
                try {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        list.add(arr.getInt(i))
                    }
                } catch (_: Exception) {}
            }
            if (!list.contains(widgetId)) {
                list.add(widgetId)
            }
            val newArray = JSONArray()
            list.forEach { newArray.put(it) }
            prefs[KEY_SYSTEM_APP_WIDGET_IDS] = newArray.toString()
        }
    }

    suspend fun removeSystemAppWidget(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_SYSTEM_APP_WIDGET_IDS]
            val list = mutableListOf<Int>()
            if (!raw.isNullOrBlank()) {
                try {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val id = arr.getInt(i)
                        if (id != widgetId) {
                            list.add(id)
                        }
                    }
                } catch (_: Exception) {}
            }
            val newArray = JSONArray()
            list.forEach { newArray.put(it) }
            prefs[KEY_SYSTEM_APP_WIDGET_IDS] = newArray.toString()
        }
    }

    suspend fun recordAppLaunch(packageName: String) {
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_RECENT_APPS]
            val list = mutableListOf<String>()
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    for (i in 0 until array.length()) {
                        val pkg = array.getString(i)
                        if (pkg != packageName) {
                            list.add(pkg)
                        }
                    }
                } catch (_: Exception) {}
            }
            // Put latest at the beginning
            list.add(0, packageName)
            val trimmed = list.take(8)
            val newArray = JSONArray()
            trimmed.forEach { newArray.put(it) }
            prefs[KEY_RECENT_APPS] = newArray.toString()
        }
    }

    suspend fun saveFolders(folders: List<AppFolder>) {
        context.dataStore.edit { prefs ->
            val array = JSONArray()
            folders.forEach { array.put(it.toJson()) }
            prefs[KEY_FOLDERS] = array.toString()
        }
    }

    suspend fun createFolder(name: String, initialPackages: List<String> = emptyList(), inHome: Boolean = true): AppFolder {
        val newFolder = AppFolder(
            id = System.currentTimeMillis().toString(),
            name = name,
            packageNames = initialPackages,
            showInHome = inHome,
            showInDrawer = true
        )
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_FOLDERS]
            val list = mutableListOf<AppFolder>()
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    for (i in 0 until array.length()) {
                        list.add(AppFolder.fromJson(array.getJSONObject(i)))
                    }
                } catch (_: Exception) {}
            }
            list.add(newFolder)
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            prefs[KEY_FOLDERS] = array.toString()
        }
        return newFolder
    }

    suspend fun deleteFolder(folderId: String) {
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_FOLDERS]
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<AppFolder>()
                    for (i in 0 until array.length()) {
                        val folder = AppFolder.fromJson(array.getJSONObject(i))
                        if (folder.id != folderId) {
                            list.add(folder)
                        }
                    }
                    val newArray = JSONArray()
                    list.forEach { newArray.put(it.toJson()) }
                    prefs[KEY_FOLDERS] = newArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun renameFolder(folderId: String, newName: String) {
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_FOLDERS]
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<AppFolder>()
                    for (i in 0 until array.length()) {
                        val folder = AppFolder.fromJson(array.getJSONObject(i))
                        if (folder.id == folderId) {
                            list.add(folder.copy(name = newName))
                        } else {
                            list.add(folder)
                        }
                    }
                    val newArray = JSONArray()
                    list.forEach { newArray.put(it.toJson()) }
                    prefs[KEY_FOLDERS] = newArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun addAppToFolder(folderId: String, packageName: String) {
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_FOLDERS]
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<AppFolder>()
                    for (i in 0 until array.length()) {
                        val folder = AppFolder.fromJson(array.getJSONObject(i))
                        if (folder.id == folderId) {
                            val updatedPkgs = if (!folder.packageNames.contains(packageName)) {
                                folder.packageNames + packageName
                            } else {
                                folder.packageNames
                            }
                            list.add(folder.copy(packageNames = updatedPkgs))
                        } else {
                            list.add(folder)
                        }
                    }
                    val newArray = JSONArray()
                    list.forEach { newArray.put(it.toJson()) }
                    prefs[KEY_FOLDERS] = newArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun removeAppFromFolder(folderId: String, packageName: String) {
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_FOLDERS]
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<AppFolder>()
                    for (i in 0 until array.length()) {
                        val folder = AppFolder.fromJson(array.getJSONObject(i))
                        if (folder.id == folderId) {
                            list.add(folder.copy(packageNames = folder.packageNames - packageName))
                        } else {
                            list.add(folder)
                        }
                    }
                    val newArray = JSONArray()
                    list.forEach { newArray.put(it.toJson()) }
                    prefs[KEY_FOLDERS] = newArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun toggleFolderPlacement(folderId: String, inHome: Boolean? = null, inDrawer: Boolean? = null) {
        context.dataStore.edit { prefs ->
            val rawJson = prefs[KEY_FOLDERS]
            if (!rawJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(rawJson)
                    val list = mutableListOf<AppFolder>()
                    for (i in 0 until array.length()) {
                        val folder = AppFolder.fromJson(array.getJSONObject(i))
                        if (folder.id == folderId) {
                            list.add(
                                folder.copy(
                                    showInHome = inHome ?: folder.showInHome,
                                    showInDrawer = inDrawer ?: folder.showInDrawer
                                )
                            )
                        } else {
                            list.add(folder)
                        }
                    }
                    val newArray = JSONArray()
                    list.forEach { newArray.put(it.toJson()) }
                    prefs[KEY_FOLDERS] = newArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun setGestureAction(gesture: GestureType, action: LauncherAction) {
        context.dataStore.edit { prefs ->
            val key = when (gesture) {
                GestureType.SWIPE_UP -> KEY_GESTURE_SWIPE_UP
                GestureType.SWIPE_DOWN -> KEY_GESTURE_SWIPE_DOWN
                GestureType.SWIPE_LEFT -> KEY_GESTURE_SWIPE_LEFT
                GestureType.SWIPE_RIGHT -> KEY_GESTURE_SWIPE_RIGHT
                GestureType.DOUBLE_TAP -> KEY_GESTURE_DOUBLE_TAP
                GestureType.LONG_PRESS -> KEY_GESTURE_LONG_PRESS
            }
            prefs[key] = action.name
        }
    }
}
