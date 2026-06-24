package com.ost.application.util

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

enum class TooltipSide { TOP, BOTTOM, START, END }

@Stable
data class TooltipAction(
    val text: String,
    val onClick: () -> Unit
)

@Stable
data class TooltipData(
    val title: String,
    val subtitle: String? = null,
    @DrawableRes val dismissIconResource: Int? = null,
    val primaryAction: TooltipAction? = null,
    val secondaryAction: TooltipAction? = null
)

@Composable
fun rememberTooltipState(): TooltipState = remember { TooltipState() }

@Stable
class TooltipState internal constructor() {
    internal var tooltipWrapperWidth: Int by mutableIntStateOf(0)
    internal var tooltipWrapperHeight: Int by mutableIntStateOf(0)
    private var tooltipWrapperLayoutCoordinates: LayoutCoordinates? = null

    internal var data: TooltipData? by mutableStateOf(null)
    var isVisible: Boolean by mutableStateOf(false)
        private set

    internal var tooltipOffset: IntOffset by mutableStateOf(IntOffset.Zero)
    private var tooltipLayoutCoordinates: LayoutCoordinates? = null
    internal var lastTooltipSize: IntSize = IntSize.Zero

    internal var side: TooltipSide by mutableStateOf(TooltipSide.BOTTOM)
    internal var arrowLocalPos: Float by mutableFloatStateOf(0f)

    internal var anchorLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null)

    private var density: Density? = null
    internal val arrowWidthDp = 30f
    internal val arrowHeightDp = 14f
    internal val bubbleCornerRadiusDp = 22f

    fun hide() { isVisible = false }
    fun show() { isVisible = true }

    internal fun initialize(data: TooltipData, initialVisibility: Boolean) {
        this.data = data
        if (initialVisibility) show()
    }

    internal fun changeTooltipWrapperLayoutCoordinates(layoutCoordinates: LayoutCoordinates, density: Density) {
        tooltipWrapperLayoutCoordinates = layoutCoordinates
        tooltipWrapperWidth = layoutCoordinates.size.width
        tooltipWrapperHeight = layoutCoordinates.size.height
        this.density = density
        syncTooltipOffset()
    }

    internal fun changeAnchorLayoutCoordinates(layoutCoordinates: LayoutCoordinates) {
        anchorLayoutCoordinates = layoutCoordinates
        syncTooltipOffset()
    }

    internal fun changeTooltipLayoutCoordinates(layoutCoordinates: LayoutCoordinates) {
        tooltipLayoutCoordinates = layoutCoordinates
        lastTooltipSize = layoutCoordinates.size
        syncTooltipOffset()
    }

    private fun syncTooltipOffset() {
        val tooltipWrapperLC = tooltipWrapperLayoutCoordinates ?: return
        val anchorLC = anchorLayoutCoordinates ?: return
        val density = density ?: return

        if (!tooltipWrapperLC.isAttached || !anchorLC.isAttached) return

        val parentOffset = try {
            tooltipWrapperLC.localPositionOf(anchorLC, Offset.Zero)
        } catch (_: IllegalArgumentException) { return }

        val anchorLeft = parentOffset.x
        val anchorTop = parentOffset.y
        val anchorWidth = anchorLC.size.width
        val anchorHeight = anchorLC.size.height
        val anchorCenterX = anchorLeft + anchorWidth / 2f
        val anchorCenterY = anchorTop + anchorHeight / 2f
        val anchorRight = anchorLeft + anchorWidth
        val anchorBottom = anchorTop + anchorHeight

        val tooltipWidth = (tooltipLayoutCoordinates?.size?.width ?: lastTooltipSize.width).toFloat().coerceAtLeast(1f)
        val tooltipHeight = (tooltipLayoutCoordinates?.size?.height ?: lastTooltipSize.height).toFloat().coerceAtLeast(1f)

        with(density) {
            val spacing = 6.dp.toPx()
            val padding = 12.dp.toPx()
            val arrowHeight = arrowHeightDp.dp.toPx()
            val arrowWidth = arrowWidthDp.dp.toPx()
            val cornerRadius = bubbleCornerRadiusDp.dp.toPx()

            val currentAhV = if (side == TooltipSide.TOP || side == TooltipSide.BOTTOM) arrowHeight else 0f
            val currentAhH = if (side == TooltipSide.START || side == TooltipSide.END) arrowHeight else 0f

            val baseWidth = tooltipWidth - currentAhH
            val baseHeight = tooltipHeight - currentAhV

            val projVHeight = baseHeight + arrowHeight
            val projHWidth = baseWidth + arrowHeight

            val spaceBottom = tooltipWrapperHeight - anchorBottom
            val spaceEnd = tooltipWrapperWidth - anchorRight

            side = when {
                spaceBottom >= projVHeight + spacing -> TooltipSide.BOTTOM
                anchorTop >= projVHeight + spacing -> TooltipSide.TOP
                anchorLeft >= projHWidth + spacing -> TooltipSide.START
                spaceEnd >= projHWidth + spacing -> TooltipSide.END
                spaceBottom >= anchorTop && spaceBottom >= anchorLeft && spaceBottom >= spaceEnd -> TooltipSide.BOTTOM
                anchorTop >= spaceBottom && anchorTop >= anchorLeft && anchorTop >= spaceEnd -> TooltipSide.TOP
                anchorLeft >= spaceBottom && anchorLeft >= anchorTop && anchorLeft >= spaceEnd -> TooltipSide.START
                else -> TooltipSide.END
            }

            val safeMinArrowX = cornerRadius + arrowWidth / 2f
            val safeMaxArrowX = (tooltipWidth - cornerRadius - arrowWidth / 2f).coerceAtLeast(safeMinArrowX)

            val safeMinArrowY = cornerRadius + arrowWidth / 2f
            val safeMaxArrowY = (tooltipHeight - cornerRadius - arrowWidth / 2f).coerceAtLeast(safeMinArrowY)

            when (side) {
                TooltipSide.BOTTOM, TooltipSide.TOP -> {
                    val idealX = anchorCenterX - tooltipWidth / 2f
                    val maxX = (tooltipWrapperWidth - tooltipWidth - padding).coerceAtLeast(padding)
                    val clampedX = idealX.coerceIn(padding, maxX)

                    val rawArrowLocalPos = anchorCenterX - clampedX
                    arrowLocalPos = rawArrowLocalPos.coerceIn(safeMinArrowX, safeMaxArrowX)

                    tooltipOffset = IntOffset(
                        x = clampedX.toInt(),
                        y = if (side == TooltipSide.BOTTOM) (anchorBottom + spacing).toInt()
                        else (anchorTop - tooltipHeight - spacing).toInt()
                    )
                }
                TooltipSide.START, TooltipSide.END -> {
                    val idealY = anchorCenterY - tooltipHeight / 2f
                    val maxY = (tooltipWrapperHeight - tooltipHeight - padding).coerceAtLeast(padding)
                    val clampedY = idealY.coerceIn(padding, maxY)

                    val rawArrowLocalPos = anchorCenterY - clampedY
                    arrowLocalPos = rawArrowLocalPos.coerceIn(safeMinArrowY, safeMaxArrowY)

                    tooltipOffset = IntOffset(
                        x = if (side == TooltipSide.START) (anchorLeft - tooltipWidth - spacing).toInt()
                        else (anchorRight + spacing).toInt(),
                        y = clampedY.toInt()
                    )
                }
            }
        }
    }

    internal fun getTooltipBounds(): Rect? {
        val tLc = tooltipLayoutCoordinates ?: return null
        val wLc = tooltipWrapperLayoutCoordinates ?: return null
        if (!tLc.isAttached || !wLc.isAttached) return null
        return try { Rect(wLc.localPositionOf(tLc, Offset.Zero), tLc.size.toSize()) } catch (_: IllegalArgumentException) { null }
    }

    internal fun getAnchorBounds(): Rect? {
        val aLc = anchorLayoutCoordinates ?: return null
        val wLc = tooltipWrapperLayoutCoordinates ?: return null
        if (!aLc.isAttached || !wLc.isAttached) return null
        return try { Rect(wLc.localPositionOf(aLc, Offset.Zero), aLc.size.toSize()) } catch (_: IllegalArgumentException) { null }
    }
}

class SmartTooltipShape(
    private val cornerRadiusDp: Float,
    private val arrowWidthDp: Float,
    private val arrowHeightDp: Float,
    private val arrowLocalPos: Float,
    private val side: TooltipSide
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(Path().apply {
            val cr = with(density) { cornerRadiusDp.dp.toPx() }
            val aw = with(density) { arrowWidthDp.dp.toPx() }
            val ah = with(density) { arrowHeightDp.dp.toPx() }
            val W = size.width
            val H = size.height
            val pos = arrowLocalPos

            when (side) {
                TooltipSide.BOTTOM -> {
                    moveTo(cr, ah)
                    lineTo(pos - aw / 2f, ah)
                    cubicTo(pos - aw * 0.15f, ah, pos, 0f, pos, 0f)
                    cubicTo(pos, 0f, pos + aw * 0.15f, ah, pos + aw / 2f, ah)
                    lineTo(W - cr, ah)
                    arcTo(Rect(W - 2 * cr, ah, W, ah + 2 * cr), -90f, 90f, false)
                    lineTo(W, H - cr)
                    arcTo(Rect(W - 2 * cr, H - 2 * cr, W, H), 0f, 90f, false)
                    lineTo(cr, H)
                    arcTo(Rect(0f, H - 2 * cr, 2 * cr, H), 90f, 90f, false)
                    lineTo(0f, ah + cr)
                    arcTo(Rect(0f, ah, 2 * cr, ah + 2 * cr), 180f, 90f, false)
                }
                TooltipSide.TOP -> {
                    moveTo(cr, 0f)
                    lineTo(W - cr, 0f)
                    arcTo(Rect(W - 2 * cr, 0f, W, 2 * cr), -90f, 90f, false)
                    lineTo(W, H - ah - cr)
                    arcTo(Rect(W - 2 * cr, H - ah - 2 * cr, W, H - ah), 0f, 90f, false)
                    lineTo(pos + aw / 2f, H - ah)
                    cubicTo(pos + aw * 0.15f, H - ah, pos, H, pos, H)
                    cubicTo(pos, H, pos - aw * 0.15f, H - ah, pos - aw / 2f, H - ah)
                    lineTo(cr, H - ah)
                    arcTo(Rect(0f, H - ah - 2 * cr, 2 * cr, H - ah), 90f, 90f, false)
                    lineTo(0f, cr)
                    arcTo(Rect(0f, 0f, 2 * cr, 2 * cr), 180f, 90f, false)
                }
                TooltipSide.END -> {
                    moveTo(ah + cr, 0f)
                    lineTo(W - cr, 0f)
                    arcTo(Rect(W - 2 * cr, 0f, W, 2 * cr), -90f, 90f, false)
                    lineTo(W, H - cr)
                    arcTo(Rect(W - 2 * cr, H - 2 * cr, W, H), 0f, 90f, false)
                    lineTo(ah + cr, H)
                    arcTo(Rect(ah, H - 2 * cr, ah + 2 * cr, H), 90f, 90f, false)
                    lineTo(ah, pos + aw / 2f)
                    cubicTo(ah, pos + aw * 0.15f, 0f, pos, 0f, pos)
                    cubicTo(0f, pos, ah, pos - aw * 0.15f, ah, pos - aw / 2f)
                    lineTo(ah, cr)
                    arcTo(Rect(ah, 0f, ah + 2 * cr, 2 * cr), 180f, 90f, false)
                }
                TooltipSide.START -> {
                    moveTo(cr, 0f)
                    lineTo(W - ah - cr, 0f)
                    arcTo(Rect(W - ah - 2 * cr, 0f, W - ah, 2 * cr), -90f, 90f, false)
                    lineTo(W - ah, pos - aw / 2f)
                    cubicTo(W - ah, pos - aw * 0.15f, W, pos, W, pos)
                    cubicTo(W, pos, W - ah, pos + aw * 0.15f, W - ah, pos + aw / 2f)
                    lineTo(W - ah, H - cr)
                    arcTo(Rect(W - ah - 2 * cr, H - 2 * cr, W - ah, H), 0f, 90f, false)
                    lineTo(cr, H)
                    arcTo(Rect(0f, H - 2 * cr, 2 * cr, H), 90f, 90f, false)
                    lineTo(0f, cr)
                    arcTo(Rect(0f, 0f, 2 * cr, 2 * cr), 180f, 90f, false)
                }
            }
            close()
        })
    }
}

fun Modifier.tooltip(
    state: TooltipState,
    title: String,
    subtitle: String? = null,
    @DrawableRes dismissIconResource: Int? = null,
    primaryAction: TooltipAction? = null,
    secondaryAction: TooltipAction? = null,
    isAnchor: Boolean = true,
    initialVisibility: Boolean = false,
): Modifier = composed {
    var localCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(isAnchor, title, subtitle) {
        if (isAnchor) {
            state.initialize(
                data = TooltipData(title, subtitle, dismissIconResource, primaryAction, secondaryAction),
                initialVisibility = initialVisibility
            )
            localCoords?.let { state.changeAnchorLayoutCoordinates(it) }
        }
    }
    this.onGloballyPositioned { coords ->
        localCoords = coords
        if (isAnchor) {
            state.changeAnchorLayoutCoordinates(coords)
        }
    }
}

@Composable
fun TooltipWrapper(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(tooltipState: TooltipState) -> Unit,
) {
    val tooltipState = rememberTooltipState()
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { tooltipState.changeTooltipWrapperLayoutCoordinates(it, density) }
            .pointerInput(tooltipState.isVisible) {
                if (!tooltipState.isVisible) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.find { it.changedToDown() }
                        if (down != null) {
                            val pos = down.position
                            if (tooltipState.getTooltipBounds()?.contains(pos) == false &&
                                tooltipState.getAnchorBounds()?.contains(pos) == false) {
                                tooltipState.hide()
                            }
                        }
                    }
                }
            }
    ) {
        content(tooltipState)
        Tooltip(state = tooltipState)
    }
}

@Composable
internal fun Tooltip(state: TooltipState) {
    val data = state.data ?: return

    val visibilityProgress by animateFloatAsState(
        targetValue = if (state.isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f),
        label = "tooltip_visibility"
    )

    if (!state.isVisible && visibilityProgress == 0f) return

    val bgColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    val tooltipWidth = state.lastTooltipSize.width.toFloat().coerceAtLeast(1f)
    val tooltipHeight = state.lastTooltipSize.height.toFloat().coerceAtLeast(1f)

    val safePivotX = when (state.side) {
        TooltipSide.BOTTOM, TooltipSide.TOP -> (state.arrowLocalPos / tooltipWidth).coerceIn(0f, 1f)
        TooltipSide.START -> 1f
        TooltipSide.END -> 0f
    }
    val safePivotY = when (state.side) {
        TooltipSide.BOTTOM -> 0f
        TooltipSide.TOP -> 1f
        TooltipSide.START, TooltipSide.END -> (state.arrowLocalPos / tooltipHeight).coerceIn(0f, 1f)
    }

    val tooltipShape = remember(state.arrowLocalPos, state.side) {
        SmartTooltipShape(
            cornerRadiusDp = state.bubbleCornerRadiusDp,
            arrowWidthDp = state.arrowWidthDp,
            arrowHeightDp = state.arrowHeightDp,
            arrowLocalPos = state.arrowLocalPos,
            side = state.side
        )
    }

    val arrowSpace = state.arrowHeightDp.dp
    val basePadH = 16.dp
    val basePadV = 12.dp

    Column(
        modifier = Modifier
            .widthIn(min = 80.dp, max = 300.dp)
            .offset { state.tooltipOffset }
            .onGloballyPositioned { state.changeTooltipLayoutCoordinates(it) }
            .graphicsLayer {
                transformOrigin = TransformOrigin(pivotFractionX = safePivotX, pivotFractionY = safePivotY)
                scaleX = visibilityProgress
                scaleY = visibilityProgress
                alpha = visibilityProgress
            }
            .clip(tooltipShape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(
                top = if (state.side == TooltipSide.BOTTOM) arrowSpace + basePadV else basePadV,
                bottom = if (state.side == TooltipSide.TOP) arrowSpace + basePadV else basePadV,
                start = if (state.side == TooltipSide.END) arrowSpace + basePadH else basePadH,
                end = if (state.side == TooltipSide.START) arrowSpace + basePadH else basePadH
            )
    ) {
        Text(
            text = data.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.fillMaxWidth()
        )

        data.subtitle?.let {
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (data.primaryAction != null || data.secondaryAction != null) {
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (data.secondaryAction != null) {
                    TextButton(
                        onClick = {
                            data.secondaryAction.onClick()
                            state.hide()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                    ) {
                        Text(text = data.secondaryAction.text)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }

                if (data.primaryAction != null) {
                    Button(
                        onClick = {
                            data.primaryAction.onClick()
                            state.hide()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contentColor,
                            contentColor = bgColor
                        )
                    ) {
                        Text(text = data.primaryAction.text)
                    }
                }
            }
        }
    }
}