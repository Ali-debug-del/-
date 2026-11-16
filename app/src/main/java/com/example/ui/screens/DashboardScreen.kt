package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AppShieldUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.BlockModeBadge
import com.example.ui.components.StatMetricCard
import com.example.ui.components.formatBytesHuman
import com.example.ui.components.formatTimeHHmm
import com.example.ui.components.formatTimestampTime
import com.example.ui.theme.AmberSchedule
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.VioletMemory

@Composable
fun DashboardScreen(
    uiState: AppShieldUiState,
    loadIcon: suspend (String) -> ImageBitmap?,
    onToggleMasterShield: (Boolean) -> Unit,
    onEnforceMemorySweep: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToCharts: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val activeRules = uiState.rules.filter { it.isActive }
    val internetBlockedCount = activeRules.count { it.blockMode.blocksInternet }
    val memoryBlockedCount = activeRules.count { it.blockMode.blocksMemory }
    val isShieldProtecting = uiState.runtimeState.isShieldMasterEnabled &&
        (uiState.runtimeState.isVpnActive || memoryBlockedCount > 0)

    val animatedRamProgress by animateFloatAsState(
        targetValue = uiState.memorySnapshot.usagePercent,
        label = "ram_progress"
    )

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
            // Hero Banner Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = ObsidianBg,
                    border = BorderStroke(
                        1.5.dp,
                        if (isShieldProtecting) EmeraldPrimary.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shield_hero_1794789621034),
                            contentDescription = stringResource(R.string.hero_banner_desc),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.48f
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ObsidianBg.copy(alpha = 0.45f),
                                            ObsidianBg.copy(alpha = 0.94f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isShieldProtecting) {
                                        EmeraldPrimary.copy(alpha = 0.2f)
                                    } else {
                                        AmberSchedule.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isShieldProtecting) EmeraldPrimary else AmberSchedule
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isShieldProtecting) EmeraldPrimary else AmberSchedule
                                                )
                                        )
                                        Text(
                                            text = when {
                                                isShieldProtecting -> stringResource(R.string.shield_active)
                                                activeRules.isEmpty() -> "جاهز للحماية • لا توجد تطبيقات محجوبة"
                                                else -> stringResource(R.string.shield_inactive)
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = stringResource(R.string.app_name),
                                    tint = if (isShieldProtecting) EmeraldPrimary else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "تحكم كامل بحجب التطبيقات عن استهلاك الذاكرة والإنترنت فوراً أو حسب جدول زمني.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (activeRules.isEmpty()) {
                                            onNavigateToApps()
                                        } else {
                                            onToggleMasterShield(!uiState.runtimeState.isShieldMasterEnabled)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isShieldProtecting) CoralDanger else EmeraldPrimary,
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("master_shield_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            activeRules.isEmpty() -> "حدد تطبيقات للحجب"
                                            isShieldProtecting -> stringResource(R.string.shield_deactivate)
                                            else -> stringResource(R.string.shield_activate)
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Two-column Telemetry Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        title = "حجب الإنترنت",
                        value = "$internetBlockedCount",
                        subtitle = "صد ${uiState.runtimeState.blockedPacketsSession} حزمة (${formatBytesHuman(uiState.runtimeState.blockedBytesSession)})",
                        icon = Icons.Rounded.WifiOff,
                        accentColor = CyanSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "حجب الذاكرة",
                        value = "$memoryBlockedCount",
                        subtitle = if (uiState.memorySnapshot.lastFreedMb > 0) {
                            "آخر تحرير: ${uiState.memorySnapshot.lastFreedMb} MB"
                        } else {
                            "${uiState.runtimeState.totalMemorySweeps} عملية تنظيف"
                        },
                        icon = Icons.Rounded.Memory,
                        accentColor = VioletMemory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Real-time RAM Monitor Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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
                            Column {
                                Text(
                                    text = "مراقب ذاكرة النظام (RAM)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "المتاح الآن: ${uiState.memorySnapshot.availableRamMb} MB من ${uiState.memorySnapshot.totalRamMb} MB",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(uiState.memorySnapshot.usagePercent * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (uiState.memorySnapshot.usagePercent > 0.85f) CoralDanger else EmeraldPrimary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedRamProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = if (uiState.memorySnapshot.usagePercent > 0.85f) CoralDanger else VioletMemory,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onEnforceMemorySweep,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VioletMemory,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("memory_sweep_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحرير الذاكرة فوراً", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = onNavigateToCharts,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("open_memory_charts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Analytics,
                                    contentDescription = "الرسوم البيانية",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الرسوم البيانية")
                            }
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToApps,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_pick_apps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديد التطبيقات", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onNavigateToSchedules,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_schedules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الجدولة (${uiState.schedules.size})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Active Blocked Apps Preview
            if (uiState.rules.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التطبيقات المقيدة حالياً (${uiState.rules.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "تعديل القائمة",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onNavigateToApps)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                items(uiState.rules.take(4), key = { it.packageName }) { rule ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToApps)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIconThumbnail(
                                packageName = rule.packageName,
                                appName = rule.appName,
                                loadIcon = loadIcon,
                                size = 40.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "حزم مصدودة: ${rule.blockedPacketsCount} • تنظيف ذاكرة: ${rule.memoryKillsCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BlockModeBadge(blockMode = rule.blockMode, isActive = rule.isActive)
                        }
                    }
                }
            }

            // Upcoming Schedules Preview
            if (uiState.schedules.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الجداول الزمنية النشطة",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "عرض الكل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onNavigateToSchedules)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                items(uiState.schedules.take(2), key = { it.id }) { schedule ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AmberSchedule.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToSchedules)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = AmberSchedule,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schedule.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${schedule.appNamesPreview} • يبدأ ${formatTimeHHmm(schedule.startHour, schedule.startMinute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BlockModeBadge(blockMode = schedule.blockMode, isActive = schedule.isEnabled)
                        }
                    }
                }
            }

            // Recent Activity Feed
            if (uiState.logs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آخر الأحداث",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "السجل الكامل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onNavigateToLogs)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                items(uiState.logs.take(3), key = { it.id }) { log ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.appName,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatTimestampTime(log.timestamp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
