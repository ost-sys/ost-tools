@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ost.application.minigames.activity.games

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.minigames.components.MorphPolygonShape
import com.ost.application.ui.theme.OSTToolsTheme

class MinesweeperGameActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinesweeperScreen(onNavigateBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MinesweeperScreen(
    viewModel: MinesweeperViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minesweeper") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back_24dp),
                            stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!isLandscape) {
                BottomActionBar(
                    currentMode = viewModel.interactionMode,
                    onModeSelected = { viewModel.interactionMode = it }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        BoardGrid(
                            grid = viewModel.grid,
                            onCellClicked = { x, y -> viewModel.onCellClicked(x, y) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(min = 200.dp, max = 280.dp)
                            .padding(end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LandscapeStats(
                            timeElapsed = viewModel.timeElapsed,
                            minesLeft = viewModel.minesLeft,
                            onRefresh = { viewModel.resetGame() }
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Box(modifier = Modifier.wrapContentWidth()) {
                            BottomActionBar(
                                currentMode = viewModel.interactionMode,
                                onModeSelected = { viewModel.interactionMode = it }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopHeader(
                        timeElapsed = viewModel.timeElapsed,
                        minesLeft = viewModel.minesLeft,
                        onRefresh = { viewModel.resetGame() }
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    BoardGrid(
                        grid = viewModel.grid,
                        onCellClicked = { x, y -> viewModel.onCellClicked(x, y) }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (viewModel.gameStatus == GameStatus.WON || viewModel.gameStatus == GameStatus.LOST) {
            GameOverDialog(
                status = viewModel.gameStatus,
                timeElapsed = viewModel.timeElapsed,
                minesLeft = viewModel.minesLeft,
                onRestart = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
fun LandscapeStats(timeElapsed: Int, minesLeft: Int, onRefresh: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .width(120.dp)
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = timeElapsed.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Stopwatch",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryFixedVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledIconButton(
            onClick = onRefresh,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh_24dp),
                contentDescription = "Refresh",
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .width(120.dp)
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onTertiary)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = minesLeft.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mines left",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TopHeader(timeElapsed: Int, minesLeft: Int, onRefresh: () -> Unit) {
    val cardHeight = 64.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 4.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = timeElapsed.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stopwatch",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryFixedVariant
            )
        }

        Box(
            modifier = Modifier
                .height(cardHeight)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            FilledIconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp),
                onClick = onRefresh,
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh_24dp),
                    contentDescription = "Refresh",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 24.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 24.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = minesLeft.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mines left",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BoardGrid(
    grid: List<List<Cell>>,
    onCellClicked: (Int, Int) -> Unit
) {
    if (grid.isEmpty()) return
    val columns = grid[0].size

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.widthIn(max = 400.dp)
    ) {
        items(grid.flatten().size) { index ->
            val x = index % columns
            val y = index / columns
            CellItem(
                cell = grid[y][x],
                onClick = { onCellClicked(x, y) }
            )
        }
    }
}

@Composable
fun CellItem(
    cell: Cell,
    onClick: () -> Unit
) {
    val squareShape = remember {
        RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(0.15f))
    }
    val cookie9Shape = remember {
        RoundedPolygon.star(9, innerRadius = 0.85f, rounding = CornerRounding(0.15f))
    }
    val circleShape = remember {
        RoundedPolygon(12, rounding = CornerRounding(1f))
    }

    val morph = remember(cell.isMine, cell.adjacentMines) {
        val targetShape = when {
            cell.isMine -> circleShape
            cell.adjacentMines > 0 -> cookie9Shape
            else -> squareShape
        }
        Morph(squareShape, targetShape)
    }

    val morphProgress by animateFloatAsState(
        targetValue = if (cell.isRevealed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "shapeMorph"
    )

    val targetColor = when {
        cell.isRevealed && cell.isMine -> MaterialTheme.colorScheme.errorContainer
        cell.isRevealed -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "cellColor"
    )

    val scale = remember { Animatable(1f) }
    LaunchedEffect(cell.isExploding) {
        if (cell.isExploding) {
            scale.animateTo(1.4f, tween(150))
            scale.animateTo(1f, tween(250))
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale.value)
            .zIndex(if (cell.isExploding) 1f else 0f)
            .clip(MorphPolygonShape(morph, morphProgress))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (cell.isRevealed) {
            if (cell.isMine) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_explosion_24dp),
                    contentDescription = "Mine",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            } else if (cell.adjacentMines > 0) {
                Text(
                    text = cell.adjacentMines.toString(),
                    color = getNumberColor(cell.adjacentMines),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            if (cell.isFlagged) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flag_24dp),
                    contentDescription = "Flag",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else if (cell.hasQuestion) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_question_mark_24dp),
                    contentDescription = "Question",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomActionBar(currentMode: InteractionMode, onModeSelected: (InteractionMode) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val modes = listOf(
            InteractionMode.TOUCH to R.drawable.ic_touch_app_24dp,
            InteractionMode.FLAG to R.drawable.ic_flag_24dp,
            InteractionMode.QUESTION to R.drawable.ic_question_mark_24dp
        )

        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            modes.forEachIndexed { index, (mode, iconRes) ->
                val isSelected = currentMode == mode

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onModeSelected(mode) },
                    modifier = Modifier
                        .semantics { role = Role.RadioButton }
                        .height(56.dp)
                        .width(72.dp),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = mode.name,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    status: GameStatus,
    timeElapsed: Int,
    minesLeft: Int,
    onRestart: () -> Unit
) {
    val title = if (status == GameStatus.WON) "Victory!" else "Game Over"
    val color = if (status == GameStatus.WON) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = title, color = color, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Would you like to play again?")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Time: $timeElapsed seconds", fontWeight = FontWeight.SemiBold)
                Text("Mines left: $minesLeft", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text("Restart")
            }
        }
    )
}

@Composable
fun getNumberColor(count: Int): Color {
    return when (count) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.secondary
        3 -> MaterialTheme.colorScheme.tertiary
        4 -> MaterialTheme.colorScheme.surface
        5 -> MaterialTheme.colorScheme.error
        6 -> MaterialTheme.colorScheme.onPrimaryContainer
        7 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiary
    }
}