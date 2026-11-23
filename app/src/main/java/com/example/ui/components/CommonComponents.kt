package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.formatPkr
import com.example.services.MonthlyTrendPoint
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import kotlin.math.max

@DrawableRes
fun resolveDishDrawable(imageKey: String): Int = when (imageKey.lowercase()) {
    "karahi" -> R.drawable.img_karahi_special_1795434279193
    "platter" -> R.drawable.img_bbq_platter_1795434277784
    "kebabs", "bbq" -> R.drawable.img_smoked_kebabs_1795434278896
    else -> R.drawable.img_hero_banner_1795434279848
}

@Composable
fun SmokedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = EmberBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OfflineStatusBanner(isOnline: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = TandoorCopper.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, TandoorCopper)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Offline Mode active",
                    tint = SmokedAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Offline Mode active — cached data available, changes will sync automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    SmokedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KpiMetricCard(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    SmokedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MonoMetricStyle,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MonthlyTrendChartCard(
    trends: List<MonthlyTrendPoint>,
    modifier: Modifier = Modifier
) {
    SmokedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Sales vs. Expenses Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot("Sales", SmokedAmber)
                LegendDot("Expenses", CrimsonEmber)
                LegendDot("Net Profit", CorianderGreen)
            }

            if (trends.isEmpty()) {
                Text(
                    text = "Complete COD orders or log expenses to visualize monthly trends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = max(
                    1L,
                    trends.maxOf { max(it.salesPkr, max(it.expensesPkr, it.profitPkr.coerceAtLeast(0L))) }
                ).toFloat()

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    val slotWidth = size.width / trends.size.coerceAtLeast(1)
                    val chartBottom = size.height - 16f
                    val chartTop = 16f
                    val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

                    // Subtle horizontal grid lines
                    for (i in 0..3) {
                        val y = chartTop + (chartHeight * i / 3f)
                        drawLine(
                            color = Color(0xFF3A2F28),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    val profitPath = Path()
                    trends.forEachIndexed { idx, point ->
                        val centerX = idx * slotWidth + slotWidth / 2f
                        val barW = (slotWidth * 0.24f).coerceIn(10f, 32f)

                        val salesH = (point.salesPkr.coerceAtLeast(0L) / maxVal) * chartHeight
                        drawRoundRect(
                            color = SmokedAmber,
                            topLeft = Offset(centerX - barW - 3f, chartBottom - salesH),
                            size = Size(barW, salesH.coerceAtLeast(3f)),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        val expH = (point.expensesPkr.coerceAtLeast(0L) / maxVal) * chartHeight
                        drawRoundRect(
                            color = CrimsonEmber,
                            topLeft = Offset(centerX + 3f, chartBottom - expH),
                            size = Size(barW, expH.coerceAtLeast(3f)),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        val profitY = chartBottom - ((point.profitPkr.coerceAtLeast(0L) / maxVal) * chartHeight)
                        if (idx == 0) profitPath.moveTo(centerX, profitY) else profitPath.lineTo(centerX, profitY)
                        drawCircle(
                            color = CorianderGreen,
                            radius = 5.5f,
                            center = Offset(centerX, profitY)
                        )
                    }

                    if (trends.size > 1) {
                        drawPath(
                            path = profitPath,
                            color = CorianderGreen,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    trends.forEach { point ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatPkr(point.profitPkr),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (point.profitPkr >= 0) CorianderGreen else CrimsonEmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
