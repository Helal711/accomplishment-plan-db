package com.helal.accomplishmentplan.webview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.helal.accomplishmentplan.webview.ui.WebAppScreen
import com.helal.accomplishmentplan.webview.ui.theme.AccomplishmentPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AccomplishmentPlanTheme {
                WebAppScreen(
                    onExitApp = { finish() }
                )
            }
        }
    }
}
