package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockMode
import com.example.data.model.InstalledAppInfo
import com.example.ui.AppShieldUiState
import com.example.ui.components.AppMemoryBarRow
import com.example.ui.components.BlockDistributionDonutChart
import com.example.ui.components.DonutSlice
import com.example.ui.components.MemoryTimelineAreaChart
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VioletMemory
import java.util.Locale

@Composable
fun MemoryChartsScreen(
    uiState: AppShieldUiState,
    loadIcon: suspend (String) -> ImageBitmap?,
    onCaptureSampleNow: () -> Unit,
    onEnforceMemorySweep: () -> Unit,
    onConfigureApp: (InstalledAppInfo) -> Unit,
    onNavigateToApps: () -> Unit
) {
    var showSystemRamSeries by remember { mutableStateOf(false) }
    var includeRunningUnblocked by remember { mutableStateOf(false) }

    val blockedApps = remember(uiState.apps) {
        uiState.apps.filter { it.blockMode.isBlocked }
            .sortedByDescending { it.estimatedRamMb }
    }
    val ruleByPkg = remember(uiState.rules) {
        uiState.rules.associateBy { it.packageName }
    }
    val blockedRamTotalMb = remember(blockedApps) {
        blockedApps.sumOf { it.estimatedRamMb }
    }

    val displayedBarApps = remember(blockedApps, uiState.apps, includeRunningUnblocked) {
        if (includeRunningUnblocked || blockedApps.isEmpty()) {
            (blockedApps + uiState.apps.filter { !it.blockMode.isBlocked && it.isRunningInBackground })
                .distinctBy { it.packageName }
                .sortedByDescending { it.estimatedRamMb }
                .take(12)
        } else {
            blockedApps
        }
    }
    val maxBarRam = remember(displayedBarApps) {
        (displayedBarApps.maxOfOrNull { it.estimatedRamMb } ?: 10.0).coerceAtLeast(1.0)
    }

    val memOnlyCount = blockedApps.count { it.blockMode == BlockMode.MEMORY_ONLY }
    val bothCount = blockedApps.count { it.blockMode == BlockMode.BOTH }
    val netOnlyCount = blockedApps.count { it.blockMode == BlockMode.INTERNET_ONLY }
    val stoppedBlockedCount = blockedApps.count { !it.isRunningInBackground }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "تحليلات الذاكرة وأثر الحجب",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "راقب استهلاك ذاكرة التطبيقات المحجوبة والمساحة المحررة بمرور الوقت",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // KPI Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        title = "إجمالي المحرر",
                        value = "${uiState.totalFreedMb} MB",
                        subtitle = "عبر ${uiState.runtimeState.totalMemorySweeps} جولة تنظيف",
                        icon = Icons.Rounded.TrendingDown,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "ذاكرة المحجوبة",
                        value = String.format(Locale.US, "%.1f MB", blockedRamTotalMb),
                        subtitle = "$stoppedBlockedCount/${blockedApps.size} متوقفة بالخلفية",
                        icon = Icons.Rounded.Memory,
                        accentColor = VioletMemory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCaptureSampleNow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("refresh_telemetry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث القياس")
                    }
                    Button(
                        onClick = onEnforceMemorySweep,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletMemory,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("charts_memory_sweep_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنظيف وقياس الأثر")
                    }
                }
            }

            // Chart 1: Timeline Area/Line Chart
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Analytics,
                                    contentDescription = null,
                                    tint = if (showSystemRamSeries) CyanSecondary else VioletMemory
                                )
                                Text(
                                    text = "منحنى استهلاك الذاكرة",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "${uiState.telemetryPoints.size} نقطة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showSystemRamSeries,
                                onClick = { showSystemRamSeries = false },
                                label = { Text("التطبيقات المحجوبة") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletMemory.copy(alpha = 0.18f),
                                    selectedLabelColor = VioletMemory
                                ),
                                modifier = Modifier.testTag("series_blocked_apps_chip")
                            )
                            FilterChip(
                                selected = showSystemRamSeries,
                                onClick = { showSystemRamSeries = true },
                                label = { Text("ذاكرة النظام الكلية") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanSecondary.copy(alpha = 0.18f),
                                    selectedLabelColor = CyanSecondary
                                ),
                                modifier = Modifier.testTag("series_system_ram_chip")
                            )
                        }

                        MemoryTimelineAreaChart(
                            points = uiState.telemetryPoints,
                            showSystemRam = showSystemRamSeries
                        )
                    }
                }
            }

            // Chart 2: Block Distribution Donut
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "توزيع التطبيقات المحجوبة",
                            style = MaterialTheme.typography.titleMedium
                        )
                        BlockDistributionDonutChart(
                            slices = listOf(
                                DonutSlice("حجب الذاكرة فقط", memOnlyCount, VioletMemory),
                                DonutSlice("ذاكرة + إنترنت", bothCount, CoralDanger),
                                DonutSlice("حجب الإنترنت فقط", netOnlyCount, CyanSecondary)
                            ),
                            centerValue = "${blockedApps.size}",
                            centerSubtitle = "تطبيق محجوب"
                        )
                    }
                }
            }

            // Chart 3: Per-App RAM Consumption Bar Chart Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (blockedApps.isEmpty() || includeRunningUnblocked) {
                            "استهلاك الذاكرة حسب التطبيق"
                        } else {
                            "استهلاك الذاكرة للتطبيقات المحجوبة (${blockedApps.size})"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (blockedApps.isNotEmpty()) {
                        FilterChip(
                            selected = includeRunningUnblocked,
                            onClick = { includeRunningUnblocked = !includeRunningUnblocked },
                            label = { Text("مقارنة بالنشطة") },
                            modifier = Modifier.testTag("compare_running_apps_chip")
                        )
                    }
                }
            }

            if (blockedApps.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "لم تحجب أي تطبيقات بعد",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "نعرض أدناه التطبيقات النشطة في الذاكرة للمقارنة، ويمكنك تحديد تطبيقات لحجبها ومراقبة انخفاض استهلاكها.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onNavigateToApps,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحديد تطبيقات للحجب")
                            }
                        }
                    }
                }
            }

            items(displayedBarApps, key = { it.packageName }) { app ->
                val kills = ruleByPkg[app.packageName]?.memoryKillsCount ?: 0
                AppMemoryBarRow(
                    app = app,
                    maxRamMb = maxBarRam,
                    killsCount = kills,
                    loadIcon = loadIcon,
                    onClick = { onConfigureApp(app) }
                )
            }
        }
    }
}
