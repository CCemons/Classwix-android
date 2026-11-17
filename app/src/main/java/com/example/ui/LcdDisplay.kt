package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.*
import com.example.ui.theme.*
import com.example.viewmodel.*

@Composable
fun LcdDisplay(
  state: CalculatorUiState,
  onSelectHomeApp: (CalcMode) -> Unit,
  onSelectOverlayItem: (Int) -> Unit,
  onContrastChange: (Float) -> Unit,
  onSelectSpreadsheetCell: (Int, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val basePalette = if (state.settings.lcdTheme == LcdThemeMode.CLASSIC) ClassicClassWizLcd else PaperWhiteLcd
  val contrast = state.settings.lcdContrast.coerceIn(0.45f, 1.0f)
  val palette = basePalette.copy(
    inkDarkest = basePalette.inkDarkest.copy(alpha = contrast),
    inkDark = basePalette.inkDark.copy(alpha = (contrast * 0.92f).coerceIn(0.35f, 1f)),
  )

  // Glossy black outer display bezel matching Casio fx-991LA CW
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(BezelBlack, Color(0xFF171A1F), BezelBlack)
        )
      )
      .border(1.2.dp, BezelInnerRim, RoundedCornerShape(10.dp))
      .padding(horizontal = 8.dp, vertical = 7.dp),
  ) {
    if (!state.isPoweredOn) {
      // Powered-off LCD glass
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(142.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(palette.backgroundShade.copy(alpha = 0.72f))
          .border(1.dp, palette.bezelShadow, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "CASIO",
          color = palette.inkMid.copy(alpha = 0.35f),
          fontSize = 18.sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Bold,
          letterSpacing = 3.sp,
        )
      }
      return@Box
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(142.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(palette.background, palette.backgroundShade.copy(alpha = 0.45f), palette.background)
          )
        )
        .border(1.dp, palette.bezelShadow, RoundedCornerShape(4.dp))
        .testTag("lcd_screen"),
    ) {
      LcdStatusBar(state = state, palette = palette)

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 4.dp, vertical = 2.dp),
      ) {
        when (state.overlay) {
          OverlayScreen.HOME_GRID -> LcdHomeGridView(state, palette, onSelectHomeApp)
          OverlayScreen.CATALOG_MENU, OverlayScreen.TOOLS_MENU, OverlayScreen.FORMAT_MENU, OverlayScreen.FX_MENU -> {
            val top = state.overlayStack.lastOrNull()
            if (top != null) {
              LcdMenuListView(
                title = top.first,
                items = top.second,
                cursor = state.overlayCursor,
                isSpanish = state.settings.isSpanish,
                palette = palette,
                onSelectItem = onSelectOverlayItem,
              )
            }
          }
          OverlayScreen.SETTINGS_MENU -> LcdSettingsView(state, palette, onSelectOverlayItem, onContrastChange)
          OverlayScreen.VARIABLE_MENU -> LcdVariableView(state, palette, onSelectOverlayItem)
          OverlayScreen.QR_VIEW -> LcdQrView(state, palette)
          OverlayScreen.NONE -> {
            if (state.editingFxTarget != null) {
              FxInlineEditorLcdBody(state, palette)
            } else {
              val isModeCustom = when (state.mode) {
                CalcMode.CALCULATE, CalcMode.COMPLEX, CalcMode.BASE_N -> false
                CalcMode.MATRIX -> state.matrixState.phase != MatrixAppState.Phase.WORK
                CalcMode.VECTOR -> state.vectorState.phase != VectorAppState.Phase.WORK
                else -> true
              }
              if (isModeCustom) {
                LcdModeBodyView(state, palette, onSelectSpreadsheetCell)
              } else {
                StandardCalcLcdBody(state, palette)
              }
            }
          }
        }

        // Subtle ClassWiz notification toast banner (e.g. "→ A")
        state.toastBanner?.let { msg ->
          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(4.dp)
              .background(palette.inkDarkest, RoundedCornerShape(3.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Text(msg, color = palette.background, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun LcdStatusBar(state: CalculatorUiState, palette: LcdPalette) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(palette.backgroundShade.copy(alpha = 0.7f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
      // Shift indicator [S]
      if (state.isShiftActive) {
        StatusBadge("S", inverted = true, palette = palette)
      } else {
        Box(Modifier.size(13.dp, 13.dp))
      }

      // Angle unit [D] [R] [G]
      StatusBadge(state.settings.angleUnit.badge, inverted = false, palette = palette)

      // Math I/O symbol
      if (state.settings.inputOutput == InputOutputMode.MATH_MATH || state.settings.inputOutput == InputOutputMode.MATH_DEC) {
        Text("√", color = palette.inkDarkest, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
      }

      // Number format FIX / SCI
      when (state.settings.numberFormat) {
        NumberFormatMode.FIX -> StatusBadge("FIX", inverted = false, palette = palette)
        NumberFormatMode.SCI -> StatusBadge("SCI", inverted = false, palette = palette)
        else -> {}
      }

      // Complex i indicator
      if (state.mode == CalcMode.COMPLEX) {
        StatusBadge("i", inverted = false, palette = palette)
      }

      // Base-N radix indicator
      if (state.mode == CalcMode.BASE_N) {
        StatusBadge(state.baseNMode.label, inverted = true, palette = palette)
      }

      // Current mode badge or STO / f(x) indicator
      if (state.storePending) {
        StatusBadge("STO", inverted = true, palette = palette)
      }
      if (state.editingFxTarget != null) {
        StatusBadge(if (state.editingFxTarget == 0) "f(x)" else "g(x)", inverted = false, palette = palette)
      } else {
        Text(
          text = state.settings.tr(state.mode.titleEs, state.mode.titleEn),
          color = palette.inkDark,
          fontSize = 9.5.sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
      if (state.history.isNotEmpty() && state.overlay == OverlayScreen.NONE) {
        Text("▲▼", color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
      }
      // Authentic solar / battery icon
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .width(13.dp)
            .height(7.dp)
            .border(0.9.dp, palette.inkDarkest)
            .padding(1.dp)
            .background(palette.inkDarkest)
        )
        Box(
          modifier = Modifier
            .width(1.5.dp)
            .height(3.5.dp)
            .background(palette.inkDarkest)
        )
      }
    }
  }
}

@Composable
private fun StatusBadge(text: String, inverted: Boolean, palette: LcdPalette) {
  Box(
    modifier = Modifier
      .background(if (inverted) palette.inkDarkest else Color.Transparent, RoundedCornerShape(2.dp))
      .border(0.8.dp, palette.inkDarkest, RoundedCornerShape(2.dp))
      .padding(horizontal = 3.dp, vertical = 0.5.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = if (inverted) palette.background else palette.inkDarkest,
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = JetBrainsMonoFamily,
      lineHeight = 9.sp,
    )
  }
}

@Composable
private fun StandardCalcLcdBody(state: CalculatorUiState, palette: LcdPalette) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 4.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Upper expression editor
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.TopStart,
    ) {
      NaturalMathRowView(
        row = state.doc.root,
        cursor = state.doc.cursor,
        currentSteps = emptyList(),
        palette = palette,
        fontSize = 19.sp,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    // Lower result line
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 38.dp),
      contentAlignment = Alignment.BottomEnd,
    ) {
      state.currentResult?.let { res ->
        EvalResultDisplay(
          result = res,
          settings = state.settings,
          formatOverride = state.resultFormatOverride,
          palette = palette,
        )
      } ?: run {
        if (state.mode == CalcMode.MATRIX || state.mode == CalcMode.VECTOR) {
          Text(
            text = state.settings.tr("CATALOG: Mat/Vct | TOOLS: Editar", "CATALOG: Mat/Vct | TOOLS: Edit"),
            color = palette.inkMid,
            fontSize = 9.5.sp,
            fontFamily = IbmPlexSansFamily,
            modifier = Modifier.alpha(0.85f),
          )
        }
      }
    }
  }
}

@Composable
private fun FxInlineEditorLcdBody(state: CalculatorUiState, palette: LcdPalette) {
  val isF = state.editingFxTarget == 0
  val doc = if (isF) state.tableState.fDoc else state.tableState.gDoc
  val s = state.settings
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = if (isF) "f(x) =" else "g(x) =",
      color = palette.inkDarkest,
      fontSize = 12.sp,
      fontFamily = JetBrainsMonoFamily,
      fontWeight = FontWeight.Bold,
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.CenterStart,
    ) {
      NaturalMathRowView(
        row = doc.root,
        cursor = doc.cursor,
        currentSteps = emptyList(),
        palette = palette,
        fontSize = 19.sp,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Text(
      text = s.tr("OK/EXE: Guardar función | BACK: Salir", "OK/EXE: Save function | BACK: Exit"),
      color = palette.inkDark,
      fontSize = 9.5.sp,
      fontFamily = IbmPlexSansFamily,
    )
  }
}
