package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppFolder
import com.example.data.model.CustomWidgetId
import com.example.data.model.GestureType
import com.example.data.model.LauncherAction
import com.example.ui.LauncherScreen
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import com.example.ui.folder.CreateOrEditFolderDialog

@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    modifier: Modifier = Modifier
) {
    var selectedGestureForEdit by remember { mutableStateOf<GestureType?>(null) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<AppFolder?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(LauncherScreen.HOME) },
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Volver al inicio",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajustes del Launcher",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_scroll_list")
            ) {
                // Default launcher card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Launcher Predeterminado",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configurar este launcher como pantalla de inicio principal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.openSystemHomeSettings() }
                            ) {
                                Text("Fijar")
                            }
                        }
                    }
                }

                // Section: App Folders Management
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Outlined.Folder,
                            title = "Carpetas de Apps"
                        )
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                                contentDescription = "Crear carpeta",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (uiState.folders.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "No has creado ninguna carpeta",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showCreateFolderDialog = true }) {
                                    Text("Crear primera carpeta")
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.folders, key = { "settings_folder_${it.id}" }) { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${folder.packageNames.size} apps • ${if (folder.showInHome) "En Inicio" else "Oculta en inicio"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { folderToEdit = folder }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Editar carpeta",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteFolder(folder.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Eliminar carpeta",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: App Icons Customization
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Image,
                        title = "Estilo de Iconos de Apps"
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        val iconOptions = listOf(
                            "monochrome" to "Monocromático (Minimalista)",
                            "custom_badge" to "Insignia personalizada",
                            "system" to "Iconos del sistema (Color)",
                            "none" to "Sin iconos (Solo texto)"
                        )

                        iconOptions.forEach { (key, label) ->
                            val isSelected = uiState.iconStyle == key
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setIconStyle(key) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setIconStyle(key) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Section: Custom Widgets
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Widgets,
                        title = "Widgets en Pantalla de Inicio"
                    )
                }

                items(CustomWidgetId.values().size) { index ->
                    val widget = CustomWidgetId.values()[index]
                    val isEnabled = uiState.enabledWidgets.contains(widget.name)
                    SettingsToggleRow(
                        title = widget.title,
                        subtitle = widget.description,
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleWidget(widget.name, it) }
                    )
                }

                // Section: Navigation Gestures
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Gesture,
                        title = "Gestos de Navegación"
                    )
                }

                items(GestureType.values().size) { index ->
                    val gesture = GestureType.values()[index]
                    val action = uiState.gestureActions[gesture] ?: when (gesture) {
                        GestureType.SWIPE_UP -> LauncherAction.OPEN_DRAWER
                        GestureType.SWIPE_DOWN -> LauncherAction.OPEN_NOTIFICATIONS
                        GestureType.SWIPE_LEFT -> LauncherAction.OPEN_CAMERA
                        GestureType.SWIPE_RIGHT -> LauncherAction.OPEN_SETTINGS
                        GestureType.DOUBLE_TAP -> LauncherAction.TOGGLE_FLASHLIGHT
                        GestureType.LONG_PRESS -> LauncherAction.OPEN_SETTINGS
                    }

                    SettingsClickableRow(
                        title = gesture.title,
                        value = action.title,
                        onClick = { selectedGestureForEdit = gesture }
                    )
                }

                // Section: Theme & Typography
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSectionHeader(
                        icon = Icons.Outlined.ColorLens,
                        title = "Apariencia y Estilo"
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Tema Visual",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "amoled" to "AMOLED",
                                "monochrome_gray" to "Carbón",
                                "monochrome_light" to "Papel"
                            ).forEach { (mode, label) ->
                                val isSelected = uiState.themeMode == mode
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsToggleRow(
                        title = "Formato de 24 horas",
                        subtitle = "Mostrar el reloj en formato 24h o 12h (AM/PM)",
                        checked = uiState.clock24h,
                        onCheckedChange = { viewModel.setClock24h(it) }
                    )
                }

                item {
                    SettingsToggleRow(
                        title = "Apertura rápida de búsqueda",
                        subtitle = "Abrir automáticamente la aplicación si solo queda 1 resultado",
                        checked = uiState.autoOpenSingle,
                        onCheckedChange = { viewModel.setAutoOpenSingle(it) }
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Tamaño de Tipografía",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "small" to "Compacto",
                                "medium" to "Normal",
                                "large" to "Grande"
                            ).forEach { (size, label) ->
                                val isSelected = uiState.textSize == size
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { viewModel.setTextSize(size) }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: App Management
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Visibility,
                        title = "Aplicaciones Ocultas"
                    )
                }

                item {
                    SettingsClickableRow(
                        title = "Gestionar apps ocultas",
                        value = "${uiState.hiddenApps.size} apps ocultas",
                        onClick = { showHiddenAppsDialog = true }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Gesture selection dialog
    selectedGestureForEdit?.let { gesture ->
        val currentAction = uiState.gestureActions[gesture] ?: LauncherAction.NONE
        AlertDialog(
            onDismissRequest = { selectedGestureForEdit = null },
            title = {
                Text(
                    text = gesture.title,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LauncherAction.values().forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setGestureAction(gesture, action)
                                    selectedGestureForEdit = null
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            RadioButton(
                                selected = currentAction == action,
                                onClick = {
                                    viewModel.setGestureAction(gesture, action)
                                    selectedGestureForEdit = null
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGestureForEdit = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Hidden Apps Management Dialog
    if (showHiddenAppsDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenAppsDialog = false },
            title = { Text("Aplicaciones Ocultas") },
            text = {
                if (uiState.hiddenApps.isEmpty()) {
                    Text(
                        text = "No tienes ninguna aplicación oculta. Puedes ocultar aplicaciones manteniendo pulsada cualquier app en el cajón de búsqueda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(uiState.hiddenApps, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.toggleHidden(app.packageName) }
                                ) {
                                    Text("Mostrar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenAppsDialog = false }) {
                    Text("Listo")
                }
            }
        )
    }

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        CreateOrEditFolderDialog(
            initialName = "",
            showInHome = true,
            showInDrawer = true,
            onDismiss = { showCreateFolderDialog = false },
            onSave = { name, inHome, inDrawer ->
                viewModel.createFolder(name, inHome = inHome)
                showCreateFolderDialog = false
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
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
