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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppFolder
import com.example.data.model.AppInfo
import com.example.data.model.CustomWidgetId
import com.example.data.model.GestureType
import com.example.ui.LauncherScreen
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import com.example.ui.components.AppIconView
import com.example.ui.drawer.SelectFolderForAppDialog
import com.example.ui.folder.CreateOrEditFolderDialog
import com.example.ui.folder.FolderContentBottomSheet
import com.example.ui.folder.FolderItemRow
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    modifier: Modifier = Modifier
) {
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var selectedFolderForView by remember { mutableStateOf<AppFolder?>(null) }
    var folderToEdit by remember { mutableStateOf<AppFolder?>(null) }
    var appToAddToFolder by remember { mutableStateOf<AppInfo?>(null) }
    var showRecentAppsSheet by remember { mutableStateOf(false) }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var gestureFired by remember { mutableStateOf(false) }

    val appTextSize = when (uiState.textSize) {
        "small" -> 17.sp
        "large" -> 25.sp
        else -> 21.sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            // Gestures detection: Instant response on drag and background taps
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                        gestureFired = false
                    },
                    onDragEnd = {
                        if (!gestureFired) {
                            val absX = abs(totalDragX)
                            val absY = abs(totalDragY)
                            if (absY > absX && absY > 25f) {
                                if (totalDragY < 0) viewModel.executeGesture(GestureType.SWIPE_UP)
                                else viewModel.executeGesture(GestureType.SWIPE_DOWN)
                            } else if (absX > absY && absX > 35f) {
                                if (totalDragX < 0) viewModel.executeGesture(GestureType.SWIPE_LEFT)
                                else viewModel.executeGesture(GestureType.SWIPE_RIGHT)
                            }
                        }
                        gestureFired = false
                    },
                    onDragCancel = {
                        gestureFired = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        // Trigger IMMEDIATELY on flick/drag (approx 28px) without waiting for finger release
                        if (!gestureFired) {
                            val absX = abs(totalDragX)
                            val absY = abs(totalDragY)
                            if (absY > absX && absY > 28f) {
                                gestureFired = true
                                if (totalDragY < 0) {
                                    viewModel.executeGesture(GestureType.SWIPE_UP)
                                } else {
                                    viewModel.executeGesture(GestureType.SWIPE_DOWN)
                                }
                            } else if (absX > absY && absX > 40f) {
                                gestureFired = true
                                if (totalDragX < 0) {
                                    viewModel.executeGesture(GestureType.SWIPE_LEFT)
                                } else {
                                    viewModel.executeGesture(GestureType.SWIPE_RIGHT)
                                }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            // Try system recents first, fallback to in-app recent apps sheet
                            val opened = viewModel.openRecents()
                            if (!opened) {
                                showRecentAppsSheet = true
                            }
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

                    if (uiState.lastLaunchedApp != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.launchApp(uiState.lastLaunchedApp)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("quick_switch_last_app")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Volver a ${uiState.lastLaunchedApp.label}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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

            // Bottom Navigation Dock: All Apps & Multitasking
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Fast switch to recents button
                IconButton(
                    onClick = { showRecentAppsSheet = true },
                    modifier = Modifier.testTag("dock_recents_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Historial reciente",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

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

                // Quick Recents / Multitasking Toggle Button
                IconButton(
                    onClick = {
                        val handled = viewModel.openRecents()
                        if (!handled) {
                            showRecentAppsSheet = true
                        }
                    },
                    modifier = Modifier.testTag("dock_menu_recents_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = "Menú de apps / Recientes",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Modal Sheet for Recent Apps / Fast Switcher
    if (showRecentAppsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecentAppsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Apps Recientes",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.openRecents()
                        }
                    ) {
                        Text("Multitarea Android")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.recentApps.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Text(
                            text = "Aún no has abierto aplicaciones recientemente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(uiState.recentApps, key = { "recent_${it.packageName}" }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showRecentAppsSheet = false
                                        viewModel.launchApp(app)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppIconView(
                                        packageName = app.packageName,
                                        label = app.label,
                                        iconStyle = uiState.iconStyle,
                                        repository = viewModel.repository,
                                        size = 30.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tocar para volver a esta app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
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
}
