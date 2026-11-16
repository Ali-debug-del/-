package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockScheduleEntity
import com.example.data.model.BlockMode
import com.example.data.model.InstalledAppInfo
import com.example.ui.theme.AmberSchedule
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VioletMemory
import java.util.Calendar

enum class BlockTimingMode {
    IMMEDIATE,
    SCHEDULED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockConfigBottomSheet(
    targetApps: List<InstalledAppInfo>,
    initialTimingMode: BlockTimingMode = BlockTimingMode.IMMEDIATE,
    loadIcon: suspend (String) -> ImageBitmap?,
    onDismiss: () -> Unit,
    onApplyImmediate: (BlockMode) -> Unit,
    onCreateSchedule: (
        title: String,
        blockMode: BlockMode,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        hasEndTime: Boolean,
        repeatDaily: Boolean,
        daysMask: Int
    ) -> Unit,
    onOpenSystemSettings: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialMode = remember(targetApps) {
        val existing = targetApps.firstOrNull()?.blockMode
        if (targetApps.size == 1 && existing != null && existing != BlockMode.NONE) {
            existing
        } else {
            BlockMode.BOTH
        }
    }

    var selectedBlockMode by remember { mutableStateOf(initialMode) }
    var timingMode by remember { mutableStateOf(initialTimingMode) }

    val defaultCal = remember {
        Calendar.getInstance().apply { add(Calendar.MINUTE, 15) }
    }
    var scheduleTitle by remember { mutableStateOf("") }
    var startHour by remember { mutableIntStateOf(defaultCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(defaultCal.get(Calendar.MINUTE)) }
    var endHour by remember { mutableIntStateOf((defaultCal.get(Calendar.HOUR_OF_DAY) + 1) % 24) }
    var endMinute by remember { mutableIntStateOf(defaultCal.get(Calendar.MINUTE)) }
    var hasEndTime by remember { mutableStateOf(true) }
    var repeatDaily by remember { mutableStateOf(false) }
    var daysMask by remember { mutableIntStateOf(BlockScheduleEntity.ALL_DAYS_MASK) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Apps Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (targetApps.size == 1) {
                    AppIconThumbnail(
                        packageName = targetApps[0].packageName,
                        appName = targetApps[0].appName,
                        loadIcon = loadIcon,
                        size = 48.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${targetApps.size}",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (targetApps.size == 1) {
                            targetApps[0].appName
                        } else {
                            "ضبط الحجب لـ ${targetApps.size} تطبيقات"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (targetApps.size == 1) {
                            targetApps[0].packageName
                        } else {
                            targetApps.take(3).joinToString("، ") { it.appName }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (targetApps.size == 1) {
                    OutlinedButton(
                        onClick = { onOpenSystemSettings(targetApps[0].packageName) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = "إعدادات النظام للتطبيق",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Section 1: Block Type Selection
            Text(
                text = "1. اختر نوع الحجب",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BlockTypeOptionCard(
                    title = "حجب الذاكرة فقط",
                    subtitle = "إيقاف عمليات الخلفية وتقليل استهلاك ذاكرة الوصول العشوائي (RAM)",
                    icon = Icons.Rounded.Memory,
                    accentColor = VioletMemory,
                    selected = selectedBlockMode == BlockMode.MEMORY_ONLY,
                    testTag = "mode_memory_only",
                    onClick = { selectedBlockMode = BlockMode.MEMORY_ONLY }
                )
                BlockTypeOptionCard(
                    title = "حجب الإنترنت فقط",
                    subtitle = "قطع اتصال الواي فاي وبيانات الهاتف عبر جدار الحماية المحلي (VPN)",
                    icon = Icons.Rounded.WifiOff,
                    accentColor = CyanSecondary,
                    selected = selectedBlockMode == BlockMode.INTERNET_ONLY,
                    testTag = "mode_internet_only",
                    onClick = { selectedBlockMode = BlockMode.INTERNET_ONLY }
                )
                BlockTypeOptionCard(
                    title = "حجب الذاكرة والإنترنت معاً",
                    subtitle = "عزل كامل يمنع الشبكة ويحرر الذاكرة في الخلفية في آنٍ واحد",
                    icon = Icons.Rounded.Block,
                    accentColor = CoralDanger,
                    selected = selectedBlockMode == BlockMode.BOTH,
                    testTag = "mode_both",
                    onClick = { selectedBlockMode = BlockMode.BOTH }
                )

                if (targetApps.any { it.blockMode.isBlocked }) {
                    BlockTypeOptionCard(
                        title = "إلغاء الحجب (السماح الكامل)",
                        subtitle = "رفع جميع القيود عن الذاكرة والإنترنت لهذه التطبيقات",
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = EmeraldPrimary,
                        selected = selectedBlockMode == BlockMode.NONE,
                        testTag = "mode_none",
                        onClick = {
                            selectedBlockMode = BlockMode.NONE
                            timingMode = BlockTimingMode.IMMEDIATE
                        }
                    )
                }
            }

            // Section 2: Timing Selection (Immediate vs Schedule)
            AnimatedVisibility(visible = selectedBlockMode != BlockMode.NONE) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "2. توقيت التنفيذ",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TimingModeCard(
                            title = "تطبيق فوري",
                            subtitle = "تفعيل الحجب الآن",
                            icon = Icons.Rounded.Bolt,
                            accent = EmeraldPrimary,
                            selected = timingMode == BlockTimingMode.IMMEDIATE,
                            testTag = "timing_immediate_option",
                            modifier = Modifier.weight(1f),
                            onClick = { timingMode = BlockTimingMode.IMMEDIATE }
                        )
                        TimingModeCard(
                            title = "جدولة زمنية",
                            subtitle = "تحديد وقت بدء/انتهاء",
                            icon = Icons.Rounded.Schedule,
                            accent = AmberSchedule,
                            selected = timingMode == BlockTimingMode.SCHEDULED,
                            testTag = "timing_scheduled_option",
                            modifier = Modifier.weight(1f),
                            onClick = { timingMode = BlockTimingMode.SCHEDULED }
                        )
                    }

                    AnimatedVisibility(visible = timingMode == BlockTimingMode.SCHEDULED) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = scheduleTitle,
                                    onValueChange = { scheduleTitle = it },
                                    label = { Text("اسم الجدولة (اختياري)") },
                                    placeholder = { Text("مثال: ساعات العمل، وقت النوم...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("schedule_title_input")
                                )

                                // Quick Presets
                                Text(
                                    text = "قوالب سريعة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SchedulePresetChip("بعد 15 دقيقة") {
                                        val c = Calendar.getInstance().apply { add(Calendar.MINUTE, 15) }
                                        startHour = c.get(Calendar.HOUR_OF_DAY)
                                        startMinute = c.get(Calendar.MINUTE)
                                        val e = Calendar.getInstance().apply { add(Calendar.MINUTE, 75) }
                                        endHour = e.get(Calendar.HOUR_OF_DAY)
                                        endMinute = e.get(Calendar.MINUTE)
                                        hasEndTime = true
                                        repeatDaily = false
                                    }
                                    SchedulePresetChip("بعد ساعة") {
                                        val c = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                                        startHour = c.get(Calendar.HOUR_OF_DAY)
                                        startMinute = c.get(Calendar.MINUTE)
                                        endHour = (startHour + 2) % 24
                                        endMinute = startMinute
                                        hasEndTime = true
                                        repeatDaily = false
                                    }
                                    SchedulePresetChip("وقت النوم (23:00 - 07:00)") {
                                        startHour = 23; startMinute = 0
                                        endHour = 7; endMinute = 0
                                        hasEndTime = true
                                        repeatDaily = true
                                        if (scheduleTitle.isBlank()) scheduleTitle = "وقت النوم"
                                    }
                                    SchedulePresetChip("العمل (09:00 - 17:00)") {
                                        startHour = 9; startMinute = 0
                                        endHour = 17; endMinute = 0
                                        hasEndTime = true
                                        repeatDaily = true
                                        if (scheduleTitle.isBlank()) scheduleTitle = "ساعات العمل"
                                    }
                                }

                                // Time Pickers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TimePickerTile(
                                        label = "وقت البدء",
                                        timeText = formatTimeHHmm(startHour, startMinute),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("start_time_picker_button"),
                                        onClick = { showStartTimePicker = true }
                                    )
                                    TimePickerTile(
                                        label = if (hasEndTime) "وقت الانتهاء" else "بدون انتهاء تلقائي",
                                        timeText = if (hasEndTime) formatTimeHHmm(endHour, endMinute) else "--:--",
                                        enabled = hasEndTime,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("end_time_picker_button"),
                                        onClick = {
                                            if (hasEndTime) showEndTimePicker = true
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "فك الحجب تلقائياً عند وقت الانتهاء",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = hasEndTime,
                                        onCheckedChange = { hasEndTime = it }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "تكرار يومي / أيام محددة",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = repeatDaily,
                                        onCheckedChange = { repeatDaily = it }
                                    )
                                }

                                AnimatedVisibility(visible = repeatDaily) {
                                    DaysOfWeekSelector(
                                        daysMask = daysMask,
                                        onDaysMaskChanged = { daysMask = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action CTA Button
            val primaryColor = when {
                selectedBlockMode == BlockMode.NONE -> EmeraldPrimary
                timingMode == BlockTimingMode.SCHEDULED -> AmberSchedule
                else -> MaterialTheme.colorScheme.primary
            }
            Button(
                onClick = {
                    if (selectedBlockMode == BlockMode.NONE || timingMode == BlockTimingMode.IMMEDIATE) {
                        onApplyImmediate(selectedBlockMode)
                    } else {
                        onCreateSchedule(
                            scheduleTitle,
                            selectedBlockMode,
                            startHour,
                            startMinute,
                            endHour,
                            endMinute,
                            hasEndTime,
                            repeatDaily,
                            daysMask
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_block_action_button")
            ) {
                Icon(
                    imageVector = when {
                        selectedBlockMode == BlockMode.NONE -> Icons.Rounded.CheckCircle
                        timingMode == BlockTimingMode.SCHEDULED -> Icons.Rounded.Schedule
                        else -> Icons.Rounded.Bolt
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        selectedBlockMode == BlockMode.NONE -> "إلغاء الحجب فوراً"
                        timingMode == BlockTimingMode.SCHEDULED -> "حفظ وجدولة الحجب"
                        else -> "تطبيق الحجب فوراً"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showStartTimePicker) {
        ComposeTimePickerDialog(
            initialHour = startHour,
            initialMinute = startMinute,
            title = "اختر وقت بدء الحجب",
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, m ->
                startHour = h
                startMinute = m
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        ComposeTimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            title = "اختر وقت انتهاء الحجب",
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, m ->
                endHour = h
                endMinute = m
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun BlockTypeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimingModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SchedulePresetChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun TimePickerTile(
    label: String,
    timeText: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DaysOfWeekSelector(
    daysMask: Int,
    onDaysMaskChanged: (Int) -> Unit
) {
    val days = listOf(
        Calendar.SATURDAY to "س",
        Calendar.SUNDAY to "ح",
        Calendar.MONDAY to "ن",
        Calendar.TUESDAY to "ث",
        Calendar.WEDNESDAY to "ر",
        Calendar.THURSDAY to "خ",
        Calendar.FRIDAY to "ج"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (calDay, shortLabel) ->
            val bit = 1 shl (calDay - 1)
            val isSelected = (daysMask and bit) != 0
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AmberSchedule else MaterialTheme.colorScheme.surface
                    )
                    .clickable {
                        val updated = if (isSelected) daysMask and bit.inv() else daysMask or bit
                        if (updated != 0) onDaysMaskChanged(updated)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
