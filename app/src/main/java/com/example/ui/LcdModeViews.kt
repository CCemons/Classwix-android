package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.*
import com.example.ui.theme.IbmPlexSansFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LcdPalette
import com.example.viewmodel.*
import kotlinx.coroutines.flow.first
import kotlin.math.abs

@Composable
private fun rememberLcdListState(
  resetKey: Any?,
  cursor: Int,
  itemCount: Int,
): LazyListState {
  val listState = remember(resetKey) { LazyListState(0, 0) }
  LaunchedEffect(resetKey, cursor, itemCount) {
    if (itemCount <= 0) return@LaunchedEffect
    val target = cursor.coerceIn(0, itemCount - 1)
    if (listState.layoutInfo.visibleItemsInfo.isEmpty()) {
      snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
    }
    val info = listState.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) {
      listState.scrollToItem(target)
      return@LaunchedEffect
    }
    val viewportStart = info.viewportStartOffset
    val viewportEnd = info.viewportEndOffset
    val fullyVisible = visible.filter {
      it.offset >= viewportStart && (it.offset + it.size) <= viewportEnd
    }
    val firstFull = fullyVisible.firstOrNull()?.index ?: visible.first().index
    val lastFull = fullyVisible.lastOrNull()?.index ?: visible.last().index
    if (target < firstFull) {
      listState.scrollToItem(target)
    } else if (target > lastFull) {
      val visibleCount = (lastFull - firstFull + 1).coerceAtLeast(1)
      val newFirst = (target - visibleCount + 1).coerceIn(0, target)
      listState.scrollToItem(newFirst)
    }
  }
  return listState
}

@Composable
private fun rememberLcdGridState(
  cursor: Int,
  itemCount: Int,
): LazyGridState {
  val gridState = remember { LazyGridState(0, 0) }
  LaunchedEffect(cursor, itemCount) {
    if (itemCount <= 0) return@LaunchedEffect
    val target = cursor.coerceIn(0, itemCount - 1)
    if (gridState.layoutInfo.visibleItemsInfo.isEmpty()) {
      snapshotFlow { gridState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
    }
    val info = gridState.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) {
      gridState.scrollToItem(target)
      return@LaunchedEffect
    }
    val viewportStart = info.viewportStartOffset
    val viewportEnd = info.viewportEndOffset
    val fullyVisible = visible.filter {
      it.offset.y >= viewportStart && (it.offset.y + it.size.height) <= viewportEnd
    }
    val firstFull = fullyVisible.firstOrNull()?.index ?: visible.first().index
    val lastFull = fullyVisible.lastOrNull()?.index ?: visible.last().index
    if (target < firstFull) {
      gridState.scrollToItem((target / 4) * 4)
    } else if (target > lastFull) {
      val visibleRows = (((lastFull - firstFull + 1) + 3) / 4).coerceAtLeast(1)
      val targetRow = target / 4
      val newFirstRow = (targetRow - visibleRows + 1).coerceAtLeast(0)
      gridState.scrollToItem(newFirstRow * 4)
    }
  }
  return gridState
}

@Composable
private fun LcdScrollbar(
  listState: LazyListState,
  totalItems: Int,
  palette: LcdPalette,
  modifier: Modifier = Modifier,
) {
  val show by remember(totalItems) {
    derivedStateOf {
      val vis = listState.layoutInfo.visibleItemsInfo.size
      totalItems > vis && vis > 0
    }
  }
  if (!show) return
  Canvas(
    modifier = modifier
      .width(3.dp)
      .fillMaxHeight()
      .padding(start = 1.dp)
      .background(palette.backgroundShade)
  ) {
    val info = listState.layoutInfo
    val vis = info.visibleItemsInfo.size.coerceAtLeast(1)
    val first = listState.firstVisibleItemIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
    val thumbFrac = (vis.toFloat() / totalItems).coerceIn(0.15f, 1f)
    val topFrac = (first.toFloat() / totalItems).coerceIn(0f, 1f - thumbFrac)
    drawRect(
      color = palette.inkDarkest,
      topLeft = Offset(0f, size.height * topFrac),
      size = Size(size.width, size.height * thumbFrac),
    )
  }
}

@Composable
fun LcdHomeGridView(
  state: CalculatorUiState,
  palette: LcdPalette,
  onSelectMode: (CalcMode) -> Unit,
) {
  val gridState = rememberLcdGridState(state.overlayCursor, CalcMode.entries.size)

  Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
    Text(
      text = state.settings.tr("INICIO — Seleccione aplicación", "HOME — Select App"),
      color = palette.inkDark,
      fontSize = 10.sp,
      fontFamily = IbmPlexSansFamily,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 3.dp, start = 2.dp),
    )
    LazyVerticalGrid(
      columns = GridCells.Fixed(4),
      state = gridState,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      itemsIndexed(CalcMode.entries) { idx, mode ->
        val selected = idx == state.overlayCursor
        val bg = if (selected) palette.inkDarkest else palette.backgroundShade
        val fg = if (selected) palette.background else palette.inkDarkest
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier
            .testTag("home_app_${mode.name.lowercase()}")
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(0.8.dp, if (selected) palette.inkDarkest else palette.inkMid, RoundedCornerShape(4.dp))
            .clickable { onSelectMode(mode) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        ) {
          Icon(
            imageVector = modeIcon(mode),
            contentDescription = mode.titleEs,
            tint = fg,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.height(1.dp))
          Text(
            text = state.settings.tr(mode.titleEs, mode.titleEn),
            color = fg,
            fontSize = 8.5.sp,
            fontFamily = IbmPlexSansFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

private fun modeIcon(mode: CalcMode): ImageVector = when (mode) {
  CalcMode.CALCULATE -> Icons.Outlined.Calculate
  CalcMode.STATISTICS -> Icons.Outlined.BarChart
  CalcMode.DISTRIBUTION -> Icons.AutoMirrored.Outlined.ShowChart
  CalcMode.SPREADSHEET -> Icons.Outlined.TableChart
  CalcMode.TABLE -> Icons.AutoMirrored.Outlined.ViewList
  CalcMode.EQUATION -> Icons.Outlined.Functions
  CalcMode.INEQUALITY -> Icons.AutoMirrored.Outlined.CompareArrows
  CalcMode.COMPLEX -> Icons.Outlined.BlurOn
  CalcMode.BASE_N -> Icons.Outlined.Memory
  CalcMode.MATRIX -> Icons.Outlined.GridOn
  CalcMode.VECTOR -> Icons.Outlined.NorthEast
  CalcMode.RATIO -> Icons.Outlined.Balance
  CalcMode.MATH_BOX -> Icons.Outlined.Casino
}

@Composable
fun LcdMenuListView(
  title: String,
  items: List<CatalogEntry>,
  cursor: Int,
  isSpanish: Boolean,
  palette: LcdPalette,
  onSelectItem: (Int) -> Unit,
) {
  val listState = rememberLcdListState(title, cursor, items.size)
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(title, color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
      Text("${cursor + 1}/${items.size.coerceAtLeast(1)}", color = palette.backgroundShade, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily)
    }
    Row(modifier = Modifier.fillMaxSize()) {
      LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
        itemsIndexed(items) { idx, entry ->
          val selected = idx == cursor
          val bg = if (selected) palette.inkDark else palette.background
          val fg = if (selected) palette.background else palette.inkDarkest
          val subFg = if (selected) palette.backgroundShade else palette.inkDark
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(bg)
              .clickable { onSelectItem(idx) }
              .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${idx + 1}. ${if (isSpanish) entry.labelEs else entry.labelEn}",
                color = fg,
                fontSize = 11.5.sp,
                fontFamily = IbmPlexSansFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (entry.subtitle.isNotEmpty()) {
                Text(entry.subtitle, color = subFg, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
                Spacer(Modifier.width(4.dp))
              }
              if (entry is CatalogEntry.SubCategory) {
                Text("▶", color = fg, fontSize = 9.sp)
              }
            }
          }
        }
      }
      LcdScrollbar(listState, items.size, palette)
    }
  }
}

@Composable
fun LcdSettingsView(
  state: CalculatorUiState,
  palette: LcdPalette,
  onSelect: (Int) -> Unit,
  onContrastChange: (Float) -> Unit,
) {
  val s = state.settings
  val rows: List<Pair<String, String>> = when (state.settingsPage) {
    0 -> listOf(
      s.tr("Config. cálculo", "Calc Settings") to "▶",
      s.tr("Config. sistema", "System Settings") to "▶",
      s.tr("Reiniciar", "Reset") to "▶",
    )
    1 -> listOf(
      s.tr("Entrada/Salida", "Input/Output") to s.tr(s.inputOutput.labelEs, s.inputOutput.labelEn),
      s.tr("Unidad angular", "Angle Unit") to s.tr(s.angleUnit.labelEs, s.angleUnit.labelEn),
      s.tr("Formato número", "Number Format") to s.numberFormat.name,
      s.tr("Result. fracc.", "Fraction Result") to if (s.fractionFormat == FractionFormat.IMPROPER) "d/c" else "ab/c",
      s.tr("Complejos", "Complex Result") to s.complexFormat.labelEs,
      s.tr("Frecuencia Est.", "Stat Frequency") to if (s.statFrequency) s.tr("Sí", "On") else s.tr("No", "Off"),
    )
    2 -> listOf(
      s.tr("Contraste LCD", "LCD Contrast") to "${(s.lcdContrast * 100).toInt()}%",
      s.tr("Tema pantalla", "Display Theme") to s.tr(s.lcdTheme.labelEs, s.lcdTheme.labelEn),
      s.tr("Idioma / Lang", "Language") to s.language.label,
      s.tr("Vibración táctil", "Haptic Feedback") to if (s.hapticsEnabled) s.tr("Sí", "On") else s.tr("No", "Off"),
    )
    3 -> listOf(
      s.tr("Reiniciar config.", "Reset Settings") to "OK",
      s.tr("Borrar memoria", "Clear Memory") to "OK",
      s.tr("Inicializar todo", "Initialize All") to "OK",
    )
    else -> when (state.settingsDetailKey) {
      "IO" -> InputOutputMode.entries.map { s.tr(it.labelEs, it.labelEn) to if (it == s.inputOutput) "●" else "" }
      "ANGLE" -> AngleUnit.entries.map { s.tr(it.labelEs, it.labelEn) to if (it == s.angleUnit) "●" else "" }
      else -> NumberFormatMode.entries.map { it.name to if (it == s.numberFormat) "●" else "" }
    }
  }

  val headerTitle = when (state.settingsPage) {
    0 -> s.tr("CONFIGURACIÓN", "SETTINGS")
    1 -> s.tr("Config. cálculo", "Calc Settings")
    2 -> s.tr("Config. sistema", "System Settings")
    3 -> s.tr("Reiniciar", "Reset")
    else -> s.tr("Seleccionar opción", "Select Option")
  }

  val listState = rememberLcdListState(
    resetKey = state.settingsPage to state.settingsDetailKey,
    cursor = state.overlayCursor,
    itemCount = rows.size,
  )

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(headerTitle, color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
      Text("${state.overlayCursor.coerceIn(0, (rows.size - 1).coerceAtLeast(0)) + 1}/${rows.size.coerceAtLeast(1)}", color = palette.backgroundShade, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily)
    }
    Row(modifier = Modifier.fillMaxSize()) {
      LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
        itemsIndexed(rows) { idx, (label, valStr) ->
          val selected = idx == state.overlayCursor
          val bg = if (selected) palette.inkDark else palette.background
          val fg = if (selected) palette.background else palette.inkDarkest
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(bg)
              .clickable { onSelect(idx) }
              .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(label, color = fg, fontSize = 11.5.sp, fontFamily = IbmPlexSansFamily, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (state.settingsPage == 2 && idx == 0) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("-", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onContrastChange(s.lcdContrast - 0.08f) }.padding(horizontal = 4.dp))
                Box(modifier = Modifier.width(46.dp).height(6.dp).border(0.8.dp, fg)) {
                  Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(((s.lcdContrast - 0.4f) / 0.6f).coerceIn(0.1f, 1f)).background(fg))
                }
                Text("+", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onContrastChange(s.lcdContrast + 0.08f) }.padding(horizontal = 4.dp))
              }
            } else {
              Text(valStr, color = fg, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
      LcdScrollbar(listState, rows.size, palette)
    }
  }
}

@Composable
fun LcdVariableView(
  state: CalculatorUiState,
  palette: LcdPalette,
  onSelectVar: (Int) -> Unit,
) {
  val names = listOf("A", "B", "C", "D", "E", "F", "x", "y", "z")
  val listState = rememberLcdListState(state.storePending, state.overlayCursor, names.size)
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (state.storePending) state.settings.tr("GUARDAR EN VARIABLE (STO)", "STORE TO VARIABLE")
        else state.settings.tr("VARIABLES (OK para insertar)", "VARIABLES (OK to insert)"),
        color = palette.background,
        fontSize = 10.sp,
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Bold,
      )
      Text("${state.overlayCursor.coerceIn(0, names.lastIndex) + 1}/${names.size}", color = palette.backgroundShade, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily)
    }
    Row(modifier = Modifier.fillMaxSize()) {
      LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
        itemsIndexed(names) { idx, name ->
          val selected = idx == state.overlayCursor
          val bg = if (selected) palette.inkDark else palette.background
          val fg = if (selected) palette.background else palette.inkDarkest
          val v = state.variables[name]
          val valStr = when (v) {
            is EvalResult.RealResult -> formatDoubleClassWiz(v.value.toDouble(), state.settings)
            is EvalResult.ComplexResult -> "${formatDoubleClassWiz(v.re.toDouble(), state.settings)}+${formatDoubleClassWiz(v.im.toDouble(), state.settings)}i"
            else -> "0"
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(bg)
              .clickable { onSelectVar(idx) }
              .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("$name =", color = fg, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
            Text(valStr, color = fg, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
          }
        }
      }
      LcdScrollbar(listState, names.size, palette)
    }
  }
}

@Composable
fun LcdModeBodyView(
  state: CalculatorUiState,
  palette: LcdPalette,
  onSelectSpreadsheetCell: (Int, Int) -> Unit,
) {
  when (state.mode) {
    CalcMode.TABLE -> TableLcdView(state, palette)
    CalcMode.EQUATION -> EquationLcdView(state, palette)
    CalcMode.INEQUALITY -> InequalityLcdView(state, palette)
    CalcMode.MATRIX -> MatrixLcdView(state, palette)
    CalcMode.VECTOR -> VectorLcdView(state, palette)
    CalcMode.STATISTICS -> StatisticsLcdView(state, palette)
    CalcMode.DISTRIBUTION -> DistributionLcdView(state, palette)
    CalcMode.SPREADSHEET -> SpreadsheetLcdView(state, palette, onSelectSpreadsheetCell)
    CalcMode.RATIO -> RatioLcdView(state, palette)
    CalcMode.MATH_BOX -> MathBoxLcdView(state, palette)
    else -> {}
  }
}

@Composable
private fun TableLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val ts = state.tableState
  val s = state.settings
  when (ts.phase) {
    TableState.Phase.EDIT_F, TableState.Phase.EDIT_G -> {
      val isF = ts.phase == TableState.Phase.EDIT_F
      Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = if (isF) "f(x) =" else "g(x) =",
          color = palette.inkDark,
          fontSize = 12.sp,
          fontFamily = JetBrainsMonoFamily,
          fontWeight = FontWeight.Bold,
        )
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
          val doc = if (isF) ts.fDoc else ts.gDoc
          NaturalMathRowView(doc.root, doc.cursor, emptyList(), palette, fontSize = 19.sp)
        }
        Text(
          text = s.tr("EXE: siguiente | TOOLS: Rango/Tabla", "EXE: next | TOOLS: Range/Table"),
          color = palette.inkDark,
          fontSize = 9.5.sp,
          fontFamily = IbmPlexSansFamily,
        )
      }
    }
    TableState.Phase.RANGE_SETUP -> {
      val items = listOf(
        s.tr("Inicio", "Start") to ts.start.toString(),
        s.tr("Fin", "End") to ts.end.toString(),
        s.tr("Paso", "Step") to ts.step.toString(),
        s.tr("Ejecutar Tabla", "Execute Table") to "EXE",
      )
      Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Text(s.tr("Rango de Tabla", "Table Range"), color = palette.inkDarkest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        items.forEachIndexed { idx, (k, v) ->
          val sel = idx == ts.selectedRow
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (sel) palette.inkDark else palette.background)
              .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(k, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp)
            Text(v, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
          }
        }
      }
    }
    TableState.Phase.RESULT_TABLE -> {
      val listState = rememberLcdListState("table_res", ts.selectedRow, ts.rows.size)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 4.dp, vertical = 1.dp)) {
          Text("x", color = palette.background, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f))
          Text("f(x)", color = palette.background, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f))
          Text("g(x)", color = palette.background, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            itemsIndexed(ts.rows) { rIdx, r ->
              val rowSel = rIdx == ts.selectedRow
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (rowSel) palette.backgroundShade else palette.background)
                  .padding(horizontal = 4.dp, vertical = 2.dp),
              ) {
                Text(formatDoubleClassWiz(r.x, s), color = palette.inkDarkest, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f), maxLines = 1)
                Text(r.fx?.let { formatDoubleClassWiz(it, s) } ?: "—", color = palette.inkDarkest, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f), maxLines = 1)
                Text(r.gx?.let { formatDoubleClassWiz(it, s) } ?: "—", color = palette.inkDarkest, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.weight(1f), maxLines = 1)
              }
            }
          }
          LcdScrollbar(listState, ts.rows.size, palette)
        }
      }
    }
  }
}

@Composable
private fun EquationLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val eq = state.equationState
  val s = state.settings
  when (eq.subType) {
    EquationState.SubType.MENU -> {
      val opts = listOf(
        s.tr("1. Ec. simultánea", "1. Simul Equation"),
        s.tr("2. Polinómica", "2. Polynomial"),
        s.tr("3. Solver", "3. Solver"),
      )
      val listState = rememberLcdListState("eq_menu", eq.activeRow, opts.size)
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text(s.tr("Ecuación", "Equation"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
          itemsIndexed(opts) { idx, txt ->
            val sel = idx == eq.activeRow
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (sel) palette.inkDark else palette.background)
                .padding(horizontal = 6.dp, vertical = 3.5.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(txt, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = IbmPlexSansFamily, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
              Text("▶", color = if (sel) palette.background else palette.inkDark, fontSize = 9.sp)
            }
          }
        }
      }
    }
    EquationState.SubType.SIMUL_DIM -> {
      val opts = listOf(
        s.tr("2 incógnitas", "2 Unknowns"),
        s.tr("3 incógnitas", "3 Unknowns"),
        s.tr("4 incógnitas", "4 Unknowns"),
      )
      val listState = rememberLcdListState("eq_simul_dim", eq.activeRow, opts.size)
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text(s.tr("Ec. simultánea — Incógnitas", "Simul Equation — Unknowns"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
          itemsIndexed(opts) { idx, txt ->
            val sel = idx == eq.activeRow
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (sel) palette.inkDark else palette.background)
                .padding(horizontal = 6.dp, vertical = 3.5.dp),
            ) {
              Text(txt, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = IbmPlexSansFamily, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
            }
          }
        }
      }
    }
    EquationState.SubType.POLY_DEGREE -> {
      val opts = listOf(
        s.tr("Grado 2 (ax²+bx+c=0)", "Degree 2 (ax²+bx+c=0)"),
        s.tr("Grado 3 (ax³+bx²+cx+d=0)", "Degree 3 (ax³+...)"),
        s.tr("Grado 4 (ax⁴+...+e=0)", "Degree 4 (ax⁴+...)"),
      )
      val listState = rememberLcdListState("eq_poly_deg", eq.activeRow, opts.size)
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text(s.tr("Polinómica — Grado", "Polynomial — Degree"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
          itemsIndexed(opts) { idx, txt ->
            val sel = idx == eq.activeRow
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (sel) palette.inkDark else palette.background)
                .padding(horizontal = 6.dp, vertical = 3.5.dp),
            ) {
              Text(txt, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = IbmPlexSansFamily, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
            }
          }
        }
      }
    }
    EquationState.SubType.LINEAR_INPUT -> {
      val vars = listOf("x", "y", "z", "t")
      val cols = eq.unknowns + 1
      val listState = rememberLcdListState("eq_lin_in_${eq.unknowns}", eq.activeRow, eq.unknowns)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Text(
          text = (0 until eq.unknowns).joinToString("+") { "${'a' + it}${vars[it]}" } + "=c",
          color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily,
        )
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(eq.unknowns) { r ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until cols) {
                  val active = r == eq.activeRow && c == eq.activeCol
                  val valStr = if (active && eq.inputBuffer.isNotEmpty()) "${eq.inputBuffer}|" else eq.linearCoeffs[r][c]
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .background(if (active) palette.inkDarkest else palette.backgroundShade)
                      .border(0.6.dp, palette.inkMid)
                      .padding(vertical = 2.dp, horizontal = 3.dp),
                    contentAlignment = Alignment.CenterEnd,
                  ) {
                    Text(valStr, color = if (active) palette.background else palette.inkDarkest, fontSize = 10.5.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
                  }
                }
              }
            }
          }
          LcdScrollbar(listState, eq.unknowns, palette)
        }
      }
    }
    EquationState.SubType.LINEAR_RESULT -> {
      val vars = listOf("x", "y", "z", "t")
      Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        when (val res = eq.linearResult) {
          is LinearSystemResult.Unique -> {
            val listState = rememberLcdListState("eq_lin_res", eq.activeRow, res.values.size)
            Row(modifier = Modifier.fillMaxSize()) {
              LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
                itemsIndexed(res.values) { idx, v ->
                  val sel = idx == eq.activeRow
                  Row(
                    modifier = Modifier.fillMaxWidth().background(if (sel) palette.backgroundShade else palette.background).padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text("${vars[idx]} =", color = palette.inkDarkest, fontSize = 13.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                    Text(formatDoubleClassWiz(v, s), color = palette.inkDarkest, fontSize = 14.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                  }
                }
              }
              LcdScrollbar(listState, res.values.size, palette)
            }
          }
          LinearSystemResult.NoSolution -> Text(s.tr("Sin solución", "No Solution"), color = palette.inkDarkest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          LinearSystemResult.InfiniteSolutions -> Text(s.tr("Infinitas soluciones", "Infinite Solutions"), color = palette.inkDarkest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          null -> {}
        }
      }
    }
    EquationState.SubType.POLY_INPUT -> {
      val labels = (eq.degree downTo 0).map { p -> when (p) { 0 -> "d"; 1 -> "x"; else -> "x^$p" } }
      Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Text(
          text = labels.mapIndexed { i, l -> "${('a' + i)}$l" }.joinToString(" + ").replace("d", "") + " = 0",
          color = palette.inkDark, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily,
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          for (i in 0..eq.degree) {
            val active = i == eq.activeCol
            val valStr = if (active && eq.inputBuffer.isNotEmpty()) "${eq.inputBuffer}|" else eq.polyCoeffs[i]
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${'a' + i}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (active) palette.inkDarkest else palette.backgroundShade)
                  .border(0.8.dp, palette.inkMid)
                  .padding(vertical = 4.dp, horizontal = 4.dp),
                contentAlignment = Alignment.CenterEnd,
              ) {
                Text(valStr, color = if (active) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
              }
            }
          }
        }
      }
    }
    EquationState.SubType.POLY_RESULT -> {
      val listState = rememberLcdListState("eq_poly_res", eq.activeRow, eq.polyRoots.size)
      Row(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
          itemsIndexed(eq.polyRoots) { idx, root ->
            val sel = idx == eq.activeRow
            val str = if (abs(root.im) < 1e-9) {
              formatDoubleClassWiz(root.re, s)
            } else {
              val sign = if (root.im < 0) "-" else "+"
              "${formatDoubleClassWiz(root.re, s)}$sign${formatDoubleClassWiz(abs(root.im), s)}i"
            }
            Row(
              modifier = Modifier.fillMaxWidth().background(if (sel) palette.backgroundShade else palette.background).padding(horizontal = 6.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("x${idx + 1} =", color = palette.inkDarkest, fontSize = 13.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
              Text(str, color = palette.inkDarkest, fontSize = 13.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
            }
          }
        }
        LcdScrollbar(listState, eq.polyRoots.size, palette)
      }
    }
    EquationState.SubType.SOLVER_EQ -> {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(
          modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(s.tr("Solver — Ecuación", "Solver — Equation"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
          Text("x", color = palette.backgroundShade, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily)
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
          NaturalMathRowView(eq.solverDoc.root, eq.solverDoc.cursor, emptyList(), palette, fontSize = 18.sp)
        }
        Text(
          text = s.tr("Use [=] para igualdad | EXE: Resolver", "Use [=] for equation | EXE: Solve"),
          color = palette.inkDark,
          fontSize = 9.5.sp,
          fontFamily = IbmPlexSansFamily,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }
    }
    EquationState.SubType.SOLVER_GUESS -> {
      val guessText = if (eq.inputBuffer.isNotEmpty()) "${eq.inputBuffer}|" else formatDoubleClassWiz(eq.solverInitialX, s)
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text(s.tr("Solver — Valor inicial", "Solver — Initial Value"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().background(palette.backgroundShade.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
          NaturalMathRowView(eq.solverDoc.root, null, emptyList(), palette, fontSize = 12.sp)
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (eq.activeRow == 0) palette.inkDark else palette.background)
            .padding(horizontal = 6.dp, vertical = 3.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("x₀ =", color = if (eq.activeRow == 0) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
          Text(guessText, color = if (eq.activeRow == 0) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (eq.activeRow == 1) palette.inkDarkest else palette.backgroundShade)
            .padding(horizontal = 6.dp, vertical = 3.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = s.tr("[ Ejecutar ]", "[ Execute ]"),
            color = if (eq.activeRow == 1) palette.background else palette.inkDarkest,
            fontSize = 11.5.sp,
            fontFamily = IbmPlexSansFamily,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
    EquationState.SubType.SOLVER_RESULT -> {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text("Solver", color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp), verticalArrangement = Arrangement.Center) {
          if (eq.solverError != null) {
            Text(eq.solverError, color = palette.inkDarkest, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(s.tr("OK/BACK: Cambiar valor inicial x₀", "OK/BACK: Adjust initial x₀"), color = palette.inkDark, fontSize = 10.sp)
          } else if (eq.solverResult != null) {
            Text("x = ${formatDoubleClassWiz(eq.solverResult.first, s)}", color = palette.inkDarkest, fontSize = 17.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text("${s.tr("Izq−Der", "L−R")} = ${formatDoubleClassWiz(eq.solverResult.second, s)}", color = palette.inkDark, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
          }
        }
        Text(
          text = s.tr("OK/BACK: Cambiar x₀ | AC: Nueva ec.", "OK/BACK: Edit x₀ | AC: New eq."),
          color = palette.inkDark,
          fontSize = 9.sp,
          fontFamily = IbmPlexSansFamily,
          modifier = Modifier.padding(horizontal = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun InequalityLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val iq = state.inequalityState
  val s = state.settings
  when (iq.phase) {
    InequalityState.Phase.SELECT_DEGREE -> {
      val opts = listOf(s.tr("Grado 2 (ax²+bx+c)", "Degree 2"), s.tr("Grado 3 (ax³+...)", "Degree 3"), s.tr("Grado 4 (ax⁴+...)", "Degree 4"))
      val listState = rememberLcdListState("ineq_deg", iq.activeCol, opts.size)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Text(s.tr("Seleccione grado:", "Select Degree:"), color = palette.inkDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
          itemsIndexed(opts) { idx, t ->
            val sel = idx == iq.activeCol
            Box(modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(4.dp)) {
              Text(t, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp)
            }
          }
        }
      }
    }
    InequalityState.Phase.SELECT_OP -> {
      val listState = rememberLcdListState("ineq_op", iq.activeCol, InequalityOp.entries.size)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Text(s.tr("Seleccione desigualdad:", "Select Inequality:"), color = palette.inkDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            itemsIndexed(InequalityOp.entries) { idx, op ->
              val sel = idx == iq.activeCol
              Box(modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(4.dp)) {
                Text("P(x) ${op.symbol} 0", color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
              }
            }
          }
          LcdScrollbar(listState, InequalityOp.entries.size, palette)
        }
      }
    }
    InequalityState.Phase.COEFF_INPUT -> {
      Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Text("P(x) ${iq.op.symbol} 0", color = palette.inkDark, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          for (i in 0..iq.degree) {
            val active = i == iq.activeCol
            val valStr = if (active && iq.inputBuffer.isNotEmpty()) "${iq.inputBuffer}|" else iq.coeffs[i]
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${'a' + i}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily)
              Box(
                modifier = Modifier.fillMaxWidth().background(if (active) palette.inkDarkest else palette.backgroundShade).border(0.8.dp, palette.inkMid).padding(4.dp),
                contentAlignment = Alignment.CenterEnd,
              ) {
                Text(valStr, color = if (active) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
              }
            }
          }
        }
      }
    }
    InequalityState.Phase.RESULT -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text(s.tr("Solución:", "Solution:"), color = palette.inkDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(iq.solutionText, color = palette.inkDarkest, fontSize = 15.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun MatrixLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val ms = state.matrixState
  val s = state.settings
  val names = listOf("MatA", "MatB", "MatC", "MatD")
  when (ms.phase) {
    MatrixAppState.Phase.PICK_MATRIX -> {
      val listState = rememberLcdListState("mat_pick", ms.editRow, names.size)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text(s.tr("Definir Matriz (OK)", "Define Matrix (OK)"), color = palette.background, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            itemsIndexed(names) { idx, n ->
              val sel = idx == ms.editRow
              val m = ms.matrices[n]
              val dimStr = if (m != null) "${m.rows}×${m.cols}" else s.tr("Vacía", "None")
              Row(
                modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(horizontal = 6.dp, vertical = 2.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(n, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                Text(dimStr, color = if (sel) palette.background else palette.inkDark, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily)
              }
            }
          }
          LcdScrollbar(listState, names.size, palette)
        }
      }
    }
    MatrixAppState.Phase.PICK_DIM -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text("${ms.editingMatrixName}: ${s.tr("Dimensiones (1-4)", "Dimensions (1-4)")}", color = palette.inkDarkest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().background(if (ms.editRow == 0) palette.inkDark else palette.background).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(s.tr("Filas:", "Rows:"), color = if (ms.editRow == 0) palette.background else palette.inkDarkest, fontSize = 12.sp)
          Text("${ms.rows}", color = if (ms.editRow == 0) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
        }
        Row(modifier = Modifier.fillMaxWidth().background(if (ms.editRow == 1) palette.inkDark else palette.background).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(s.tr("Columnas:", "Columns:"), color = if (ms.editRow == 1) palette.background else palette.inkDarkest, fontSize = 12.sp)
          Text("${ms.cols}", color = if (ms.editRow == 1) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
        }
      }
    }
    MatrixAppState.Phase.EDIT_GRID -> {
      val cur = ms.matrices[ms.editingMatrixName] ?: return
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Text("${ms.editingMatrixName} [${ms.editRow + 1},${ms.editCol + 1}] — EXE:${s.tr("Calcular", "Calc")}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily)
        Spacer(Modifier.height(2.dp))
        for (r in 0 until cur.rows) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (c in 0 until cur.cols) {
              val active = r == ms.editRow && c == ms.editCol
              val text = if (active && ms.inputBuffer.isNotEmpty()) "${ms.inputBuffer}|" else formatDoubleClassWiz(cur.data[r][c], s)
              Box(
                modifier = Modifier.weight(1f).background(if (active) palette.inkDarkest else palette.backgroundShade).border(0.6.dp, palette.inkMid).padding(2.dp),
                contentAlignment = Alignment.CenterEnd,
              ) {
                Text(text, color = if (active) palette.background else palette.inkDarkest, fontSize = 10.5.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
              }
            }
          }
          Spacer(Modifier.height(2.dp))
        }
      }
    }
    MatrixAppState.Phase.WORK -> {}
  }
}

@Composable
private fun VectorLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val vs = state.vectorState
  val s = state.settings
  val names = listOf("VctA", "VctB", "VctC", "VctD")
  when (vs.phase) {
    VectorAppState.Phase.PICK_VECTOR -> {
      val listState = rememberLcdListState("vct_pick", vs.editIdx, names.size)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp)) {
          Text(s.tr("Definir Vector (OK)", "Define Vector (OK)"), color = palette.background, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            itemsIndexed(names) { idx, n ->
              val sel = idx == vs.editIdx
              val v = vs.vectors[n]
              val dimStr = if (v != null) "${v.data.size}D" else s.tr("Vacío", "None")
              Row(
                modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(horizontal = 6.dp, vertical = 2.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(n, color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                Text(dimStr, color = if (sel) palette.background else palette.inkDark, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily)
              }
            }
          }
          LcdScrollbar(listState, names.size, palette)
        }
      }
    }
    VectorAppState.Phase.PICK_DIM -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text("${vs.editingVectorName}: ${s.tr("Dimensión", "Dimension")}", color = palette.inkDarkest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        listOf(2, 3).forEach { d ->
          val sel = vs.dim == d
          Box(modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(5.dp)) {
            Text("${d}D (${if (d == 2) "x, y" else "x, y, z"})", color = if (sel) palette.background else palette.inkDarkest, fontSize = 12.sp)
          }
        }
      }
    }
    VectorAppState.Phase.EDIT_GRID -> {
      val cur = vs.vectors[vs.editingVectorName] ?: return
      val axes = listOf("x", "y", "z")
      Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Text("${vs.editingVectorName} — EXE:${s.tr("Calcular", "Calc")}", color = palette.inkDark, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily)
        Spacer(Modifier.height(4.dp))
        cur.data.forEachIndexed { i, v ->
          val active = i == vs.editIdx
          val text = if (active && vs.inputBuffer.isNotEmpty()) "${vs.inputBuffer}|" else formatDoubleClassWiz(v, s)
          Row(
            modifier = Modifier.fillMaxWidth().background(if (active) palette.inkDarkest else palette.backgroundShade).padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("${axes[i]}:", color = if (active) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
            Text(text, color = if (active) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
          }
          Spacer(Modifier.height(2.dp))
        }
      }
    }
    VectorAppState.Phase.WORK -> {}
  }
}

@Composable
private fun StatisticsLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val st = state.statsState
  val s = state.settings
  when (st.phase) {
    StatsAppState.Phase.SELECT_MODEL -> {
      val listState = rememberLcdListState("stat_model", st.activeRow, StatModelType.entries.size)
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(s.tr("Estadística", "Statistics"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
          Text("${st.activeRow + 1}/${StatModelType.entries.size}", color = palette.backgroundShade, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily)
        }
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            itemsIndexed(StatModelType.entries) { idx, m ->
              val sel = idx == st.activeRow
              Box(modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(horizontal = 6.dp, vertical = 2.5.dp)) {
                Text("${idx + 1}. ${s.tr(m.labelEs, m.labelEn)}", color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.sp)
              }
            }
          }
          LcdScrollbar(listState, StatModelType.entries.size, palette)
        }
      }
    }
    StatsAppState.Phase.DATA_GRID -> {
      val showY = st.model != StatModelType.ONE_VAR
      val showFreq = s.statFrequency
      val totalRows = st.xValues.size + 1
      val listState = rememberLcdListState("stat_data", st.activeRow, totalRows)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 4.dp, vertical = 1.dp)) {
          Text("#", color = palette.background, fontSize = 9.5.sp, modifier = Modifier.width(20.dp))
          Text("x", color = palette.background, fontSize = 9.5.sp, modifier = Modifier.weight(1f))
          if (showY) Text("y", color = palette.background, fontSize = 9.5.sp, modifier = Modifier.weight(1f))
          if (showFreq) Text("Freq", color = palette.background, fontSize = 9.5.sp, modifier = Modifier.weight(0.8f))
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(totalRows) { r ->
              val xStr = if (r < st.xValues.size) formatDoubleClassWiz(st.xValues[r], s) else ""
              val yStr = if (r < st.yValues.size) formatDoubleClassWiz(st.yValues[r], s) else ""
              val fStr = if (r < st.freqValues.size) formatDoubleClassWiz(st.freqValues[r], s) else ""
              Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text("${r + 1}", color = palette.inkDark, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.width(20.dp))
                StatCell(r == st.activeRow && st.activeCol == 0, st.inputBuffer, xStr, palette, Modifier.weight(1f))
                if (showY) StatCell(r == st.activeRow && st.activeCol == 1, st.inputBuffer, yStr, palette, Modifier.weight(1f))
                if (showFreq) {
                  val fCol = if (showY) 2 else 1
                  StatCell(r == st.activeRow && st.activeCol == fCol, st.inputBuffer, fStr, palette, Modifier.weight(0.8f))
                }
              }
            }
          }
          LcdScrollbar(listState, totalRows, palette)
        }
      }
    }
    StatsAppState.Phase.RESULTS -> {
      val sum = st.summary
      val items = buildList {
        add("n" to "${sum.n}")
        add("x̄" to formatDoubleClassWiz(sum.meanX, s))
        add("Σx" to formatDoubleClassWiz(sum.sumX, s))
        add("Σx²" to formatDoubleClassWiz(sum.sumX2, s))
        add("σx" to formatDoubleClassWiz(sum.sigmaX, s))
        add("sx" to formatDoubleClassWiz(sum.sX, s))
        add("min(x)" to formatDoubleClassWiz(sum.minX, s))
        add("Q₁" to formatDoubleClassWiz(sum.q1, s))
        add("Med" to formatDoubleClassWiz(sum.med, s))
        add("Q₃" to formatDoubleClassWiz(sum.q3, s))
        add("max(x)" to formatDoubleClassWiz(sum.maxX, s))
        if (st.model != StatModelType.ONE_VAR) {
          add("ȳ" to formatDoubleClassWiz(sum.meanY, s))
          add("Σxy" to formatDoubleClassWiz(sum.sumXY, s))
          add("a" to formatDoubleClassWiz(sum.regA, s))
          add("b" to formatDoubleClassWiz(sum.regB, s))
          if (st.model == StatModelType.QUADRATIC) add("c" to formatDoubleClassWiz(sum.regC, s))
          add("r" to formatDoubleClassWiz(sum.regR, s))
          add("r²" to formatDoubleClassWiz(sum.regR2, s))
        }
      }
      val listState = rememberLcdListState("stat_res", st.activeRow, items.size)
      Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
          itemsIndexed(items) { idx, (k, v) ->
            val sel = idx == st.activeRow
            Row(
              modifier = Modifier.fillMaxWidth().background(if (sel) palette.backgroundShade else palette.background).padding(horizontal = 6.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("$k =", color = palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
              Text(v, color = palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
            }
          }
        }
        LcdScrollbar(listState, items.size, palette)
      }
    }
  }
}

@Composable
private fun StatCell(active: Boolean, buf: String, value: String, palette: LcdPalette, modifier: Modifier) {
  val text = if (active && buf.isNotEmpty()) "$buf|" else value
  Box(
    modifier = modifier.background(if (active) palette.inkDarkest else palette.backgroundShade).border(0.5.dp, palette.inkMid).padding(horizontal = 3.dp, vertical = 1.dp),
    contentAlignment = Alignment.CenterEnd,
  ) {
    Text(text, color = if (active) palette.background else palette.inkDarkest, fontSize = 10.5.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
  }
}

@Composable
private fun DistributionLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val ds = state.distributionState
  val s = state.settings
  when (ds.phase) {
    DistributionState.Phase.SELECT_TYPE -> {
      val listState = rememberLcdListState("dist_type", ds.selectedParamIdx, DistType.entries.size)
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 6.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(s.tr("Distribución", "Distribution"), color = palette.background, fontSize = 10.sp, fontFamily = IbmPlexSansFamily, fontWeight = FontWeight.Bold)
          Text("${ds.selectedParamIdx + 1}/${DistType.entries.size}", color = palette.backgroundShade, fontSize = 9.sp, fontFamily = JetBrainsMonoFamily)
        }
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            itemsIndexed(DistType.entries) { idx, t ->
              val sel = idx == ds.selectedParamIdx
              Box(modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(horizontal = 6.dp, vertical = 2.5.dp)) {
                Text("${idx + 1}. ${s.tr(t.labelEs, t.labelEn)}", color = if (sel) palette.background else palette.inkDarkest, fontSize = 11.5.sp)
              }
            }
          }
          LcdScrollbar(listState, DistType.entries.size, palette)
        }
      }
    }
    DistributionState.Phase.PARAM_INPUT -> {
      val keys = ModeKeyHandler.distParamKeys(ds.type)
      val listState = rememberLcdListState("dist_params_${ds.type.name}", ds.selectedParamIdx, keys.size)
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Text(s.tr(ds.type.labelEs, ds.type.labelEn), color = palette.inkDark, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(modifier = Modifier.fillMaxSize()) {
          LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(keys) { idx, k ->
              val active = idx == ds.selectedParamIdx
              val valStr = if (active && ds.inputBuffer.isNotEmpty()) "${ds.inputBuffer}|" else (ds.params[k] ?: "0")
              Row(
                modifier = Modifier.fillMaxWidth().background(if (active) palette.inkDarkest else palette.backgroundShade).padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("$k :", color = if (active) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
                Text(valStr, color = if (active) palette.background else palette.inkDarkest, fontSize = 11.5.sp, fontFamily = JetBrainsMonoFamily)
              }
            }
          }
          LcdScrollbar(listState, keys.size, palette)
        }
      }
    }
    DistributionState.Phase.RESULT -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text("${s.tr(ds.type.labelEs, ds.type.labelEn)}:", color = palette.inkDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("p = ${formatDoubleClassWiz(ds.resultValue ?: 0.0, s)}", color = palette.inkDarkest, fontSize = 19.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun SpreadsheetLcdView(
  state: CalculatorUiState,
  palette: LcdPalette,
  onSelectCell: (Int, Int) -> Unit,
) {
  val sh = state.spreadsheetState
  val s = state.settings
  val activeName = sh.cellName()
  val formulaBar = if (sh.editing) "${sh.editBuffer}|" else (sh.cells[activeName] ?: "")
  Column(modifier = Modifier.fillMaxSize().padding(3.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().background(palette.inkDarkest).padding(horizontal = 5.dp, vertical = 1.5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text("$activeName: $formulaBar", color = palette.background, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1)
      Text("OK:${s.tr("Editar", "Edit")}", color = palette.backgroundShade, fontSize = 9.sp)
    }
    Row(modifier = Modifier.fillMaxWidth().background(palette.backgroundShade)) {
      Text("", modifier = Modifier.width(18.dp), fontSize = 9.sp)
      for (c in 0..4) {
        Text("${'A' + c}", color = palette.inkDarkest, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
      }
    }
    val startRow = (sh.activeRow - 2).coerceIn(0, 10)
    for (r in startRow until (startRow + 5).coerceAtMost(15)) {
      Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Text("${r + 1}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, textAlign = TextAlign.Center, modifier = Modifier.width(18.dp))
        for (c in 0..4) {
          val key = "${'A' + c}${r + 1}"
          val sel = c == sh.activeCol && r == sh.activeRow
          val valNum = sh.evaluated[key]
          val txt = when {
            valNum != null && valNum.isNaN() -> "Err"
            valNum != null -> formatDoubleClassWiz(valNum, s)
            else -> sh.cells[key] ?: ""
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .background(if (sel) palette.inkDarkest else palette.background)
              .border(0.5.dp, palette.inkMid)
              .clickable { onSelectCell(c, r) }
              .padding(horizontal = 2.dp),
            contentAlignment = Alignment.CenterEnd,
          ) {
            Text(txt, color = if (sel) palette.background else palette.inkDarkest, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, maxLines = 1, overflow = TextOverflow.Clip)
          }
        }
      }
    }
  }
}

@Composable
private fun RatioLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val rs = state.ratioState
  val s = state.settings
  when (rs.phase) {
    RatioState.Phase.SELECT_TYPE -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text(s.tr("Formato de proporción:", "Ratio Form:"), color = palette.inkDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().background(if (rs.xOnLeft) palette.inkDark else palette.background).padding(5.dp)) {
          Text("1. A : B = X : D", color = if (rs.xOnLeft) palette.background else palette.inkDarkest, fontSize = 13.sp, fontFamily = JetBrainsMonoFamily)
        }
        Box(modifier = Modifier.fillMaxWidth().background(if (!rs.xOnLeft) palette.inkDark else palette.background).padding(5.dp)) {
          Text("2. A : B = C : X", color = if (!rs.xOnLeft) palette.background else palette.inkDarkest, fontSize = 13.sp, fontFamily = JetBrainsMonoFamily)
        }
      }
    }
    RatioState.Phase.INPUT -> {
      val labels = if (rs.xOnLeft) listOf("A", "B", "D") else listOf("A", "B", "C")
      val vals = listOf(rs.a, rs.b, rs.cOrD)
      Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(if (rs.xOnLeft) "A : B = X : D" else "A : B = C : X", color = palette.inkDark, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          labels.forEachIndexed { i, l ->
            val act = i == rs.activeField
            val txt = if (act && rs.inputBuffer.isNotEmpty()) "${rs.inputBuffer}|" else vals[i]
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(l, color = palette.inkDark, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily)
              Box(modifier = Modifier.fillMaxWidth().background(if (act) palette.inkDarkest else palette.backgroundShade).border(0.8.dp, palette.inkMid).padding(5.dp), contentAlignment = Alignment.CenterEnd) {
                Text(txt, color = if (act) palette.background else palette.inkDarkest, fontSize = 13.sp, fontFamily = JetBrainsMonoFamily)
              }
            }
          }
        }
      }
    }
    RatioState.Phase.RESULT -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text("X =", color = palette.inkDark, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
        rs.solution?.let { ExactRealView(it, s, ResultFormatMode.STANDARD, palette) }
      }
    }
  }
}

@Composable
private fun MathBoxLcdView(state: CalculatorUiState, palette: LcdPalette) {
  val mb = state.mathBoxState
  val s = state.settings
  when (mb.phase) {
    MathBoxState.Phase.SELECT_TYPE -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text(s.tr("Caja Matemática:", "Math Box:"), color = palette.inkDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        MathBoxType.entries.forEachIndexed { idx, t ->
          val sel = t == mb.type
          Box(modifier = Modifier.fillMaxWidth().background(if (sel) palette.inkDark else palette.background).padding(5.dp)) {
            Text("${idx + 1}. ${s.tr(t.labelEs, t.labelEn)}", color = if (sel) palette.background else palette.inkDarkest, fontSize = 12.5.sp)
          }
        }
      }
    }
    MathBoxState.Phase.PARAMS -> {
      Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
        Text(s.tr(mb.type.labelEs, mb.type.labelEn), color = palette.inkDarkest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        val cText = if (mb.activeParam == 0 && mb.inputBuffer.isNotEmpty()) "${mb.inputBuffer}|" else "${mb.objectCount}"
        val tText = if (mb.activeParam == 1 && mb.inputBuffer.isNotEmpty()) "${mb.inputBuffer}|" else "${mb.trials}"
        Row(modifier = Modifier.fillMaxWidth().background(if (mb.activeParam == 0) palette.inkDarkest else palette.backgroundShade).padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(if (mb.type == MathBoxType.DICE) s.tr("Dados (1-3):", "Dice (1-3):") else s.tr("Monedas (1-3):", "Coins (1-3):"), color = if (mb.activeParam == 0) palette.background else palette.inkDarkest, fontSize = 11.5.sp)
          Text(cText, color = if (mb.activeParam == 0) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
        }
        Spacer(Modifier.height(3.dp))
        Row(modifier = Modifier.fillMaxWidth().background(if (mb.activeParam == 1) palette.inkDarkest else palette.backgroundShade).padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(s.tr("Intentos (1-250):", "Trials (1-250):"), color = if (mb.activeParam == 1) palette.background else palette.inkDarkest, fontSize = 11.5.sp)
          Text(tText, color = if (mb.activeParam == 1) palette.background else palette.inkDarkest, fontSize = 12.sp, fontFamily = JetBrainsMonoFamily)
        }
      }
    }
    MathBoxState.Phase.RESULTS -> {
      val maxCount = mb.counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
      Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Text("${s.tr("Total", "Total")}: ${mb.trials} — EXE:${s.tr("Repetir", "Retry")}", color = palette.inkDark, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          itemsIndexed(mb.counts.entries.toList()) { _, (sum, cnt) ->
            val pct = (cnt * 100.0) / mb.trials
            val label = if (mb.type == MathBoxType.COIN) "${sum}C" else "$sum"
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
              Text(label, color = palette.inkDarkest, fontSize = 10.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.width(28.dp))
              Box(modifier = Modifier.weight(1f).height(8.dp).background(palette.backgroundShade)) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(cnt.toFloat() / maxCount).background(palette.inkDarkest))
              }
              Spacer(Modifier.width(4.dp))
              Text("$cnt (${pct.toInt()}%)", color = palette.inkDarkest, fontSize = 9.5.sp, fontFamily = JetBrainsMonoFamily, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
            }
          }
        }
      }
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
    // Crisp monochrome QR matrix with quiet zone
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
          text = state.qrTitle,
          color = palette.inkDarkest,
          fontSize = 11.sp,
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
