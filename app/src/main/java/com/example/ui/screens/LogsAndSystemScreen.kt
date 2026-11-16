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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockLogEntity
import com.example.ui.AppShieldUiState
import com.example.ui.components.formatTimestampTime
import com.example.ui.theme.AmberSchedule
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VioletMemory

@Composable
fun LogsAndSystemScreen(
    uiState: AppShieldUiState,
    hasNotificationPermission: Boolean,
    onRequestVpnConsent: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onClearAllRules: () -> Unit,
    onClearLogs: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "صلاحيات النظام والسجل",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // Permissions Section
            item {
                PermissionStatusCard(
                    title = "إذن جدار الحماية المحلي (VPN)",
                    subtitle = "مطلوب لقطع الإنترنت عن التطبيقات المحجوبة محلياً دون روت",
                    isGranted = uiState.hasVpnConsent,
                    icon = Icons.Rounded.VpnKey,
                    actionLabel = if (uiState.hasVpnConsent) "مفعّل" else "منح الإذن",
                    testTag = "grant_vpn_button",
                    onClick = onRequestVpnConsent
                )
            }

            item {
                PermissionStatusCard(
                    title = "إذن الإشعارات",
                    subtitle = "لعرض تنبيهات تفعيل وانتهاء الجدولة وحالة الدرع",
                    isGranted = hasNotificationPermission,
                    icon = Icons.Rounded.NotificationsActive,
                    actionLabel = if (hasNotificationPermission) "ممنوح" else "تفعيل",
                    testTag = "grant_notifications_button",
                    onClick = onRequestNotificationPermission
                )
            }

            item {
                PermissionStatusCard(
                    title = "إذن إحصائيات الاستخدام (اختياري)",
                    subtitle = "يساعد على استثناء التطبيق المفتوح حالياً عند تنظيف الذاكرة",
                    isGranted = uiState.hasUsageStatsPermission,
                    icon = Icons.Rounded.QueryStats,
                    actionLabel = if (uiState.hasUsageStatsPermission) "مفعّل" else "فتح الإعدادات",
                    testTag = "grant_usage_stats_button",
                    onClick = onOpenUsageAccessSettings
                )
            }

            if (uiState.rules.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = onClearAllRules,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger),
                        border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إلغاء الحجب عن جميع التطبيقات (${uiState.rules.size})")
                    }
                }
            }

            // Activity Logs Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "سجل الحجب والذاكرة (${uiState.logs.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (uiState.logs.isNotEmpty()) {
                        TextButton(onClick = onClearLogs) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح السجل")
                        }
                    }
                }
            }

            if (uiState.logs.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "السجل فارغ حالياً",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "ستظهر هنا عمليات صد الإنترنت وتحرير الذاكرة وتنفيذ الجداول.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.logs, key = { it.id }) { log ->
                    LogItemCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: ImageVector,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    val accent = if (isGranted) EmeraldPrimary else AmberSchedule
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Icon(
                        imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        contentDescription = if (isGranted) "ممنوح" else "مطلوب",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isGranted) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag(testTag)
                ) {
                    Text(actionLabel)
                }
            } else {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LogItemCard(log: BlockLogEntity) {
    val (icon, tint) = when (log.actionType) {
        BlockLogEntity.ACTION_INTERNET_BLOCKED -> Icons.Rounded.WifiOff to CyanSecondary
        BlockLogEntity.ACTION_MEMORY_FREED -> Icons.Rounded.Memory to VioletMemory
        BlockLogEntity.ACTION_SCHEDULE_CREATED,
        BlockLogEntity.ACTION_SCHEDULE_STARTED,
        BlockLogEntity.ACTION_SCHEDULE_ENDED -> Icons.Rounded.Schedule to AmberSchedule
        BlockLogEntity.ACTION_UNBLOCKED -> Icons.Rounded.CheckCircle to EmeraldPrimary
        else -> Icons.Rounded.Bolt to EmeraldPrimary
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = log.actionType,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.appName,
                    style = MaterialTheme.typography.titleSmall,
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
