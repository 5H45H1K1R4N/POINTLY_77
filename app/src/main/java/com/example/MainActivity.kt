package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.PointlyBentoScreen
import com.example.ui.viewmodel.PointlyViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: PointlyViewModel = viewModel()
      val themeState by viewModel.appTheme.collectAsStateWithLifecycle()
      val darkTheme = when (themeState) {
          "Light" -> false
          "Dark" -> true
          else -> isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = darkTheme) {
        PointlyBentoScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
      }
    }
  }
}

