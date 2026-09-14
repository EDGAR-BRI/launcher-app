package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppFolder
import com.example.data.model.AppInfo
import com.example.data.model.CustomWidgetId
import com.example.data.model.GestureType
import com.example.ui.LauncherScreen
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import com.example.ui.components.AppIconView
import com.example.ui.drawer.SelectFolderForAppDialog
import com.example.ui.folder.CreateOrEditFolderDialog
import com.example.ui.folder.FolderContentBottomSheet
import com.example.ui.folder.FolderItemRow
import com.example.ui.widgets.AppWidgetPickerDialog
import com.example.ui.widgets.SystemAppWidgetView
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    appWidgetHost: AppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    isDrawerPartiallyOpen: () -> Boolean = { false },
    onSwipeUpDrag: ((Float) -> Unit)? = null,
    onSwipeUpRelease: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val favoritesListState = rememberLazyListState()
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var selectedFolderForView by remember { mutableStateOf<AppFolder?>(null) }
    var folderToEdit by remember { mutableStateOf<AppFolder?>(null) }
    var appToAddToFolder by remember { mutableStateOf<AppInfo?>(null) }
    var showWidgetPicker by remember { mutableStateOf(false) }

    // Nested scroll connection for favorites list:
    // Enables seamless swipe up to open all apps drawer when finishing favorites scroll,
    // or when favorites fits on screen without needing to swipe from system bar edge.
    val favoritesNestedScrollConnection = remember(favoritesListState, isDrawerPartiallyOpen) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If the drawer is already pulled open and user drags down, close it first
                if (available.y > 0 && isDrawerPartiallyOpen()) {
                    onSwipeUpDrag?.invoke(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When dragging UP and favorites cannot scroll further (or fits on screen),
                // smoothly pull up the all-apps drawer
                if (available.y < 0) {
                    onSwipeUpDrag?.invoke(available.y)
                    return Offset(0f, available.y)
                }
                // If drawer is partially pulled and dragging down, continue closing it
                if (available.y > 0 && isDrawerPartiallyOpen()) {
                    onSwipeUpDrag?.invoke(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isDrawerPartiallyOpen()) {
                    onSwipeUpRelease?.invoke(available.y)
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (isDrawerPartiallyOpen() || available.y < 0) {
                    onSwipeUpRelease?.invoke(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val appTextSize = when (uiState.textSize) {
        "small" -> 17.sp
        "large" -> 25.sp
        else -> 21.sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (uiState.hideStatusBar) Modifier else Modifier.statusBarsPadding())
            .navigationBarsPadding()
            // Gestures detection: Real-time 1:1 finger tracking on vertical swipe up, plus horizontal/down gestures
            .pointerInput(Unit) {
                var isVertical: Boolean? = null
                var totalX = 0f
                var totalY = 0f
                val velocityTracker = VelocityTracker()

                detectDragGestures(
                    onDragStart = {
                        isVertical = null
                        totalX = 0f
                        totalY = 0f
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val velocityY = velocityTracker.calculateVelocity().y
                        if (isVertical == true) {
                            if (totalY < 0) {
                                if (onSwipeUpRelease != null) {
                                    onSwipeUpRelease(velocityY)
                                } else {
                                    viewModel.navigateTo(LauncherScreen.DRAWER)
                                }
                            } else if (totalY > 50f) {
                                viewModel.executeGesture(GestureType.SWIPE_DOWN)
                            }
                        } else if (isVertical == false) {
                            if (abs(totalX) > 40f) {
                                if (totalX < 0) viewModel.executeGesture(GestureType.SWIPE_LEFT)
                                else viewModel.executeGesture(GestureType.SWIPE_RIGHT)
                            }
                        }
                        isVertical = null
                    },
                    onDragCancel = {
                        if (isVertical == true && totalY < 0) {
                            onSwipeUpRelease?.invoke(0f)
                        }
                        isVertical = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        totalX += dragAmount.x
                        totalY += dragAmount.y

                        if (isVertical == null) {
                            if (abs(totalY) > abs(totalX) && abs(totalY) > 8f) {
                                isVertical = true
                            } else if (abs(totalX) > abs(totalY) && abs(totalX) > 12f) {
                                isVertical = false
                            }
                        }

                        if (isVertical == true) {
                            if (dragAmount.y < 0 || totalY < 0) {
                                // Real-time 1:1 finger tracking! The drawer slides up immediately with the finger
                                onSwipeUpDrag?.invoke(dragAmount.y)
                            }
                        }
                    }
                )
            }
            .testTag("home_screen")
    ) {
        // Background tap detector for Double Tap & Long Press on empty home areas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            viewModel.executeGesture(GestureType.DOUBLE_TAP)
                        },
                        onLongPress = {
                            viewModel.executeGesture(GestureType.LONG_PRESS)
                        }
                    )
                }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Top Bar: Settings & Multitasking buttons
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Multitasking / Recent apps quick button
                IconButton(
                    onClick = {
                        viewModel.showRecentsSheet()
                    },
                    modifier = Modifier.testTag("home_recents_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = "Apps recientes / Multitarea",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showWidgetPicker = true },
                        modifier = Modifier.testTag("home_add_widget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = "Añadir widget",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(LauncherScreen.SETTINGS) },
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notice Banner
            AnimatedVisibility(
                visible = uiState.notificationNotice != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.notificationNotice?.let { notice ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissNotice() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Custom Widgets Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.enabledWidgets.contains(CustomWidgetId.CLOCK_DATE.name)) {
                    ClockDateWidget(
                        is24Hour = uiState.clock24h,
                        onClockClick = { viewModel.openClock() }
                    )
                }

                if (uiState.enabledWidgets.contains(CustomWidgetId.BATTERY.name)) {
                    BatteryWidget(
                        onBatteryClick = { viewModel.openBatterySettings() }
                    )
                }

                if (uiState.enabledWidgets.contains(CustomWidgetId.DAILY_INTENTION.name)) {
                    DailyIntentionWidget(
                        intention = uiState.dailyIntention,
                        onSaveIntention = { newText: String -> viewModel.updateDailyIntention(newText) }
                    )
                }

                // System App Widgets
                if (appWidgetHost != null && appWidgetManager != null && uiState.systemAppWidgetIds.isNotEmpty()) {
                    uiState.systemAppWidgetIds.forEach { widgetId ->
                        SystemAppWidgetView(
                            appWidgetId = widgetId,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onRemoveWidget = {
                                viewModel.removeSystemAppWidget(widgetId)
                                try {
                                    appWidgetHost.deleteAppWidgetId(widgetId)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Favorites & Home Folders Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FAVORITOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.favoriteApps.isNotEmpty() || uiState.homeFolders.isNotEmpty()) {
                        val totalCount = uiState.favoriteApps.size + uiState.homeFolders.size
                        Text(
                            text = "$totalCount elementos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Favorites & Folders List
            val hasContent = uiState.favoriteApps.isNotEmpty() || uiState.homeFolders.isNotEmpty()

            if (!hasContent) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.navigateTo(LauncherScreen.DRAWER) }
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sin aplicaciones favoritas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toca aquí o desliza hacia arriba para abrir el cajón. Mantén presionada cualquier app para fijarla en favoritos o crear carpetas.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = favoritesListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(favoritesNestedScrollConnection)
                        .testTag("favorite_apps_list")
                ) {
                    // 1. Folders in Home
                    items(uiState.homeFolders, key = { "home_folder_${it.id}" }) { folder ->
                        FolderItemRow(
                            folder = folder,
                            appsCount = folder.packageNames.size,
                            textSize = appTextSize,
                            onClick = { selectedFolderForView = folder },
                            onLongClick = { folderToEdit = folder }
                        )
                    }

                    // 2. Favorite Apps
                    items(uiState.favoriteApps, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .pointerInput(app.packageName) {
                                    detectTapGestures(
                                        onTap = { viewModel.launchApp(app) },
                                        onLongPress = { selectedAppForMenu = app }
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("favorite_app_${app.packageName}")
                        ) {
                            AppIconView(
                                packageName = app.packageName,
                                label = app.label,
                                iconStyle = uiState.iconStyle,
                                repository = viewModel.repository,
                                size = 30.dp
                            )

                            if (uiState.iconStyle != "none") {
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            Text(
                                text = app.label.lowercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = appTextSize,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.4.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Dock: All Apps swipe up hint
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Center Swipe up hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.navigateTo(LauncherScreen.DRAWER) }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("swipe_up_hint")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Desliza para ver todas las apps",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "todas las aplicaciones",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }

    // Modal Sheet for App Options (Favorites)
    selectedAppForMenu?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppForMenu = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconView(
                        packageName = app.packageName,
                        label = app.label,
                        iconStyle = uiState.iconStyle,
                        repository = viewModel.repository,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add to Folder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            appToAddToFolder = app
                            selectedAppForMenu = null
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Añadir a carpeta...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Remove from favorites
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.toggleFavorite(app.packageName)
                            selectedAppForMenu = null
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Quitar de favoritos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // App Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.openAppDetails(app.packageName)
                            selectedAppForMenu = null
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Detalles de la aplicación",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Uninstall
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.uninstallApp(app.packageName)
                            selectedAppForMenu = null
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Desinstalar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // View Folder Content BottomSheet
    selectedFolderForView?.let { folder ->
        val currentFolder = uiState.folders.find { it.id == folder.id } ?: folder
        FolderContentBottomSheet(
            folder = currentFolder,
            allApps = uiState.drawerApps,
            iconStyle = uiState.iconStyle,
            viewModel = viewModel,
            onDismiss = { selectedFolderForView = null },
            onEditFolder = {
                folderToEdit = it
            }
        )
    }

    // Edit Folder Dialog
    folderToEdit?.let { folder ->
        CreateOrEditFolderDialog(
            initialName = folder.name,
            folderId = folder.id,
            showInHome = folder.showInHome,
            showInDrawer = folder.showInDrawer,
            onDismiss = { folderToEdit = null },
            onSave = { name, inHome, inDrawer ->
                viewModel.renameFolder(folder.id, name)
                viewModel.toggleFolderPlacement(folder.id, inHome, inDrawer)
                folderToEdit = null
            }
        )
    }

    // Add App to Folder Dialog
    appToAddToFolder?.let { app ->
        SelectFolderForAppDialog(
            app = app,
            folders = uiState.folders,
            onDismiss = { appToAddToFolder = null },
            onSelectFolder = { folderId ->
                viewModel.addAppToFolder(folderId, app.packageName)
                appToAddToFolder = null
            },
            onCreateNewFolderWithApp = { folderName ->
                viewModel.createFolder(folderName, initialPackages = listOf(app.packageName), inHome = true)
                appToAddToFolder = null
            }
        )
    }

    // App Widget Picker Dialog
    if (showWidgetPicker && appWidgetHost != null && appWidgetManager != null) {
        AppWidgetPickerDialog(
            appWidgetHost = appWidgetHost,
            appWidgetManager = appWidgetManager,
            onWidgetAdded = { widgetId ->
                viewModel.addSystemAppWidget(widgetId)
            },
            onDismiss = { showWidgetPicker = false }
        )
    }
}
