package com.johirxofficial.x

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import com.johirxofficial.x.data.Repository
import com.johirxofficial.x.data.Settings
import com.johirxofficial.x.data.UpdateManager
import com.johirxofficial.x.data.UserDataStore
import com.johirxofficial.x.ui.AppRoot
import com.johirxofficial.x.ui.MainViewModel
import com.johirxofficial.x.ui.theme.BetaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = Repository(applicationContext),
            settings = Settings(applicationContext),
            userData = UserDataStore(applicationContext),
            updateManager = UpdateManager(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val darkTheme = when (viewModel.home.theme) {
                Settings.THEME_DARK -> true
                Settings.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }

            BetaTheme(darkTheme = darkTheme) {
                AppRoot(viewModel)
            }
        }
    }
}
