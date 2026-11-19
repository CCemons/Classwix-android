package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.*
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LcdPalette
import java.math.BigInteger
import kotlin.math.*

@Composable
fun BlinkingCursor(ink: Color, heightDp: Int = 18) {
  val transition = rememberInfiniteTransition(label = "cursor")
  val alpha by transition.animateFloat(
    initialValue = 1f,
    targetValue = 0.05f,
    animationSpec = infiniteRepeatable(
      animation = keyframes {
        durationMillis = 900
        1f at 0
        1f at 420
        0f at 460
        0f at 860
      },
      repeatMode = RepeatMode.Restart
    ),
    label = "cursorAlpha"
  )
  Box(
    modifier = Modifier
      .width(2.dp)
      .height(heightDp.dp)
      .background(ink.copy(alpha = alpha))
  )
}

@Composable
fun NaturalMathRowView(
  row: MathRow,
  cursor: CursorPath?,
  currentSteps: List<PathStep>,
  palette: LcdPalette,
  fontSize: TextUnit = 19.sp,
  showEmptyPlaceholder: Boolean = false,
  linearMode: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val isActiveRow = cursor != null && cursor.steps == currentSteps
  val scrollState = rememberScrollState()

  LaunchedEffect(row.items.size, cursor) {
    if (isActiveRow) {
      scrollState.animateScrollTo(scrollState.maxValue)
    }
  }

  Row(
    modifier = modifier.let {
      if (currentSteps.isEmpty()) it.horizontalScroll(scrollState) else it
    },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (row.items.isEmpty()) {
      if (isActiveRow) {
        BlinkingCursor(palette.inkDarkest, heightDp = (fontSize.value * 1.05f).roundToInt())
      }
      if (showEmptyPlaceholder) {
        Box(
          modifier = Modifier
            .padding(horizontal = 1.dp)
            .size((fontSize.value * 0.62f).dp, (fontSize.value * 0.8f).dp)
            .border(1.dp, palette.inkMid)
        )
      }
      return@Row
    }

    row.items.forEachIndexed { idx, element ->
      if (isActiveRow && cursor?.index == idx) {
        BlinkingCursor(palette.inkDarkest, heightDp = (fontSize.value * 1.05f).roundToInt())
      }
      if (linearMode) {
        LinearElementView(
          element = element,
          cursor = cursor,
          elementPath = currentSteps,
          elementIdx = idx,
          palette = palette,
          fontSize = fontSize,
        )
      } else {
        NaturalElementView(
          element = element,
          cursor = cursor,
          elementPath = currentSteps,
          elementIdx = idx,
          palette = palette,
          fontSize = fontSize,
        )
      }
    }
    if (isActiveRow && cursor?.index == row.items.size) {
      BlinkingCursor(palette.inkDarkest, heightDp = (fontSize.value * 1.05f).roundToInt())
    }
  }
}

@Composable
private fun LinearElementView(
  element: MathElement,
  cursor: CursorPath?,
  elementPath: List<PathStep>,
  elementIdx: Int,
  palette: LcdPalette,
  fontSize: TextUnit,
) {
  val f = fontSize
  @Composable
  fun LinearSlot(row: MathRow, slotIdx: Int) {
    NaturalMathRowView(
      row = row,
      cursor = cursor,
      currentSteps = elementPath + PathStep(elementIdx, slotIdx),
      palette = palette,
      fontSize = f,
      showEmptyPlaceholder = true,
      linearMode = true,
    )
  }
  @Composable
  fun LinText(s: String) {
    Text(s, color = palette.inkDarkest, fontSize = f, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, maxLines = 1)
  }

  when (element) {
    is MathElement.Token -> LinText(element.text)
    is MathElement.Fraction -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinearSlot(element.numerator, 0); LinText("⌟"); LinearSlot(element.denominator, 1)
    }
    is MathElement.MixedFraction -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinearSlot(element.whole, 0); LinText("⌟"); LinearSlot(element.numerator, 1); LinText("⌟"); LinearSlot(element.denominator, 2)
    }
    is MathElement.Superscript -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("^("); LinearSlot(element.exponent, 0); LinText(")")
    }
    is MathElement.Sqrt -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("√("); LinearSlot(element.radicand, 0); LinText(")")
    }
    is MathElement.NthRoot -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinearSlot(element.index, 0); LinText("ˣ√("); LinearSlot(element.radicand, 1); LinText(")")
    }
    is MathElement.LogBase -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("log("); LinearSlot(element.base, 0); LinText(","); LinearSlot(element.argument, 1); LinText(")")
    }
    is MathElement.AbsBox -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("Abs("); LinearSlot(element.inner, 0); LinText(")")
    }
    is MathElement.Integral -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("∫("); LinearSlot(element.integrand, 0); LinText(","); LinearSlot(element.lower, 1); LinText(","); LinearSlot(element.upper, 2); LinText(")")
    }
    is MathElement.Derivative -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("d/dx("); LinearSlot(element.expr, 0); LinText(","); LinearSlot(element.atValue, 1); LinText(")")
    }
    is MathElement.Summation -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("Σ("); LinearSlot(element.expr, 2); LinText(","); LinearSlot(element.lower, 0); LinText(","); LinearSlot(element.upper, 1); LinText(")")
    }
    is MathElement.ProductOp -> Row(verticalAlignment = Alignment.CenterVertically) {
      LinText("Π("); LinearSlot(element.expr, 2); LinText(","); LinearSlot(element.lower, 0); LinText(","); LinearSlot(element.upper, 1); LinText(")")
    }
  }
}

@Composable
private fun NaturalElementView(
  element: MathElement,
  cursor: CursorPath?,
  elementPath: List<PathStep>,
  elementIdx: Int,
  palette: LcdPalette,
  fontSize: TextUnit,
) {
  val subSize = (fontSize.value * 0.72f).coerceAtLeast(10f).sp
  val tinySize = (fontSize.value * 0.58f).coerceAtLeast(9f).sp

  when (element) {
    is MathElement.Token -> {
      Text(
        text = element.text,
        color = palette.inkDarkest,
        fontSize = fontSize,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 0.5.dp),
      )
    }

    is MathElement.Fraction -> {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 2.dp),
      ) {
        NaturalMathRowView(
          row = element.numerator,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 0),
          palette = palette,
          fontSize = subSize,
          showEmptyPlaceholder = true,
        )
        Box(
          modifier = Modifier
            .padding(vertical = 1.5.dp)
            .widthIn(min = 16.dp)
            .fillMaxWidth()
            .height(1.4.dp)
            .background(palette.inkDarkest)
        )
        NaturalMathRowView(
          row = element.denominator,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 1),
          palette = palette,
          fontSize = subSize,
          showEmptyPlaceholder = true,
        )
      }
    }

    is MathElement.MixedFraction -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
        NaturalMathRowView(
          row = element.whole,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 0),
          palette = palette,
          fontSize = fontSize,
          showEmptyPlaceholder = true,
        )
        Spacer(Modifier.width(2.dp))
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.width(IntrinsicSize.Max),
        ) {
          NaturalMathRowView(
            row = element.numerator,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 1),
            palette = palette,
            fontSize = subSize,
            showEmptyPlaceholder = true,
          )
          Box(
            modifier = Modifier
              .padding(vertical = 1.dp)
              .widthIn(min = 16.dp)
              .fillMaxWidth()
              .height(1.4.dp)
              .background(palette.inkDarkest)
          )
          NaturalMathRowView(
            row = element.denominator,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 2),
            palette = palette,
            fontSize = subSize,
            showEmptyPlaceholder = true,
          )
        }
      }
    }

    is MathElement.Superscript -> {
      Box(
        modifier = Modifier.padding(bottom = (fontSize.value * 0.42f).dp, start = 1.dp, end = 1.dp)
      ) {
        NaturalMathRowView(
          row = element.exponent,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 0),
          palette = palette,
          fontSize = subSize,
          showEmptyPlaceholder = true,
        )
      }
    }

    is MathElement.Sqrt -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 1.dp)) {
        RadicalHook(palette.inkDarkest, heightDp = (fontSize.value * 1.15f).dp)
        Box(
          modifier = Modifier
            .drawBehind {
              drawLine(
                color = palette.inkDarkest,
                start = Offset(0f, 1.5f),
                end = Offset(size.width, 1.5f),
                strokeWidth = 2.4f
              )
            }
            .padding(top = 3.dp, start = 2.dp, end = 2.dp)
        ) {
          NaturalMathRowView(
            row = element.radicand,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 0),
            palette = palette,
            fontSize = fontSize,
            showEmptyPlaceholder = true,
          )
        }
      }
    }

    is MathElement.NthRoot -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 1.dp)) {
        Box(modifier = Modifier.padding(bottom = (fontSize.value * 0.45f).dp)) {
          NaturalMathRowView(
            row = element.index,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 0),
            palette = palette,
            fontSize = tinySize,
            showEmptyPlaceholder = true,
          )
        }
        RadicalHook(palette.inkDarkest, heightDp = (fontSize.value * 1.15f).dp)
        Box(
          modifier = Modifier
            .drawBehind {
              drawLine(
                color = palette.inkDarkest,
                start = Offset(0f, 1.5f),
                end = Offset(size.width, 1.5f),
                strokeWidth = 2.4f
              )
            }
            .padding(top = 3.dp, start = 2.dp, end = 2.dp)
        ) {
          NaturalMathRowView(
            row = element.radicand,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 1),
            palette = palette,
            fontSize = fontSize,
            showEmptyPlaceholder = true,
          )
        }
      }
    }

    is MathElement.LogBase -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 1.dp)) {
        Text("log", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
        Box(modifier = Modifier.padding(top = (fontSize.value * 0.35f).dp)) {
          NaturalMathRowView(
            row = element.base,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 0),
            palette = palette,
            fontSize = tinySize,
            showEmptyPlaceholder = true,
          )
        }
        Text("(", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
        NaturalMathRowView(
          row = element.argument,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 1),
          palette = palette,
          fontSize = fontSize,
          showEmptyPlaceholder = true,
        )
        Text(")", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
      }
    }

    is MathElement.AbsBox -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 1.dp)) {
        Box(Modifier.width(1.5.dp).height((fontSize.value * 1.05f).dp).background(palette.inkDarkest))
        Box(Modifier.padding(horizontal = 2.dp)) {
          NaturalMathRowView(
            row = element.inner,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 0),
            palette = palette,
            fontSize = fontSize,
            showEmptyPlaceholder = true,
          )
        }
        Box(Modifier.width(1.5.dp).height((fontSize.value * 1.05f).dp).background(palette.inkDarkest))
      }
    }

    is MathElement.Integral -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
        Text("∫", color = palette.inkDarkest, fontSize = (fontSize.value * 1.35f).sp, fontFamily = JetBrainsMonoFamily)
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(end = 2.dp)) {
          NaturalMathRowView(
            row = element.upper,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 2),
            palette = palette,
            fontSize = tinySize,
            showEmptyPlaceholder = true,
          )
          Spacer(Modifier.height(4.dp))
          NaturalMathRowView(
            row = element.lower,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 1),
            palette = palette,
            fontSize = tinySize,
            showEmptyPlaceholder = true,
          )
        }
        Text("(", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
        NaturalMathRowView(
          row = element.integrand,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 0),
          palette = palette,
          fontSize = fontSize,
          showEmptyPlaceholder = true,
        )
        Text(")dx", color = palette.inkDarkest, fontSize = subSize, fontFamily = JetBrainsMonoFamily)
      }
    }

    is MathElement.Derivative -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("d", color = palette.inkDarkest, fontSize = tinySize, fontFamily = JetBrainsMonoFamily)
          Box(Modifier.width(14.dp).height(1.dp).background(palette.inkDarkest))
          Text("dx", color = palette.inkDarkest, fontSize = tinySize, fontFamily = JetBrainsMonoFamily)
        }
        Text("(", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
        NaturalMathRowView(
          row = element.expr,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 0),
          palette = palette,
          fontSize = fontSize,
          showEmptyPlaceholder = true,
        )
        Text(")|", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
        Text("x=", color = palette.inkDarkest, fontSize = tinySize, fontFamily = JetBrainsMonoFamily)
        NaturalMathRowView(
          row = element.atValue,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 1),
          palette = palette,
          fontSize = tinySize,
          showEmptyPlaceholder = true,
        )
      }
    }

    is MathElement.Summation, is MathElement.ProductOp -> {
      val sym = if (element is MathElement.Summation) "Σ" else "Π"
      val lower = if (element is MathElement.Summation) element.lower else (element as MathElement.ProductOp).lower
      val upper = if (element is MathElement.Summation) element.upper else (element as MathElement.ProductOp).upper
      val expr = if (element is MathElement.Summation) element.expr else (element as MathElement.ProductOp).expr
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          NaturalMathRowView(
            row = upper,
            cursor = cursor,
            currentSteps = elementPath + PathStep(elementIdx, 1),
            palette = palette,
            fontSize = tinySize,
            showEmptyPlaceholder = true,
          )
          Text(sym, color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("x=", color = palette.inkDarkest, fontSize = tinySize, fontFamily = JetBrainsMonoFamily)
            NaturalMathRowView(
              row = lower,
              cursor = cursor,
              currentSteps = elementPath + PathStep(elementIdx, 0),
              palette = palette,
              fontSize = tinySize,
              showEmptyPlaceholder = true,
            )
          }
        }
        Text("(", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
        NaturalMathRowView(
          row = expr,
          cursor = cursor,
          currentSteps = elementPath + PathStep(elementIdx, 2),
          palette = palette,
          fontSize = fontSize,
          showEmptyPlaceholder = true,
        )
        Text(")", color = palette.inkDarkest, fontSize = fontSize, fontFamily = JetBrainsMonoFamily)
      }
    }
  }
}

@Composable
private fun RadicalHook(color: Color, heightDp: androidx.compose.ui.unit.Dp) {
  Canvas(modifier = Modifier.width(9.dp).height(heightDp)) {
    val path = Path().apply {
      moveTo(0f, size.height * 0.56f)
      lineTo(size.width * 0.28f, size.height * 0.50f)
      lineTo(size.width * 0.58f, size.height * 0.94f)
      lineTo(size.width, 1.5f)
    }
    drawPath(
      path = path,
      color = color,
      style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
  }
}

@Composable
fun EvalResultDisplay(
  result: EvalResult,
  settings: CalcSettings,
  formatOverride: ResultFormatMode?,
  palette: LcdPalette,
) {
  val effectiveFormat = when {
    formatOverride != null -> formatOverride
    settings.inputOutput == InputOutputMode.MATH_DEC || settings.inputOutput == InputOutputMode.LINE_DEC -> ResultFormatMode.DECIMAL
    else -> ResultFormatMode.STANDARD
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    contentAlignment = Alignment.CenterEnd,
  ) {
    when (result) {
      is EvalResult.ErrorResult -> {
        Column(horizontalAlignment = Alignment.End) {
          Text(result.title, color = palette.inkDarkest, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
          Text(result.message, color = palette.inkDark, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily)
        }
      }
      is EvalResult.BaseNResult -> {
        val text = when (result.base) {
          BaseNMode.DEC -> result.value.toString(10)
          BaseNMode.HEX -> Integer.toHexString(result.value).uppercase()
          BaseNMode.BIN -> Integer.toBinaryString(result.value).padStart(8, '0').chunked(4).joinToString(" ")
          BaseNMode.OCT -> Integer.toOctalString(result.value)
        }
        Text(text, color = palette.inkDarkest, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
      }
      is EvalResult.VerifyResult -> {
        val verdict = if (result.isTrue) {
          settings.tr("VERDADERO", "TRUE")
        } else {
          settings.tr("FALSO", "FALSE")
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(verdict, color = palette.inkDarkest, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
          Text(
            "${result.lhsSummary} [${result.relationOp}] ${result.rhsSummary}",
            color = palette.inkDark,
            fontSize = 10.sp,
            fontFamily = JetBrainsMonoFamily,
          )
        }
      }
      is EvalResult.UnreducedFraction -> {
        val neg = result.num < BigInteger.ZERO
        val absN = result.num.abs()
        val factorTxt = result.lastDivisor?.let { "÷$it  " } ?: "÷?  "
        if (effectiveFormat == ResultFormatMode.DECIMAL) {
          val dec = result.num.toDouble() / result.den.toDouble()
          Text(formatDoubleClassWiz(dec, settings), color = palette.inkDarkest, fontSize = 21.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
        } else if (settings.inputOutput == InputOutputMode.LINE_LINE) {
          Text(
            "$factorTxt${result.num}⌟${result.den}",
            color = palette.inkDarkest,
            fontSize = 19.sp,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
          )
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(factorTxt, color = palette.inkDark, fontSize = 10.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
            if (neg) Text("-", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily)
            StackedNumDen(absN.toString(), result.den.toString(), palette)
          }
        }
      }
      is EvalResult.PolarPoint -> {
        val lbl1 = if (result.isPol) "r=" else "x="
        val lbl2 = if (result.isPol) "θ=" else "y="
        Column(horizontalAlignment = Alignment.End) {
          Text("$lbl1${formatDoubleClassWiz(result.r, settings)}", color = palette.inkDarkest, fontSize = 16.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
          Text("$lbl2${formatDoubleClassWiz(result.theta, settings)}", color = palette.inkDarkest, fontSize = 16.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
        }
      }
      is EvalResult.MatrixResult -> {
        Column(horizontalAlignment = Alignment.End) {
          result.data.forEach { row ->
            Text(
              "[ " + row.joinToString("  ") { formatDoubleClassWiz(it, settings) } + " ]",
              color = palette.inkDarkest,
              fontSize = 13.sp,
              fontFamily = JetBrainsMonoFamily,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
      is EvalResult.VectorResult -> {
        Text(
          "[ " + result.data.joinToString("  ") { formatDoubleClassWiz(it, settings) } + " ]",
          color = palette.inkDarkest,
          fontSize = 15.sp,
          fontFamily = JetBrainsMonoFamily,
          fontWeight = FontWeight.SemiBold,
        )
      }
      is EvalResult.ComplexResult -> {
        val wantPolar = effectiveFormat == ResultFormatMode.POLAR ||
          (effectiveFormat != ResultFormatMode.RECTANGULAR && settings.complexFormat == ComplexFormat.POLAR)
        if (wantPolar) {
          val re = result.re.toDouble()
          val im = result.im.toDouble()
          val r = hypot(re, im)
          val rad = atan2(im, re)
          val ang = when (settings.angleUnit) {
            AngleUnit.DEG -> Math.toDegrees(rad)
            AngleUnit.RAD -> rad
            AngleUnit.GRAD -> rad * 200.0 / PI
          }
          Text(
            "${formatDoubleClassWiz(r, settings)}∠${formatDoubleClassWiz(ang, settings)}",
            color = palette.inkDarkest,
            fontSize = 19.sp,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
          )
        } else if (effectiveFormat == ResultFormatMode.DECIMAL) {
          val reStr = formatDoubleClassWiz(result.re.toDouble(), settings)
          val imVal = result.im.toDouble()
          val sign = if (imVal < 0) "-" else "+"
          val imStr = formatDoubleClassWiz(abs(imVal), settings)
          Text("$reStr $sign ${imStr}i", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (!result.re.isZero) {
              ExactRealView(result.re, settings, ResultFormatMode.STANDARD, palette)
              val imD = result.im.toDouble()
              Text(if (imD < 0) " - " else " + ", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily)
              val absIm = if (imD < 0) -result.im else result.im
              if (abs(absIm.toDouble() - 1.0) > 1e-12) {
                ExactRealView(absIm, settings, ResultFormatMode.STANDARD, palette)
              }
              Text("i", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
            } else {
              val imD = result.im.toDouble()
              if (abs(abs(imD) - 1.0) < 1e-12) {
                Text(if (imD < 0) "-i" else "i", color = palette.inkDarkest, fontSize = 20.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
              } else {
                ExactRealView(result.im, settings, ResultFormatMode.STANDARD, palette)
                Text("i", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
      is EvalResult.RemainderResult -> {
        Text(
          text = "${result.quotient}, R=${result.remainder}",
          color = palette.inkDarkest,
          fontSize = 20.sp,
          fontFamily = JetBrainsMonoFamily,
          fontWeight = FontWeight.SemiBold,
        )
      }
      is EvalResult.RealResult -> {
        ExactRealView(result.value, settings, effectiveFormat, palette)
      }
    }
  }
}

@Composable
fun ExactRealView(
  value: ExactReal,
  settings: CalcSettings,
  format: ResultFormatMode,
  palette: LcdPalette,
) {
  if (format == ResultFormatMode.PRIME_FACTOR) {
    val d = value.toDouble()
    val rounded = round(d).toLong()
    val factorized = if (abs(d - rounded) < 1e-11) primeFactorize(rounded) else null
    Text(
      text = factorized ?: formatDoubleClassWiz(d, settings),
      color = palette.inkDarkest,
      fontSize = 20.sp,
      fontFamily = JetBrainsMonoFamily,
      fontWeight = FontWeight.SemiBold,
    )
    return
  }

  if (format in setOf(ResultFormatMode.DECIMAL, ResultFormatMode.SEXAGESIMAL, ResultFormatMode.SCIENTIFIC, ResultFormatMode.ENGINEERING)) {
    Text(
      text = formatDoubleClassWiz(value.toDouble(), settings, format),
      color = palette.inkDarkest,
      fontSize = 21.sp,
      fontFamily = JetBrainsMonoFamily,
      fontWeight = FontWeight.SemiBold,
    )
    return
  }

  val fracFmt = when (format) {
    ResultFormatMode.MIXED_FRACTION -> FractionFormat.MIXED
    ResultFormatMode.IMPROPER_FRACTION -> FractionFormat.IMPROPER
    else -> settings.fractionFormat
  }

  // Linear Output (LinS / LineO) renders ⌟ fractions when not forced to Decimal
  if (settings.inputOutput == InputOutputMode.LINE_LINE &&
    format in setOf(ResultFormatMode.STANDARD, ResultFormatMode.MIXED_FRACTION, ResultFormatMode.IMPROPER_FRACTION)
  ) {
    Text(
      text = formatExactRealLinear(value, settings, fracFmt),
      color = palette.inkDarkest,
      fontSize = 20.sp,
      fontFamily = JetBrainsMonoFamily,
      fontWeight = FontWeight.SemiBold,
    )
    return
  }

  when (value) {
    is ExactReal.Approx -> {
      Text(
        text = formatDoubleClassWiz(value.value, settings),
        color = palette.inkDarkest,
        fontSize = 21.sp,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
      )
    }
    is ExactReal.PiMultiple -> {
      val r = value.coeff.asRationalOrNull()
      if (r != null && !r.isInteger) {
        val neg = r.isNegative
        val absN = r.num.abs()
        val numText = if (absN == BigInteger.ONE) "π" else "${applyDigitGrouping(absN.toString(), settings.digitSeparator)}π"
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (neg) Text("-", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily)
          StackedNumDen(numText, applyDigitGrouping(r.den.toString(), settings.digitSeparator), palette)
        }
      } else {
        val str = if (r != null) {
          when (r.num) {
            BigInteger.ONE -> "π"
            BigInteger.valueOf(-1L) -> "-π"
            BigInteger.ZERO -> "0"
            else -> "${applyDigitGrouping(r.num.toString(), settings.digitSeparator)}π"
          }
        } else "${value.coeff.formatLatexLike(fracFmt)}π"
        Text(str, color = palette.inkDarkest, fontSize = 21.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
      }
    }
    is ExactReal.Surd -> {
      val r = value.value.asRationalOrNull()
      if (r != null) {
        if (r.isInteger) {
          Text(
            applyDigitGrouping(r.num.toString(), settings.digitSeparator),
            color = palette.inkDarkest,
            fontSize = 21.sp,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
          )
        } else {
          val neg = r.isNegative
          val absN = r.num.abs()
          if (fracFmt == FractionFormat.MIXED && absN > r.den) {
            val whole = absN / r.den
            val rem = absN % r.den
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (neg) Text("-", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily)
              Text(applyDigitGrouping(whole.toString(), settings.digitSeparator), color = palette.inkDarkest, fontSize = 20.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
              Spacer(Modifier.width(3.dp))
              StackedNumDen(rem.toString(), r.den.toString(), palette)
            }
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (neg) Text("-", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily)
              StackedNumDen(absN.toString(), r.den.toString(), palette)
            }
          }
        }
      } else {
        val str = value.value.formatLatexLike(fracFmt)
        if (str.startsWith("(") && str.contains(")/")) {
          val num = str.substringAfter("(").substringBefore(")/")
          val den = str.substringAfter(")/")
          StackedNumDen(num, den, palette)
        } else if (value.value.terms.size == 1) {
          val (rad, rat) = value.value.terms.entries.first()
          val neg = rat.isNegative
          val absN = rat.num.abs()
          val numText = if (absN == BigInteger.ONE) "√$rad" else "${absN}√$rad"
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (neg) Text("-", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily)
            if (rat.den == BigInteger.ONE) {
              Text(numText, color = palette.inkDarkest, fontSize = 20.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
            } else {
              StackedNumDen(numText, rat.den.toString(), palette)
            }
          }
        } else {
          Text(str, color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun StackedNumDen(num: String, den: String, palette: LcdPalette) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(IntrinsicSize.Max),
  ) {
    Text(num, color = palette.inkDarkest, fontSize = 15.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
    Box(Modifier.padding(vertical = 1.dp).widthIn(min = 18.dp).fillMaxWidth().height(1.5.dp).background(palette.inkDarkest))
    Text(den, color = palette.inkDarkest, fontSize = 15.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
  }
}
