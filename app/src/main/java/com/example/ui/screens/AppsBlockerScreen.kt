package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.ui.AppFilterTab
import com.example.ui.AppShieldUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.BlockModeBadge
import com.example.ui.theme.AmberSchedule
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VioletMemory
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsBlockerScreen(
    uiState: AppShieldUiState,
    loadIcon: suspend (String) -> ImageBitmap?,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (AppFilterTab) -> Unit,
    onToggleMultiSelect: (Boolean) -> Unit,
    onTogglePackageSelection: (String) -> Unit,
    onSelectAllVisible: (List<InstalledAppInfo>) -> Unit,
    onClearSelection: () -> Unit,
    onRefreshApps: () -> Unit,
    onConfigureApps: (List<InstalledAppInfo>) -> Unit
) {
    val selectedApps = uiState.apps.filter { it.packageName in uiState.selectedPackages }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "قائمة التطبيقات المثبتة",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "اضغط على أي تطبيق أو فعّل التحديد المتعدد لضبط الحجب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleMultiSelect(!uiState.isMultiSelectMode) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("multi_select_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckBox,
                                contentDescription = "وضع التحديد المتعدد",
                                tint = if (uiState.isMultiSelectMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(
                            onClick = onRefreshApps,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_apps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "تحديث قائمة التطبيقات"
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("ابحث باسم التطبيق أو الحزمة...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "بحث"
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "مسح البحث"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFilterTab.entries.forEach { tab ->
                        val selected = uiState.activeFilter == tab
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterSelected(tab) },
                            label = {
                                val countSuffix = when (tab) {
                                    AppFilterTab.ALL -> " (${uiState.apps.size})"
                                    AppFilterTab.BLOCKED -> " (${uiState.apps.count { it.blockMode.isBlocked }})"
                                    else -> ""
                                }
                                Text("${tab.labelAr}$countSuffix")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("filter_chip_${tab.name}")
                        )
                    }
                }
            }

            // Apps List
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoadingApps && uiState.apps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "جاري فحص التطبيقات المثبتة...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    uiState.filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "لا توجد تطبيقات مطابقة",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "جرّب تغيير نص البحث أو الفلتر المحدد.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (uiState.isMultiSelectMode || selectedApps.isNotEmpty()) 96.dp else 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.filteredApps, key = { it.packageName }) { app ->
                                val isChecked = app.packageName in uiState.selectedPackages
                                AppListItemCard(
                                    app = app,
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    isChecked = isChecked,
                                    loadIcon = loadIcon,
                                    onClick = {
                                        if (uiState.isMultiSelectMode) {
                                            onTogglePackageSelection(app.packageName)
                                        } else {
                                            onConfigureApps(listOf(app))
                                        }
                                    },
                                    onLongClick = {
                                        onTogglePackageSelection(app.packageName)
                                    },
                                    onCheckboxToggle = {
                                        onTogglePackageSelection(app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Batch Selection Action Bar
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isMultiSelectMode || selectedApps.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "إلغاء التحديد"
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تم تحديد ${selectedApps.size} تطبيق",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                TextButton(
                                    onClick = { onSelectAllVisible(uiState.filteredApps) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SelectAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تحديد الظاهر (${uiState.filteredApps.size})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (selectedApps.isNotEmpty()) {
                                        onConfigureApps(selectedApps)
                                    }
                                },
                                enabled = selectedApps.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("configure_selected_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ضبط الحجب")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItemCard(
    app: InstalledAppInfo,
    isMultiSelectMode: Boolean,
    isChecked: Boolean,
    loadIcon: suspend (String) -> ImageBitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckboxToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("app_card_${app.packageName}"),
        color = if (isChecked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isChecked || app.blockMode.isBlocked) 1.2.dp else 1.dp,
            color = when {
                isChecked -> MaterialTheme.colorScheme.primary
                app.blockMode.isBlocked -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckboxToggle() },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }

            AppIconThumbnail(
                packageName = app.packageName,
                appName = app.appName,
                loadIcon = loadIcon,
                size = 46.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.scheduledCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberSchedule.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "مجدول",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberSchedule,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.isRunningInBackground) {
                        val ramText = if (app.estimatedRamMb > 0.1) {
                            String.format(Locale.US, "نشط • %.1f MB", app.estimatedRamMb)
                        } else {
                            "نشط بالخلفية"
                        }
                        Text(
                            text = ramText,
                            style = MaterialTheme.typography.labelSmall,
                            color = VioletMemory,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (app.hasInternetPermission) {
                        Text(
                            text = "شبكة",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanSecondary
                        )
                    }
                    if (app.isSystemApp) {
                        Text(
                            text = "نظام",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            BlockModeBadge(
                blockMode = app.blockMode,
                isActive = app.isRuleActive
            )
        }
    }
}
