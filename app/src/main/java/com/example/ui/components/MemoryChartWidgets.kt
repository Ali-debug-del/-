package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MemoryTelemetryEntity
import com.example.data.model.BlockMode
import com.example.data.model.InstalledAppInfo
import com.example.ui.theme.AmberSchedule
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VioletMemory
import java.util.Locale
import kotlin.math.abs

@Composable
fun MemoryTimelineAreaChart(
    points: List<MemoryTelemetryEntity>,
    showSystemRam: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points.size, showSystemRam) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    val lineColor = if (showSystemRam) CyanSecondary else VioletMemory
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val surfaceBg = MaterialTheme.colorScheme.surface

    val values = remember(points, showSystemRam) {
        points.map { pt ->
            if (showSystemRam) pt.usedRamMb.toFloat() else pt.blockedAppsRamMb.toFloat()
        }
    }
    val maxVal = remember(values) {
        val peak = values.maxOrNull() ?: 100f
        if (peak <= 1f) 100f else peak * 1.15f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "بانتظار تسجيل قراءات الذاكرة...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val inspectedPoint = points.getOrNull(selectedIndex.coerceIn(0, points.lastIndex))
            if (inspectedPoint != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestampTime(inspectedPoint.timestamp),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showSystemRam) {
                                "المستخدم: ${inspectedPoint.usedRamMb} MB"
                            } else {
                                String.format(
                                    Locale.US,
                                    "استهلاك المحجوبة: %.1f MB",
                                    inspectedPoint.blockedAppsRamMb
                                )
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = lineColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (inspectedPoint.freedDeltaMb > 0) {
                            Text(
                                text = "+${inspectedPoint.freedDeltaMb} MB محررة",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .testTag("memory_timeline_canvas")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points.size) {
                            detectTapGestures { tapOffset ->
                                if (points.isNotEmpty() && size.width > 0) {
                                    val paddingPx = 16.dp.toPx()
                                    val chartW = (size.width - paddingPx * 2).coerceAtLeast(1f)
                                    val ratio = ((tapOffset.x - paddingPx) / chartW).coerceIn(0f, 1f)
                                    val idx = if (points.size == 1) 0 else {
                                        (ratio * (points.size - 1)).toInt()
                                            .coerceIn(0, points.lastIndex)
                                    }
                                    selectedIndex = idx
                                }
                            }
                        }
                ) {
                    val padH = 16.dp.toPx()
                    val padV = 14.dp.toPx()
                    val chartW = (size.width - padH * 2).coerceAtLeast(1f)
                    val chartH = (size.height - padV * 2).coerceAtLeast(1f)

                    // Horizontal Gridlines
                    for (i in 0..3) {
                        val y = padV + chartH * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(padH, y),
                            end = Offset(padH + chartW, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val coords = values.mapIndexed { index, v ->
                        val x = if (values.size == 1) {
                            padH + chartW / 2f
                        } else {
                            padH + (index.toFloat() / (values.size - 1)) * chartW
                        }
                        val normY = (v / maxVal).coerceIn(0f, 1f)
                        val y = padV + chartH * (1f - normY)
                        Offset(x, y)
                    }

                    if (coords.size >= 2) {
                        val areaPath = Path().apply {
                            moveTo(coords.first().x, padV + chartH)
                            coords.forEach { pt -> lineTo(pt.x, pt.y) }
                            lineTo(coords.last().x, padV + chartH)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.36f),
                                    lineColor.copy(alpha = 0.02f)
                                ),
                                startY = padV,
                                endY = padV + chartH
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                lineTo(coords[i].x, coords[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    coords.forEachIndexed { idx, pt ->
                        val entity = points[idx]
                        val isSweep = entity.freedDeltaMb > 0L ||
                            entity.eventType == MemoryTelemetryEntity.EVENT_SWEEP ||
                            entity.eventType == MemoryTelemetryEntity.EVENT_BLOCK_APPLIED
                        val isSelected = idx == selectedIndex

                        if (isSelected) {
                            drawLine(
                                color = lineColor.copy(alpha = 0.45f),
                                start = Offset(pt.x, padV),
                                end = Offset(pt.x, padV + chartH),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        val dotColor = if (isSweep) EmeraldPrimary else lineColor
                        val radius = when {
                            isSelected -> 6.5.dp.toPx()
                            isSweep -> 5.dp.toPx()
                            else -> 3.dp.toPx()
                        }
                        drawCircle(
                            color = dotColor,
                            radius = radius,
                            center = pt
                        )
                        if (isSelected || isSweep) {
                            drawCircle(
                                color = surfaceBg,
                                radius = radius * 0.45f,
                                center = pt
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimestampTime(points.first().timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "اضغط على المنحنى لفحص القراءة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestampTime(points.last().timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppMemoryBarRow(
    app: InstalledAppInfo,
    maxRamMb: Double,
    killsCount: Int,
    loadIcon: suspend (String) -> ImageBitmap?,
    onClick: () -> Unit
) {
    val barColor = when (app.blockMode) {
        BlockMode.MEMORY_ONLY -> VioletMemory
        BlockMode.BOTH -> CoralDanger
        BlockMode.INTERNET_ONLY -> CyanSecondary
        BlockMode.NONE -> EmeraldPrimary
    }
    val fractionTarget = if (maxRamMb > 0.5) {
        (app.estimatedRamMb / maxRamMb).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fractionTarget,
        animationSpec = tween(durationMillis = 450),
        label = "bar_fraction"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("chart_app_bar_${app.packageName}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIconThumbnail(
                    packageName = app.packageName,
                    appName = app.appName,
                    loadIcon = loadIcon,
                    size = 34.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (app.isRunningInBackground) {
                            "نشط بالخلفية • تنظيف الذاكرة: $killsCount"
                        } else {
                            "متوقف بالخلفية (0 MB) • تنظيف: $killsCount"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1f MB", app.estimatedRamMb),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = barColor
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                drawRoundRect(
                    color = trackColor,
                    size = Size(size.width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                )
                val fillW = (size.width * animatedFraction).coerceAtLeast(
                    if (app.estimatedRamMb > 0.05) 8.dp.toPx() else 0f
                )
                if (fillW > 0f) {
                    drawRoundRect(
                        color = barColor,
                        size = Size(fillW, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    )
                }
            }
        }
    }
}

data class DonutSlice(
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun BlockDistributionDonutChart(
    slices: List<DonutSlice>,
    centerValue: String,
    centerSubtitle: String,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.count }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .testTag("block_distribution_donut"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 18.dp.toPx()
                val inset = strokeWidth / 2f + 4.dp.toPx()
                val arcSize = Size(
                    width = (size.width - inset * 2).coerceAtLeast(1f),
                    height = (size.height - inset * 2).coerceAtLeast(1f)
                )
                val topLeft = Offset(inset, inset)

                if (total <= 0) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                } else {
                    var startAngle = -90f
                    slices.filter { it.count > 0 }.forEach { slice ->
                        val sweep = (slice.count.toFloat() / total.toFloat()) * 360f
                        val gap = if (slices.count { it.count > 0 } > 1) 4f else 0f
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle + gap / 2f,
                            sweepAngle = (sweep - gap).coerceAtLeast(2f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerValue,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = centerSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${slice.count}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = slice.color
                    )
                }
            }
        }
    }
}
