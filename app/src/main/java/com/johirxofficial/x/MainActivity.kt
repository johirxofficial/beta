package com.johirxofficial.x

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.johirxofficial.x.data.Repository
import com.johirxofficial.x.data.Settings
import com.johirxofficial.x.ui.AppRoot
import com.johirxofficial.x.ui.MainViewModel
import com.johirxofficial.x.ui.theme.XTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(Repository(settings), settings)
    }

    private val settings by lazy { Settings(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            XTheme {
                AppRoot(viewModel)
            }
        }
    }
}
