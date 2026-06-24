package com.ost.application.explorer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.FailDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private val EDITABLE_EXTENSIONS = setOf(
    "txt", "md", "markdown", "log", "json", "xml", "yaml", "yml",
    "toml", "ini", "cfg", "conf", "properties", "csv", "tsv",
    "html", "htm", "css", "js", "ts", "kt", "java", "py", "rb",
    "sh", "bash", "zsh", "bat", "ps1", "c", "cpp", "h", "hpp",
    "rs", "go", "swift", "dart", "sql", "gradle", "kts", "gitignore"
)

private fun isEditable(filePath: String): Boolean {
    val ext = File(filePath).extension.lowercase()
    return ext in EDITABLE_EXTENSIONS
}

data class EditorDialogState(val message: String, val isError: Boolean)

class TextEditorActivity : ComponentActivity() {

    private val filePath get() = intent.getStringExtra("filePath")
    private val fileContent = mutableStateOf<String?>(null)
    private val isLoading = mutableStateOf(true)
    private val dialogState = mutableStateOf<EditorDialogState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (filePath == null) {
            Log.e("TextEditor", "File path is missing!")
            dialogState.value = EditorDialogState("File path missing", true)
        } else {
            loadContent()
        }

        setContent {
            OSTToolsTheme {
                TextEditorScreen()
            }
        }
    }

    private fun loadContent() {
        isLoading.value = true
        lifecycleScope.launch {
            val result = readFile(filePath)
            fileContent.value = if (result.isSuccess) result.getOrNull() ?: "" else ""
            if (result.isFailure) {
                Log.e("TextEditor", "Error reading file", result.exceptionOrNull())
                dialogState.value = EditorDialogState(
                    result.exceptionOrNull()?.localizedMessage ?: "Error reading file",
                    true
                )
            }
            isLoading.value = false
        }
    }

    private suspend fun readFile(path: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            path?.let {
                val file = File(it)
                if (file.exists() && file.canRead()) Result.success(file.readText())
                else Result.failure(IOException("File not found or unreadable: $path"))
            } ?: Result.failure(IllegalArgumentException("File path is null"))
        } catch (e: Exception) {
            Result.failure(IOException("Error reading file", e))
        }
    }

    private suspend fun saveFile(path: String?, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (path == null) return@withContext Result.failure(IllegalArgumentException("File path is null"))
            try {
                val file = File(path)
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { it.write(content.toByteArray()) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(IOException("Error saving file", e))
            }
        }

    @Composable
    fun TextEditorScreen() {
        val content by fileContent
        val loading by isLoading
        val dialog by dialogState

        val editable = filePath?.let { isEditable(it) } ?: false
        val editorText = remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val scrollState = rememberScrollState()
        var isSaving by remember { mutableStateOf(false) }

        LaunchedEffect(content) {
            editorText.value = content ?: ""
        }

        Scaffold(
            timeText = { TimeText() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> {
                        CircularProgressIndicator()
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!editable) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lock_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Read only",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            BasicTextField(
                                value = editorText.value,
                                onValueChange = { if (editable) editorText.value = it },
                                readOnly = !editable,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (editable)
                                            MaterialTheme.colorScheme.outline
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                    .verticalScroll(scrollState)
                                    .padding(8.dp)
                                    .then(if (editable) Modifier.focusRequester(focusRequester) else Modifier),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = TextStyle(
                                    color = if (editable)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Start,
                                    fontFamily = FontFamily(Font(R.font.consola))
                                )
                            )

                            if (editable) {
                                FilledIconButton(
                                    onClick = {
                                        isSaving = true
                                        focusManager.clearFocus()
                                        lifecycleScope.launch {
                                            val result = saveFile(filePath, editorText.value)
                                            isSaving = false
                                            dialogState.value = if (result.isSuccess) {
                                                EditorDialogState("Saved", false)
                                            } else {
                                                EditorDialogState(
                                                    result.exceptionOrNull()?.localizedMessage
                                                        ?: "Error saving file",
                                                    true
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_save_24dp),
                                            contentDescription = "Save"
                                        )
                                    }
                                }
                            }

                            LaunchedEffect(loading) {
                                if (!loading && editable) {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                    }
                }

                dialog?.let { state ->
                    FailDialog(
                        message = state.message,
                        iconResId = if (state.isError) R.drawable.ic_error_24dp else R.drawable.ic_check_circle_24dp,
                        onDismiss = { dialogState.value = null },
                        showDialog = true
                    )
                }
            }
        }
    }
}