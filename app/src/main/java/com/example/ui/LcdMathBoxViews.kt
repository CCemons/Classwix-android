package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.formatDoubleClassWiz
import com.example.ui.theme.IbmPlexSansFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LcdPalette
import com.example.viewmodel.*
import kotlin.math.*

@Composable
fun MathBoxLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val mb = state.mathBoxState
  val s = state.settings
  when (mb.phase) {
    MathBoxState.Phase.SELECT_TYPE -> {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(s.tr("Caja Matemática", "Math Box"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
          Text("${mb.type.ordinal + 1}/${MathBoxType.entries.size}", color = palette.backgroundShade, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          itemsIndexed(MathBoxType.entries) { idx, t ->
            val sel = t == mb.type
            Row(
              modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(horizontal = 6.dp, vertical = 3.5.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("${idx + 1}. ${s.tr(t.labelEs, t.labelEn)}", color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
              Text("▶", color = if (sel) palette.background else palette.inkDark, fontSize = 9.sp)
            }
          }
        }
      }
    }
    MathBoxState.Phase.PARAMS -> when (mb.type) {
      MathBoxType.DICE, MathBoxType.COIN -> {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.Center) {
          Text(s.tr(mb.type.labelEs, mb.type.labelEn), color = palette.inkDarkest, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(5.dp))
          val cText = if (mb.activeParam == 0 && mb.inputBuffer.isNotEmpty()) "${mb.inputBuffer}|" else "${mb.objectCount}"
          val tText = if (mb.activeParam == 1 && mb.inputBuffer.isNotEmpty()) "${mb.inputBuffer}|" else "${mb.trials}"
          Row(modifier = Modifier.fillMaxWidth().background(if (mb.activeParam == 0) palette.inkDarkest else palette.backgroundShade).padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (mb.type == MathBoxType.DICE) s.tr("Dados (1-3):", "Dice (1-3):") else s.tr("Monedas (1-3):", "Coins (1-3):"), color = if (mb.activeParam == 0) palette.background else palette.inkDarkest, fontSize = 11.sp)
            Text(cText, color = if (mb.activeParam == 0) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
          }
          Spacer(Modifier.height(3.dp))
          Row(modifier = Modifier.fillMaxWidth().background(if (mb.activeParam == 1) palette.inkDarkest else palette.backgroundShade).padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.tr("Intentos (1-250):", "Trials (1-250):"), color = if (mb.activeParam == 1) palette.background else palette.inkDarkest, fontSize = 11.sp)
            Text(tText, color = if (mb.activeParam == 1) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
          }
        }
      }
      MathBoxType.NUMBER_LINE -> {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Row(
            modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 1.5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(s.tr("Recta numérica", "Number Line"), color = palette.background, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("◀▶:Op  OK:On/Off  EXE:Ver", color = palette.backgroundShade, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
          }
          Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            mb.nlEntries.forEachIndexed { idx, entry ->
              val sel = idx == mb.nlActiveSlot
              val valTxt = if (sel && mb.nlEditingValue) "${mb.inputBuffer}|" else formatDoubleClassWiz(entry.value, s)
              val check = if (entry.enabled) "✓" else " "
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (sel) palette.inkDark else palette.backgroundShade)
                  .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("[$check] ${entry.label}: x ${entry.op.symbol}", color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                Text(valTxt, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
              }
            }
          }
        }
      }
      MathBoxType.CIRCLE -> {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Text(s.tr("Círculo — Configurar", "Circle — Setup"), color = palette.inkDarkest, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.fillMaxWidth().background(palette.backgroundShade).padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.tr("Tipo (◀▶):", "Type (◀▶):"), color = palette.inkDarkest, fontSize = 11.sp)
            Text(s.tr(mb.circleMode.labelEs, mb.circleMode.labelEn), color = palette.inkDarkest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Row(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.tr("Ángulo θ°:", "Angle θ°:"), color = palette.background, fontSize = 11.sp)
            Text(if (mb.inputBuffer.isNotEmpty()) "${mb.inputBuffer}|" else "${mb.circleAngleDeg}°", color = palette.background, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
          }
          Text(s.tr("EXE: Mostrar círculo", "EXE: Show circle"), color = palette.inkDark, fontSize = 9.5.sp)
        }
      }
    }
    MathBoxState.Phase.RESULTS -> when (mb.type) {
      MathBoxType.DICE, MathBoxType.COIN -> {
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              text = if (mb.diceShowList) s.tr("Lista (${mb.trials})", "List (${mb.trials})") else s.tr("Frecuencia (${mb.trials})", "Freq (${mb.trials})"),
              color = palette.inkDarkest, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold
            )
            Text("◀▶:Vista  EXE:Repetir", color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
          }
          if (mb.diceShowList) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
              itemsIndexed(mb.rollSamples) { idx, draw ->
                val txt = if (mb.type == MathBoxType.DICE) draw.joinToString(", ") else draw.joinToString(", ") { if (it == 1) "C" else "X" }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("#${idx + 1}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.width(34.dp))
                  Text(txt, color = palette.inkDarkest, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f))
                  Text("Σ=${draw.sum()}", color = palette.inkDarkest, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily)
                }
              }
            }
          } else {
            val maxCount = mb.counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            LazyColumn(modifier = Modifier.fillMaxSize()) {
              itemsIndexed(mb.counts.entries.toList()) { _, (sum, cnt) ->
                val pct = (cnt * 100.0) / mb.trials.coerceAtLeast(1)
                val label = if (mb.type == MathBoxType.COIN) "${sum}C" else "$sum"
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text(label, color = palette.inkDarkest, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.width(26.dp))
                  Box(modifier = Modifier.weight(1f).height(8.dp).background(palette.backgroundShade)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(cnt.toFloat() / maxCount).background(palette.inkDarkest))
                  }
                  Spacer(Modifier.width(4.dp))
                  Text("$cnt (${pct.toInt()}%)", color = palette.inkDarkest, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.width(62.dp), textAlign = TextAlign.End)
                }
              }
            }
          }
        }
      }
      MathBoxType.NUMBER_LINE -> NumberLineCanvasView(state, palette)
      MathBoxType.CIRCLE -> CircleCanvasView(state, palette)
    }
  }
}

@Composable
private fun NumberLineCanvasView(state: CalculatorUiState, palette: LcdPalette) {
  val mb = state.mathBoxState
  val s = state.settings
  val halfSpan = (5.0 / mb.nlZoom).coerceIn(0.6, 25.0)
  val minX = mb.nlPanOffset - halfSpan
  val maxX = mb.nlPanOffset + halfSpan
  val enabled = mb.nlEntries.filter { it.enabled }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        enabled.joinToString("  ") { "${it.label}:${it.op.symbol}${formatDoubleClassWiz(it.value, s)}" }.ifEmpty { s.tr("Sin intervalos", "No intervals") },
        color = palette.inkDarkest, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
      )
      Text("◀▶:Mov ▲▼:Zoom", color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
    }
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val leftPad = 14f
        val rightPad = size.width - 14f
        val axisY = size.height * 0.72f
        fun toX(v: Double): Float = (leftPad + ((v - minX) / (maxX - minX)).toFloat() * (rightPad - leftPad))

        // Main axis
        drawLine(palette.inkDarkest, Offset(leftPad, axisY), Offset(rightPad, axisY), strokeWidth = 2.2f)
        // Integer ticks
        val step = if (halfSpan > 12) 5 else if (halfSpan > 6) 2 else 1
        val startTick = floor(minX).toInt()
        val endTick = ceil(maxX).toInt()
        for (t in startTick..endTick) {
          if (t % step == 0) {
            val tx = toX(t.toDouble())
            if (tx in leftPad..rightPad) {
              drawLine(palette.inkDark, Offset(tx, axisY - 5f), Offset(tx, axisY + 5f), strokeWidth = if (t == 0) 2.5f else 1.2f)
            }
          }
        }
        // Plot each enabled interval above the axis
        enabled.forEachIndexed { idx, entry ->
          val rowY = size.height * (0.20f + idx * 0.16f)
          val px = toX(entry.value).coerceIn(leftPad, rightPad)
          val inView = entry.value in minX..maxX
          val isInclusive = entry.op == NumberLineOp.LE || entry.op == NumberLineOp.GE || entry.op == NumberLineOp.EQ
          when (entry.op) {
            NumberLineOp.LT, NumberLineOp.LE -> {
              drawLine(palette.inkDarkest, Offset(leftPad, rowY), Offset(px, rowY), strokeWidth = 3.2f)
            }
            NumberLineOp.GT, NumberLineOp.GE -> {
              drawLine(palette.inkDarkest, Offset(px, rowY), Offset(rightPad, rowY), strokeWidth = 3.2f)
            }
            NumberLineOp.EQ -> {}
          }
          if (inView) {
            drawLine(
              palette.inkMid,
              Offset(px, rowY),
              Offset(px, axisY),
              strokeWidth = 1.2f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
            if (isInclusive) {
              drawCircle(palette.inkDarkest, radius = 5.2f, center = Offset(px, rowY))
            } else {
              drawCircle(palette.background, radius = 5.2f, center = Offset(px, rowY))
              drawCircle(palette.inkDarkest, radius = 5.2f, center = Offset(px, rowY), style = Stroke(width = 2.2f))
            }
          }
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(formatDoubleClassWiz(minX, s), color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
      Text("0", color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
      Text(formatDoubleClassWiz(maxX, s), color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
    }
  }
}

@Composable
private fun CircleCanvasView(state: CalculatorUiState, palette: LcdPalette) {
  val mb = state.mathBoxState
  val s = state.settings
  val deg = mb.circleAngleDeg
  val rad = Math.toRadians(deg.toDouble())
  val cosV = cos(rad)
  val sinV = sin(rad)
  val tanV = if (abs(cosV) < 1e-10) null else tan(rad)

  Row(modifier = Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Canvas(modifier = Modifier.size(98.dp)) {
      val cx = size.width * 0.5f
      val cy = if (mb.circleMode == CircleModeType.HALF) size.height * 0.78f else size.height * 0.5f
      val r = if (mb.circleMode == CircleModeType.HALF) size.width * 0.42f else size.width * 0.40f

      // Axes
      drawLine(palette.inkMid, Offset(cx - r * 1.15f, cy), Offset(cx + r * 1.15f, cy), strokeWidth = 1.2f)
      drawLine(palette.inkMid, Offset(cx, cy - r * 1.15f), Offset(cx, if (mb.circleMode == CircleModeType.HALF) cy + 4f else cy + r * 1.15f), strokeWidth = 1.2f)

      if (mb.circleMode == CircleModeType.HALF) {
        drawArc(
          color = palette.inkDarkest,
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(cx - r, cy - r),
          size = Size(r * 2, r * 2),
          style = Stroke(width = 2.2f),
        )
      } else {
        drawCircle(palette.inkDarkest, radius = r, center = Offset(cx, cy), style = Stroke(width = 2.2f))
      }

      if (mb.circleMode == CircleModeType.CLOCK) {
        for (h in 0 until 12) {
          val a = Math.toRadians(h * 30.0 - 90.0)
          drawLine(
            palette.inkDark,
            Offset((cx + cos(a) * r * 0.85f).toFloat(), (cy + sin(a) * r * 0.85f).toFloat()),
            Offset((cx + cos(a) * r).toFloat(), (cy + sin(a) * r).toFloat()),
            strokeWidth = 1.6f,
          )
        }
        val handA = Math.toRadians(deg.toDouble() - 90.0)
        drawLine(palette.inkDarkest, Offset(cx, cy), Offset((cx + cos(handA) * r * 0.82f).toFloat(), (cy + sin(handA) * r * 0.82f).toFloat()), strokeWidth = 2.8f)
        drawCircle(palette.inkDarkest, radius = 3.5f, center = Offset(cx, cy))
      } else {
        val px = (cx + cosV * r).toFloat()
        val py = (cy - sinV * r).toFloat()
        drawLine(palette.inkDarkest, Offset(cx, cy), Offset(px, py), strokeWidth = 2.4f)
        drawLine(palette.inkDark, Offset(px, py), Offset(px, cy), strokeWidth = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
        drawCircle(palette.inkDarkest, radius = 4f, center = Offset(px, py))
      }
    }

    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
      Column {
        Text(
          "${s.tr(mb.circleMode.labelEs, mb.circleMode.labelEn)} (◀▶ ±${mb.circleStepDeg}°)",
          color = palette.inkDark, fontSize = 8.5.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text("θ = $deg°", color = palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
        if (mb.circleMode == CircleModeType.CLOCK) {
          val totalMin = ((deg % 360) * 2) // 1° = 2 min on 12h dial
          val hh = ((totalMin / 60) % 12).let { if (it == 0) 12 else it }
          val mm = totalMin % 60
          Text("${s.tr("Hora", "Time")}: %02d:%02d".format(hh, mm), color = palette.inkDarkest, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily)
        } else {
          Text("sin = ${formatDoubleClassWiz(sinV, s)}", color = palette.inkDarkest, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
          Text("cos = ${formatDoubleClassWiz(cosV, s)}", color = palette.inkDarkest, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
          Text("tan = ${tanV?.let { formatDoubleClassWiz(it, s) } ?: "Undef"}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
        }
      }
      Text("▲▼:Tipo  OK:Paso", color = palette.inkDark, fontSize = 8.5.sp, fontFamily = JetBrainsMonoFamily)
    }
  }
}

@Composable
fun LcdQrView(state: CalculatorUiState, palette: LcdPalette) {
  val s = state.settings
  val matrix = state.qrMatrix
  Row(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier = Modifier
        .size(104.dp)
        .background(Color.White)
        .border(1.dp, palette.inkDarkest)
        .padding(4.dp)
        .testTag("qr_code_matrix"),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val n = matrix.size
        if (n > 0) {
          val cellW = size.width / n
          val cellH = size.height / n
          for (r in 0 until n) {
            for (c in 0 until n) {
              if (matrix[r][c]) {
                drawRect(
                  color = Color.Black,
                  topLeft = Offset(c * cellW, r * cellH),
                  size = Size(cellW + 0.5f, cellH + 0.5f),
                )
              }
            }
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "${state.qrTitle} (${s.tr(s.qrVersion.labelEs, s.qrVersion.labelEn)})",
          color = palette.inkDarkest,
          fontSize = 10.5.sp,
          fontFamily = IbmPlexSansFamily,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = state.qrSubtitle,
          color = palette.inkDark,
          fontSize = 9.5.sp,
          fontFamily = JetBrainsMonoFamily,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = s.tr("OK / BACK: Cerrar", "OK / BACK: Close"),
        color = palette.inkDarkest,
        fontSize = 9.5.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
