package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CalcKey

/**
 * Pixel-faithful 48-key physical layout of the Casio ClassWiz fx-991LA CW:
 *  - Upper Navigation + Cross D-Pad + Command Bar (6 cols × 3 rows interlocked)
 *    Row 1: ON | HOME | (gap) | ∧ | (gap) | PgUp (top of vertical pill)
 *    Row 2: CONFIG | BACK | < | OK | > | PgDn (bottom of vertical pill)
 *    Row 3: SHIFT (gold ⇧) | VARIABLE (⇄[x]) | FUNCTION (f(x)) | ∨ | CATALOG (book) | TOOLS (○○○)
 *  - Scientific Circular Matrix (2 rows × 6 cols = 12 keys, every key with top-left yellow SHIFT + └)
 *    Row 1: x (QR) | ■/□ (□ ■/□) | √■ (ˣ√□) | ■^□ (■⁻¹) | ■² (log) | log_■□ (ln)
 *    Row 2: Ans (PreAns) | sen (Arc sen) | cos (Arc cos) | tan (Arc tan) | ( (=) | ) (;)
 *  - Numeric & Operator Circular Pad (4 rows × 5 cols = 20 keys, all yellow SHIFT legends + └)
 *    Row 1: 7(π) | 8(e) | 9(i) | ⌫(INS) | AC(OFF)
 *    Row 2: 4(A) | 5(B) | 6(C) | × | ÷(└)
 *    Row 3: 1(D) | 2(E) | 3(F) | +(° ' ") | −((−))
 *    Row 4: 0(x) | ,(y) | ×10^■(z) | FORMAT | EXE(≈)
 */
@Composable
fun CalculatorKeypad(
  isShiftActive: Boolean,
  hapticsEnabled: Boolean,
  onKey: (CalcKey) -> Unit,
  keyScale: Float = 1f,
  modifier: Modifier = Modifier,
) {
  val view = LocalView.current
  val sc = keyScale.coerceIn(0.75f, 1.08f)

  fun fire(key: CalcKey) {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    onKey(key)
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy((4f * sc).dp),
  ) {
    // 1. Interlocked Upper Navigation + Cross D-Pad + Command Bar (16 keys)
    UpperNavAndCommandSection(
      isShiftActive = isShiftActive,
      sc = sc,
      onFire = ::fire,
    )

    // 2. Scientific Circular Matrix (2 rows × 6 columns = 12 keys)
    ScientificCircularSection(sc = sc, onFire = ::fire)

    Spacer(Modifier.height((1.5f * sc).dp))

    // 3. Lower Numeric & Operator Circular Pad (4 rows × 5 columns = 20 keys)
    NumericOperatorCircularSection(sc = sc, onFire = ::fire)
  }
}

// =================================================================================================
// SECTION 1: UPPER NAVIGATION + CROSS D-PAD + COMMAND ROW (EXACT PHOTO GEOMETRY)
// =================================================================================================

@Composable
private fun UpperNavAndCommandSection(
  isShiftActive: Boolean,
  sc: Float,
  onFire: (CalcKey) -> Unit,
) {
  val roundSize = (36f * sc).dp
  val smallOnSize = (30f * sc).dp
  val labelHeight = (12f * sc).dp
  val colGap = 4.dp

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy((3f * sc).dp),
  ) {
    // Top 2 rows: Cols 1–5 on left (weight 5f), Col 6 vertical PgUp/PgDn pill on right (weight 1f)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(colGap),
    ) {
      Column(
        modifier = Modifier.weight(5f),
        verticalArrangement = Arrangement.spacedBy((3f * sc).dp),
      ) {
        // Row A: ON | HOME | (empty) | D-Pad UP (∧) | (empty)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(colGap),
        ) {
          // Col 1: ON
          LabeledRoundNavCell(
            topLabel = "ON",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.ON,
              contentDesc = "ON",
              size = smallOnSize,
              onFire = onFire,
            ) {
              OnPowerNotchGlyph(sc = sc)
            }
          }

          // Col 2: HOME
          LabeledRoundNavCell(
            topLabel = "HOME",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.HOME,
              contentDesc = "HOME",
              size = roundSize,
              onFire = onFire,
            ) {
              Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                tint = LegendPrimary,
                modifier = Modifier.size((17f * sc).dp),
              )
            }
          }

          // Col 3: empty above LEFT
          Spacer(Modifier.weight(1f))

          // Col 4: D-Pad UP (∧)
          LabeledRoundNavCell(
            topLabel = "",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.UP,
              contentDesc = "Arriba",
              size = roundSize,
              onFire = onFire,
            ) {
              DPadChevronGlyph(direction = 0, sc = sc)
            }
          }

          // Col 5: empty above RIGHT
          Spacer(Modifier.weight(1f))
        }

        // Row B: CONFIG | BACK | D-Pad LEFT (<) | D-Pad OK | D-Pad RIGHT (>)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(colGap),
        ) {
          // Col 1: CONFIG (3 horizontal sliders icon)
          LabeledRoundNavCell(
            topLabel = "CONFIG",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.SETTINGS,
              contentDesc = "CONFIG",
              size = roundSize,
              onFire = onFire,
            ) {
              ConfigSlidersGlyph(sc = sc)
            }
          }

          // Col 2: BACK (curved return arrow, no label above)
          LabeledRoundNavCell(
            topLabel = "",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.BACK,
              contentDesc = "BACK",
              size = roundSize,
              onFire = onFire,
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = null,
                tint = LegendPrimary,
                modifier = Modifier.size((17f * sc).dp),
              )
            }
          }

          // Col 3: D-Pad LEFT (<)
          LabeledRoundNavCell(
            topLabel = "",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.LEFT,
              contentDesc = "Izquierda",
              size = roundSize,
              onFire = onFire,
            ) {
              DPadChevronGlyph(direction = 3, sc = sc)
            }
          }

          // Col 4: D-Pad OK
          LabeledRoundNavCell(
            topLabel = "",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.OK,
              contentDesc = "OK",
              size = roundSize,
              topColor = KeyOkTop,
              bottomColor = KeyOkBottom,
              borderColor = KeyOkBorder,
              onFire = onFire,
            ) {
              Text(
                text = "OK",
                color = LegendPrimary,
                fontSize = (11.5f * sc).sp,
                fontFamily = IbmPlexSansFamily,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          // Col 5: D-Pad RIGHT (>)
          LabeledRoundNavCell(
            topLabel = "",
            labelHeight = labelHeight,
            sc = sc,
            modifier = Modifier.weight(1f),
          ) {
            RoundHardwareButton(
              key = CalcKey.RIGHT,
              contentDesc = "Derecha",
              size = roundSize,
              onFire = onFire,
            ) {
              DPadChevronGlyph(direction = 1, sc = sc)
            }
          }
        }
      }

      // Col 6: Vertical PgUp / PgDn Capsule spanning Rows A & B
      Box(
        modifier = Modifier
          .weight(1f)
          .padding(top = (8f * sc).dp),
        contentAlignment = Alignment.Center,
      ) {
        PageRockerCapsule(sc = sc, onFire = onFire)
      }
    }

    // Row C (6 cols): SHIFT (gold ⇧) | VARIABLE (⇄[x]) | FUNCTION (f(x)) | D-Pad DOWN (∨) | CATALOG | TOOLS
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(colGap),
    ) {
      // Col 1: SHIFT (Champagne-gold circular button with hollow up-arrow)
      LabeledRoundNavCell(
        topLabel = "SHIFT",
        topLabelColor = LegendSecondaryShift,
        labelHeight = labelHeight,
        sc = sc,
        modifier = Modifier.weight(1f),
      ) {
        RoundHardwareButton(
          key = CalcKey.SHIFT,
          contentDesc = "SHIFT",
          size = roundSize,
          topColor = if (isShiftActive) KeyShiftGoldActiveTop else KeyShiftGoldTop,
          bottomColor = KeyShiftGoldBottom,
          borderColor = if (isShiftActive) KeyShiftActive else KeyShiftGoldBorder,
          onFire = onFire,
        ) {
          ShiftHollowArrowGlyph(sc = sc)
        }
      }

      // Col 2: VARIABLE (⇄[x])
      LabeledRoundNavCell(
        topLabel = "VARIABLE",
        labelHeight = labelHeight,
        sc = sc,
        modifier = Modifier.weight(1f),
      ) {
        RoundHardwareButton(
          key = CalcKey.VARIABLE,
          contentDesc = "VARIABLE",
          size = roundSize,
          onFire = onFire,
        ) {
          VariableBoxGlyph(sc = sc)
        }
      }

      // Col 3: FUNCTION (f(x))
      LabeledRoundNavCell(
        topLabel = "FUNCTION",
        labelHeight = labelHeight,
        sc = sc,
        modifier = Modifier.weight(1f),
      ) {
        RoundHardwareButton(
          key = CalcKey.FX,
          contentDesc = "FUNCTION",
          size = roundSize,
          onFire = onFire,
        ) {
          Text(
            text = "f(x)",
            color = LegendPrimary,
            fontSize = (12.5f * sc).sp,
            fontFamily = IbmPlexSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
          )
        }
      }

      // Col 4: D-Pad DOWN (∨) — interlocked between FUNCTION and CATALOG!
      LabeledRoundNavCell(
        topLabel = "",
        labelHeight = labelHeight,
        sc = sc,
        modifier = Modifier.weight(1f),
      ) {
        RoundHardwareButton(
          key = CalcKey.DOWN,
          contentDesc = "Abajo",
          size = roundSize,
          onFire = onFire,
        ) {
          DPadChevronGlyph(direction = 2, sc = sc)
        }
      }

      // Col 5: CATALOG (open book icon)
      LabeledRoundNavCell(
        topLabel = "CATALOG",
        labelHeight = labelHeight,
        sc = sc,
        modifier = Modifier.weight(1f),
      ) {
        RoundHardwareButton(
          key = CalcKey.CATALOG,
          contentDesc = "CATALOG",
          size = roundSize,
          onFire = onFire,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = LegendPrimary,
            modifier = Modifier.size((17f * sc).dp),
          )
        }
      }

      // Col 6: TOOLS (three horizontal hollow circles ○ ○ ○)
      LabeledRoundNavCell(
        topLabel = "TOOLS",
        labelHeight = labelHeight,
        sc = sc,
        modifier = Modifier.weight(1f),
      ) {
        RoundHardwareButton(
          key = CalcKey.TOOLS,
          contentDesc = "TOOLS",
          size = roundSize,
          onFire = onFire,
        ) {
          ToolsThreeCirclesGlyph(sc = sc)
        }
      }
    }
  }
}

@Composable
private fun LabeledRoundNavCell(
  topLabel: String,
  labelHeight: Dp,
  sc: Float,
  modifier: Modifier = Modifier,
  topLabelColor: Color = LegendPrimary.copy(alpha = 0.92f),
  content: @Composable () -> Unit,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom,
  ) {
    Box(
      modifier = Modifier.height(labelHeight),
      contentAlignment = Alignment.BottomCenter,
    ) {
      if (topLabel.isNotEmpty()) {
        Text(
          text = topLabel,
          color = topLabelColor,
          fontSize = (7.2f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.2.sp,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
    Spacer(Modifier.height(1.5.dp))
    content()
  }
}

// =================================================================================================
// SECTION 2: SCIENTIFIC CIRCULAR MATRIX (2 ROWS × 6 COLUMNS = 12 KEYS)
// =================================================================================================

@Composable
private fun ScientificCircularSection(sc: Float, onFire: (CalcKey) -> Unit) {
  val colGap = 4.dp
  val rowGap = (3.5f * sc).dp

  Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
    // Sci Row 1: x(QR) | ■/□(□ ■/□) | √■(ˣ√□) | ■^□(■⁻¹) | ■²(log) | log_■□(ln)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(colGap),
    ) {
      ShiftedCircularKey(
        key = CalcKey.VAR_X,
        contentDesc = "x",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTextLegend("QR", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "x",
          color = LegendPrimary,
          fontSize = (15f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
          fontStyle = FontStyle.Italic,
        )
      }

      ShiftedCircularKey(
        key = CalcKey.FRAC,
        contentDesc = "Fracción",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftMixedFractionLegend(sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        FractionBoxGlyph(sc = sc)
      }

      ShiftedCircularKey(
        key = CalcKey.SQRT,
        contentDesc = "Raíz cuadrada",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftNthRootLegend(sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        SqrtBoxGlyph(sc = sc)
      }

      ShiftedCircularKey(
        key = CalcKey.POWER,
        contentDesc = "Potencia",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftReciprocalLegend(sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        PowerBoxGlyph(sc = sc)
      }

      ShiftedCircularKey(
        key = CalcKey.SQUARE,
        contentDesc = "Cuadrado",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTextLegend("log", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        SquareBoxGlyph(sc = sc)
      }

      ShiftedCircularKey(
        key = CalcKey.LOG_BASE,
        contentDesc = "Logaritmo base",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTextLegend("ln", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        LogBaseBoxGlyph(sc = sc)
      }
    }

    // Sci Row 2: Ans(PreAns) | sen(Arc sen) | cos(Arc cos) | tan(Arc tan) | ( (=) | ) (;)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(colGap),
    ) {
      ShiftedCircularKey(
        key = CalcKey.ANS,
        contentDesc = "Ans",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTwoLineLegend("Pre", "Ans", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "Ans",
          color = LegendPrimary,
          fontSize = (11.5f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
        )
      }

      ShiftedCircularKey(
        key = CalcKey.SIN,
        contentDesc = "Seno",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTwoLineLegend("Arc", "sen", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "sen",
          color = LegendPrimary,
          fontSize = (12f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
        )
      }

      ShiftedCircularKey(
        key = CalcKey.COS,
        contentDesc = "Coseno",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTwoLineLegend("Arc", "cos", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "cos",
          color = LegendPrimary,
          fontSize = (12f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
        )
      }

      ShiftedCircularKey(
        key = CalcKey.TAN,
        contentDesc = "Tangente",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTwoLineLegend("Arc", "tan", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "tan",
          color = LegendPrimary,
          fontSize = (12f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
        )
      }

      ShiftedCircularKey(
        key = CalcKey.PAREN_OPEN,
        contentDesc = "Paréntesis izquierdo",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTextLegend("=", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "(",
          color = LegendPrimary,
          fontSize = (14.5f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
        )
      }

      ShiftedCircularKey(
        key = CalcKey.PAREN_CLOSE,
        contentDesc = "Paréntesis derecho",
        sc = sc,
        keyHeight = (33f * sc).dp,
        shiftHeader = { ShiftTextLegend(";", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = ")",
          color = LegendPrimary,
          fontSize = (14.5f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

// =================================================================================================
// SECTION 3: NUMERIC & OPERATOR CIRCULAR PAD (4 ROWS × 5 COLUMNS = 20 KEYS)
// =================================================================================================

@Composable
private fun NumericOperatorCircularSection(sc: Float, onFire: (CalcKey) -> Unit) {
  val colGap = 6.dp
  val rowGap = (3.5f * sc).dp
  val numKeyH = (40f * sc).dp

  Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
    // Row 1: 7(π) | 8(e) | 9(i) | ⌫(INS) | AC(OFF)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colGap)) {
      NumShiftKey(CalcKey.D7, "π", "7", Modifier.weight(1f), numKeyH, sc, onFire, italicShift = true)
      NumShiftKey(CalcKey.D8, "e", "8", Modifier.weight(1f), numKeyH, sc, onFire, italicShift = true)
      NumShiftKey(CalcKey.D9, "i", "9", Modifier.weight(1f), numKeyH, sc, onFire, italicShift = true)
      ShiftedCircularKey(
        key = CalcKey.DEL,
        contentDesc = "Borrar",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftTextLegend("INS", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        FilledBackspaceTagGlyph(sc = sc)
      }
      ShiftedCircularKey(
        key = CalcKey.AC,
        contentDesc = "AC",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftTextLegend("OFF", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "AC",
          color = LegendPrimary,
          fontSize = (15.5f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    // Row 2: 4(A) | 5(B) | 6(C) | × | ÷(└)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colGap)) {
      NumShiftKey(CalcKey.D4, "A", "4", Modifier.weight(1f), numKeyH, sc, onFire)
      NumShiftKey(CalcKey.D5, "B", "5", Modifier.weight(1f), numKeyH, sc, onFire)
      NumShiftKey(CalcKey.D6, "C", "6", Modifier.weight(1f), numKeyH, sc, onFire)
      ShiftedCircularKey(
        key = CalcKey.MUL,
        contentDesc = "Multiplicar",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = null,
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "×",
          color = LegendPrimary,
          fontSize = (21f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
        )
      }
      ShiftedCircularKey(
        key = CalcKey.DIV,
        contentDesc = "Dividir",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftRemainderDivLegend(sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "÷",
          color = LegendPrimary,
          fontSize = (21f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    // Row 3: 1(D) | 2(E) | 3(F) | +(° ' ") | −((−))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colGap)) {
      NumShiftKey(CalcKey.D1, "D", "1", Modifier.weight(1f), numKeyH, sc, onFire)
      NumShiftKey(CalcKey.D2, "E", "2", Modifier.weight(1f), numKeyH, sc, onFire)
      NumShiftKey(CalcKey.D3, "F", "3", Modifier.weight(1f), numKeyH, sc, onFire)
      ShiftedCircularKey(
        key = CalcKey.ADD,
        contentDesc = "Sumar",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftTextLegend("° ' \"", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "+",
          color = LegendPrimary,
          fontSize = (21f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
        )
      }
      ShiftedCircularKey(
        key = CalcKey.SUB,
        contentDesc = "Restar",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftTextLegend("(−)", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "−",
          color = LegendPrimary,
          fontSize = (21f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    // Row 4: 0(x) | ,(y) | ×10^■(z) | FORMAT (curved arrows) | EXE(≈)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colGap)) {
      NumShiftKey(CalcKey.D0, "x", "0", Modifier.weight(1f), numKeyH, sc, onFire, italicShift = true)
      NumShiftKey(CalcKey.DOT, "y", ",", Modifier.weight(1f), numKeyH, sc, onFire, italicShift = true, mainFontSizeSp = 21f)
      ShiftedCircularKey(
        key = CalcKey.EXP10,
        contentDesc = "Por diez a la x",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftTextLegend("z", sc = sc, italic = true) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Exp10BoxGlyph(sc = sc)
      }
      ShiftedCircularKey(
        key = CalcKey.FORMAT,
        contentDesc = "FORMAT",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = null,
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        FormatCircularArrowsGlyph(sc = sc)
      }
      ShiftedCircularKey(
        key = CalcKey.EXE,
        contentDesc = "EXE",
        sc = sc,
        keyHeight = numKeyH,
        topColor = KeyNumDarkTop,
        bottomColor = KeyNumDarkBottom,
        borderColor = KeyNumBorder,
        shiftHeader = { ShiftTextLegend("≈", sc = sc) },
        modifier = Modifier.weight(1f),
        onFire = onFire,
      ) {
        Text(
          text = "EXE",
          color = LegendPrimary,
          fontSize = (14.5f * sc).sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.4.sp,
        )
      }
    }
  }
}

@Composable
private fun NumShiftKey(
  key: CalcKey,
  shiftLabel: String,
  mainLabel: String,
  modifier: Modifier,
  keyHeight: Dp,
  sc: Float,
  onFire: (CalcKey) -> Unit,
  italicShift: Boolean = false,
  mainFontSizeSp: Float = 18.5f,
) {
  ShiftedCircularKey(
    key = key,
    contentDesc = mainLabel,
    sc = sc,
    keyHeight = keyHeight,
    topColor = KeyNumDarkTop,
    bottomColor = KeyNumDarkBottom,
    borderColor = KeyNumBorder,
    shiftHeader = { ShiftTextLegend(shiftLabel, sc = sc, italic = italicShift) },
    modifier = modifier,
    onFire = onFire,
  ) {
    Text(
      text = mainLabel,
      color = LegendPrimary,
      fontSize = (mainFontSizeSp * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.Medium,
    )
  }
}

// =================================================================================================
// SHARED HARDWARE KEY TEMPLATES & CASIO TOP-LEFT YELLOW SHIFT + └ ELBOW LEGENDS
// =================================================================================================

@Composable
private fun ShiftedCircularKey(
  key: CalcKey,
  contentDesc: String,
  sc: Float,
  keyHeight: Dp,
  shiftHeader: (@Composable () -> Unit)?,
  modifier: Modifier = Modifier,
  topColor: Color = KeyRoundDarkTop,
  bottomColor: Color = KeyRoundDarkBottom,
  borderColor: Color = KeySciBorder,
  onFire: (CalcKey) -> Unit,
  content: @Composable BoxScope.() -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(55), label = "key")
  val shape = CircleShape

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top-left aligned Casio yellow SHIFT legend + tiny yellow └ elbow indicator
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height((15.5f * sc).dp),
      contentAlignment = Alignment.BottomStart,
    ) {
      if (shiftHeader != null) {
        shiftHeader()
      }
    }

    // Recessed dark socket well + circular dome keycap
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(keyHeight + 3.dp),
      contentAlignment = Alignment.Center,
    ) {
      val capWidth = keyHeight + 4.dp
      Box(
        modifier = Modifier
          .widthIn(max = capWidth)
          .fillMaxWidth()
          .height(keyHeight + 3.dp)
          .clip(RoundedCornerShape(50))
          .background(KeySocketWell)
          .padding(1.5.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(if (pressed) 1.dp else 3.dp, shape)
            .clip(RoundedCornerShape(50))
            .background(
              Brush.verticalGradient(
                colors = if (pressed) listOf(bottomColor, topColor) else listOf(topColor, bottomColor)
              )
            )
            .border(0.9.dp, borderColor, RoundedCornerShape(50))
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
                onTap = { onFire(key) },
              )
            },
          contentAlignment = Alignment.Center,
          content = content,
        )
      }
    }
  }
}

@Composable
private fun RoundHardwareButton(
  key: CalcKey,
  contentDesc: String,
  size: Dp,
  topColor: Color = KeyRoundDarkTop,
  bottomColor: Color = KeyRoundDarkBottom,
  borderColor: Color = KeySciBorder,
  onFire: (CalcKey) -> Unit,
  content: @Composable BoxScope.() -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (pressed) 0.91f else 1f, tween(55), label = "round")

  Box(
    modifier = Modifier
      .size(size + 3.dp)
      .clip(CircleShape)
      .background(KeySocketWell),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(size)
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .shadow(if (pressed) 1.dp else 3.dp, CircleShape)
        .clip(CircleShape)
        .background(
          Brush.verticalGradient(
            if (pressed) listOf(bottomColor, topColor) else listOf(topColor, bottomColor)
          )
        )
        .border(0.95.dp, borderColor, CircleShape)
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
            onTap = { onFire(key) },
          )
        },
      contentAlignment = Alignment.Center,
      content = content,
    )
  }
}

@Composable
private fun PageRockerCapsule(sc: Float, onFire: (CalcKey) -> Unit) {
  val width = (35f * sc).dp
  val halfH = (38f * sc).dp

  Column(
    modifier = Modifier
      .width(width + 4.dp)
      .clip(RoundedCornerShape(50))
      .background(KeySocketWell)
      .padding(2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(1.5.dp),
  ) {
    RockerHalfButton(
      key = CalcKey.PAGE_UP,
      contentDesc = "Página arriba",
      icon = Icons.Rounded.KeyboardDoubleArrowUp,
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
      width = width,
      height = halfH,
      sc = sc,
      onFire = onFire,
    )
    RockerHalfButton(
      key = CalcKey.PAGE_DOWN,
      contentDesc = "Página abajo",
      icon = Icons.Rounded.KeyboardDoubleArrowDown,
      shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
      width = width,
      height = halfH,
      sc = sc,
      onFire = onFire,
    )
  }
}

@Composable
private fun RockerHalfButton(
  key: CalcKey,
  contentDesc: String,
  icon: ImageVector,
  shape: RoundedCornerShape,
  width: Dp,
  height: Dp,
  sc: Float,
  onFire: (CalcKey) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(55), label = "rocker")

  Box(
    modifier = Modifier
      .width(width)
      .height(height)
      .graphicsLayer(scaleX = scale, scaleY = scale)
      .clip(shape)
      .background(
        Brush.verticalGradient(
          if (pressed) listOf(KeyRoundDarkBottom, KeyRoundDarkTop)
          else listOf(KeyRoundDarkTop, KeyRoundDarkBottom)
        )
      )
      .border(0.85.dp, KeySciBorder, shape)
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
          onTap = { onFire(key) },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = LegendPrimary,
      modifier = Modifier.size((18f * sc).dp),
    )
  }
}

// =================================================================================================
// YELLOW SHIFT LEGEND COMPONENTS (WITH SIGNATURE CASIO └ ELBOW BRACKET)
// =================================================================================================

@Composable
private fun ShiftElbowMark(sc: Float) {
  Canvas(modifier = Modifier.size((5f * sc).dp, (4.5f * sc).dp)) {
    val sw = 1.dp.toPx()
    drawLine(LegendSecondaryShift, Offset(sw / 2f, 0f), Offset(sw / 2f, size.height - sw / 2f), strokeWidth = sw)
    drawLine(LegendSecondaryShift, Offset(sw / 2f, size.height - sw / 2f), Offset(size.width, size.height - sw / 2f), strokeWidth = sw)
  }
}

@Composable
private fun ShiftTextLegend(text: String, sc: Float, italic: Boolean = false) {
  Column(
    modifier = Modifier.padding(start = 1.dp),
    horizontalAlignment = Alignment.Start,
  ) {
    Text(
      text = text,
      color = LegendSecondaryShift,
      fontSize = (8.4f * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.SemiBold,
      fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
      lineHeight = (8.8f * sc).sp,
      maxLines = 1,
      softWrap = false,
    )
    ShiftElbowMark(sc = sc)
  }
}

@Composable
private fun ShiftTwoLineLegend(line1: String, line2: String, sc: Float) {
  Row(
    modifier = Modifier.padding(start = 1.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    Column(horizontalAlignment = Alignment.Start) {
      Text(
        text = line1,
        color = LegendSecondaryShift,
        fontSize = (6.8f * sc).sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (7f * sc).sp,
        maxLines = 1,
      )
      Text(
        text = line2,
        color = LegendSecondaryShift,
        fontSize = (7.2f * sc).sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (7.4f * sc).sp,
        maxLines = 1,
      )
    }
    Spacer(Modifier.width(1.dp))
    ShiftElbowMark(sc = sc)
  }
}

@Composable
private fun ShiftMixedFractionLegend(sc: Float) {
  Column(modifier = Modifier.padding(start = 1.dp)) {
    Canvas(modifier = Modifier.size((15f * sc).dp, (10f * sc).dp)) {
      val sw = 0.9.dp.toPx()
      val sq = size.height * 0.36f
      // Whole outlined box on left
      drawRect(LegendSecondaryShift, Offset(0f, (size.height - sq) / 2f), Size(sq, sq), style = Stroke(sw))
      // Fraction numerator filled box
      val fx = sq + 2.5.dp.toPx()
      drawRect(LegendSecondaryShift, Offset(fx, 0f), Size(sq, sq))
      // Bar
      drawLine(LegendSecondaryShift, Offset(fx - 1.dp.toPx(), size.height / 2f), Offset(fx + sq + 1.dp.toPx(), size.height / 2f), strokeWidth = sw)
      // Denominator outlined box
      drawRect(LegendSecondaryShift, Offset(fx, size.height - sq), Size(sq, sq), style = Stroke(sw))
    }
    ShiftElbowMark(sc = sc)
  }
}

@Composable
private fun ShiftNthRootLegend(sc: Float) {
  Column(modifier = Modifier.padding(start = 1.dp)) {
    Canvas(modifier = Modifier.size((15f * sc).dp, (10f * sc).dp)) {
      val sw = 1.dp.toPx()
      // Index small box
      val idxSq = size.height * 0.32f
      drawRect(LegendSecondaryShift, Offset(0f, 0.5.dp.toPx()), Size(idxSq, idxSq), style = Stroke(sw * 0.85f))
      // Radical hook
      val rx = idxSq + 1.dp.toPx()
      val path = Path().apply {
        moveTo(rx, size.height * 0.55f)
        lineTo(rx + 2.dp.toPx(), size.height * 0.9f)
        lineTo(rx + 4.5.dp.toPx(), 1.dp.toPx())
        lineTo(size.width, 1.dp.toPx())
      }
      drawPath(path, LegendSecondaryShift, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
      // Radicand box
      val radSq = size.height * 0.44f
      drawRect(LegendSecondaryShift, Offset(rx + 5.5.dp.toPx(), size.height - radSq - 0.5.dp.toPx()), Size(radSq, radSq), style = Stroke(sw * 0.85f))
    }
    ShiftElbowMark(sc = sc)
  }
}

@Composable
private fun ShiftReciprocalLegend(sc: Float) {
  Column(modifier = Modifier.padding(start = 1.dp)) {
    Row(verticalAlignment = Alignment.Top) {
      Canvas(modifier = Modifier.size((5f * sc).dp, (8f * sc).dp)) {
        drawRect(LegendSecondaryShift, Offset(0f, size.height - size.width), Size(size.width, size.width))
      }
      Text(
        text = "⁻¹",
        color = LegendSecondaryShift,
        fontSize = (7.2f * sc).sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Bold,
        lineHeight = (7.5f * sc).sp,
      )
    }
    ShiftElbowMark(sc = sc)
  }
}

@Composable
private fun ShiftRemainderDivLegend(sc: Float) {
  Column(modifier = Modifier.padding(start = 1.dp)) {
    // Authentic Latin American Casio └ division-with-remainder bracket symbol
    Canvas(modifier = Modifier.size((11f * sc).dp, (8f * sc).dp)) {
      val sw = 1.5.dp.toPx()
      drawLine(LegendSecondaryShift, Offset(sw / 2f, 1.dp.toPx()), Offset(sw / 2f, size.height - 1.dp.toPx()), strokeWidth = sw)
      drawLine(LegendSecondaryShift, Offset(sw / 2f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = sw)
    }
    Spacer(Modifier.height(1.dp))
    ShiftElbowMark(sc = sc)
  }
}

// =================================================================================================
// CUSTOM HARDWARE VECTOR GLYPHS (EXACT CASIO fx-991LA CW KEYCAP ARTWORK)
// =================================================================================================

@Composable
private fun OnPowerNotchGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((12f * sc).dp)) {
    val sw = 1.3.dp.toPx()
    drawLine(
      color = LegendMuted.copy(alpha = 0.7f),
      start = Offset(size.width / 2f, 0f),
      end = Offset(size.width / 2f, size.height * 0.42f),
      strokeWidth = sw,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
private fun ConfigSlidersGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((16f * sc).dp, (13f * sc).dp)) {
    val sw = 1.3.dp.toPx()
    val ys = listOf(size.height * 0.2f, size.height * 0.5f, size.height * 0.8f)
    val knobs = listOf(0.68f, 0.32f, 0.62f)
    ys.forEachIndexed { idx, y ->
      drawLine(LegendPrimary, Offset(0f, y), Offset(size.width, y), strokeWidth = sw, cap = StrokeCap.Round)
      drawCircle(LegendPrimary, radius = 2.1.dp.toPx(), center = Offset(size.width * knobs[idx], y))
    }
  }
}

@Composable
private fun DPadChevronGlyph(direction: Int, sc: Float) {
  // 0 = UP, 1 = RIGHT, 2 = DOWN, 3 = LEFT
  Canvas(modifier = Modifier.size((12f * sc).dp)) {
    val sw = 1.8.dp.toPx()
    val w = size.width
    val h = size.height
    val path = Path().apply {
      when (direction) {
        0 -> { moveTo(w * 0.15f, h * 0.68f); lineTo(w * 0.5f, h * 0.3f); lineTo(w * 0.85f, h * 0.68f) }
        1 -> { moveTo(w * 0.32f, h * 0.15f); lineTo(w * 0.7f, h * 0.5f); lineTo(w * 0.32f, h * 0.85f) }
        2 -> { moveTo(w * 0.15f, h * 0.32f); lineTo(w * 0.5f, h * 0.7f); lineTo(w * 0.85f, h * 0.32f) }
        else -> { moveTo(w * 0.68f, h * 0.15f); lineTo(w * 0.3f, h * 0.5f); lineTo(w * 0.68f, h * 0.85f) }
      }
    }
    drawPath(path, LegendPrimary, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

@Composable
private fun ShiftHollowArrowGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((15f * sc).dp, (16f * sc).dp)) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
      moveTo(w * 0.5f, h * 0.08f)
      lineTo(w * 0.90f, h * 0.48f)
      lineTo(w * 0.66f, h * 0.48f)
      lineTo(w * 0.66f, h * 0.92f)
      lineTo(w * 0.34f, h * 0.92f)
      lineTo(w * 0.34f, h * 0.48f)
      lineTo(w * 0.10f, h * 0.48f)
      close()
    }
    drawPath(path, KeyShiftGoldIcon, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
  }
}

@Composable
private fun VariableBoxGlyph(sc: Float) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = "⇄",
      color = LegendPrimary,
      fontSize = (9.5f * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.Bold,
    )
    Box(
      modifier = Modifier
        .border(1.dp, LegendPrimary, RoundedCornerShape(2.dp))
        .padding(horizontal = 3.dp, vertical = 0.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "x",
        color = LegendPrimary,
        fontSize = (10f * sc).sp,
        fontFamily = IbmPlexSansFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun ToolsThreeCirclesGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((19f * sc).dp, (8f * sc).dp)) {
    val r = size.height * 0.33f
    val cy = size.height / 2f
    val xs = listOf(size.width * 0.18f, size.width * 0.5f, size.width * 0.82f)
    xs.forEach { x ->
      drawCircle(LegendPrimary, radius = r, center = Offset(x, cy), style = Stroke(1.3.dp.toPx()))
    }
  }
}

@Composable
private fun FractionBoxGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((14f * sc).dp, (16f * sc).dp)) {
    val boxSize = size.width * 0.46f
    val left = (size.width - boxSize) / 2f
    val sw = 1.25.dp.toPx()
    drawRoundRect(LegendPrimary, Offset(left, 1.dp.toPx()), Size(boxSize, boxSize), CornerRadius(1.dp.toPx()))
    drawLine(LegendPrimary, Offset(1.dp.toPx(), size.height / 2f), Offset(size.width - 1.dp.toPx(), size.height / 2f), strokeWidth = sw)
    drawRoundRect(LegendPrimary, Offset(left, size.height - boxSize - 1.dp.toPx()), Size(boxSize, boxSize), CornerRadius(1.dp.toPx()), style = Stroke(sw))
  }
}

@Composable
private fun SqrtBoxGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((18f * sc).dp, (13f * sc).dp)) {
    val sw = 1.4.dp.toPx()
    val path = Path().apply {
      moveTo(1.dp.toPx(), size.height * 0.56f)
      lineTo(3.5.dp.toPx(), size.height * 0.90f)
      lineTo(7.dp.toPx(), 1.5.dp.toPx())
      lineTo(size.width - 1.dp.toPx(), 1.5.dp.toPx())
    }
    drawPath(path, LegendPrimary, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val sq = size.height * 0.48f
    drawRoundRect(
      LegendPrimary,
      Offset(size.width - sq - 2.dp.toPx(), size.height - sq - 1.dp.toPx()),
      Size(sq, sq),
      CornerRadius(1.dp.toPx()),
    )
  }
}

@Composable
private fun PowerBoxGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((15f * sc).dp, (14f * sc).dp)) {
    val big = size.width * 0.48f
    val small = size.width * 0.30f
    drawRoundRect(LegendPrimary, Offset(1.dp.toPx(), size.height - big - 1.dp.toPx()), Size(big, big), CornerRadius(1.dp.toPx()))
    drawRoundRect(LegendPrimary, Offset(size.width - small - 1.dp.toPx(), 1.dp.toPx()), Size(small, small), CornerRadius(0.8.dp.toPx()), style = Stroke(1.1.dp.toPx()))
  }
}

@Composable
private fun SquareBoxGlyph(sc: Float) {
  Row(verticalAlignment = Alignment.Top) {
    Canvas(modifier = Modifier.size((8f * sc).dp, (13f * sc).dp)) {
      val sq = size.width * 0.92f
      drawRoundRect(LegendPrimary, Offset(0f, size.height - sq - 1.dp.toPx()), Size(sq, sq), CornerRadius(1.dp.toPx()))
    }
    Spacer(Modifier.width(1.dp))
    Text(
      text = "2",
      color = LegendPrimary,
      fontSize = (9.5f * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.Bold,
      lineHeight = (10f * sc).sp,
    )
  }
}

@Composable
private fun LogBaseBoxGlyph(sc: Float) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = "log",
      color = LegendPrimary,
      fontSize = (9.5f * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.SemiBold,
    )
    Canvas(modifier = Modifier.size((10f * sc).dp, (12f * sc).dp)) {
      val subSq = size.width * 0.34f
      val argSq = size.width * 0.46f
      drawRect(LegendPrimary, Offset(0.5.dp.toPx(), size.height - subSq - 0.5.dp.toPx()), Size(subSq, subSq))
      drawRect(LegendPrimary, Offset(size.width - argSq, (size.height - argSq) / 2f), Size(argSq, argSq), style = Stroke(1.dp.toPx()))
    }
  }
}

@Composable
private fun FilledBackspaceTagGlyph(sc: Float) {
  Canvas(modifier = Modifier.size((22f * sc).dp, (14f * sc).dp)) {
    val w = size.width
    val h = size.height
    val tag = Path().apply {
      moveTo(w * 0.28f, 0f)
      lineTo(w * 0.94f, 0f)
      lineTo(w * 0.94f, h)
      lineTo(w * 0.28f, h)
      lineTo(0f, h * 0.5f)
      close()
    }
    drawPath(tag, LegendPrimary)
    // Dark 'x' inside white tag
    val cx = w * 0.60f
    val cy = h * 0.50f
    val r = h * 0.24f
    drawLine(KeyNumDarkBottom, Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(KeyNumDarkBottom, Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
  }
}

@Composable
private fun Exp10BoxGlyph(sc: Float) {
  Row(verticalAlignment = Alignment.Top) {
    Text(
      text = "×10",
      color = LegendPrimary,
      fontSize = (13.5f * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
    )
    Canvas(modifier = Modifier.size((5.5f * sc).dp, (8f * sc).dp)) {
      val sq = size.width * 0.85f
      drawRect(LegendPrimary, Offset(0.5.dp.toPx(), 1.dp.toPx()), Size(sq, sq))
    }
  }
}

@Composable
private fun FormatCircularArrowsGlyph(sc: Float) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding((4f * sc).dp)) {
      val sw = 1.1.dp.toPx()
      val pad = 1.dp.toPx()
      val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
      // Top arc sweeping right
      drawArc(
        color = LegendPrimary.copy(alpha = 0.85f),
        startAngle = 215f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = arcSize,
        style = Stroke(sw, cap = StrokeCap.Round),
      )
      // Bottom arc sweeping left
      drawArc(
        color = LegendPrimary.copy(alpha = 0.85f),
        startAngle = 35f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = arcSize,
        style = Stroke(sw, cap = StrokeCap.Round),
      )
    }
    Text(
      text = "FORMAT",
      color = LegendPrimary,
      fontSize = (8.2f * sc).sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.1.sp,
      maxLines = 1,
    )
  }
}
