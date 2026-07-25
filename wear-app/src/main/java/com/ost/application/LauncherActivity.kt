package com.ost.application
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.ost.application.presentation.WelcomeActivity
@SuppressLint("CustomSplashScreen")
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("wear_app_prefs", MODE_PRIVATE)
        val isSetupComplete = prefs.getBoolean("setup_complete", false)
        if (isSetupComplete) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        finish()
    }
}
