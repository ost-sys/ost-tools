package com.ost.application.service
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.ost.application.MainActivity
import com.ost.application.R
import com.ost.application.core.settings.sync.SettingsSyncPaths
import com.ost.application.data.remote.RetrofitClient
import com.ost.application.settings.GithubTokenRepository
import com.ost.application.ui.activity.about.AboutActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
class PhoneDataLayerListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: ${messageEvent.path}")
        when (messageEvent.path) {
            OPEN_ABOUT_PATH -> deliver(
                Intent(this, AboutActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                getString(R.string.about_app)
            )
            SettingsSyncPaths.MESSAGE_OPEN_SETTINGS_PATH -> deliver(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true),
                getString(R.string.open_settings)
            )
            SettingsSyncPaths.STARGAZERS_REQUEST_PATH -> respondWithStargazers(messageEvent.sourceNodeId)
        }
    }
    private fun respondWithStargazers(nodeId: String) {
        val payload = runBlocking {
            try {
                withTimeout(15_000) { buildStargazersJson() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build stargazers payload", e)
                JSONObject().put("error", "fetch_failed").toString()
            }
        }
        runCatching {
            runBlocking {
                Wearable.getMessageClient(this@PhoneDataLayerListenerService)
                    .sendMessage(nodeId, SettingsSyncPaths.STARGAZERS_RESPONSE_PATH, payload.toByteArray(Charsets.UTF_8))
                    .await()
            }
        }.onFailure { Log.e(TAG, "Failed to send stargazers response", it) }
    }
    private suspend fun buildStargazersJson(): String {
        val token = GithubTokenRepository(applicationContext, CoroutineScope(Dispatchers.IO)).token.value
        if (token.isBlank()) return JSONObject().put("error", "no_token").toString()
        val tokenHeader = if (token.startsWith("token ")) token else "token $token"
        val repos = RetrofitClient.api.getUserRepos(token = tokenHeader)
        val reposArray = JSONArray()
        repos.sortedByDescending { it.starsCount }.take(30).forEach { repo ->
            reposArray.put(JSONObject().put("name", repo.name).put("stars", repo.starsCount))
        }
        return JSONObject()
            .put("total", repos.sumOf { it.starsCount })
            .put("repos", reposArray)
            .toString()
    }
    private fun deliver(intent: Intent, label: String) {
        if (isAppInForeground()) {
            runCatching { startActivity(intent) }
                .onFailure { Log.e(TAG, "Failed to start activity for '$label' directly", it) }
        } else {
            postOpenNotification(intent, label)
        }
    }
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = activityManager.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }
        return processInfo?.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
    private fun postOpenNotification(intent: Intent, label: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
        )
        val pendingIntent = PendingIntent.getActivity(
            this, label.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_update_24dp)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(label)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { notificationManager.notify(label.hashCode(), notification) }
            .onFailure { Log.w(TAG, "Failed to post notification for '$label'", it) }
    }
    companion object {
        private const val TAG = "PhoneDataLayerListener"
        private const val OPEN_ABOUT_PATH = "/open_about"
        private const val CHANNEL_ID = "ost_remote_open_channel"
    }
}
