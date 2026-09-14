package com.example.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppFolder
import com.example.data.model.AppInfo
import com.example.ui.LauncherScreen
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import com.example.ui.components.AppIconView
import com.example.ui.folder.CreateOrEditFolderDialog
import com.example.ui.folder.FolderContentBottomSheet
import com.example.ui.folder.FolderItemRow
import com.example.ui.folder.SelectAppsForFolderDialog
import kotlinx.coroutines.launch

private fun normalizeLetterChar(c: Char): Char = when (c.uppercaseChar()) {
    'Á', 'À', 'Â', 'Ã', 'Ä' -> 'A'
    'É', 'È', 'Ê', 'Ë' -> 'E'
    'Í', 'Ì', 'Î', 'Ï' -> 'I'
    'Ó', 'Ò', 'Ô', 'Õ', 'Ö' -> 'O'
    'Ú', 'Ù', 'Û', 'Ü' -> 'U'
    'Ñ' -> 'N'
    else -> c.uppercaseChar()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    onSwipeDownDrag: ((Float) -> Unit)? = null,
    onSwipeDownRelease: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    val nestedScrollConnection = remember(listState) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // If pulling DOWN while at the very top of the list, slide the drawer down with the finger
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    onSwipeDownDrag?.invoke(available.y)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 && available.y > 0) {
                    onSwipeDownRelease?.invoke(available.y)
                    return available
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var selectedFolderForView by remember { mutableStateOf<AppFolder?>(null) }
    var folderToEdit by remember { mutableStateOf<AppFolder?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var appToAddToFolder by remember { mutableStateOf<AppInfo?>(null) }

    var totalDragY by remember { mutableFloatStateOf(0f) }
    var drawerSwipeTriggered by remember { mutableStateOf(false) }

    // Alphabet fast scrubber state
    var isScrubbing by remember { mutableStateOf(false) }
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }
    var alphabetHeightPx by remember { mutableIntStateOf(1) }

    val alphabet = remember { ('A'..'Z').toList() }
    val groupedApps = remember(uiState.drawerApps) {
        uiState.drawerApps.groupBy {
            val firstChar = it.label.trim().firstOrNull() ?: '#'
            val norm = normalizeLetterChar(firstChar)
            if (norm in 'A'..'Z') norm else '#'
        }
    }

    val selectLetter: (Char) -> Unit = { letter ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (uiState.searchQuery.equals(letter.toString(), ignoreCase = true)) {
            viewModel.clearSearch()
            activeScrubLetter = null
        } else {
            activeScrubLetter = letter
            viewModel.onSearchQueryChanged(letter.toString())
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }

    val handleLetterAtY: (Float) -> Unit = { yPos ->
        if (alphabetHeightPx > 0) {
            val fraction = (yPos / alphabetHeightPx).coerceIn(0f, 0.999f)
            val letterIndex = (fraction * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
            val letter = alphabet[letterIndex]

            if (letter != activeScrubLetter) {
                activeScrubLetter = letter
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onSearchQueryChanged(letter.toString())
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.searchQuery.isNotEmpty()) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (uiState.hideStatusBar) Modifier else Modifier.statusBarsPadding())
            .navigationBarsPadding()
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                var totalY = 0f

                detectDragGestures(
                    onDragStart = {
                        totalY = 0f
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val velocityY = velocityTracker.calculateVelocity().y
                        if (onSwipeDownRelease != null && totalY > 0f) {
                            onSwipeDownRelease(velocityY)
                        } else if (totalY > 40f) {
                            viewModel.navigateTo(LauncherScreen.HOME)
                        }
                    },
                    onDragCancel = {
                        if (onSwipeDownRelease != null && totalY > 0f) {
                            onSwipeDownRelease(0f)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        totalY += dragAmount.y
                        if (dragAmount.y > 0 || totalY > 0) {
                            onSwipeDownDrag?.invoke(dragAmount.y)
                        }
                    }
                )
            }
            .testTag("app_drawer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp)
        ) {
            // Top Search & Navigation Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(LauncherScreen.HOME) },
                    modifier = Modifier.testTag("drawer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Volver al inicio",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { query ->
                        viewModel.onSearchQueryChanged(query)
                        if (uiState.autoOpenSingle && query.isNotBlank() && query.length > 1) {
                            val matches = uiState.drawerApps.filter {
                                it.label.lowercase().contains(query.trim().lowercase())
                            }
                            if (matches.size == 1) {
                                viewModel.launchApp(matches.first())
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Buscar ${uiState.drawerApps.size} apps...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = "Borrar búsqueda",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { showCreateFolderDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = "Nueva carpeta",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (uiState.filteredApps.isNotEmpty()) {
                                viewModel.launchApp(uiState.filteredApps.first())
                            }
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("drawer_search_input")
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (uiState.filteredApps.isEmpty() && uiState.drawerFolders.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (uiState.searchQuery.length == 1) {
                                    "Sin aplicaciones con '${uiState.searchQuery.uppercase()}'"
                                } else {
                                    "No se encontraron aplicaciones"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (uiState.searchQuery.length == 1) {
                                    "Sigue deslizando el dedo por el abecedario"
                                } else {
                                    "Intenta buscar con otro término"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.clearSearch() },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Mostrar todas")
                            }
                        }
                    }
                } else {
                    // Main Apps & Folders List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(nestedScrollConnection)
                            .testTag("drawer_apps_list")
                    ) {
                        // Letter search active indicator header
                        if (uiState.searchQuery.length == 1) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = "Letra ${uiState.searchQuery.uppercase()}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = "${uiState.filteredApps.size} apps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.clearSearch() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Mostrar todas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Display Drawer Folders if search query is empty
                        if (uiState.searchQuery.isEmpty() && uiState.drawerFolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "CARPETAS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                                )
                            }

                            items(uiState.drawerFolders, key = { "folder_${it.id}" }) { folder ->
                                FolderItemRow(
                                    folder = folder,
                                    appsCount = folder.packageNames.size,
                                    textSize = 17.sp,
                                    onClick = { selectedFolderForView = folder },
                                    onLongClick = { folderToEdit = folder },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "TODAS LAS APLICACIONES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                                )
                            }
                        }

                        // App Items
                        items(uiState.filteredApps, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerInput(app.packageName) {
                                        detectTapGestures(
                                            onTap = { viewModel.launchApp(app) },
                                            onLongPress = { selectedAppForMenu = app }
                                        )
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                                    .testTag("drawer_app_${app.packageName}")
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
                                        size = 28.dp
                                    )

                                    if (uiState.iconStyle != "none") {
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }

                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 17.sp,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                if (app.isFavorite) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Favorita",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Alphabet Scrubber Column on Right: Taps & Continuous Drag/Scrub
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp)
                            .onGloballyPositioned { coordinates ->
                                alphabetHeightPx = coordinates.size.height
                            }
                            .pointerInput(alphabet, uiState.drawerApps) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isScrubbing = true
                                    val fraction = (down.position.y / alphabetHeightPx).coerceIn(0f, 0.999f)
                                    val initialIndex = (fraction * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                                    selectLetter(alphabet[initialIndex])

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            isScrubbing = false
                                            change.consume()
                                            break
                                        }
                                        change.consume()
                                        handleLetterAtY(change.position.y)
                                    }
                                }
                            }
                            .testTag("drawer_alphabet_column")
                    ) {
                        alphabet.forEach { letter ->
                            val hasApps = groupedApps.containsKey(letter)
                            val isCurrentFiltered = uiState.searchQuery.equals(letter.toString(), ignoreCase = true)
                            val isScrubActive = isScrubbing && activeScrubLetter == letter
                            val isSelected = isCurrentFiltered || isScrubActive

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(if (isSelected) 22.dp else 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                            ) {
                                Text(
                                    text = letter.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (isSelected) 11.sp else 9.sp,
                                        fontWeight = if (isSelected || hasApps) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        hasApps -> MaterialTheme.colorScheme.onBackground
                                        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

        // Scrubbing Bubble Floating Indicator
        AnimatedVisibility(
            visible = isScrubbing && activeScrubLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 56.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeScrubLetter?.toString() ?: "",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    // Modal Sheet for App Options
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

                Spacer(modifier = Modifier.height(18.dp))

                // Add to folder
                DrawerOptionRow(
                    icon = Icons.Outlined.Folder,
                    title = "Añadir a carpeta...",
                    onClick = {
                        appToAddToFolder = app
                        selectedAppForMenu = null
                    }
                )

                // Toggle Favorite
                DrawerOptionRow(
                    icon = if (app.isFavorite) Icons.Outlined.StarBorder else Icons.Outlined.Star,
                    title = if (app.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    onClick = {
                        viewModel.toggleFavorite(app.packageName)
                        selectedAppForMenu = null
                    }
                )

                // Hide App
                DrawerOptionRow(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "Ocultar app del cajón",
                    onClick = {
                        viewModel.toggleHidden(app.packageName)
                        selectedAppForMenu = null
                    }
                )

                // App Details
                DrawerOptionRow(
                    icon = Icons.Outlined.Info,
                    title = "Detalles de la aplicación",
                    onClick = {
                        viewModel.openAppDetails(app.packageName)
                        selectedAppForMenu = null
                    }
                )

                // Uninstall
                DrawerOptionRow(
                    icon = Icons.Outlined.Delete,
                    title = "Desinstalar",
                    isDestructive = true,
                    onClick = {
                        viewModel.uninstallApp(app.packageName)
                        selectedAppForMenu = null
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // View Folder Content BottomSheet
    selectedFolderForView?.let { folder ->
        // Retrieve fresh instance from state
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
                viewModel.createFolder(folderName, initialPackages = listOf(app.packageName))
                appToAddToFolder = null
            }
        )
    }
}

@Composable
fun SelectFolderForAppDialog(
    app: AppInfo,
    folders: List<AppFolder>,
    onDismiss: () -> Unit,
    onSelectFolder: (folderId: String) -> Unit,
    onCreateNewFolderWithApp: (name: String) -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Añadir '${app.label}' a carpeta")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCreatingNew) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Nombre de la nueva carpeta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (folders.isEmpty()) {
                        Text(
                            text = "No hay carpetas creadas. Puedes crear una nueva carpeta para agrupar tus aplicaciones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            items(folders, key = { it.id }) { folder ->
                                val alreadyIn = folder.packageNames.contains(app.packageName)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectFolder(folder.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (alreadyIn) {
                                        Text(
                                            text = "Ya añadida",
                                            style = MaterialTheme.typography.labelSmall,
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
            if (isCreatingNew) {
                androidx.compose.material3.Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateNewFolderWithApp(newFolderName.trim())
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Crear y Añadir")
                }
            } else {
                androidx.compose.material3.Button(
                    onClick = { isCreatingNew = true }
                ) {
                    Text("Crear Nueva")
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DrawerOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
