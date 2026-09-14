package com.example.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppFolder
import com.example.data.model.AppInfo
import com.example.data.repository.AppRepository
import com.example.ui.LauncherViewModel
import com.example.ui.components.AppIconView

@Composable
fun FolderItemRow(
    folder: AppFolder,
    appsCount: Int,
    textSize: TextUnit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(folder.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("folder_row_${folder.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$appsCount apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentBottomSheet(
    folder: AppFolder,
    allApps: List<AppInfo>,
    iconStyle: String,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onEditFolder: (AppFolder) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFolder = uiState.folders.find { it.id == folder.id } ?: folder

    val folderApps = remember(currentFolder.packageNames, allApps) {
        val map = allApps.associateBy { it.packageName }
        currentFolder.packageNames.mapNotNull { map[it] }
    }

    var showAddAppsDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.90f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentFolder.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${folderApps.size} aplicaciones agrupadas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAddAppsDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Añadir apps a la carpeta",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onEditFolder(currentFolder) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar carpeta",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            if (folderApps.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Carpeta vacía",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showAddAppsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Añadir aplicaciones")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(folderApps, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
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
                                    iconStyle = iconStyle,
                                    repository = viewModel.repository,
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.removeAppFromFolder(currentFolder.id, app.packageName)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Quitar de carpeta",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAppsDialog) {
        SelectAppsForFolderDialog(
            folder = currentFolder,
            allApps = allApps,
            iconStyle = iconStyle,
            repository = viewModel.repository,
            currentPackageNames = currentFolder.packageNames.toSet(),
            onDismiss = { showAddAppsDialog = false },
            onAddApp = { pkg ->
                viewModel.addAppToFolder(currentFolder.id, pkg)
            },
            onRemoveApp = { pkg ->
                viewModel.removeAppFromFolder(currentFolder.id, pkg)
            }
        )
    }
}

@Composable
fun SelectAppsForFolderDialog(
    folder: AppFolder,
    allApps: List<AppInfo>,
    iconStyle: String = "none",
    repository: AppRepository? = null,
    currentPackageNames: Set<String> = folder.packageNames.toSet(),
    onDismiss: () -> Unit,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPkgs by remember(currentPackageNames) { mutableStateOf(currentPackageNames) }

    val filtered = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Apps en '${folder.name}'",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtrar apps...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isContained = selectedPkgs.contains(app.packageName)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isContained) {
                                        selectedPkgs = selectedPkgs - app.packageName
                                        onRemoveApp(app.packageName)
                                    } else {
                                        selectedPkgs = selectedPkgs + app.packageName
                                        onAddApp(app.packageName)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp)
                        ) {
                            Checkbox(
                                checked = isContained,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedPkgs = selectedPkgs + app.packageName
                                        onAddApp(app.packageName)
                                    } else {
                                        selectedPkgs = selectedPkgs - app.packageName
                                        onRemoveApp(app.packageName)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (repository != null && iconStyle != "none") {
                                AppIconView(
                                    packageName = app.packageName,
                                    label = app.label,
                                    iconStyle = iconStyle,
                                    repository = repository,
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}

@Composable
fun CreateOrEditFolderDialog(
    initialName: String = "",
    folderId: String? = null,
    showInHome: Boolean = true,
    showInDrawer: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (name: String, inHome: Boolean, inDrawer: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var inHomeState by remember { mutableStateOf(showInHome) }
    var inDrawerState by remember { mutableStateOf(showInDrawer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (folderId == null) "Crear Carpeta" else "Editar Carpeta")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { inHomeState = !inHomeState }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = inHomeState,
                        onCheckedChange = { inHomeState = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mostrar en Favoritos (Inicio)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { inDrawerState = !inDrawerState }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = inDrawerState,
                        onCheckedChange = { inDrawerState = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mostrar en Cajón de Apps",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), inHomeState, inDrawerState)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
