package com.ost.application.presentation.stargazers
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.ost.application.R
import com.ost.application.core.settings.sync.SettingsSyncPaths
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
data class RepoStars(val name: String, val stars: Int)
sealed class StargazersState {
    object Loading : StargazersState()
    object PhoneUnavailable : StargazersState()
    object NoToken : StargazersState()
    object Error : StargazersState()
    data class Loaded(val total: Int, val repos: List<RepoStars>) : StargazersState()
}
class WearStargazersViewModel(application: Application) : AndroidViewModel(application),
    MessageClient.OnMessageReceivedListener {
    private val messageClient = Wearable.getMessageClient(application)
    private val _state = MutableStateFlow<StargazersState>(StargazersState.Loading)
    val state: StateFlow<StargazersState> = _state.asStateFlow()
    init {
        messageClient.addListener(this)
        requestFromPhone()
    }
    fun requestFromPhone() {
        _state.value = StargazersState.Loading
        viewModelScope.launch {
            try {
                val nodes = Wearable.getNodeClient(getApplication<Application>()).connectedNodes.await()
                if (nodes.isEmpty()) {
                    _state.value = StargazersState.PhoneUnavailable
                    return@launch
                }
                messageClient.sendMessage(nodes.first().id, SettingsSyncPaths.STARGAZERS_REQUEST_PATH, ByteArray(0)).await()
                delay(20_000)
                if (_state.value is StargazersState.Loading) {
                    _state.value = StargazersState.PhoneUnavailable
                }
            } catch (e: Exception) {
                _state.value = StargazersState.PhoneUnavailable
            }
        }
    }
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != SettingsSyncPaths.STARGAZERS_RESPONSE_PATH) return
        _state.value = try {
            val json = JSONObject(String(messageEvent.data, Charsets.UTF_8))
            when {
                json.optString("error") == "no_token" -> StargazersState.NoToken
                json.has("error") -> StargazersState.Error
                else -> {
                    val reposJson = json.getJSONArray("repos")
                    val repos = (0 until reposJson.length()).map { i ->
                        val repo = reposJson.getJSONObject(i)
                        RepoStars(repo.getString("name"), repo.getInt("stars"))
                    }
                    StargazersState.Loaded(total = json.getInt("total"), repos = repos)
                }
            }
        } catch (e: Exception) {
            StargazersState.Error
        }
    }
    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(this)
    }
}
class StargazersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                StargazersScreen()
            }
        }
    }
}
@Composable
fun StargazersScreen(viewModel: WearStargazersViewModel = viewModel()) {
    val listState = rememberScalingLazyListState()
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            when (val current = state) {
                is StargazersState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                is StargazersState.Loaded -> InfoListScreenContent(
                    listState = listState,
                    screenTitle = stringResource(R.string.stargazers),
                    icon = R.drawable.ic_stars_24dp,
                    items = current.repos.mapIndexed { index, repo ->
                        ListItem(
                            repo.name,
                            "★ ${repo.stars}",
                            null, true,
                            when {
                                current.repos.size == 1 -> CardPosition.SINGLE
                                index == 0 -> CardPosition.TOP
                                index == current.repos.lastIndex -> CardPosition.BOTTOM
                                else -> CardPosition.MIDDLE
                            },
                            null
                        )
                    }
                )
                else -> InfoListScreenContent(
                    listState = listState,
                    screenTitle = stringResource(R.string.stargazers),
                    icon = R.drawable.ic_stars_24dp,
                    items = listOf(
                        ListItem(
                            stringResource(
                                when (current) {
                                    is StargazersState.NoToken -> R.string.sign_in_on_phone
                                    is StargazersState.Error -> R.string.update_check_error
                                    else -> R.string.phone_unavailable
                                }
                            ),
                            null, null, true, CardPosition.SINGLE
                        ) { viewModel.requestFromPhone() }
                    )
                )
            }
        }
    }
}
