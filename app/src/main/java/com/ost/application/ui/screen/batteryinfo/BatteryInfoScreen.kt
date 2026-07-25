package com.ost.application.ui.screen.batteryinfo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.core.battery.BatteryDisplayMode
import com.ost.application.core.battery.BatteryHealth
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import kotlin.math.max
import java.util.Locale
private enum class GridCardKind { HEALTH, TEMPERATURE, VOLTAGE, CYCLE_COUNT }
private fun GridCardKind.narrowGridShape(bigRadius: Dp, smallRadius: Dp): RoundedCornerShape =
    when (this) {
        GridCardKind.HEALTH -> RoundedCornerShape(
            topStart = bigRadius, topEnd = smallRadius,
            bottomStart = smallRadius, bottomEnd = smallRadius
        )
        GridCardKind.TEMPERATURE -> RoundedCornerShape(
            topStart = smallRadius, topEnd = bigRadius,
            bottomStart = smallRadius, bottomEnd = smallRadius
        )
        GridCardKind.VOLTAGE -> RoundedCornerShape(
            topStart = smallRadius, topEnd = smallRadius,
            bottomStart = bigRadius, bottomEnd = smallRadius
        )
        GridCardKind.CYCLE_COUNT -> RoundedCornerShape(
            topStart = smallRadius, topEnd = smallRadius,
            bottomStart = smallRadius, bottomEnd = bigRadius
        )
    }
private enum class GridLayoutTier { NARROW, WIDE_STACK, WIDE_ROW }
private fun BatteryHealth.toHeartIconResId(): Int = when (this) {
    BatteryHealth.GOOD, BatteryHealth.COLD -> R.drawable.ic_favorite_fill_24dp
    BatteryHealth.OVERHEAT, BatteryHealth.DEAD,
    BatteryHealth.OVER_VOLTAGE, BatteryHealth.UNSPECIFIED_FAILURE -> R.drawable.ic_heart_broken_24dp
    BatteryHealth.UNKNOWN -> R.drawable.ic_favorite_outline_24dp
}
@Composable
private fun BatteryHealth.toHeartColor(): Color = when (this) {
    BatteryHealth.GOOD -> MaterialTheme.colorScheme.primary
    BatteryHealth.COLD -> MaterialTheme.colorScheme.tertiary
    BatteryHealth.OVERHEAT, BatteryHealth.DEAD,
    BatteryHealth.OVER_VOLTAGE, BatteryHealth.UNSPECIFIED_FAILURE -> MaterialTheme.colorScheme.error
    BatteryHealth.UNKNOWN -> MaterialTheme.colorScheme.outline
}
@Composable
private fun HealthCardContent(
    healthStatus: BatteryHealth,
    healthText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heart_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse_scale"
    )
    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = healthText,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = healthStatus.toHeartColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
        Icon(
            painter = painterResource(healthStatus.toHeartIconResId()),
            contentDescription = null,
            tint = healthStatus.toHeartColor(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.72f)
                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
        )
        Text(
            text = stringResource(R.string.health),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}
@Composable
private fun SparklineGraph(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gradientTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val gradientBottom = MaterialTheme.colorScheme.primary.copy(alpha = 0f)
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minValue = values.min()
        val maxValue = values.max()
        val hasRange = maxValue > minValue
        val range = if (hasRange) maxValue - minValue else 1f
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val y = if (hasRange) {
                val normalized = (value - minValue) / range
                size.height - (normalized * size.height)
            } else {
                size.height / 2f
            }
            Offset(x, y)
        }
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientTop, gradientBottom),
                startY = 0f,
                endY = size.height
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
@Composable
private fun GraphCardContent(
    titleText: String,
    minText: String,
    maxText: String,
    history: List<Float>,
    isNarrowLayout: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            if (isNarrowLayout) {
                Text(
                    text = "$minText | $maxText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Column {
                    Text(
                        text = minText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = maxText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (history.size >= 2) {
                Spacer(modifier = Modifier.width(8.dp))
                SparklineGraph(
                    values = history,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(0.85f)
                )
            }
        }
    }
}
@Composable
private fun CycleCountCardContent(
    cycleCountText: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = cycleCountText,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 57.sp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
        )
        Text(
            text = stringResource(R.string.cycle_count),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily(Font(R.font.google_sans_flex)),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}
@Composable
private fun BatteryLevelIndicator(
    levelPercent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = levelPercent.coerceIn(0, 100) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "battery_level_progress"
    )
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "wave_amplitude"
    )
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            amplitude = { animatedAmplitude },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "$levelPercent%",
            fontFamily = FontFamily(Font(R.font.google_sans_flex)),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun BatteryInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: BatteryInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current
    val bigRadius = 24.dp
    val smallRadius = 4.dp
    val minCardSize = 150.dp
    val wideRowMinWidth = 850.dp
    val gridItems = listOf(
        GridCardKind.HEALTH,
        GridCardKind.TEMPERATURE,
        GridCardKind.VOLTAGE,
        GridCardKind.CYCLE_COUNT
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth - 32.dp
        val columnsCount = max(2, (availableWidth / minCardSize).toInt())
        val isNarrow = columnsCount == 2
        val tier = when {
            isNarrow -> GridLayoutTier.NARROW
            maxWidth >= wideRowMinWidth -> GridLayoutTier.WIDE_ROW
            else -> GridLayoutTier.WIDE_STACK
        }
        val gridColumns = when (tier) {
            GridLayoutTier.NARROW -> columnsCount
            GridLayoutTier.WIDE_STACK -> 6
            GridLayoutTier.WIDE_ROW -> 12
        }
        val cardSpacing = 4.dp
        val unitWidth = (availableWidth - cardSpacing * (gridColumns - 1)) / gridColumns
        val isCharging = uiState.displayMode == BatteryDisplayMode.CHARGING
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 16.dp, bottom = 16.dp + bottomSpacing
            ),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            verticalArrangement = Arrangement.spacedBy(cardSpacing)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BatteryLevelIndicator(
                        levelPercent = uiState.levelPercent,
                        isCharging = isCharging
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isCharging) stringResource(R.string.charging) else stringResource(R.string.discharging),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    if (isCharging) {
                        Text(
                            text = uiState.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(
                items = gridItems,
                span = { kind ->
                    when (tier) {
                        GridLayoutTier.NARROW -> GridItemSpan(1)
                        GridLayoutTier.WIDE_STACK, GridLayoutTier.WIDE_ROW -> when (kind) {
                            GridCardKind.HEALTH, GridCardKind.CYCLE_COUNT -> GridItemSpan(2)
                            GridCardKind.TEMPERATURE, GridCardKind.VOLTAGE -> GridItemSpan(4)
                        }
                    }
                }
            ) { kind ->
                val shape = if (tier == GridLayoutTier.NARROW) {
                    kind.narrowGridShape(bigRadius, smallRadius)
                } else {
                    RoundedCornerShape(smallRadius)
                }
                val cardModifier = if (tier == GridLayoutTier.NARROW) {
                    Modifier.fillMaxWidth().aspectRatio(1f)
                } else {
                    val cardHeight = unitWidth * 2 + cardSpacing
                    Modifier.fillMaxWidth().height(cardHeight)
                }
                Card(shape = shape, modifier = cardModifier) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (kind) {
                            GridCardKind.HEALTH -> HealthCardContent(
                                healthStatus = uiState.healthStatus,
                                healthText = uiState.health
                            )
                            GridCardKind.TEMPERATURE -> {
                                val tempValues = uiState.temperatureHistory.map { it.temperatureCelsius }
                                GraphCardContent(
                                    titleText = uiState.temperatureHistory.lastOrNull()
                                        ?.temperatureCelsius
                                        ?.let { "${it.toInt()}°" }
                                        ?: "...",
                                    minText = uiState.temperatureMin?.let { stringResource(R.string.min_value, it.toInt()) }
                                        ?: "...",
                                    maxText = uiState.temperatureMax?.let { stringResource(R.string.max_value, it.toInt()) }
                                        ?: "...",
                                    history = tempValues,
                                    isNarrowLayout = tier == GridLayoutTier.NARROW
                                )
                            }
                            GridCardKind.VOLTAGE -> {
                                val voltValues = uiState.voltageHistory.map { it.voltageVolts }
                                GraphCardContent(
                                    titleText = uiState.voltageHistory.lastOrNull()
                                        ?.voltageVolts
                                        ?.let { String.format(Locale.getDefault(), "%.2f", it) }
                                        ?: "...",
                                    minText = uiState.voltageMin?.let { stringResource(R.string.min_value_decimal, it) }
                                        ?: "...",
                                    maxText = uiState.voltageMax?.let { stringResource(R.string.max_value_decimal, it) }
                                        ?: "...",
                                    history = voltValues,
                                    isNarrowLayout = tier == GridLayoutTier.NARROW
                                )
                            }
                            GridCardKind.CYCLE_COUNT -> CycleCountCardContent(
                                cycleCountText = uiState.cycleCount
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val summaryText = if (uiState.isLoadingCapacity) stringResource(R.string.loading) else uiState.capacity
                CustomCardItem(
                    title = stringResource(R.string.capacity),
                    summary = summaryText,
                    position = CardPosition.SINGLE
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CustomCardItem(
                    title = stringResource(R.string.technology),
                    summary = uiState.technology,
                    position = CardPosition.SINGLE
                )
            }
        }
    }
}