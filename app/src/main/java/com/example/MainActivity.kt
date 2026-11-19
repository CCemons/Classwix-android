package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CalculatorShell
import com.example.ui.theme.ClassWizTheme
import com.example.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      ClassWizTheme {
        val vm: CalculatorViewModel = viewModel()
        val state by vm.uiState.collectAsStateWithLifecycle()
        CalculatorShell(
          state = state,
          onKey = vm::onKey,
          onSelectHomeApp = vm::selectHomeApp,
          onSelectOverlayItem = vm::selectOverlayItem,
          onContrastChange = vm::selectSettingsContrast,
          onSelectSpreadsheetCell = vm::selectSpreadsheetCell,
          onSpreadsheetInsert = vm::insertSpreadsheetSymbol,
        )
      }
    }
  }
}
