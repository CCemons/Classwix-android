package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CalcKey

@Composable
fun CalculatorKeypad(
  isShiftActive: Boolean,
  hapticsEnabled: Boolean,
  onKey: (CalcKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val view = LocalView.current
  fun fire(key: CalcKey) {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    onKey(key)
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // 1. Upper System & Circular D-Pad Cluster (ON, HOME, CONFIG | D-Pad + OK | BACK, PgUp/PgDn)
    UpperNavigationSection(onFire = ::fire)

    Spacer(Modifier.height(2.dp))

    // 2. ClassWiz CW 5-Pill Utility Row: SHIFT | VARIABLE (STO) | f(x) (g(x)) | CATALOG | TOOLS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      UtilityPillKey(
        key = CalcKey.SHIFT,
        shiftLabel = "",
        mainLabel = "SHIFT",
        isHighlighted = isShiftActive,
        highlightColor = KeyShiftActive,
        mainLabelColor = if (isShiftActive) KeyShiftActive else LegendSecondaryShift,
        modifier = Modifier.weight(1f),
        onFire = ::fire,
      )
      UtilityPillKey(
        key = CalcKey.VARIABLE,
        shiftLabel = "STO",
        mainLabel = "VARIABLE",
        mainSize = 8.5.sp,
        modifier = Modifier.weight(1f),
        onFire = ::fire,
      )
      UtilityPillKey(
        key = CalcKey.FX,
        shiftLabel = "g(x)",
        mainLabel = "f(x)",
        mainSize = 10.5.sp,
        italic = true,
        modifier = Modifier.weight(1f),
        onFire = ::fire,
      )
      UtilityPillKey(
        key = CalcKey.CATALOG,
        shiftLabel = "",
        mainLabel = "CATALOG",
        mainSize = 8.5.sp,
        modifier = Modifier.weight(1f),
        onFire = ::fire,
      )
      UtilityPillKey(
        key = CalcKey.TOOLS,
        shiftLabel = "",
        mainLabel = "TOOLS",
        mainSize = 9.sp,
        modifier = Modifier.weight(1f),
        onFire = ::fire,
      )
    }

    // 3. Scientific Function Matrix (6 columns × 3 rows — exact fx-991LA CW layout)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        SciKey(CalcKey.VAR_X, "QR", "x", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.FRAC, "a b/c", "□/□", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.SQRT, "³√□", "√□", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.SQUARE, "x⁻¹", "x²", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.POWER, "ˣ√□", "x^□", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.LOG_BASE, "∫", "logₐ", Modifier.weight(1f), ::fire)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        SciKey(CalcKey.LOG10, "10^□", "log", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.LN, "e^□", "ln", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.SIN, "sin⁻¹", "sin", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.COS, "cos⁻¹", "cos", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.TAN, "tan⁻¹", "tan", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.NEG, "|□|", "(-)", Modifier.weight(1f), ::fire)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        SciKey(CalcKey.DMS, "x!", "° ' \"", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.PAREN_OPEN, "%", "(", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.PAREN_CLOSE, ",", ")", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.DERIV, "Σ", "d/dx", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.EQ, "≠", "=", Modifier.weight(1f), ::fire)
        SciKey(CalcKey.ANS, "PreAns", "Ans", Modifier.weight(1f), ::fire)
      }
    }

    Spacer(Modifier.height(2.dp))

    // 4. Lower Numeric & Operator Pad (5 columns × 4 rows — exact CW SHIFT: 7=π, 8=e, 9=i, AC=OFF, FORMAT=S⇔D, EXE=≈)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumKey(CalcKey.D7, "π", "7", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.D8, "e", "8", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.D9, "i", "9", Modifier.weight(1f), onFire = ::fire)
        NumKey(
          key = CalcKey.DEL,
          shiftLabel = "UNDO",
          mainLabel = "⌫",
          modifier = Modifier.weight(1f),
          topColor = KeyActionBlueTop,
          bottomColor = KeyActionBlueBottom,
          onFire = ::fire,
        )
        NumKey(
          key = CalcKey.AC,
          shiftLabel = "OFF",
          mainLabel = "AC",
          modifier = Modifier.weight(1f),
          topColor = KeyActionBlueTop,
          bottomColor = KeyActionBlueBottom,
          onFire = ::fire,
        )
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumKey(CalcKey.D4, "", "4", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.D5, "", "5", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.D6, "", "6", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.MUL, "", "×", Modifier.weight(1f), mainSize = 18.sp, onFire = ::fire)
        NumKey(CalcKey.DIV, "", "÷", Modifier.weight(1f), mainSize = 18.sp, onFire = ::fire)
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumKey(CalcKey.D1, "", "1", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.D2, "", "2", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.D3, "", "3", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.ADD, "", "+", Modifier.weight(1f), mainSize = 18.sp, onFire = ::fire)
        NumKey(CalcKey.SUB, "", "−", Modifier.weight(1f), mainSize = 18.sp, onFire = ::fire)
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumKey(CalcKey.D0, "", "0", Modifier.weight(1f), onFire = ::fire)
        NumKey(CalcKey.DOT, "", ".", Modifier.weight(1f), mainSize = 20.sp, onFire = ::fire)
        NumKey(CalcKey.EXP10, "", "×10ˣ", Modifier.weight(1f), mainSize = 11.5.sp, onFire = ::fire)
        NumKey(CalcKey.FORMAT, "S⇔D", "FORMAT", Modifier.weight(1f), mainSize = 9.sp, onFire = ::fire)
        NumKey(
          key = CalcKey.EXE,
          shiftLabel = "≈",
          mainLabel = "EXE",
          modifier = Modifier.weight(1f),
          mainSize = 12.5.sp,
          topColor = KeyExeAccentTop,
          bottomColor = KeyExeAccentBottom,
          onFire = ::fire,
        )
      }
    }
  }
}

@Composable
private fun UpperNavigationSection(onFire: (CalcKey) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Left Cluster: ON + HOME (row 1), CONFIG/SETTINGS (row 2)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        SystemPillKey(
          key = CalcKey.ON,
          label = "ON",
          icon = Icons.Outlined.PowerSettingsNew,
          modifier = Modifier.weight(0.95f),
          onFire = onFire,
        )
        SystemPillKey(
          key = CalcKey.HOME,
          label = "HOME",
          icon = Icons.Outlined.Home,
          modifier = Modifier.weight(1.15f),
          onFire = onFire,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        SystemPillKey(
          key = CalcKey.SETTINGS,
          label = "CONFIG",
          icon = Icons.Outlined.Settings,
          modifier = Modifier.weight(1f),
          onFire = onFire,
        )
        Spacer(Modifier.weight(0.45f))
      }
    }

    // Center Circular 4-Way D-Pad with Raised OK Key
    CircularDPad(onFire = onFire)

    // Right Cluster: BACK + vertical Page Up / Page Down rocker
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Column(
        modifier = Modifier.weight(1.05f),
        verticalArrangement = Arrangement.Bottom,
      ) {
        Spacer(Modifier.height(14.dp))
        SystemPillKey(
          key = CalcKey.BACK,
          label = "BACK",
          icon = Icons.AutoMirrored.Outlined.Undo,
          modifier = Modifier.fillMaxWidth(),
          onFire = onFire,
        )
      }
      Column(
        modifier = Modifier.weight(0.95f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        SystemPillKey(
          key = CalcKey.PAGE_UP,
          label = "PgUp",
          icon = Icons.Outlined.KeyboardDoubleArrowUp,
          height = 31.dp,
          modifier = Modifier.fillMaxWidth(),
          onFire = onFire,
        )
        SystemPillKey(
          key = CalcKey.PAGE_DOWN,
          label = "PgDn",
          icon = Icons.Outlined.KeyboardDoubleArrowDown,
          height = 31.dp,
          modifier = Modifier.fillMaxWidth(),
          onFire = onFire,
        )
      }
    }
  }
}

@Composable
private fun CircularDPad(onFire: (CalcKey) -> Unit) {
  Box(
    modifier = Modifier
      .size(100.dp)
      .shadow(4.dp, CircleShape)
      .clip(CircleShape)
      .background(
        Brush.radialGradient(
          colors = listOf(KeyDPadInner, KeyDPadOuter)
        )
      )
      .border(1.2.dp, ChassisBorderHighlight, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    DPadArrowButton(
      key = CalcKey.UP,
      symbol = "▲",
      contentDesc = "Arriba",
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
      onFire = onFire,
    )
    DPadArrowButton(
      key = CalcKey.DOWN,
      symbol = "▼",
      contentDesc = "Abajo",
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
      onFire = onFire,
    )
    DPadArrowButton(
      key = CalcKey.LEFT,
      symbol = "◀",
      contentDesc = "Izquierda",
      modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp),
      onFire = onFire,
    )
    DPadArrowButton(
      key = CalcKey.RIGHT,
      symbol = "▶",
      contentDesc = "Derecha",
      modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
      onFire = onFire,
    )

    var okPressed by remember { mutableStateOf(false) }
    val okScale by animateFloatAsState(if (okPressed) 0.92f else 1f, tween(70), label = "ok")
    Box(
      modifier = Modifier
        .size(40.dp)
        .graphicsLayer(scaleX = okScale, scaleY = okScale)
        .shadow(3.dp, CircleShape)
        .clip(CircleShape)
        .background(Brush.verticalGradient(listOf(KeyOkTop, KeyOkBottom)))
        .border(1.dp, Color(0xFF6B7587), CircleShape)
        .testTag("key_ok")
        .semantics {
          contentDescription = "OK"
          role = Role.Button
        }
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
              okPressed = true
              tryAwaitRelease()
              okPressed = false
            },
            onTap = { onFire(CalcKey.OK) }
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "OK",
        color = LegendPrimary,
        fontSize = 11.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun DPadArrowButton(
  key: CalcKey,
  symbol: String,
  contentDesc: String,
  modifier: Modifier,
  onFire: (CalcKey) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (pressed) 0.86f else 1f, tween(60), label = "dpad")
  Box(
    modifier = modifier
      .size(29.dp)
      .graphicsLayer(scaleX = scale, scaleY = scale)
      .clip(CircleShape)
      .background(if (pressed) Color(0x33FFFFFF) else Color.Transparent)
      .testTag("key_${key.name.lowercase()}")
      .semantics {
        contentDescription = contentDesc
        role = Role.Button
      }
      .pointerInput(key) {
        detectTapGestures(
          onPress = {
            pressed = true
            tryAwaitRelease()
            pressed = false
          },
          onTap = { onFire(key) }
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Text(symbol, color = LegendPrimary, fontSize = 11.sp)
  }
}

@Composable
private fun SystemPillKey(
  key: CalcKey,
  label: String,
  icon: ImageVector?,
  modifier: Modifier = Modifier,
  height: Dp = 32.dp,
  onFire: (CalcKey) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(60), label = "sys")
  Box(
    modifier = modifier
      .height(height)
      .graphicsLayer(scaleX = scale, scaleY = scale)
      .shadow(2.dp, RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .background(Brush.verticalGradient(listOf(KeyRoundDarkTop, KeyRoundDarkBottom)))
      .border(0.8.dp, Color(0xFF49515E), RoundedCornerShape(16.dp))
      .testTag("key_${key.name.lowercase()}")
      .semantics {
        contentDescription = label
        role = Role.Button
      }
      .pointerInput(key) {
        detectTapGestures(
          onPress = {
            pressed = true
            tryAwaitRelease()
            pressed = false
          },
          onTap = { onFire(key) }
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (key == CalcKey.ON) BrandCyan else LegendPrimary,
        modifier = Modifier.size(16.dp),
      )
    } else {
      Text(
        text = label,
        color = LegendPrimary,
        fontSize = 8.5.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun UtilityPillKey(
  key: CalcKey,
  shiftLabel: String,
  mainLabel: String,
  modifier: Modifier = Modifier,
  mainSize: TextUnit = 9.5.sp,
  italic: Boolean = false,
  isHighlighted: Boolean = false,
  highlightColor: Color = LegendSecondaryShift,
  mainLabelColor: Color = LegendPrimary,
  onFire: (CalcKey) -> Unit,
) {
  ConvexKeyTemplate(
    testTag = "key_${key.name.lowercase()}",
    shiftLabel = shiftLabel,
    mainLabel = mainLabel,
    mainSize = mainSize,
    keyHeight = 28.dp,
    topColor = KeyRoundDarkTop,
    bottomColor = KeyRoundDarkBottom,
    mainLabelColor = mainLabelColor,
    borderColor = if (isHighlighted) highlightColor else Color(0xFF525A68),
    isCircle = true,
    italic = italic,
    modifier = modifier,
    onTap = { onFire(key) },
  )
}

@Composable
private fun SciKey(
  key: CalcKey,
  shiftLabel: String,
  mainLabel: String,
  modifier: Modifier = Modifier,
  onFire: (CalcKey) -> Unit,
  isHighlighted: Boolean = false,
  highlightColor: Color = LegendSecondaryShift,
  mainLabelColor: Color = LegendPrimary,
) {
  ConvexKeyTemplate(
    testTag = "key_${key.name.lowercase()}",
    shiftLabel = shiftLabel,
    mainLabel = mainLabel,
    mainSize = if (mainLabel.length >= 4) 9.5.sp else 11.5.sp,
    keyHeight = 30.dp,
    topColor = KeyRoundDarkTop,
    bottomColor = KeyRoundDarkBottom,
    mainLabelColor = mainLabelColor,
    borderColor = if (isHighlighted) highlightColor else Color(0xFF474F5B),
    isCircle = false,
    italic = false,
    modifier = modifier,
    onTap = { onFire(key) },
  )
}

@Composable
private fun NumKey(
  key: CalcKey,
  shiftLabel: String,
  mainLabel: String,
  modifier: Modifier = Modifier,
  mainSize: TextUnit = 16.sp,
  topColor: Color = KeyNumDarkTop,
  bottomColor: Color = KeyNumDarkBottom,
  onFire: (CalcKey) -> Unit,
) {
  ConvexKeyTemplate(
    testTag = "key_${key.name.lowercase()}",
    shiftLabel = shiftLabel,
    mainLabel = mainLabel,
    mainSize = mainSize,
    keyHeight = 36.dp,
    topColor = topColor,
    bottomColor = bottomColor,
    mainLabelColor = LegendPrimary,
    borderColor = Color(0xFF525A68),
    isCircle = true,
    italic = false,
    modifier = modifier,
    onTap = { onFire(key) },
  )
}

@Composable
private fun ConvexKeyTemplate(
  testTag: String,
  shiftLabel: String,
  mainLabel: String,
  mainSize: TextUnit,
  keyHeight: Dp,
  topColor: Color,
  bottomColor: Color,
  mainLabelColor: Color,
  borderColor: Color,
  isCircle: Boolean,
  italic: Boolean = false,
  modifier: Modifier = Modifier,
  onTap: () -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(60), label = "key")
  val shape = if (isCircle) RoundedCornerShape(18.dp) else RoundedCornerShape(13.dp)

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = shiftLabel.ifEmpty { " " },
      color = LegendSecondaryShift,
      fontSize = 8.5.sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      lineHeight = 9.sp,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(1.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(keyHeight)
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .shadow(if (pressed) 1.dp else 3.dp, shape)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            colors = if (pressed) listOf(bottomColor, topColor) else listOf(topColor, bottomColor)
          )
        )
        .border(0.9.dp, borderColor, shape)
        .testTag(testTag)
        .semantics {
          contentDescription = mainLabel
          role = Role.Button
        }
        .pointerInput(testTag) {
          detectTapGestures(
            onPress = {
              pressed = true
              tryAwaitRelease()
              pressed = false
            },
            onTap = { onTap() }
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      if (mainLabel == "⌫") {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.Backspace,
          contentDescription = "Borrar",
          tint = mainLabelColor,
          modifier = Modifier.size(16.dp),
        )
      } else {
        Text(
          text = mainLabel,
          color = mainLabelColor,
          fontSize = mainSize,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Bold,
          fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
          maxLines = 1,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
