package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.AppFolder
import com.example.data.model.AppInfo
import com.example.data.model.CustomWidgetId
import com.example.data.model.GestureType
import com.example.data.model.LauncherAction
import com.example.data.receiver.PackageReceiver
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LauncherScreen {
    HOME,
    DRAWER,
    SETTINGS
}

data class LauncherUiState(
    val favoriteApps: List<AppInfo> = emptyList(),
    val drawerApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    val homeFolders: List<AppFolder> = emptyList(),
    val drawerFolders: List<AppFolder> = emptyList(),
    val recentApps: List<AppInfo> = emptyList(),
    val lastLaunchedApp: AppInfo? = null,
    val iconStyle: String = "monochrome", // monochrome, custom_badge, system, none
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val enabledWidgets: Set<String> = setOf(
        CustomWidgetId.CLOCK_DATE.name,
        CustomWidgetId.BATTERY.name,
        CustomWidgetId.DAILY_INTENTION.name
    ),
    val systemAppWidgetIds: List<Int> = emptyList(),
    val dailyIntention: String = "Concéntrate en lo esencial hoy.",
    val themeMode: String = "amoled",
    val clock24h: Boolean = true,
    val autoOpenSingle: Boolean = false,
    val hideStatusBar: Boolean = false,
    val textSize: String = "medium",
    val gestureActions: Map<GestureType, LauncherAction> = emptyMap(),
    val currentScreen: LauncherScreen = LauncherScreen.HOME,
    val notificationNotice: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesManager = PreferencesManager(application)
    val repository = AppRepository(application, preferencesManager)

    private val _searchQuery = MutableStateFlow("")
    private val _currentScreen = MutableStateFlow(LauncherScreen.HOME)
    private val _notificationNotice = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    private val packageReceiver = PackageReceiver {
        repository.reloadApps()
    }

    private val baseConfigFlow = combine(
        combine(
            preferencesManager.enabledWidgetsFlow,
            preferencesManager.dailyIntentionFlow,
            preferencesManager.themeModeFlow
        ) { widgets, intention, theme -> Triple(widgets, intention, theme) },
        combine(
            preferencesManager.clock24hFlow,
            preferencesManager.autoOpenSingleFlow,
            preferencesManager.hideStatusBarFlow
        ) { c24, autoOpen, hideStatusBar -> Triple(c24, autoOpen, hideStatusBar) }
    ) { (widgets, intention, theme), (c24, autoOpen, hideStatusBar) ->
        BaseConfig(widgets, intention, theme, c24, autoOpen, hideStatusBar)
    }

    private val extendedConfigFlow = combine(
        preferencesManager.textSizeFlow,
        preferencesManager.iconStyleFlow,
        preferencesManager.gesturesFlow,
        preferencesManager.foldersFlow,
        combine(preferencesManager.recentAppsFlow, preferencesManager.systemAppWidgetIdsFlow) { recentPkgs, widgetIds ->
            recentPkgs to widgetIds
        }
    ) { size, iconStyle, gestures, folders, (recentPkgs, widgetIds) ->
        ExtendedConfig(size, iconStyle, gestures, folders, recentPkgs, widgetIds)
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        repository.appsWithPreferences,
        baseConfigFlow,
        extendedConfigFlow,
        _searchQuery,
        _currentScreen
    ) { apps, baseConfig, extConfig, query, screen ->
        val favorites = apps.filter { it.isFavorite && !it.isHidden }
        val drawer = apps.filter { !it.isHidden }
        val hidden = apps.filter { it.isHidden }

        val appsMap = apps.associateBy { it.packageName }
        val recentAppsList = extConfig.recentPkgs.mapNotNull { appsMap[it] }
        val lastLaunched = recentAppsList.firstOrNull()

        val homeFolders = extConfig.folders.filter { it.showInHome }
        val drawerFolders = extConfig.folders.filter { it.showInDrawer }

        val filtered = if (query.isBlank()) {
            drawer
        } else {
            val cleanQuery = query.trim()
            if (cleanQuery.length == 1) {
                // When searching by a single letter (from alphabet scrubber or typed 1 char),
                // specifically find apps whose name STARTS with this letter!
                val targetLetter = normalizeLetter(cleanQuery.first())
                val startsWithApps = drawer.filter { app ->
                    val firstChar = app.label.trim().firstOrNull()
                    firstChar != null && normalizeLetter(firstChar) == targetLetter
                }
                if (startsWithApps.isNotEmpty()) {
                    startsWithApps
                } else {
                    drawer.filter { it.label.contains(cleanQuery, ignoreCase = true) }
                }
            } else {
                val startsWithApps = drawer.filter { it.label.trim().startsWith(cleanQuery, ignoreCase = true) }
                val containsApps = drawer.filter {
                    it.label.contains(cleanQuery, ignoreCase = true) && !it.label.trim().startsWith(cleanQuery, ignoreCase = true)
                }
                startsWithApps + containsApps
            }
        }

        LauncherUiState(
            favoriteApps = favorites,
            drawerApps = drawer,
            filteredApps = filtered,
            hiddenApps = hidden,
            folders = extConfig.folders,
            homeFolders = homeFolders,
            drawerFolders = drawerFolders,
            recentApps = recentAppsList,
            lastLaunchedApp = lastLaunched,
            iconStyle = extConfig.iconStyle,
            searchQuery = query,
            isLoading = _isLoading.value,
            enabledWidgets = baseConfig.widgets,
            systemAppWidgetIds = extConfig.systemAppWidgetIds,
            dailyIntention = baseConfig.intention,
            themeMode = baseConfig.theme,
            clock24h = baseConfig.c24,
            autoOpenSingle = baseConfig.autoOpen,
            hideStatusBar = baseConfig.hideStatusBar,
            textSize = extConfig.size,
            gestureActions = extConfig.gestures,
            currentScreen = screen,
            notificationNotice = _notificationNotice.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    init {
        PackageReceiver.register(application, packageReceiver)
        viewModelScope.launch {
            _isLoading.value = true
            repository.reloadApps()
            _isLoading.value = false
        }
    }

    private fun normalizeLetter(c: Char): Char = when (c.uppercaseChar()) {
        'Á', 'À', 'Â', 'Ã', 'Ä' -> 'A'
        'É', 'È', 'Ê', 'Ë' -> 'E'
        'Í', 'Ì', 'Î', 'Ï' -> 'I'
        'Ó', 'Ò', 'Ô', 'Õ', 'Ö' -> 'O'
        'Ú', 'Ù', 'Û', 'Ü' -> 'U'
        'Ñ' -> 'N'
        else -> c.uppercaseChar()
    }

    override fun onCleared() {
        super.onCleared()
        PackageReceiver.unregister(getApplication(), packageReceiver)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun navigateTo(screen: LauncherScreen) {
        _currentScreen.value = screen
        if (screen == LauncherScreen.HOME) {
            clearSearch()
        }
    }

    fun launchApp(app: AppInfo): Boolean {
        val success = repository.launchApp(app.packageName, app.activityName)
        if (success) {
            viewModelScope.launch {
                preferencesManager.recordAppLaunch(app.packageName)
            }
            _currentScreen.value = LauncherScreen.HOME
            clearSearch()
        }
        return success
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            preferencesManager.toggleFavorite(packageName)
        }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch {
            preferencesManager.toggleHidden(packageName)
        }
    }

    fun toggleWidget(widgetId: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.toggleWidget(widgetId, enabled)
        }
    }

    fun updateDailyIntention(text: String) {
        viewModelScope.launch {
            preferencesManager.setDailyIntention(text)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setClock24h(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setClock24h(enabled)
        }
    }

    fun setAutoOpenSingle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoOpenSingle(enabled)
        }
    }

    fun setTextSize(size: String) {
        viewModelScope.launch {
            preferencesManager.setTextSize(size)
        }
    }

    fun setIconStyle(style: String) {
        viewModelScope.launch {
            preferencesManager.setIconStyle(style)
        }
    }

    fun setGestureAction(gesture: GestureType, action: LauncherAction) {
        viewModelScope.launch {
            preferencesManager.setGestureAction(gesture, action)
        }
    }

    // Folder Management
    fun createFolder(name: String, initialPackages: List<String> = emptyList(), inHome: Boolean = true) {
        viewModelScope.launch {
            preferencesManager.createFolder(name, initialPackages, inHome)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            preferencesManager.deleteFolder(folderId)
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            preferencesManager.renameFolder(folderId, newName)
        }
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            preferencesManager.addAppToFolder(folderId, packageName)
        }
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            preferencesManager.removeAppFromFolder(folderId, packageName)
        }
    }

    fun toggleFolderPlacement(folderId: String, inHome: Boolean? = null, inDrawer: Boolean? = null) {
        viewModelScope.launch {
            preferencesManager.toggleFolderPlacement(folderId, inHome, inDrawer)
        }
    }

    // Recent Apps & Fast Switching
    fun openRecents(): Boolean {
        val success = repository.openRecents()
        if (!success) {
            // If system toggle wasn't supported directly, switch to previous app
            switchToPreviousApp()
        }
        return success
    }

    fun switchToPreviousApp(): Boolean {
        val recents = uiState.value.recentApps
        if (recents.isNotEmpty()) {
            val targetApp = recents.first()
            return launchApp(targetApp)
        }
        return false
    }

    fun executeGesture(gesture: GestureType) {
        val action = uiState.value.gestureActions[gesture] ?: when (gesture) {
            GestureType.SWIPE_UP -> LauncherAction.OPEN_DRAWER
            GestureType.SWIPE_DOWN -> LauncherAction.OPEN_NOTIFICATIONS
            GestureType.SWIPE_LEFT -> LauncherAction.OPEN_CAMERA
            GestureType.SWIPE_RIGHT -> LauncherAction.OPEN_SETTINGS
            GestureType.DOUBLE_TAP -> LauncherAction.TOGGLE_FLASHLIGHT
            GestureType.LONG_PRESS -> LauncherAction.OPEN_SETTINGS
        }

        when (action) {
            LauncherAction.OPEN_DRAWER -> navigateTo(LauncherScreen.DRAWER)
            LauncherAction.OPEN_NOTIFICATIONS -> repository.expandNotifications()
            LauncherAction.OPEN_SETTINGS -> navigateTo(LauncherScreen.SETTINGS)
            LauncherAction.OPEN_QUICK_SEARCH -> navigateTo(LauncherScreen.DRAWER)
            LauncherAction.OPEN_CAMERA -> repository.openCamera()
            LauncherAction.OPEN_CLOCK -> repository.openClock()
            LauncherAction.TOGGLE_FLASHLIGHT -> repository.toggleFlashlight()
            LauncherAction.OPEN_RECENTS -> openRecents()
            LauncherAction.SWITCH_TO_PREVIOUS_APP -> switchToPreviousApp()
            LauncherAction.NONE -> {}
        }
    }

    fun openAppDetails(packageName: String) {
        repository.openAppDetails(packageName)
    }

    fun uninstallApp(packageName: String) {
        repository.uninstallApp(packageName)
    }

    fun openSystemHomeSettings() {
        repository.openHomeSettings()
    }

    fun openClock() {
        repository.openClock()
    }

    fun openBatterySettings() {
        repository.openBatterySettings()
    }

    fun dismissNotice() {
        _notificationNotice.value = null
    }

    fun showNotice(msg: String) {
        _notificationNotice.value = msg
    }

    fun setHideStatusBar(hide: Boolean) {
        viewModelScope.launch {
            preferencesManager.setHideStatusBar(hide)
        }
    }

    fun addSystemAppWidget(appWidgetId: Int) {
        viewModelScope.launch {
            preferencesManager.addSystemAppWidget(appWidgetId)
        }
    }

    fun removeSystemAppWidget(appWidgetId: Int) {
        viewModelScope.launch {
            preferencesManager.removeSystemAppWidget(appWidgetId)
        }
    }
}

private data class BaseConfig(
    val widgets: Set<String>,
    val intention: String,
    val theme: String,
    val c24: Boolean,
    val autoOpen: Boolean,
    val hideStatusBar: Boolean = false
)

private data class ExtendedConfig(
    val size: String,
    val iconStyle: String,
    val gestures: Map<GestureType, LauncherAction>,
    val folders: List<AppFolder>,
    val recentPkgs: List<String>,
    val systemAppWidgetIds: List<Int> = emptyList()
)
