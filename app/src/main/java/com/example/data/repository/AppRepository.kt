package com.example.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.graphics.drawable.Drawable
import com.example.data.local.PreferencesManager
import com.example.data.model.AppInfo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val packageManager: PackageManager = context.packageManager
    private val _rawApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val rawApps: Flow<List<AppInfo>> = _rawApps.asStateFlow()
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    private var isTorchOn = false

    suspend fun reloadApps() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        val myPackage = context.packageName

        val apps = resolveInfos
            .filter { it.activityInfo != null && it.activityInfo.packageName != myPackage }
            .map { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim()
                    ?: resolveInfo.activityInfo.name
                val pkg = resolveInfo.activityInfo.packageName
                val activityName = resolveInfo.activityInfo.name
                AppInfo(
                    label = label,
                    packageName = pkg,
                    activityName = activityName
                )
            }
            // Distinct by package or activity to prevent duplicate entries
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

        _rawApps.value = apps
    }

    val appsWithPreferences: Flow<List<AppInfo>> = combine(
        rawApps,
        preferencesManager.favoritesFlow,
        preferencesManager.hiddenFlow
    ) { apps, favorites, hidden ->
        apps.map { app ->
            app.copy(
                isFavorite = favorites.contains(app.packageName),
                isHidden = hidden.contains(app.packageName)
            )
        }
    }

    fun launchApp(packageName: String, activityName: String = ""): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(launchIntent)
                true
            } else if (activityName.isNotEmpty()) {
                val explicitIntent = Intent().apply {
                    setClassName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(explicitIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun expandNotifications() {
        try {
            @Suppress("WrongConstant")
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (_: Exception) {}
    }

    fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
            }
        } catch (_: Exception) {}
    }

    fun openCamera() {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    fun openClock() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_DATE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openBatterySettings() {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openHomeSettings() {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        iconCache[packageName]?.let { return it }
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            iconCache[packageName] = drawable
            drawable
        } catch (_: Exception) {
            null
        }
    }

    fun openRecents(): Boolean {
        // Method 1: System UI recent apps broadcast/intent
        try {
            val intent = Intent("com.android.systemui.recent.action.TOGGLE_RECENTS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {}

        // Method 2: StatusBarManager toggleRecentApps via reflection
        try {
            @Suppress("WrongConstant")
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("toggleRecentApps")
            method.invoke(statusBarService)
            return true
        } catch (_: Exception) {}

        return false
    }
}
