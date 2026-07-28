package com.ost.application.presentation.network
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.net.Inet4Address
import java.net.NetworkInterface
enum class WearTransport { WIFI, BLUETOOTH, CELLULAR, NONE }
data class WearNetworkUiState(
    val transport: WearTransport = WearTransport.NONE,
    val localIp: String? = null
)
class WearNetworkViewModel(application: Application) : AndroidViewModel(application) {
    val uiState: StateFlow<WearNetworkUiState> = flow {
        while (currentCoroutineContext().isActive) {
            emit(readState())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearNetworkUiState())
    private fun readState(): WearNetworkUiState {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return WearNetworkUiState()
        val capabilities = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val transport = when {
            capabilities == null -> WearTransport.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> WearTransport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> WearTransport.BLUETOOTH
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> WearTransport.CELLULAR
            else -> WearTransport.NONE
        }
        return WearNetworkUiState(transport = transport, localIp = readLocalIp())
    }
    private fun readLocalIp(): String? = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}
class NetworkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                NetworkScreen()
            }
        }
    }
}
@Composable
fun NetworkScreen(viewModel: WearNetworkViewModel = viewModel()) {
    val listState = rememberScalingLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transportName = stringResource(
        when (uiState.transport) {
            WearTransport.WIFI -> R.string.transport_wifi
            WearTransport.BLUETOOTH -> R.string.transport_bluetooth
            WearTransport.CELLULAR -> R.string.transport_cellular
            WearTransport.NONE -> R.string.transport_none
        }
    )
    val transportIcon = when (uiState.transport) {
        WearTransport.WIFI -> R.drawable.ic_wifi_24dp
        WearTransport.BLUETOOTH -> R.drawable.ic_bluetooth_24dp
        WearTransport.CELLULAR -> R.drawable.ic_signal_cellular_24dp
        WearTransport.NONE -> R.drawable.ic_no_wifi_24dp
    }
    val items = listOf(
        ListItem(
            title = stringResource(R.string.connection_source),
            summary = transportName,
            icon = transportIcon,
            status = true,
            position = CardPosition.TOP
        ),
        ListItem(
            title = stringResource(R.string.ip_address),
            summary = uiState.localIp ?: stringResource(R.string.transport_none),
            icon = null,
            status = true,
            position = CardPosition.BOTTOM
        )
    )
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = stringResource(R.string.network),
                icon = transportIcon,
                items = items
            )
        }
    }
}
