package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.calcDataStore by preferencesDataStore(name = "classwiz_991la_prefs")

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  private object PrefKeys {
    val ANGLE = stringPreferencesKey("angle_unit")
    val IO_MODE = stringPreferencesKey("io_mode")
    val NUM_FMT = stringPreferencesKey("num_fmt")
    val FIX_DIGITS = intPreferencesKey("fix_digits")
    val SCI_DIGITS = intPreferencesKey("sci_digits")
    val FRAC_FMT = stringPreferencesKey("frac_fmt")
    val CPLX_FMT = stringPreferencesKey("cplx_fmt")
    val STAT_FREQ = booleanPreferencesKey("stat_freq")
    val CONTRAST = floatPreferencesKey("lcd_contrast")
    val LCD_THEME = stringPreferencesKey("lcd_theme")
    val LANGUAGE = stringPreferencesKey("calc_lang")
    val HAPTICS = booleanPreferencesKey("haptics")
  }

  init {
    viewModelScope.launch {
      val prefs = getApplication<Application>().calcDataStore.data.first()
      val loaded = CalcSettings(
        angleUnit = prefs[PrefKeys.ANGLE]?.let { runCatching { AngleUnit.valueOf(it) }.getOrNull() } ?: AngleUnit.DEG,
        inputOutput = prefs[PrefKeys.IO_MODE]?.let { runCatching { InputOutputMode.valueOf(it) }.getOrNull() } ?: InputOutputMode.MATH_MATH,
        numberFormat = prefs[PrefKeys.NUM_FMT]?.let { runCatching { NumberFormatMode.valueOf(it) }.getOrNull() } ?: NumberFormatMode.NORM_1,
        fixDigits = prefs[PrefKeys.FIX_DIGITS] ?: 2,
        sciDigits = prefs[PrefKeys.SCI_DIGITS] ?: 3,
        fractionFormat = prefs[PrefKeys.FRAC_FMT]?.let { runCatching { FractionFormat.valueOf(it) }.getOrNull() } ?: FractionFormat.IMPROPER,
        complexFormat = prefs[PrefKeys.CPLX_FMT]?.let { runCatching { ComplexFormat.valueOf(it) }.getOrNull() } ?: ComplexFormat.RECTANGULAR,
        statFrequency = prefs[PrefKeys.STAT_FREQ] ?: false,
        lcdContrast = prefs[PrefKeys.CONTRAST] ?: 0.85f,
        lcdTheme = prefs[PrefKeys.LCD_THEME]?.let { runCatching { LcdThemeMode.valueOf(it) }.getOrNull() } ?: LcdThemeMode.CLASSIC,
        language = prefs[PrefKeys.LANGUAGE]?.let { runCatching { CalcLanguage.valueOf(it) }.getOrNull() } ?: CalcLanguage.ES,
        hapticsEnabled = prefs[PrefKeys.HAPTICS] ?: true,
      )
      _uiState.update { it.copy(settings = loaded) }
    }
  }

  private fun persistSettings(s: CalcSettings) {
    viewModelScope.launch {
      getApplication<Application>().calcDataStore.edit { p ->
        p[PrefKeys.ANGLE] = s.angleUnit.name
        p[PrefKeys.IO_MODE] = s.inputOutput.name
        p[PrefKeys.NUM_FMT] = s.numberFormat.name
        p[PrefKeys.FIX_DIGITS] = s.fixDigits
        p[PrefKeys.SCI_DIGITS] = s.sciDigits
        p[PrefKeys.FRAC_FMT] = s.fractionFormat.name
        p[PrefKeys.CPLX_FMT] = s.complexFormat.name
        p[PrefKeys.STAT_FREQ] = s.statFrequency
        p[PrefKeys.CONTRAST] = s.lcdContrast
        p[PrefKeys.LCD_THEME] = s.lcdTheme.name
        p[PrefKeys.LANGUAGE] = s.language.name
        p[PrefKeys.HAPTICS] = s.hapticsEnabled
      }
    }
  }

  fun onKey(key: CalcKey) {
    _uiState.update { cur -> processKey(cur.copy(toastBanner = null), key) }
  }

  fun selectHomeApp(mode: CalcMode) {
    _uiState.update { cur -> switchToMode(cur, mode) }
  }

  fun selectOverlayItem(index: Int) {
    _uiState.update { cur ->
      val positioned = cur.copy(overlayCursor = index)
      when (cur.overlay) {
        OverlayScreen.CATALOG_MENU, OverlayScreen.TOOLS_MENU, OverlayScreen.FORMAT_MENU, OverlayScreen.FX_MENU -> confirmCatalogOrMenu(positioned)
        OverlayScreen.SETTINGS_MENU -> confirmSettingsItem(positioned)
        OverlayScreen.VARIABLE_MENU -> confirmVariableItem(positioned)
        else -> positioned
      }
    }
  }

  fun selectSettingsContrast(value: Float) {
    val clamped = value.coerceIn(0.45f, 1.0f)
    _uiState.update { cur ->
      val updated = cur.settings.copy(lcdContrast = clamped)
      persistSettings(updated)
      cur.copy(settings = updated)
    }
  }

  fun selectSpreadsheetCell(col: Int, row: Int) {
    _uiState.update { cur ->
      if (cur.mode != CalcMode.SPREADSHEET) cur
      else cur.copy(spreadsheetState = cur.spreadsheetState.copy(activeCol = col.coerceIn(0, 4), activeRow = row.coerceIn(0, 14)))
    }
  }

  private fun processKey(state: CalculatorUiState, key: CalcKey): CalculatorUiState {
    // Power management
    if (!state.isPoweredOn) {
      return if (key == CalcKey.ON) state.copy(isPoweredOn = true, isShiftActive = false) else state
    }

    val wasShift = state.isShiftActive
    if (key == CalcKey.SHIFT) {
      return state.copy(isShiftActive = !wasShift)
    }
    if (key == CalcKey.AC && wasShift) {
      return state.copy(isPoweredOn = false, isShiftActive = false, overlay = OverlayScreen.NONE)
    }
    if (key == CalcKey.ON) {
      return state.copy(
        isShiftActive = false,
        overlay = OverlayScreen.NONE,
        editingFxTarget = null,
        storePending = false,
        doc = EditorDocument(),
        currentResult = null,
        resultFormatOverride = null,
      )
    }

    // Preserve SHIFT if user simply navigates D-pad/page inside a menu overlay; otherwise consume it cleanly
    val baseState = state.copy(isShiftActive = false)

    // System & utility overlays (match ClassWiz CW behavior)
    when (key) {
      CalcKey.HOME -> {
        if (wasShift) return openQrOverlay(baseState, welcome = true)
        return baseState.copy(
          overlay = if (baseState.overlay == OverlayScreen.HOME_GRID) OverlayScreen.NONE else OverlayScreen.HOME_GRID,
          overlayCursor = baseState.mode.ordinal,
          overlayStack = emptyList(),
          editingFxTarget = null,
          storePending = false,
        )
      }
      CalcKey.SETTINGS -> return baseState.copy(
        overlay = if (baseState.overlay == OverlayScreen.SETTINGS_MENU) OverlayScreen.NONE else OverlayScreen.SETTINGS_MENU,
        overlayCursor = 0,
        settingsPage = 0,
        storePending = false,
      )
      CalcKey.CATALOG -> {
        val root = MenuBuilders.buildCatalogRoot(baseState)
        return baseState.copy(
          overlay = OverlayScreen.CATALOG_MENU,
          overlayCursor = 0,
          overlayStack = listOf(baseState.settings.tr("Catálogo", "Catalog") to root),
          storePending = false,
        )
      }
      CalcKey.TOOLS -> {
        val tools = MenuBuilders.buildToolsMenu(baseState)
        return baseState.copy(
          overlay = OverlayScreen.TOOLS_MENU,
          overlayCursor = 0,
          overlayStack = listOf(baseState.settings.tr("Herramientas", "Tools") to tools),
          storePending = false,
        )
      }
      CalcKey.VARIABLE -> {
        return baseState.copy(
          overlay = OverlayScreen.VARIABLE_MENU,
          overlayCursor = 0,
          storePending = wasShift, // SHIFT + VARIABLE = STO
        )
      }
      CalcKey.FX -> {
        if (wasShift) {
          return if (baseState.mode == CalcMode.TABLE) {
            baseState.copy(
              overlay = OverlayScreen.NONE,
              tableState = baseState.tableState.copy(phase = TableState.Phase.EDIT_G),
            )
          } else {
            baseState.copy(
              overlay = OverlayScreen.NONE,
              editingFxTarget = 1,
              storePending = false,
            )
          }
        }
        val fxItems = MenuBuilders.buildFxMenu(baseState)
        return baseState.copy(
          overlay = OverlayScreen.FX_MENU,
          overlayCursor = 0,
          overlayStack = listOf("f(x) / g(x)" to fxItems),
          storePending = false,
        )
      }
      CalcKey.FORMAT -> {
        if (wasShift) {
          // SHIFT + FORMAT = S<=>D quick toggle
          val nextFmt = if (baseState.resultFormatOverride == ResultFormatMode.DECIMAL) ResultFormatMode.STANDARD else ResultFormatMode.DECIMAL
          return baseState.copy(resultFormatOverride = nextFmt)
        }
        val fmtItems = MenuBuilders.buildFormatMenu(baseState)
        return baseState.copy(
          overlay = OverlayScreen.FORMAT_MENU,
          overlayCursor = 0,
          overlayStack = listOf(baseState.settings.tr("Formato", "Format") to fmtItems),
        )
      }
      else -> {}
    }

    // SHIFT + x -> QR Code view (authentic ClassWiz fx-991LA CW location)
    if (wasShift && key == CalcKey.VAR_X) {
      return if (baseState.overlay == OverlayScreen.QR_VIEW) {
        baseState.copy(overlay = OverlayScreen.NONE)
      } else {
        openQrOverlay(baseState, welcome = false)
      }
    }

    // If an overlay is open, route keys to overlay
    if (baseState.overlay != OverlayScreen.NONE) {
      return handleOverlayKey(baseState, key)
    }

    // Exiting quick f(x)/g(x) editor cleanly with BACK
    if (baseState.editingFxTarget != null && key == CalcKey.BACK) {
      return baseState.copy(editingFxTarget = null)
    }

    // Route to active mode handler first if applicable
    val modeHandled = ModeKeyHandler.handleModeKey(baseState, key, wasShift)
    if (modeHandled != null) return modeHandled

    // Otherwise handle expression editor (Calculate / Complex / Base-N / f(x)&g(x) / Solver / Matrix&Vector WORK)
    return handleExpressionEditorKey(baseState, key, wasShift)
  }

  private fun handleOverlayKey(state: CalculatorUiState, key: CalcKey): CalculatorUiState {
    return when (state.overlay) {
      OverlayScreen.HOME_GRID -> handleHomeGridKey(state, key)
      OverlayScreen.CATALOG_MENU, OverlayScreen.TOOLS_MENU, OverlayScreen.FORMAT_MENU, OverlayScreen.FX_MENU -> handleMenuStackKey(state, key)
      OverlayScreen.SETTINGS_MENU -> handleSettingsKey(state, key)
      OverlayScreen.VARIABLE_MENU -> handleVariableMenuKey(state, key)
      OverlayScreen.QR_VIEW -> when (key) {
        CalcKey.BACK, CalcKey.AC, CalcKey.OK, CalcKey.EXE -> state.copy(overlay = OverlayScreen.NONE)
        else -> state
      }
      OverlayScreen.NONE -> state
    }
  }

  private fun handleHomeGridKey(state: CalculatorUiState, key: CalcKey): CalculatorUiState {
    val total = CalcMode.entries.size
    val cols = 4
    val idx = state.overlayCursor.coerceIn(0, total - 1)
    return when (key) {
      CalcKey.LEFT -> state.copy(overlayCursor = (idx - 1 + total) % total)
      CalcKey.RIGHT -> state.copy(overlayCursor = (idx + 1) % total)
      CalcKey.UP -> state.copy(overlayCursor = (idx - cols).let { if (it < 0) (idx + ((total - 1) / cols) * cols).coerceAtMost(total - 1) else it })
      CalcKey.DOWN -> state.copy(overlayCursor = (idx + cols).let { if (it >= total) idx % cols else it })
      CalcKey.PAGE_UP -> state.copy(overlayCursor = (idx - cols * 2).coerceAtLeast(0))
      CalcKey.PAGE_DOWN -> state.copy(overlayCursor = (idx + cols * 2).coerceAtMost(total - 1))
      CalcKey.OK, CalcKey.EXE -> switchToMode(state, CalcMode.entries[idx])
      CalcKey.BACK, CalcKey.AC -> state.copy(overlay = OverlayScreen.NONE)
      else -> state
    }
  }

  private fun switchToMode(state: CalculatorUiState, mode: CalcMode): CalculatorUiState {
    val resetDoc = EditorDocument()
    return when (mode) {
      CalcMode.MATRIX -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        matrixState = state.matrixState.copy(phase = MatrixAppState.Phase.PICK_MATRIX, editRow = 0)
      )
      CalcMode.VECTOR -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        vectorState = state.vectorState.copy(phase = VectorAppState.Phase.PICK_VECTOR, editIdx = 0)
      )
      CalcMode.TABLE -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        tableState = state.tableState.copy(phase = TableState.Phase.EDIT_F)
      )
      CalcMode.EQUATION -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        equationState = state.equationState.copy(subType = EquationState.SubType.MENU, activeRow = 0)
      )
      CalcMode.INEQUALITY -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        inequalityState = state.inequalityState.copy(phase = InequalityState.Phase.SELECT_DEGREE, activeCol = 0)
      )
      CalcMode.STATISTICS -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        statsState = state.statsState.copy(phase = StatsAppState.Phase.SELECT_MODEL, activeRow = 0)
      )
      CalcMode.DISTRIBUTION -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        distributionState = state.distributionState.copy(phase = DistributionState.Phase.SELECT_TYPE, selectedParamIdx = 0)
      )
      CalcMode.RATIO -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        ratioState = state.ratioState.copy(phase = RatioState.Phase.SELECT_TYPE)
      )
      CalcMode.MATH_BOX -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        mathBoxState = state.mathBoxState.copy(phase = MathBoxState.Phase.SELECT_TYPE)
      )
      else -> state.copy(
        mode = mode, overlay = OverlayScreen.NONE, doc = resetDoc, currentResult = null,
        resultFormatOverride = null
      )
    }
  }

  private fun handleMenuStackKey(state: CalculatorUiState, key: CalcKey): CalculatorUiState {
    val currentList = state.overlayStack.lastOrNull()?.second ?: return state.copy(overlay = OverlayScreen.NONE)
    val maxIdx = (currentList.size - 1).coerceAtLeast(0)
    return when (key) {
      CalcKey.UP -> state.copy(overlayCursor = if (state.overlayCursor <= 0) maxIdx else state.overlayCursor - 1)
      CalcKey.DOWN -> state.copy(overlayCursor = if (state.overlayCursor >= maxIdx) 0 else state.overlayCursor + 1)
      CalcKey.PAGE_UP -> state.copy(overlayCursor = (state.overlayCursor - 4).coerceAtLeast(0))
      CalcKey.PAGE_DOWN -> state.copy(overlayCursor = (state.overlayCursor + 4).coerceAtMost(maxIdx))
      CalcKey.OK, CalcKey.EXE, CalcKey.RIGHT -> confirmCatalogOrMenu(state)
      CalcKey.BACK, CalcKey.LEFT -> {
        if (state.overlayStack.size > 1) {
          state.copy(overlayStack = state.overlayStack.dropLast(1), overlayCursor = 0)
        } else {
          state.copy(overlay = OverlayScreen.NONE, overlayStack = emptyList())
        }
      }
      CalcKey.AC -> state.copy(overlay = OverlayScreen.NONE, overlayStack = emptyList())
      CalcKey.D1, CalcKey.D2, CalcKey.D3, CalcKey.D4, CalcKey.D5, CalcKey.D6, CalcKey.D7, CalcKey.D8, CalcKey.D9 -> {
        val digitIdx = when (key) {
          CalcKey.D1 -> 0; CalcKey.D2 -> 1; CalcKey.D3 -> 2; CalcKey.D4 -> 3; CalcKey.D5 -> 4
          CalcKey.D6 -> 5; CalcKey.D7 -> 6; CalcKey.D8 -> 7; else -> 8
        }
        if (digitIdx < currentList.size) confirmCatalogOrMenu(state.copy(overlayCursor = digitIdx)) else state
      }
      else -> state
    }
  }

  private fun confirmCatalogOrMenu(state: CalculatorUiState): CalculatorUiState {
    val currentList = state.overlayStack.lastOrNull()?.second ?: return state.copy(overlay = OverlayScreen.NONE)
    val entry = currentList.getOrNull(state.overlayCursor) ?: return state.copy(overlay = OverlayScreen.NONE)
    if (entry is CatalogEntry.SubCategory) {
      val title = state.settings.tr(entry.labelEs, entry.labelEn)
      return state.copy(
        overlayStack = state.overlayStack + (title to entry.children),
        overlayCursor = 0
      )
    }
    val action = (entry as CatalogEntry.InsertAction).actionId
    val closed = state.copy(overlay = OverlayScreen.NONE, overlayStack = emptyList())
    return executeActionId(closed, action)
  }

  private fun executeActionId(state: CalculatorUiState, action: String): CalculatorUiState {
    return when {
      action.startsWith("fmt:") -> {
        val fmtName = action.removePrefix("fmt:")
        if (fmtName == "POLAR") {
          val upd = state.settings.copy(complexFormat = ComplexFormat.POLAR)
          persistSettings(upd)
          return state.copy(settings = upd, resultFormatOverride = ResultFormatMode.POLAR)
        }
        if (fmtName == "RECTANGULAR") {
          val upd = state.settings.copy(complexFormat = ComplexFormat.RECTANGULAR)
          persistSettings(upd)
          return state.copy(settings = upd, resultFormatOverride = ResultFormatMode.RECTANGULAR)
        }
        val mode = runCatching { ResultFormatMode.valueOf(fmtName) }.getOrNull()
        state.copy(resultFormatOverride = mode)
      }
      action.startsWith("base:") -> {
        val b = BaseNMode.valueOf(action.removePrefix("base:"))
        val newRes = (state.currentResult as? EvalResult.BaseNResult)?.copy(base = b)
        state.copy(baseNMode = b, currentResult = newRes)
      }
      action == "tool:edit_matrix" -> state.copy(matrixState = state.matrixState.copy(phase = MatrixAppState.Phase.PICK_MATRIX, editRow = 0))
      action == "tool:work_matrix" -> state.copy(matrixState = state.matrixState.copy(phase = MatrixAppState.Phase.WORK))
      action == "tool:edit_vector" -> state.copy(vectorState = state.vectorState.copy(phase = VectorAppState.Phase.PICK_VECTOR, editIdx = 0))
      action == "tool:work_vector" -> state.copy(vectorState = state.vectorState.copy(phase = VectorAppState.Phase.WORK))
      action == "tool:table_f" -> state.copy(tableState = state.tableState.copy(phase = TableState.Phase.EDIT_F))
      action == "tool:table_g" -> state.copy(tableState = state.tableState.copy(phase = TableState.Phase.EDIT_G))
      action == "tool:table_range" -> state.copy(tableState = state.tableState.copy(phase = TableState.Phase.RANGE_SETUP, selectedRow = 0))
      action == "tool:table_exec" -> ModeKeyHandler.generateTable(state)
      action == "tool:stat_results" -> {
        val summary = AdvancedMathSolvers.computeStatistics(
          state.statsState.model, state.statsState.xValues, state.statsState.yValues,
          if (state.settings.statFrequency) state.statsState.freqValues else List(state.statsState.xValues.size) { 1.0 }
        )
        state.copy(statsState = state.statsState.copy(phase = StatsAppState.Phase.RESULTS, summary = summary, activeRow = 0))
      }
      action == "tool:stat_data" -> state.copy(statsState = state.statsState.copy(phase = StatsAppState.Phase.DATA_GRID, activeRow = 0))
      action == "tool:stat_type" -> state.copy(statsState = state.statsState.copy(phase = StatsAppState.Phase.SELECT_MODEL, activeRow = 0))
      action == "tool:stat_freq" -> {
        val upd = state.settings.copy(statFrequency = !state.settings.statFrequency)
        persistSettings(upd)
        state.copy(settings = upd)
      }
      action == "tool:eq_menu" -> state.copy(
        equationState = state.equationState.copy(subType = EquationState.SubType.MENU, activeRow = 0, inputBuffer = "")
      )
      action == "tool:eq_solver" -> state.copy(
        equationState = state.equationState.copy(subType = EquationState.SubType.SOLVER_EQ, activeRow = 2, inputBuffer = "")
      )
      action == "tool:sheet_sum" || action == "tool:sheet_mean" -> {
        val formula = if (action == "tool:sheet_sum") "=SUM(A1:A5)" else "=MEAN(A1:A5)"
        val sh = state.spreadsheetState
        val newCells = sh.cells + (sh.cellName() to formula)
        state.copy(spreadsheetState = sh.copy(cells = newCells, evaluated = AdvancedMathSolvers.evaluateSpreadsheet(newCells)))
      }
      action == "tool:sheet_clear" -> state.copy(spreadsheetState = SpreadsheetState())
      action == "tool:undo" -> {
        val prev = state.undoStack.lastOrNull() ?: return state
        state.copy(doc = prev, undoStack = state.undoStack.dropLast(1))
      }
      action == "tool:clear_hist" -> state.copy(history = emptyList(), historyBrowseIdx = -1)
      action == "tool:store" -> state.copy(overlay = OverlayScreen.VARIABLE_MENU, overlayCursor = 0, storePending = true)
      action == "tool:fx_menu" -> state.copy(
        overlay = OverlayScreen.FX_MENU,
        overlayCursor = 0,
        overlayStack = listOf("f(x) / g(x)" to MenuBuilders.buildFxMenu(state)),
      )
      action == "tool:qr" -> openQrOverlay(state, welcome = false)
      action == "fx:def_f" -> if (state.mode == CalcMode.TABLE) {
        state.copy(tableState = state.tableState.copy(phase = TableState.Phase.EDIT_F))
      } else {
        state.copy(editingFxTarget = 0)
      }
      action == "fx:def_g" -> if (state.mode == CalcMode.TABLE) {
        state.copy(tableState = state.tableState.copy(phase = TableState.Phase.EDIT_G))
      } else {
        state.copy(editingFxTarget = 1)
      }
      action == "tpl:integral" -> insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertTemplate(d, MathElement.Integral(), initialSlot = 0) }
      action == "tpl:deriv" -> insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertTemplate(d, MathElement.Derivative(), initialSlot = 0) }
      action == "tpl:sum" -> insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertTemplate(d, MathElement.Summation(), initialSlot = 2) }
      action == "tpl:prod" -> insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertTemplate(d, MathElement.ProductOp(), initialSlot = 2) }
      action == "tpl:abs" -> insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertTemplate(d, MathElement.AbsBox(), initialSlot = 0) }
      action == "tpl:logbase" -> insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertTemplate(d, MathElement.LogBase(), initialSlot = 0) }
      action.startsWith("fn:") -> {
        val fn = action.removePrefix("fn:")
        insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertToken(d, MathElement.Token(fn, TokenKind.FUNC_OPEN, fn)) }
      }
      action.startsWith("const:") -> {
        val c = action.removePrefix("const:")
        insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertToken(d, MathElement.Token(c, TokenKind.CONSTANT, c)) }
      }
      action.startsWith("var:") -> {
        val v = action.removePrefix("var:")
        insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertToken(d, MathElement.Token(v, TokenKind.VARIABLE, v)) }
      }
      action.startsWith("op:") -> {
        val op = action.removePrefix("op:")
        insertIntoActiveDoc(state, continueWithAns = true) { d -> MathEditor.insertToken(d, MathElement.Token(op, TokenKind.OPERATOR, op)) }
      }
      action.startsWith("post:") -> {
        val p = action.removePrefix("post:")
        insertIntoActiveDoc(state, continueWithAns = true) { d -> MathEditor.insertToken(d, MathElement.Token(p, TokenKind.POSTFIX, p)) }
      }
      action.startsWith("conv:") -> {
        val cv = action.removePrefix("conv:")
        insertIntoActiveDoc(state, continueWithAns = true) { d -> MathEditor.insertToken(d, MathElement.Token(cv, TokenKind.UNIT_CONVERSION, cv)) }
      }
      action.startsWith("logic:") -> {
        val lg = action.removePrefix("logic:")
        insertIntoActiveDoc(state, continueWithAns = true) { d -> MathEditor.insertToken(d, MathElement.Token(" $lg ", TokenKind.LOGIC_OP, lg)) }
      }
      action.startsWith("hex:") -> {
        val hx = action.removePrefix("hex:")
        insertIntoActiveDoc(state, continueWithAns = false) { d -> MathEditor.insertToken(d, MathElement.Token(hx, TokenKind.DIGIT, hx)) }
      }
      else -> state
    }
  }

  private fun handleSettingsKey(state: CalculatorUiState, key: CalcKey): CalculatorUiState {
    val maxIdx = when (state.settingsPage) {
      0 -> 2
      1 -> 5
      2 -> 3
      3 -> 2
      else -> 3
    }
    return when (key) {
      CalcKey.UP -> state.copy(overlayCursor = if (state.overlayCursor <= 0) maxIdx else state.overlayCursor - 1)
      CalcKey.DOWN -> state.copy(overlayCursor = if (state.overlayCursor >= maxIdx) 0 else state.overlayCursor + 1)
      CalcKey.PAGE_UP -> state.copy(overlayCursor = (state.overlayCursor - 4).coerceAtLeast(0))
      CalcKey.PAGE_DOWN -> state.copy(overlayCursor = (state.overlayCursor + 4).coerceAtMost(maxIdx))
      CalcKey.LEFT -> {
        if (state.settingsPage == 2 && state.overlayCursor == 0) {
          selectSettingsContrastValue(state, state.settings.lcdContrast - 0.08f)
        } else state
      }
      CalcKey.RIGHT -> {
        if (state.settingsPage == 2 && state.overlayCursor == 0) {
          selectSettingsContrastValue(state, state.settings.lcdContrast + 0.08f)
        } else state
      }
      CalcKey.OK, CalcKey.EXE -> confirmSettingsItem(state)
      CalcKey.BACK -> {
        when (state.settingsPage) {
          0 -> state.copy(overlay = OverlayScreen.NONE)
          4 -> state.copy(settingsPage = 1, overlayCursor = 0)
          else -> state.copy(settingsPage = 0, overlayCursor = 0)
        }
      }
      CalcKey.AC -> state.copy(overlay = OverlayScreen.NONE)
      else -> state
    }
  }

  private fun selectSettingsContrastValue(state: CalculatorUiState, v: Float): CalculatorUiState {
    val upd = state.settings.copy(lcdContrast = v.coerceIn(0.45f, 1.0f))
    persistSettings(upd)
    return state.copy(settings = upd)
  }

  private fun confirmSettingsItem(state: CalculatorUiState): CalculatorUiState {
    val idx = state.overlayCursor
    return when (state.settingsPage) {
      0 -> when (idx) {
        0 -> state.copy(settingsPage = 1, overlayCursor = 0)
        1 -> state.copy(settingsPage = 2, overlayCursor = 0)
        else -> state.copy(settingsPage = 3, overlayCursor = 0)
      }
      1 -> when (idx) {
        0 -> state.copy(settingsPage = 4, settingsDetailKey = "IO", overlayCursor = state.settings.inputOutput.ordinal)
        1 -> state.copy(settingsPage = 4, settingsDetailKey = "ANGLE", overlayCursor = state.settings.angleUnit.ordinal)
        2 -> state.copy(settingsPage = 4, settingsDetailKey = "NUMFMT", overlayCursor = state.settings.numberFormat.ordinal)
        3 -> {
          val next = if (state.settings.fractionFormat == FractionFormat.IMPROPER) FractionFormat.MIXED else FractionFormat.IMPROPER
          val upd = state.settings.copy(fractionFormat = next)
          persistSettings(upd)
          state.copy(settings = upd)
        }
        4 -> {
          val next = if (state.settings.complexFormat == ComplexFormat.RECTANGULAR) ComplexFormat.POLAR else ComplexFormat.RECTANGULAR
          val upd = state.settings.copy(complexFormat = next)
          persistSettings(upd)
          state.copy(settings = upd)
        }
        else -> {
          val upd = state.settings.copy(statFrequency = !state.settings.statFrequency)
          persistSettings(upd)
          state.copy(settings = upd)
        }
      }
      2 -> when (idx) {
        0 -> selectSettingsContrastValue(state, if (state.settings.lcdContrast >= 0.95f) 0.6f else state.settings.lcdContrast + 0.1f)
        1 -> {
          val next = if (state.settings.lcdTheme == LcdThemeMode.CLASSIC) LcdThemeMode.PAPER else LcdThemeMode.CLASSIC
          val upd = state.settings.copy(lcdTheme = next)
          persistSettings(upd)
          state.copy(settings = upd)
        }
        2 -> {
          val next = if (state.settings.language == CalcLanguage.ES) CalcLanguage.EN else CalcLanguage.ES
          val upd = state.settings.copy(language = next)
          persistSettings(upd)
          state.copy(settings = upd)
        }
        else -> {
          val upd = state.settings.copy(hapticsEnabled = !state.settings.hapticsEnabled)
          persistSettings(upd)
          state.copy(settings = upd)
        }
      }
      3 -> when (idx) {
        0 -> {
          val def = CalcSettings()
          persistSettings(def)
          state.copy(settings = def, history = emptyList(), overlay = OverlayScreen.NONE, toastBanner = state.settings.tr("Config. reiniciada", "Settings reset"))
        }
        1 -> state.copy(
          variables = state.variables.mapValues { EvalResult.RealResult(ExactReal.ZERO) },
          ans = null, preAns = null, overlay = OverlayScreen.NONE,
          toastBanner = state.settings.tr("Memoria borrada", "Memory cleared")
        )
        else -> {
          val def = CalcSettings()
          persistSettings(def)
          CalculatorUiState(settings = def, toastBanner = state.settings.tr("Todo inicializado", "Initialized All"))
        }
      }
      4 -> {
        val upd = when (state.settingsDetailKey) {
          "IO" -> state.settings.copy(inputOutput = InputOutputMode.entries[idx.coerceIn(0, 3)])
          "ANGLE" -> state.settings.copy(angleUnit = AngleUnit.entries[idx.coerceIn(0, 2)])
          "NUMFMT" -> state.settings.copy(numberFormat = NumberFormatMode.entries[idx.coerceIn(0, 3)])
          else -> state.settings
        }
        persistSettings(upd)
        state.copy(settings = upd, settingsPage = 1, overlayCursor = 0)
      }
      else -> state
    }
  }

  private fun handleVariableMenuKey(state: CalculatorUiState, key: CalcKey): CalculatorUiState {
    val varNames = listOf("A", "B", "C", "D", "E", "F", "x", "y", "z")
    return when (key) {
      CalcKey.UP -> state.copy(overlayCursor = if (state.overlayCursor <= 0) varNames.lastIndex else state.overlayCursor - 1)
      CalcKey.DOWN -> state.copy(overlayCursor = if (state.overlayCursor >= varNames.lastIndex) 0 else state.overlayCursor + 1)
      CalcKey.PAGE_UP -> state.copy(overlayCursor = (state.overlayCursor - 4).coerceAtLeast(0))
      CalcKey.PAGE_DOWN -> state.copy(overlayCursor = (state.overlayCursor + 4).coerceAtMost(varNames.lastIndex))
      CalcKey.OK, CalcKey.EXE -> confirmVariableItem(state)
      CalcKey.BACK, CalcKey.AC -> state.copy(overlay = OverlayScreen.NONE, storePending = false)
      else -> state
    }
  }

  private fun confirmVariableItem(state: CalculatorUiState): CalculatorUiState {
    val varNames = listOf("A", "B", "C", "D", "E", "F", "x", "y", "z")
    val name = varNames[state.overlayCursor.coerceIn(0, varNames.lastIndex)]
    if (state.storePending) {
      val valueToStore = state.currentResult ?: state.ans ?: evaluateCurrent(state)
      return if (valueToStore != null && valueToStore !is EvalResult.ErrorResult) {
        state.copy(
          variables = state.variables + (name to valueToStore),
          overlay = OverlayScreen.NONE,
          storePending = false,
          toastBanner = "→ $name"
        )
      } else {
        state.copy(overlay = OverlayScreen.NONE, storePending = false)
      }
    }
    val closed = state.copy(overlay = OverlayScreen.NONE, storePending = false)
    return insertIntoActiveDoc(closed, continueWithAns = false) { d ->
      MathEditor.insertToken(d, MathElement.Token(name, TokenKind.VARIABLE, name))
    }
  }

  // --- Main Natural Display Expression Editor ---

  private fun getActiveDoc(state: CalculatorUiState): EditorDocument {
    return when {
      state.editingFxTarget == 0 -> state.tableState.fDoc
      state.editingFxTarget == 1 -> state.tableState.gDoc
      state.mode == CalcMode.TABLE && state.tableState.phase == TableState.Phase.EDIT_F -> state.tableState.fDoc
      state.mode == CalcMode.TABLE && state.tableState.phase == TableState.Phase.EDIT_G -> state.tableState.gDoc
      state.mode == CalcMode.EQUATION && state.equationState.subType == EquationState.SubType.SOLVER_EQ ->
        state.equationState.solverDoc
      else -> state.doc
    }
  }

  private fun setActiveDoc(state: CalculatorUiState, newDoc: EditorDocument): CalculatorUiState {
    return when {
      state.editingFxTarget == 0 ->
        state.copy(tableState = state.tableState.copy(fDoc = newDoc))
      state.editingFxTarget == 1 ->
        state.copy(tableState = state.tableState.copy(gDoc = newDoc))
      state.mode == CalcMode.TABLE && state.tableState.phase == TableState.Phase.EDIT_F ->
        state.copy(tableState = state.tableState.copy(fDoc = newDoc))
      state.mode == CalcMode.TABLE && state.tableState.phase == TableState.Phase.EDIT_G ->
        state.copy(tableState = state.tableState.copy(gDoc = newDoc))
      state.mode == CalcMode.EQUATION && state.equationState.subType == EquationState.SubType.SOLVER_EQ ->
        state.copy(equationState = state.equationState.copy(solverDoc = newDoc, solverResult = null, solverError = null))
      else -> state.copy(
        doc = newDoc,
        undoStack = (state.undoStack + state.doc).takeLast(20),
        currentResult = null,
        resultFormatOverride = null,
        historyBrowseIdx = -1,
      )
    }
  }

  private inline fun insertIntoActiveDoc(
    state: CalculatorUiState,
    continueWithAns: Boolean,
    transform: (EditorDocument) -> EditorDocument,
  ): CalculatorUiState {
    val cur = getActiveDoc(state)
    val justEvaluated = state.currentResult != null &&
      state.editingFxTarget == null &&
      state.mode in listOf(CalcMode.CALCULATE, CalcMode.COMPLEX, CalcMode.BASE_N, CalcMode.MATRIX, CalcMode.VECTOR)
    val prepared = if (justEvaluated) {
      if (continueWithAns) {
        val ansName = when (state.mode) {
          CalcMode.MATRIX -> "MatAns"
          CalcMode.VECTOR -> "VctAns"
          else -> "Ans"
        }
        EditorDocument(MathRow(listOf(MathElement.Token(ansName, TokenKind.VARIABLE, ansName))), CursorPath(emptyList(), 1))
      } else {
        EditorDocument()
      }
    } else cur
    return setActiveDoc(state, transform(prepared))
  }

  private fun handleExpressionEditorKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState {
    val activeDoc = getActiveDoc(state)
    val justEvaluated = state.currentResult != null &&
      state.editingFxTarget == null &&
      state.mode in listOf(CalcMode.CALCULATE, CalcMode.COMPLEX, CalcMode.BASE_N, CalcMode.MATRIX, CalcMode.VECTOR)

    fun ansTokenName(): String = when (state.mode) {
      CalcMode.MATRIX -> "MatAns"
      CalcMode.VECTOR -> "VctAns"
      else -> "Ans"
    }

    fun startWithAnsIfOperator(token: MathElement.Token): CalculatorUiState {
      val continuesAns = justEvaluated && (
        (token.kind == TokenKind.OPERATOR && token.evalCode != "(-)") ||
          token.kind == TokenKind.POSTFIX ||
          token.kind == TokenKind.UNIT_CONVERSION
        )
      val baseDoc = if (continuesAns) {
        val ansName = ansTokenName()
        val d0 = EditorDocument(MathRow(listOf(MathElement.Token(ansName, TokenKind.VARIABLE, ansName))), CursorPath(emptyList(), 1))
        MathEditor.insertToken(d0, token)
      } else if (justEvaluated) {
        MathEditor.insertToken(EditorDocument(), token)
      } else {
        MathEditor.insertToken(activeDoc, token)
      }
      return setActiveDoc(state, baseDoc)
    }

    return when (key) {
      CalcKey.LEFT -> setActiveDocKeepResult(state, MathEditor.moveLeft(activeDoc))
      CalcKey.RIGHT -> setActiveDocKeepResult(state, MathEditor.moveRight(activeDoc))
      CalcKey.UP -> {
        val slotUp = MathEditor.moveUp(activeDoc)
        if (slotUp != null) setActiveDocKeepResult(state, slotUp)
        else if (state.editingFxTarget == null && state.history.isNotEmpty()) {
          val nextIdx = (state.historyBrowseIdx + 1).coerceAtMost(state.history.lastIndex)
          val entry = state.history[state.history.size - 1 - nextIdx]
          state.copy(
            doc = EditorDocument(entry.expression, CursorPath(emptyList(), entry.expression.items.size)),
            currentResult = entry.result,
            historyBrowseIdx = nextIdx,
            resultFormatOverride = null,
          )
        } else state
      }
      CalcKey.DOWN -> {
        val slotDown = MathEditor.moveDown(activeDoc)
        if (slotDown != null) setActiveDocKeepResult(state, slotDown)
        else if (state.editingFxTarget == null && state.historyBrowseIdx > 0) {
          val nextIdx = state.historyBrowseIdx - 1
          val entry = state.history[state.history.size - 1 - nextIdx]
          state.copy(
            doc = EditorDocument(entry.expression, CursorPath(emptyList(), entry.expression.items.size)),
            currentResult = entry.result,
            historyBrowseIdx = nextIdx,
            resultFormatOverride = null,
          )
        } else if (state.editingFxTarget == null && state.historyBrowseIdx == 0) {
          state.copy(doc = EditorDocument(), currentResult = null, historyBrowseIdx = -1)
        } else state
      }
      CalcKey.DEL -> {
        if (isShift) {
          val prev = state.undoStack.lastOrNull() ?: return state
          state.copy(doc = prev, undoStack = state.undoStack.dropLast(1))
        } else {
          setActiveDoc(state, MathEditor.backspace(activeDoc))
        }
      }
      CalcKey.AC -> {
        if (state.editingFxTarget != null) {
          if (activeDoc.root.isEmpty()) state.copy(editingFxTarget = null)
          else setActiveDoc(state, EditorDocument())
        } else {
          setActiveDoc(state, EditorDocument())
        }
      }
      CalcKey.EXE, CalcKey.OK -> {
        if (state.editingFxTarget != null) {
          val msg = if (state.editingFxTarget == 0) "f(x) ✓" else "g(x) ✓"
          return state.copy(editingFxTarget = null, toastBanner = msg)
        }
        // SHIFT + EXE (≈) on an already evaluated result switches directly to Decimal without duplicating history
        if (justEvaluated && isShift) {
          return state.copy(resultFormatOverride = ResultFormatMode.DECIMAL)
        }
        if (activeDoc.root.isEmpty()) return state
        val result = evaluateCurrent(state) ?: return state
        val isError = result is EvalResult.ErrorResult
        val newAns = if (!isError && result !is EvalResult.VerifyResult) result else state.ans
        val newPreAns = if (!isError && result !is EvalResult.VerifyResult && state.ans != null) state.ans else state.preAns
        val newMats = if (result is EvalResult.MatrixResult) state.matrixState.matrices + ("MatAns" to result) else state.matrixState.matrices
        val newVcts = if (result is EvalResult.VectorResult) state.vectorState.vectors + ("VctAns" to result) else state.vectorState.vectors
        val newVars = if (result is EvalResult.PolarPoint) {
          state.variables + ("x" to EvalResult.RealResult(ExactReal.fromDouble(result.r))) + ("y" to EvalResult.RealResult(ExactReal.fromDouble(result.theta)))
        } else state.variables
        val newHist = if (!isError) (state.history + HistoryEntry(activeDoc.root, result, state.mode)).takeLast(30) else state.history
        state.copy(
          currentResult = result,
          resultFormatOverride = if (isShift) ResultFormatMode.DECIMAL else null,
          ans = newAns,
          preAns = newPreAns,
          variables = newVars,
          matrixState = state.matrixState.copy(matrices = newMats),
          vectorState = state.vectorState.copy(vectors = newVcts),
          history = newHist,
          historyBrowseIdx = -1,
        )
      }

      // Numeric Pad (exact ClassWiz fx-991LA CW SHIFT mapping: 7=π, 8=e, 9=i; 0..6 & ops clean)
      CalcKey.D0 -> startWithAnsIfOperator(MathElement.Token("0", TokenKind.DIGIT, "0"))
      CalcKey.D1 -> startWithAnsIfOperator(MathElement.Token("1", TokenKind.DIGIT, "1"))
      CalcKey.D2 -> startWithAnsIfOperator(MathElement.Token("2", TokenKind.DIGIT, "2"))
      CalcKey.D3 -> startWithAnsIfOperator(MathElement.Token("3", TokenKind.DIGIT, "3"))
      CalcKey.D4 -> startWithAnsIfOperator(MathElement.Token("4", TokenKind.DIGIT, "4"))
      CalcKey.D5 -> startWithAnsIfOperator(MathElement.Token("5", TokenKind.DIGIT, "5"))
      CalcKey.D6 -> startWithAnsIfOperator(MathElement.Token("6", TokenKind.DIGIT, "6"))
      CalcKey.D7 -> if (isShift) startWithAnsIfOperator(MathElement.Token("π", TokenKind.CONSTANT, "π")) else startWithAnsIfOperator(MathElement.Token("7", TokenKind.DIGIT, "7"))
      CalcKey.D8 -> if (isShift) startWithAnsIfOperator(MathElement.Token("e", TokenKind.CONSTANT, "e")) else startWithAnsIfOperator(MathElement.Token("8", TokenKind.DIGIT, "8"))
      CalcKey.D9 -> if (isShift) startWithAnsIfOperator(MathElement.Token("i", TokenKind.CONSTANT, "i")) else startWithAnsIfOperator(MathElement.Token("9", TokenKind.DIGIT, "9"))
      CalcKey.DOT -> startWithAnsIfOperator(MathElement.Token(".", TokenKind.DECIMAL, "."))
      CalcKey.EXP10 -> startWithAnsIfOperator(MathElement.Token("×10^", TokenKind.DIGIT, "×10^"))

      // Basic Operators
      CalcKey.ADD -> startWithAnsIfOperator(MathElement.Token("+", TokenKind.OPERATOR, "+"))
      CalcKey.SUB -> startWithAnsIfOperator(MathElement.Token("-", TokenKind.OPERATOR, "-"))
      CalcKey.MUL -> startWithAnsIfOperator(MathElement.Token("×", TokenKind.OPERATOR, "×"))
      CalcKey.DIV -> startWithAnsIfOperator(MathElement.Token("÷", TokenKind.OPERATOR, "÷"))

      // Scientific Grid (3x6 exact ClassWiz fx-991LA CW)
      CalcKey.VAR_X -> startWithAnsIfOperator(MathElement.Token("x", TokenKind.VARIABLE, "x"))
      CalcKey.FRAC -> {
        if (isShift) {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.MixedFraction(), 0, false)
        } else {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.Fraction(), 0, pullPreceding = true)
        }
      }
      CalcKey.SQRT -> {
        if (isShift) {
          val cubeRoot = MathElement.NthRoot(index = MathRow(listOf(MathElement.Token("3", TokenKind.DIGIT, "3"))), radicand = MathRow())
          insertTemplateSmart(state, activeDoc, justEvaluated, cubeRoot, 1, false)
        } else {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.Sqrt(), 0, false)
        }
      }
      CalcKey.SQUARE -> {
        val tok = if (isShift) MathElement.Token("⁻¹", TokenKind.POSTFIX, "⁻¹") else MathElement.Token("²", TokenKind.POSTFIX, "²")
        startWithAnsIfOperator(tok)
      }
      CalcKey.POWER -> {
        if (isShift) {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.NthRoot(), 0, false)
        } else {
          val baseDoc = if (justEvaluated) {
            val ansName = ansTokenName()
            EditorDocument(MathRow(listOf(MathElement.Token(ansName, TokenKind.VARIABLE, ansName))), CursorPath(emptyList(), 1))
          } else activeDoc
          setActiveDoc(state, MathEditor.insertTemplate(baseDoc, MathElement.Superscript(), initialSlot = 0))
        }
      }
      CalcKey.LOG_BASE -> {
        if (isShift) {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.Integral(), 0, false)
        } else {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.LogBase(), 0, false)
        }
      }
      CalcKey.LOG10 -> {
        if (isShift) {
          val base10 = listOf(MathElement.Token("1", TokenKind.DIGIT, "1"), MathElement.Token("0", TokenKind.DIGIT, "0"))
          val baseDoc = if (justEvaluated) EditorDocument() else activeDoc
          val with10 = base10.fold(baseDoc) { acc, t -> MathEditor.insertToken(acc, t) }
          setActiveDoc(state, MathEditor.insertTemplate(with10, MathElement.Superscript(), initialSlot = 0))
        } else {
          startWithAnsIfOperator(MathElement.Token("log(", TokenKind.FUNC_OPEN, "log("))
        }
      }
      CalcKey.LN -> {
        if (isShift) {
          val baseDoc = if (justEvaluated) EditorDocument() else activeDoc
          val withE = MathEditor.insertToken(baseDoc, MathElement.Token("e", TokenKind.CONSTANT, "e"))
          setActiveDoc(state, MathEditor.insertTemplate(withE, MathElement.Superscript(), initialSlot = 0))
        } else {
          startWithAnsIfOperator(MathElement.Token("ln(", TokenKind.FUNC_OPEN, "ln("))
        }
      }
      CalcKey.SIN -> startWithAnsIfOperator(MathElement.Token(if (isShift) "sin⁻¹(" else "sin(", TokenKind.FUNC_OPEN, if (isShift) "sin⁻¹(" else "sin("))
      CalcKey.COS -> startWithAnsIfOperator(MathElement.Token(if (isShift) "cos⁻¹(" else "cos(", TokenKind.FUNC_OPEN, if (isShift) "cos⁻¹(" else "cos("))
      CalcKey.TAN -> startWithAnsIfOperator(MathElement.Token(if (isShift) "tan⁻¹(" else "tan(", TokenKind.FUNC_OPEN, if (isShift) "tan⁻¹(" else "tan("))
      CalcKey.NEG -> {
        if (isShift) insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.AbsBox(), 0, false)
        else startWithAnsIfOperator(MathElement.Token("-", TokenKind.OPERATOR, "(-)"))
      }
      CalcKey.DMS -> {
        if (isShift) {
          startWithAnsIfOperator(MathElement.Token("!", TokenKind.POSTFIX, "!"))
        } else {
          val base = if (justEvaluated) EditorDocument() else activeDoc
          val countDms = base.root.items.count { it is MathElement.Token && (it.evalCode == "°" || it.evalCode == "'" || it.evalCode == "\"") }
          val sym = when (countDms % 3) { 0 -> "°"; 1 -> "'"; else -> "\"" }
          setActiveDoc(state, MathEditor.insertToken(base, MathElement.Token(sym, TokenKind.POSTFIX, sym)))
        }
      }
      CalcKey.PAREN_OPEN -> {
        if (isShift) {
          startWithAnsIfOperator(MathElement.Token("%", TokenKind.POSTFIX, "%"))
        } else {
          startWithAnsIfOperator(MathElement.Token("(", TokenKind.PAREN_OPEN, "("))
        }
      }
      CalcKey.PAREN_CLOSE -> {
        if (isShift) startWithAnsIfOperator(MathElement.Token(",", TokenKind.SEPARATOR, ","))
        else setActiveDoc(state, MathEditor.insertToken(activeDoc, MathElement.Token(")", TokenKind.PAREN_CLOSE, ")")))
      }
      CalcKey.DERIV -> {
        if (isShift) {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.Summation(), 2, false)
        } else {
          insertTemplateSmart(state, activeDoc, justEvaluated, MathElement.Derivative(), 0, false)
        }
      }
      CalcKey.EQ -> {
        val op = if (isShift) "≠" else "="
        startWithAnsIfOperator(MathElement.Token(op, TokenKind.OPERATOR, op))
      }
      CalcKey.ANS -> {
        val code = if (isShift) "PreAns" else "Ans"
        val base = if (justEvaluated) EditorDocument() else activeDoc
        setActiveDoc(state, MathEditor.insertToken(base, MathElement.Token(code, TokenKind.VARIABLE, code)))
      }
      else -> state
    }
  }

  private fun setActiveDocKeepResult(state: CalculatorUiState, newDoc: EditorDocument): CalculatorUiState {
    return when {
      state.editingFxTarget == 0 ->
        state.copy(tableState = state.tableState.copy(fDoc = newDoc))
      state.editingFxTarget == 1 ->
        state.copy(tableState = state.tableState.copy(gDoc = newDoc))
      state.mode == CalcMode.TABLE && state.tableState.phase == TableState.Phase.EDIT_F ->
        state.copy(tableState = state.tableState.copy(fDoc = newDoc))
      state.mode == CalcMode.TABLE && state.tableState.phase == TableState.Phase.EDIT_G ->
        state.copy(tableState = state.tableState.copy(gDoc = newDoc))
      state.mode == CalcMode.EQUATION && state.equationState.subType == EquationState.SubType.SOLVER_EQ ->
        state.copy(equationState = state.equationState.copy(solverDoc = newDoc))
      else -> state.copy(doc = newDoc)
    }
  }

  private fun insertTemplateSmart(
    state: CalculatorUiState,
    activeDoc: EditorDocument,
    justEvaluated: Boolean,
    element: MathElement,
    initialSlot: Int,
    pullPreceding: Boolean,
  ): CalculatorUiState {
    val startDoc = if (justEvaluated) EditorDocument() else activeDoc
    return setActiveDoc(state, MathEditor.insertTemplate(startDoc, element, initialSlot, pullPreceding && !justEvaluated))
  }

  private fun evaluateCurrent(state: CalculatorUiState): EvalResult? {
    val ctx = EvalContext(
      settings = state.settings,
      mode = state.mode,
      baseNMode = state.baseNMode,
      variables = state.variables,
      matrices = state.matrixState.matrices,
      vectors = state.vectorState.vectors,
      ans = state.ans,
      preAns = state.preAns,
      statSummary = state.statsState.summary,
      fExpr = state.tableState.fDoc.root,
      gExpr = state.tableState.gDoc.root,
    )
    return ScientificEvaluator.evaluate(state.doc.root, ctx)
  }

  private fun openQrOverlay(state: CalculatorUiState, welcome: Boolean): CalculatorUiState {
    val s = state.settings
    val title = if (welcome) {
      s.tr("QR: fx-991LA CW", "QR: fx-991LA CW")
    } else {
      "${s.tr("QR", "QR")} — ${s.tr(state.mode.titleEs, state.mode.titleEn)}"
    }
    val payload = if (welcome) {
      "https://wes.casio.com/calc/?m=fx-991LACW"
    } else {
      buildQrPayload(state)
    }
    val matrix = QrEncoder.encode(payload)
    return state.copy(
      overlay = OverlayScreen.QR_VIEW,
      qrTitle = title,
      qrSubtitle = payload,
      qrMatrix = matrix,
      storePending = false,
    )
  }

  private fun buildQrPayload(state: CalculatorUiState): String {
    val modeCode = state.mode.name
    val body = when (state.mode) {
      CalcMode.TABLE -> "f(x)=${rowBrief(state.tableState.fDoc.root)};g(x)=${rowBrief(state.tableState.gDoc.root)};n=${state.tableState.rows.size}"
      CalcMode.STATISTICS -> "n=${state.statsState.xValues.size};mean=${formatNumBrief(state.statsState.summary.meanX)}"
      CalcMode.EQUATION -> if (state.equationState.solverDoc.root.items.isNotEmpty()) {
        "solver:${rowBrief(state.equationState.solverDoc.root)};x=${state.equationState.solverResult?.first?.let { formatNumBrief(it) } ?: "?"}"
      } else {
        "eq=${state.equationState.subType.name}"
      }
      CalcMode.RATIO -> "a=${state.ratioState.a};b=${state.ratioState.b};c=${state.ratioState.cOrD}"
      else -> {
        val expr = rowBrief(state.doc.root).ifEmpty { "0" }
        val ansStr = when (val r = state.currentResult ?: state.ans) {
          is EvalResult.RealResult -> formatNumBrief(r.value.toDouble())
          is EvalResult.ComplexResult -> "${formatNumBrief(r.re.toDouble())}+${formatNumBrief(r.im.toDouble())}i"
          is EvalResult.VerifyResult -> if (r.isTrue) "TRUE" else "FALSE"
          else -> ""
        }
        if (ansStr.isNotEmpty()) "$expr=$ansStr" else expr
      }
    }
    return "CASIO:991LACW:$modeCode:$body".take(76)
  }

  private fun rowBrief(row: MathRow): String = buildString {
    row.items.forEach { el ->
      when (el) {
        is MathElement.Token -> append(el.text)
        is MathElement.Fraction -> append("(${rowBrief(el.numerator)})/(${rowBrief(el.denominator)})")
        is MathElement.MixedFraction -> append("${rowBrief(el.whole)}+(${rowBrief(el.numerator)})/(${rowBrief(el.denominator)})")
        is MathElement.Superscript -> append("^(${rowBrief(el.exponent)})")
        is MathElement.Sqrt -> append("√(${rowBrief(el.radicand)})")
        is MathElement.NthRoot -> append("${rowBrief(el.index)}√(${rowBrief(el.radicand)})")
        is MathElement.LogBase -> append("log_${rowBrief(el.base)}(${rowBrief(el.argument)})")
        is MathElement.AbsBox -> append("|${rowBrief(el.inner)}|")
        is MathElement.Integral -> append("∫(${rowBrief(el.integrand)})")
        is MathElement.Derivative -> append("d/dx(${rowBrief(el.expr)})")
        is MathElement.Summation -> append("Σ(${rowBrief(el.expr)})")
        is MathElement.ProductOp -> append("Π(${rowBrief(el.expr)})")
      }
    }
  }

  private fun formatNumBrief(d: Double): String =
    formatDoubleClassWiz(d, CalcSettings())
}
