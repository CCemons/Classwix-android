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
  onSpreadsheetInsert: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(
    enabled = state.overlay != OverlayScreen.NONE ||
      state.editingFxTarget != null ||
      state.tableState.editingXRow != null
  ) {
    onKey(CalcKey.BACK)
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(DeskMatBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    val availableH = maxHeight
    val lcdHeight = (availableH * 0.21f).coerceIn(132.dp, 168.dp)
    val keyScale = ((availableH - lcdHeight - 92.dp) / 478.dp).coerceIn(0.76f, 1.06f)
    val needsScroll = availableH < 560.dp

    val chassisModifier = Modifier
      .widthIn(max = 446.dp)
      .then(if (needsScroll) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
      .padding(horizontal = 6.dp, vertical = 4.dp)
      .shadow(18.dp, RoundedCornerShape(26.dp))
      .clip(RoundedCornerShape(26.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(ChassisDarkTop, ChassisDarkMid, ChassisDarkBottom)
        )
      )
      .border(1.5.dp, ChassisBorderHighlight, RoundedCornerShape(26.dp))
      .then(if (needsScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
      .padding(horizontal = 12.dp, vertical = 9.dp)

    Column(
      modifier = chassisModifier,
      verticalArrangement = Arrangement.spacedBy((4.5f * keyScale).dp),
    ) {
      // Top Hardware Header: CASIO + fx-991LA CW on left, recessed Solar Cell on right
      TopHardwareBrandBar()

      // High-Resolution 4-Shade Grayscale LCD + bottom-right "C L A S S W I Z" bezel caption
      Column(modifier = Modifier.fillMaxWidth()) {
        LcdDisplay(
          state = state,
          onSelectHomeApp = onSelectHomeApp,
          onSelectOverlayItem = onSelectOverlayItem,
          onContrastChange = onContrastChange,
          onSelectSpreadsheetCell = onSelectSpreadsheetCell,
          onSpreadsheetInsert = onSpreadsheetInsert,
          lcdHeight = lcdHeight,
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, end = 6.dp),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Text(
            text = "CLASSWIZ",
            color = BrandWhite.copy(alpha = 0.88f),
            fontSize = 9.5.sp,
            fontFamily = IbmPlexSansFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.2.sp,
          )
        }
      }

      // Authentic 48-Key Casio ClassWiz fx-991LA CW Physical Keypad
      CalculatorKeypad(
        isShiftActive = state.isShiftActive,
        hapticsEnabled = state.settings.hapticsEnabled,
        onKey = onKey,
        keyScale = keyScale,
      )
    }
  }
}

@Composable
private fun TopHardwareBrandBar() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 6.dp, vertical = 1.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(
        text = "CASIO",
        color = BrandWhite,
        fontSize = 16.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.8.sp,
      )
      Text(
        text = "fx-991LA CW",
        color = LegendPrimary.copy(alpha = 0.9f),
        fontSize = 10.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
      )
    }

    // Authentic recessed dark Solar Panel matching the fx-991LA CW faceplate
    Box(
      modifier = Modifier
        .width(126.dp)
        .height(28.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(SolarFrame)
        .border(1.dp, Color(0xFF3A3E46), RoundedCornerShape(6.dp))
        .padding(3.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(3.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(SolarCellBase, SolarCellHighlight, SolarCellBase)
            )
          ),
      ) {
        repeat(4) { idx ->
          Box(modifier = Modifier.weight(1f).fillMaxHeight())
          if (idx < 3) {
            Box(modifier = Modifier.width(0.8.dp).fillMaxHeight().background(SolarGridLine))
          }
        }
      }
    }
  }
}
