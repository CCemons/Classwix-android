package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CalcMode
import com.example.ui.theme.*
import com.example.viewmodel.CalcKey
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.OverlayScreen

@Composable
fun CalculatorShell(
  state: CalculatorUiState,
  onKey: (CalcKey) -> Unit,
  onSelectHomeApp: (CalcMode) -> Unit,
  onSelectOverlayItem: (Int) -> Unit,
  onContrastChange: (Float) -> Unit,
  onSelectSpreadsheetCell: (Int, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Handle Android system Back button authentically via BACK key when in an overlay or sub-phase
  BackHandler(enabled = state.overlay != OverlayScreen.NONE || state.editingFxTarget != null) {
    onKey(CalcKey.BACK)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DeskMatBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 450.dp)
        .fillMaxHeight()
        .padding(horizontal = 6.dp, vertical = 4.dp)
        .shadow(16.dp, RoundedCornerShape(22.dp))
        .clip(RoundedCornerShape(22.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(ChassisDarkTop, ChassisDarkMid, ChassisDarkBottom)
          )
        )
        .border(1.4.dp, ChassisBorderHighlight, RoundedCornerShape(22.dp))
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Hardware Header: CASIO + fx-991LA CW + Solar Panel
      TopHardwareBrandBar()

      Spacer(Modifier.height(6.dp))

      // High-Resolution 4-Shade Grayscale LCD
      LcdDisplay(
        state = state,
        onSelectHomeApp = onSelectHomeApp,
        onSelectOverlayItem = onSelectOverlayItem,
        onContrastChange = onContrastChange,
        onSelectSpreadsheetCell = onSelectSpreadsheetCell,
      )

      Spacer(Modifier.height(8.dp))

      // Authentic Casio fx-991LA CW Physical Keypad
      CalculatorKeypad(
        isShiftActive = state.isShiftActive,
        hapticsEnabled = state.settings.hapticsEnabled,
        onKey = onKey,
      )
    }
  }
}

@Composable
private fun TopHardwareBrandBar() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column {
      Text(
        text = "CASIO",
        color = BrandWhite,
        fontSize = 16.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
      )
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "fx-991LA CW",
          color = LegendMuted,
          fontSize = 10.5.sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "CLASSWIZ",
          color = BrandCyan,
          fontSize = 10.sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Bold,
          fontStyle = FontStyle.Italic,
          letterSpacing = 0.8.sp,
        )
      }
    }

    // Authentic 4-Cell Solar Panel
    Box(
      modifier = Modifier
        .width(96.dp)
        .height(26.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(SolarFrame)
        .border(1.dp, Color(0xFF424854), RoundedCornerShape(4.dp))
        .padding(3.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(2.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(SolarCellBase, SolarCellHighlight, SolarCellBase)
            )
          ),
      ) {
        repeat(4) { idx ->
          Box(modifier = Modifier.weight(1f).fillMaxHeight())
          if (idx < 3) {
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(SolarGridLine))
          }
        }
      }
    }
  }
}
