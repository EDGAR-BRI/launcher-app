package com.example.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap

data class AvailableWidgetInfo(
    val providerInfo: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val appPackage: String
)

@Composable
fun SystemAppWidgetView(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onRemoveWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val widgetInfo = remember(appWidgetId) {
        try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }
    }

    if (widgetInfo == null) {
        LaunchedEffect(appWidgetId) {
            onRemoveWidget()
        }
        return
    }

    val widgetLabel = remember(widgetInfo) {
        widgetInfo.loadLabel(context.packageManager) ?: "Widget"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(appWidgetId) {
                detectTapGestures(
                    onLongPress = { showDeleteConfirm = true }
                )
            }
            .testTag("system_widget_$appWidgetId")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    try {
                        appWidgetHost.createView(ctx, appWidgetId, widgetInfo).apply {
                            setAppWidget(appWidgetId, widgetInfo)
                        }
                    } catch (e: Exception) {
                        android.widget.TextView(ctx).apply {
                            text = "Error cargando widget: ${e.localizedMessage}"
                            setTextColor(android.graphics.Color.GRAY)
                            setPadding(16, 16, 16, 16)
                        }
                    }
                },
                update = { view ->
                    // View update callback
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(min = 60.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(widgetLabel) },
            text = { Text("¿Deseas eliminar este widget de la pantalla de inicio?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onRemoveWidget()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AppWidgetPickerDialog(
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onWidgetAdded: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var searchQuery by remember { mutableStateOf("") }
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingProviderInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }

    // Launcher for Widget Configuration Activity
    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            onWidgetAdded(id)
            onDismiss()
        } else if (id != null) {
            try {
                appWidgetHost.deleteAppWidgetId(id)
            } catch (_: Exception) {}
        }
        pendingWidgetId = null
        pendingProviderInfo = null
    }

    // Launcher for Widget Binding Permission
    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        val info = pendingProviderInfo
        if (result.resultCode == Activity.RESULT_OK && id != null && info != null) {
            if (info.configure != null) {
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                configureLauncher.launch(configIntent)
            } else {
                onWidgetAdded(id)
                onDismiss()
            }
        } else if (id != null) {
            try {
                appWidgetHost.deleteAppWidgetId(id)
            } catch (_: Exception) {}
        }
        pendingWidgetId = null
        pendingProviderInfo = null
    }

    val availableWidgets = remember {
        try {
            val providers = appWidgetManager.installedProviders
            providers.map { info ->
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(info.provider.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    info.provider.packageName
                }
                val widgetLabel = info.loadLabel(pm) ?: appLabel
                AvailableWidgetInfo(
                    providerInfo = info,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    appPackage = info.provider.packageName
                )
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        } catch (_: Exception) {
            emptyList()
        }
    }

    val filteredWidgets = remember(searchQuery, availableWidgets) {
        if (searchQuery.isBlank()) availableWidgets
        else {
            val q = searchQuery.trim().lowercase()
            availableWidgets.filter {
                it.appLabel.lowercase().contains(q) || it.widgetLabel.lowercase().contains(q)
            }
        }
    }

    val onSelectWidget: (AppWidgetProviderInfo) -> Unit = { providerInfo ->
        try {
            val newId = appWidgetHost.allocateAppWidgetId()
            pendingWidgetId = newId
            pendingProviderInfo = providerInfo

            val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(newId, providerInfo.provider)
            if (!canBind) {
                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                }
                bindLauncher.launch(bindIntent)
            } else {
                if (providerInfo.configure != null) {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = providerInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                    }
                    configureLauncher.launch(configIntent)
                } else {
                    onWidgetAdded(newId)
                    onDismiss()
                }
            }
        } catch (_: Exception) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Añadir Widget de App", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar widget o app...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredWidgets.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Text(
                            text = "No se encontraron widgets disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        items(filteredWidgets, key = { "${it.appPackage}_${it.widgetLabel}_${it.providerInfo.provider.className}" }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectWidget(item.providerInfo) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Widgets,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.widgetLabel,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.appLabel,
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
