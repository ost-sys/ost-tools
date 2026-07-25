package com.ost.application.ui.components
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
enum class LogLevelFilter(val tagPrefix: String) {
    ALL(""),
    ERROR("E/"),
    WARN("W/"),
    INFO("I/"),
    DEBUG("D/")
}
private fun parseLogLevel(logLine: String): LogLevelFilter {
    val trimmed = logLine.trim()
    if (trimmed.isBlank()) return LogLevelFilter.ALL
    val parts = trimmed.split(Regex("\\s+"))
    if (parts.size >= 5 && parts[0].matches(Regex("\\d{2}-\\d{2}"))) {
        when (parts[4]) {
            "E", "F" -> return LogLevelFilter.ERROR
            "W" -> return LogLevelFilter.WARN
            "I" -> return LogLevelFilter.INFO
            "D" -> return LogLevelFilter.DEBUG
        }
    }
    if (trimmed.contains(" E/") || trimmed.startsWith("E/") || trimmed.contains(" E ")) return LogLevelFilter.ERROR
    if (trimmed.contains(" W/") || trimmed.startsWith("W/") || trimmed.contains(" W ")) return LogLevelFilter.WARN
    if (trimmed.contains(" I/") || trimmed.startsWith("I/") || trimmed.contains(" I ")) return LogLevelFilter.INFO
    if (trimmed.contains(" D/") || trimmed.startsWith("D/") || trimmed.contains(" D ")) return LogLevelFilter.DEBUG
    return LogLevelFilter.ALL
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rawLogs = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LogLevelFilter.ALL) }
    var isPaused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    DisposableEffect(isPaused) {
        if (isPaused) return@DisposableEffect onDispose {}
        var process: Process? = null
        val job = coroutineScope.launch(Dispatchers.IO) {
            try {
                process = Runtime.getRuntime().exec("logcat -v threadtime")
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                var line: String?
                while (isActive) {
                    line = reader.readLine() ?: break
                    val currentLine = line
                    withContext(Dispatchers.Main) {
                        if (rawLogs.size > 2000) {
                            rawLogs.removeRange(0, 500)
                        }
                        rawLogs.add(currentLine)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    rawLogs.add("E/LogcatDialog: Failed to read logcat stream: ${e.message}")
                }
            } finally {
                process?.destroy()
            }
        }
        onDispose {
            job.cancel()
            process?.destroy()
        }
    }
    val filteredLogs = remember(rawLogs.size, searchQuery, selectedFilter) {
        rawLogs.filter { logLine ->
            val level = parseLogLevel(logLine)
            val matchesLevel = when (selectedFilter) {
                LogLevelFilter.ALL -> true
                LogLevelFilter.ERROR -> level == LogLevelFilter.ERROR
                LogLevelFilter.WARN -> level == LogLevelFilter.WARN || level == LogLevelFilter.ERROR
                LogLevelFilter.INFO -> level == LogLevelFilter.INFO || level == LogLevelFilter.WARN || level == LogLevelFilter.ERROR
                LogLevelFilter.DEBUG -> level == LogLevelFilter.DEBUG || level == LogLevelFilter.INFO || level == LogLevelFilter.WARN || level == LogLevelFilter.ERROR
            }
            val matchesQuery = if (searchQuery.isBlank()) true else logLine.contains(searchQuery, ignoreCase = true)
            matchesLevel && matchesQuery
        }
    }
    LaunchedEffect(filteredLogs.size) {
        if (!isPaused && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_terminal_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.console_logs),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_logs)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                val filters = LogLevelFilter.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    filters.forEachIndexed { index, level ->
                        val label = when (level) {
                            LogLevelFilter.ALL -> stringResource(R.string.log_level_all)
                            LogLevelFilter.ERROR -> stringResource(R.string.log_level_error)
                            LogLevelFilter.WARN -> stringResource(R.string.log_level_warn)
                            LogLevelFilter.INFO -> stringResource(R.string.log_level_info)
                            LogLevelFilter.DEBUG -> stringResource(R.string.log_level_debug)
                        }
                        SegmentedButton(
                            selected = selectedFilter == level,
                            onClick = { selectedFilter = level },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {
                            items(filteredLogs) { line ->
                                val color = when (parseLogLevel(line)) {
                                    LogLevelFilter.ERROR -> Color(0xFFFF6B6B)
                                    LogLevelFilter.WARN -> Color(0xFFFFD166)
                                    LogLevelFilter.INFO -> Color(0xFF06D6A0)
                                    LogLevelFilter.DEBUG -> Color(0xFF4EA8DE)
                                    else -> Color(0xFFCCCCCC)
                                }
                                Text(
                                    text = line,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    TextButton(onClick = {
                        isPaused = !isPaused
                    }) {
                        Text(if (isPaused) stringResource(R.string.resume_logs) else stringResource(R.string.pause_logs))
                    }
                    TextButton(onClick = {
                        rawLogs.clear()
                    }) {
                        Text(stringResource(R.string.clear_logs))
                    }
                }
                Button(onClick = {
                    val logsText = filteredLogs.joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("OST_Tools_Logcat", logsText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.logs_copied), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.copy_logs))
                }
            }
        }
    )
}
