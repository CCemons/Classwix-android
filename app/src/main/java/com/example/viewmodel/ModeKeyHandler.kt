package com.example.viewmodel

import com.example.engine.*
import kotlin.math.abs
import kotlin.random.Random

object ModeKeyHandler {

  fun handleModeKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    if (state.editingFxTarget != null) return null
    return when (state.mode) {
      CalcMode.TABLE -> handleTableKey(state, key, isShift)
      CalcMode.EQUATION -> handleEquationKey(state, key, isShift)
      CalcMode.INEQUALITY -> handleInequalityKey(state, key, isShift)
      CalcMode.MATRIX -> handleMatrixKey(state, key, isShift)
      CalcMode.VECTOR -> handleVectorKey(state, key, isShift)
      CalcMode.STATISTICS -> handleStatsKey(state, key, isShift)
      CalcMode.DISTRIBUTION -> handleDistributionKey(state, key, isShift)
      CalcMode.SPREADSHEET -> handleSpreadsheetKey(state, key, isShift)
      CalcMode.RATIO -> handleRatioKey(state, key, isShift)
      CalcMode.MATH_BOX -> handleMathBoxKey(state, key, isShift)
      else -> null
    }
  }

  private fun appendNumericKey(buf: String, key: CalcKey, isShift: Boolean = false): String? {
    if (isShift) {
      return when (key) {
        CalcKey.D7 -> "3.141592654"
        CalcKey.D8 -> "2.718281828"
        CalcKey.SUB -> if (buf.isEmpty()) "-" else if (buf.startsWith("-")) buf.drop(1) else "-$buf"
        else -> null
      }
    }
    return when (key) {
      CalcKey.D0 -> buf + "0"
      CalcKey.D1 -> buf + "1"
      CalcKey.D2 -> buf + "2"
      CalcKey.D3 -> buf + "3"
      CalcKey.D4 -> buf + "4"
      CalcKey.D5 -> buf + "5"
      CalcKey.D6 -> buf + "6"
      CalcKey.D7 -> buf + "7"
      CalcKey.D8 -> buf + "8"
      CalcKey.D9 -> buf + "9"
      CalcKey.DOT -> if (buf.contains('.')) buf else if (buf.isEmpty() || buf == "-") "${buf}0." else "$buf."
      CalcKey.NEG, CalcKey.SUB -> if (buf.isEmpty()) "-" else if (buf.startsWith("-")) buf.drop(1) else "-$buf"
      CalcKey.DEL -> if (buf.isNotEmpty()) buf.dropLast(1) else ""
      CalcKey.AC -> ""
      else -> null
    }
  }

  // --- TABLE MODE ---
  private fun handleTableKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val ts = state.tableState
    return when (ts.phase) {
      TableState.Phase.EDIT_F, TableState.Phase.EDIT_G -> {
        if (key == CalcKey.EXE || key == CalcKey.OK) {
          if (ts.phase == TableState.Phase.EDIT_F) {
            if (ts.showG) state.copy(tableState = ts.copy(phase = TableState.Phase.EDIT_G))
            else state.copy(tableState = ts.copy(phase = TableState.Phase.RANGE_SETUP, selectedRow = 0))
          } else {
            state.copy(tableState = ts.copy(phase = TableState.Phase.RANGE_SETUP, selectedRow = 0))
          }
        } else null // Let main doc editor handle formula input for fDoc / gDoc!
      }
      TableState.Phase.RANGE_SETUP -> {
        when (key) {
          CalcKey.UP -> state.copy(tableState = ts.copy(selectedRow = (ts.selectedRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(tableState = ts.copy(selectedRow = (ts.selectedRow + 1).coerceAtMost(3)))
          CalcKey.EXE, CalcKey.OK -> {
            if (ts.selectedRow < 3 && key == CalcKey.OK) {
              state.copy(tableState = ts.copy(selectedRow = ts.selectedRow + 1))
            } else {
              generateTable(state)
            }
          }
          CalcKey.BACK -> state.copy(tableState = ts.copy(phase = TableState.Phase.EDIT_F))
          else -> {
            val curVal = when (ts.selectedRow) {
              0 -> formatNumTrim(ts.start)
              1 -> formatNumTrim(ts.end)
              else -> formatNumTrim(ts.step)
            }
            val nextBuf = appendNumericKey(if (curVal == "0") "" else curVal, key, isShift) ?: return null
            val parsed = nextBuf.toDoubleOrNull() ?: 0.0
            val updated = when (ts.selectedRow) {
              0 -> ts.copy(start = parsed)
              1 -> ts.copy(end = parsed)
              else -> ts.copy(step = if (parsed == 0.0) 1.0 else abs(parsed))
            }
            state.copy(tableState = updated)
          }
        }
      }
      TableState.Phase.RESULT_TABLE -> {
        val maxRow = (ts.rows.size - 1).coerceAtLeast(0)
        val maxCol = if (ts.showG) 2 else 1
        if (ts.editingXRow != null) {
          return when (key) {
            CalcKey.OK, CalcKey.EXE -> {
              val newX = ts.editingXBuffer.toDoubleOrNull()
              if (newX != null && ts.editingXRow in ts.rows.indices) {
                val baseCtx = EvalContext(
                  state.settings, CalcMode.CALCULATE,
                  variables = state.variables + ("x" to EvalResult.RealResult(ExactReal.fromDouble(newX))),
                  ans = state.ans, fExpr = ts.fDoc.root, gExpr = ts.gDoc.root
                )
                val fx = if (ts.fDoc.root.isEmpty()) null else runCatching { ScientificEvaluator.evaluateReal(ts.fDoc.root, baseCtx) }.getOrNull()
                val gx = if (!ts.showG || ts.gDoc.root.isEmpty()) null else runCatching { ScientificEvaluator.evaluateReal(ts.gDoc.root, baseCtx) }.getOrNull()
                val updatedRows = ts.rows.mapIndexed { i, r -> if (i == ts.editingXRow) TableState.TableRow(newX, fx, gx) else r }
                state.copy(tableState = ts.copy(rows = updatedRows, editingXRow = null, editingXBuffer = ""))
              } else {
                state.copy(tableState = ts.copy(editingXRow = null, editingXBuffer = ""))
              }
            }
            CalcKey.BACK -> state.copy(tableState = ts.copy(editingXRow = null, editingXBuffer = ""))
            else -> {
              val next = appendNumericKey(ts.editingXBuffer, key, isShift) ?: return state
              state.copy(tableState = ts.copy(editingXBuffer = next))
            }
          }
        }
        when (key) {
          CalcKey.UP -> state.copy(tableState = ts.copy(selectedRow = (ts.selectedRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(tableState = ts.copy(selectedRow = (ts.selectedRow + 1).coerceAtMost(maxRow)))
          CalcKey.PAGE_UP -> state.copy(tableState = ts.copy(selectedRow = (ts.selectedRow - 4).coerceAtLeast(0)))
          CalcKey.PAGE_DOWN -> state.copy(tableState = ts.copy(selectedRow = (ts.selectedRow + 4).coerceAtMost(maxRow)))
          CalcKey.LEFT -> state.copy(tableState = ts.copy(selectedCol = (ts.selectedCol - 1).coerceAtLeast(0)))
          CalcKey.RIGHT -> state.copy(tableState = ts.copy(selectedCol = (ts.selectedCol + 1).coerceAtMost(maxCol)))
          CalcKey.BACK, CalcKey.AC -> state.copy(tableState = ts.copy(phase = TableState.Phase.EDIT_F))
          else -> {
            if (ts.selectedCol == 0 && ts.rows.isNotEmpty()) {
              val ch = appendNumericKey("", key, isShift)
              if (ch != null && ch.isNotEmpty()) {
                return state.copy(tableState = ts.copy(editingXRow = ts.selectedRow, editingXBuffer = ch))
              }
            }
            state
          }
        }
      }
    }
  }

  fun generateTable(state: CalculatorUiState): CalculatorUiState {
    val ts = state.tableState
    val step = if (ts.step <= 0.0) 1.0 else ts.step
    val rows = mutableListOf<TableState.TableRow>()
    var x = ts.start
    var count = 0
    val baseCtx = EvalContext(
      state.settings,
      CalcMode.CALCULATE,
      variables = state.variables,
      ans = state.ans,
      fExpr = ts.fDoc.root,
      gExpr = ts.gDoc.root,
    )
    while ((ts.end >= ts.start && x <= ts.end + 1e-9 || ts.end < ts.start && x >= ts.end - 1e-9) && count < 45) {
      val subCtx = baseCtx.copy(variables = baseCtx.variables + ("x" to EvalResult.RealResult(ExactReal.fromDouble(x))))
      val fx = if (ts.fDoc.root.isEmpty()) null else try { ScientificEvaluator.evaluateReal(ts.fDoc.root, subCtx) } catch (_: Exception) { null }
      val gx = if (!ts.showG || ts.gDoc.root.isEmpty()) null else try { ScientificEvaluator.evaluateReal(ts.gDoc.root, subCtx) } catch (_: Exception) { null }
      rows.add(TableState.TableRow(x, fx, gx))
      x += if (ts.end >= ts.start) step else -step
      count++
    }
    return state.copy(
      tableState = ts.copy(phase = TableState.Phase.RESULT_TABLE, rows = rows, selectedRow = 0, selectedCol = 0, editingXRow = null, editingXBuffer = "")
    )
  }

  // --- EQUATION MODE (Casio fx-991LA CW authentic 3-item menu: Simul / Polynomial / Solver) ---
  private fun handleEquationKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val eq = state.equationState
    return when (eq.subType) {
      EquationState.SubType.MENU -> {
        when (key) {
          CalcKey.UP -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow + 1).coerceAtMost(2)))
          CalcKey.D1 -> selectEquationOption(state, 0)
          CalcKey.D2 -> selectEquationOption(state, 1)
          CalcKey.D3 -> selectEquationOption(state, 2)
          CalcKey.OK, CalcKey.EXE, CalcKey.RIGHT -> selectEquationOption(state, eq.activeRow)
          else -> state
        }
      }
      EquationState.SubType.SIMUL_DIM -> {
        when (key) {
          CalcKey.UP -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow + 1).coerceAtMost(2)))
          CalcKey.D2 -> startLinearInput(state, 2)
          CalcKey.D3 -> startLinearInput(state, 3)
          CalcKey.D4 -> startLinearInput(state, 4)
          CalcKey.OK, CalcKey.EXE, CalcKey.RIGHT -> startLinearInput(state, eq.activeRow + 2)
          CalcKey.BACK, CalcKey.LEFT -> state.copy(equationState = eq.copy(subType = EquationState.SubType.MENU, activeRow = 0))
          else -> state
        }
      }
      EquationState.SubType.POLY_DEGREE -> {
        when (key) {
          CalcKey.UP -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow + 1).coerceAtMost(2)))
          CalcKey.D2 -> startPolyInput(state, 2)
          CalcKey.D3 -> startPolyInput(state, 3)
          CalcKey.D4 -> startPolyInput(state, 4)
          CalcKey.OK, CalcKey.EXE, CalcKey.RIGHT -> startPolyInput(state, eq.activeRow + 2)
          CalcKey.BACK, CalcKey.LEFT -> state.copy(equationState = eq.copy(subType = EquationState.SubType.MENU, activeRow = 1))
          else -> state
        }
      }
      EquationState.SubType.LINEAR_INPUT -> {
        val cols = eq.unknowns + 1
        when (key) {
          CalcKey.LEFT -> commitLinearBuf(state).let { s ->
            val c = (s.equationState.activeCol - 1).coerceAtLeast(0)
            s.copy(equationState = s.equationState.copy(activeCol = c, inputBuffer = ""))
          }
          CalcKey.RIGHT -> commitLinearBuf(state).let { s ->
            val c = (s.equationState.activeCol + 1).coerceAtMost(cols - 1)
            s.copy(equationState = s.equationState.copy(activeCol = c, inputBuffer = ""))
          }
          CalcKey.UP -> commitLinearBuf(state).let { s ->
            val r = (s.equationState.activeRow - 1).coerceAtLeast(0)
            s.copy(equationState = s.equationState.copy(activeRow = r, inputBuffer = ""))
          }
          CalcKey.DOWN -> commitLinearBuf(state).let { s ->
            val r = (s.equationState.activeRow + 1).coerceAtMost(eq.unknowns - 1)
            s.copy(equationState = s.equationState.copy(activeRow = r, inputBuffer = ""))
          }
          CalcKey.OK -> {
            val s = commitLinearBuf(state)
            var c = s.equationState.activeCol + 1
            var r = s.equationState.activeRow
            if (c >= cols) { c = 0; r++ }
            if (r >= eq.unknowns) solveLinearNow(s)
            else s.copy(equationState = s.equationState.copy(activeRow = r, activeCol = c, inputBuffer = ""))
          }
          CalcKey.EXE -> solveLinearNow(commitLinearBuf(state))
          CalcKey.BACK -> state.copy(equationState = eq.copy(subType = EquationState.SubType.SIMUL_DIM, activeRow = (eq.unknowns - 2).coerceIn(0, 2), inputBuffer = ""))
          else -> {
            val next = appendNumericKey(eq.inputBuffer, key, isShift) ?: return state
            state.copy(equationState = eq.copy(inputBuffer = next))
          }
        }
      }
      EquationState.SubType.LINEAR_RESULT -> {
        when (key) {
          CalcKey.UP -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow + 1).coerceAtMost(eq.unknowns - 1)))
          CalcKey.BACK, CalcKey.AC -> state.copy(equationState = eq.copy(subType = EquationState.SubType.LINEAR_INPUT, activeRow = 0, activeCol = 0))
          else -> state
        }
      }
      EquationState.SubType.POLY_INPUT -> {
        val count = eq.degree + 1
        when (key) {
          CalcKey.LEFT, CalcKey.UP -> commitPolyBuf(state).let { s ->
            s.copy(equationState = s.equationState.copy(activeCol = (s.equationState.activeCol - 1).coerceAtLeast(0), inputBuffer = ""))
          }
          CalcKey.RIGHT, CalcKey.DOWN -> commitPolyBuf(state).let { s ->
            s.copy(equationState = s.equationState.copy(activeCol = (s.equationState.activeCol + 1).coerceAtMost(count - 1), inputBuffer = ""))
          }
          CalcKey.OK -> {
            val s = commitPolyBuf(state)
            if (s.equationState.activeCol + 1 >= count) solvePolyNow(s)
            else s.copy(equationState = s.equationState.copy(activeCol = s.equationState.activeCol + 1, inputBuffer = ""))
          }
          CalcKey.EXE -> solvePolyNow(commitPolyBuf(state))
          CalcKey.BACK -> state.copy(equationState = eq.copy(subType = EquationState.SubType.POLY_DEGREE, activeRow = (eq.degree - 2).coerceIn(0, 2), inputBuffer = ""))
          else -> {
            val next = appendNumericKey(eq.inputBuffer, key, isShift) ?: return state
            state.copy(equationState = eq.copy(inputBuffer = next))
          }
        }
      }
      EquationState.SubType.POLY_RESULT -> {
        val totalItems = eq.polyRoots.size + if (eq.polyVertex != null) 2 else 0
        val maxIdx = (totalItems - 1).coerceAtLeast(0)
        when (key) {
          CalcKey.UP -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow + 1).coerceAtMost(maxIdx)))
          CalcKey.PAGE_UP -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow - 4).coerceAtLeast(0)))
          CalcKey.PAGE_DOWN -> state.copy(equationState = eq.copy(activeRow = (eq.activeRow + 4).coerceAtMost(maxIdx)))
          CalcKey.BACK, CalcKey.AC -> state.copy(equationState = eq.copy(subType = EquationState.SubType.POLY_INPUT, activeCol = 0))
          else -> state
        }
      }
      EquationState.SubType.SOLVER_EQ -> {
        when (key) {
          CalcKey.EXE, CalcKey.OK -> {
            val split = splitEquationRow(eq.solverDoc.root)
            if (split == null) {
              state.copy(toastBanner = "Syntax ERROR")
            } else {
              val storedX = (state.variables["x"] as? EvalResult.RealResult)?.value?.toDouble()
              val initX = if (eq.solverResult != null) eq.solverInitialX else (storedX ?: eq.solverInitialX)
              state.copy(
                equationState = eq.copy(
                  subType = EquationState.SubType.SOLVER_GUESS,
                  activeRow = 0,
                  inputBuffer = "",
                  solverInitialX = initX,
                )
              )
            }
          }
          CalcKey.BACK -> state.copy(equationState = eq.copy(subType = EquationState.SubType.MENU, activeRow = 2, inputBuffer = ""))
          else -> null // Let main natural math editor handle typing the equation (with [=], x, trig, fractions, etc.)
        }
      }
      EquationState.SubType.SOLVER_GUESS -> {
        fun commitGuess(s: CalculatorUiState): CalculatorUiState {
          val cur = s.equationState
          if (cur.inputBuffer.isEmpty()) return s
          val parsed = cur.inputBuffer.toDoubleOrNull() ?: cur.solverInitialX
          return s.copy(equationState = cur.copy(solverInitialX = parsed, inputBuffer = ""))
        }
        when (key) {
          CalcKey.UP -> commitGuess(state).let { s -> s.copy(equationState = s.equationState.copy(activeRow = 0)) }
          CalcKey.DOWN -> commitGuess(state).let { s -> s.copy(equationState = s.equationState.copy(activeRow = 1)) }
          CalcKey.OK -> {
            val s = commitGuess(state)
            if (s.equationState.activeRow == 0) {
              s.copy(equationState = s.equationState.copy(activeRow = 1))
            } else {
              runNumericalSolver(s)
            }
          }
          CalcKey.EXE -> runNumericalSolver(commitGuess(state))
          CalcKey.BACK -> commitGuess(state).let { s ->
            s.copy(equationState = s.equationState.copy(subType = EquationState.SubType.SOLVER_EQ, inputBuffer = ""))
          }
          else -> {
            val next = appendNumericKey(eq.inputBuffer, key, isShift) ?: return state
            state.copy(equationState = eq.copy(activeRow = 0, inputBuffer = next))
          }
        }
      }
      EquationState.SubType.SOLVER_RESULT -> {
        when (key) {
          CalcKey.BACK, CalcKey.OK -> state.copy(equationState = eq.copy(subType = EquationState.SubType.SOLVER_GUESS, activeRow = 0, inputBuffer = ""))
          CalcKey.AC -> state.copy(equationState = eq.copy(subType = EquationState.SubType.SOLVER_EQ, inputBuffer = ""))
          CalcKey.EXE -> runNumericalSolver(state)
          else -> state
        }
      }
    }
  }

  fun selectEquationOption(state: CalculatorUiState, idx: Int): CalculatorUiState {
    val eq = state.equationState
    return when (idx) {
      0 -> state.copy(equationState = eq.copy(subType = EquationState.SubType.SIMUL_DIM, activeRow = (eq.unknowns - 2).coerceIn(0, 2), inputBuffer = ""))
      1 -> state.copy(equationState = eq.copy(subType = EquationState.SubType.POLY_DEGREE, activeRow = (eq.degree - 2).coerceIn(0, 2), inputBuffer = ""))
      else -> state.copy(equationState = eq.copy(subType = EquationState.SubType.SOLVER_EQ, activeRow = 2, inputBuffer = ""))
    }
  }

  fun startLinearInput(state: CalculatorUiState, unknowns: Int): CalculatorUiState {
    val eq = state.equationState
    return state.copy(
      equationState = eq.copy(
        subType = EquationState.SubType.LINEAR_INPUT,
        unknowns = unknowns.coerceIn(2, 4),
        activeRow = 0,
        activeCol = 0,
        inputBuffer = "",
      )
    )
  }

  fun startPolyInput(state: CalculatorUiState, degree: Int): CalculatorUiState {
    val eq = state.equationState
    return state.copy(
      equationState = eq.copy(
        subType = EquationState.SubType.POLY_INPUT,
        degree = degree.coerceIn(2, 4),
        activeCol = 0,
        inputBuffer = "",
      )
    )
  }

  fun splitEquationRow(row: MathRow): Pair<MathRow, MathRow>? {
    if (row.isEmpty()) return null
    var depth = 0
    var eqIdx: Int? = null
    row.items.forEachIndexed { idx, el ->
      if (el is MathElement.Token) {
        when (el.kind) {
          TokenKind.PAREN_OPEN, TokenKind.FUNC_OPEN -> depth++
          TokenKind.PAREN_CLOSE -> if (depth > 0) depth--
          TokenKind.OPERATOR -> if (depth == 0 && el.evalCode == "=") {
            if (eqIdx != null) return null
            eqIdx = idx
          }
          else -> {}
        }
      }
    }
    return if (eqIdx == null) {
      row to MathRow(listOf(MathElement.Token("0", TokenKind.DIGIT, "0")))
    } else {
      val i = eqIdx!!
      val lhs = MathRow(row.items.subList(0, i))
      val rhs = MathRow(row.items.subList(i + 1, row.items.size))
      if (lhs.isEmpty() || rhs.isEmpty()) null else lhs to rhs
    }
  }

  private fun commitLinearBuf(state: CalculatorUiState): CalculatorUiState {
    val eq = state.equationState
    if (eq.inputBuffer.isEmpty()) return state
    val updated = eq.linearCoeffs.mapIndexed { r, row ->
      if (r == eq.activeRow) row.mapIndexed { c, v -> if (c == eq.activeCol) eq.inputBuffer else v } else row
    }
    return state.copy(equationState = eq.copy(linearCoeffs = updated, inputBuffer = ""))
  }

  private fun solveLinearNow(state: CalculatorUiState): CalculatorUiState {
    val eq = state.equationState
    val n = eq.unknowns
    val mat = List(n) { r -> List(n) { c -> eq.linearCoeffs[r][c].toDoubleOrNull() ?: 0.0 } }
    val rhs = List(n) { r -> eq.linearCoeffs[r][n].toDoubleOrNull() ?: 0.0 }
    val res = AdvancedMathSolvers.solveLinearSystem(mat, rhs)
    return state.copy(equationState = eq.copy(subType = EquationState.SubType.LINEAR_RESULT, linearResult = res, activeRow = 0))
  }

  private fun commitPolyBuf(state: CalculatorUiState): CalculatorUiState {
    val eq = state.equationState
    if (eq.inputBuffer.isEmpty()) return state
    val updated = eq.polyCoeffs.mapIndexed { i, v -> if (i == eq.activeCol) eq.inputBuffer else v }
    return state.copy(equationState = eq.copy(polyCoeffs = updated, inputBuffer = ""))
  }

  private fun solvePolyNow(state: CalculatorUiState): CalculatorUiState {
    val eq = state.equationState
    val coeffs = eq.polyCoeffs.take(eq.degree + 1).map { it.toDoubleOrNull() ?: 0.0 }
    val roots = AdvancedMathSolvers.solvePolynomial(coeffs)
    val vertex = if (eq.degree == 2 && coeffs.size >= 3) {
      AdvancedMathSolvers.quadraticVertex(coeffs[0], coeffs[1], coeffs[2])
    } else null
    return state.copy(equationState = eq.copy(subType = EquationState.SubType.POLY_RESULT, polyRoots = roots, polyVertex = vertex, activeRow = 0))
  }

  private fun runNumericalSolver(state: CalculatorUiState): CalculatorUiState {
    val eq = state.equationState
    val split = splitEquationRow(eq.solverDoc.root)
      ?: return state.copy(
        equationState = eq.copy(
          subType = EquationState.SubType.SOLVER_RESULT,
          solverResult = null,
          solverError = "Syntax ERROR",
        )
      )
    val ctx = EvalContext(
      state.settings,
      CalcMode.CALCULATE,
      variables = state.variables,
      ans = state.ans,
      fExpr = state.tableState.fDoc.root,
      gExpr = state.tableState.gDoc.root,
    )
    val res = AdvancedMathSolvers.solveNumerical(split.first, split.second, eq.solverInitialX, ctx)
    return if (res != null) {
      state.copy(
        equationState = eq.copy(
          subType = EquationState.SubType.SOLVER_RESULT,
          solverResult = res,
          solverError = null,
          solverInitialX = res.first,
        ),
        variables = state.variables + ("x" to EvalResult.RealResult(ExactReal.fromDouble(res.first)))
      )
    } else {
      state.copy(
        equationState = eq.copy(
          subType = EquationState.SubType.SOLVER_RESULT,
          solverResult = null,
          solverError = state.settings.tr("No se encontró raíz", "Cannot Solve"),
        )
      )
    }
  }

  // --- INEQUALITY MODE ---
  private fun handleInequalityKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val ineq = state.inequalityState
    return when (ineq.phase) {
      InequalityState.Phase.SELECT_DEGREE -> when (key) {
        CalcKey.UP -> state.copy(inequalityState = ineq.copy(activeCol = (ineq.activeCol - 1).coerceAtLeast(0)))
        CalcKey.DOWN -> state.copy(inequalityState = ineq.copy(activeCol = (ineq.activeCol + 1).coerceAtMost(2)))
        CalcKey.D2 -> state.copy(inequalityState = ineq.copy(degree = 2, phase = InequalityState.Phase.SELECT_OP, activeCol = 0))
        CalcKey.D3 -> state.copy(inequalityState = ineq.copy(degree = 3, phase = InequalityState.Phase.SELECT_OP, activeCol = 0))
        CalcKey.D4 -> state.copy(inequalityState = ineq.copy(degree = 4, phase = InequalityState.Phase.SELECT_OP, activeCol = 0))
        CalcKey.OK, CalcKey.EXE -> state.copy(inequalityState = ineq.copy(degree = ineq.activeCol + 2, phase = InequalityState.Phase.SELECT_OP, activeCol = 0))
        else -> state
      }
      InequalityState.Phase.SELECT_OP -> when (key) {
        CalcKey.UP -> state.copy(inequalityState = ineq.copy(activeCol = (ineq.activeCol - 1).coerceAtLeast(0)))
        CalcKey.DOWN -> state.copy(inequalityState = ineq.copy(activeCol = (ineq.activeCol + 1).coerceAtMost(3)))
        CalcKey.OK, CalcKey.EXE -> {
          val op = InequalityOp.entries[ineq.activeCol.coerceIn(0, 3)]
          state.copy(inequalityState = ineq.copy(op = op, phase = InequalityState.Phase.COEFF_INPUT, activeCol = 0, inputBuffer = ""))
        }
        CalcKey.BACK -> state.copy(inequalityState = ineq.copy(phase = InequalityState.Phase.SELECT_DEGREE, activeCol = 0))
        else -> state
      }
      InequalityState.Phase.COEFF_INPUT -> {
        val count = ineq.degree + 1
        fun commitBuf(s: CalculatorUiState): CalculatorUiState {
          val cur = s.inequalityState
          if (cur.inputBuffer.isEmpty()) return s
          val upd = cur.coeffs.mapIndexed { i, v -> if (i == cur.activeCol) cur.inputBuffer else v }
          return s.copy(inequalityState = cur.copy(coeffs = upd, inputBuffer = ""))
        }
        when (key) {
          CalcKey.LEFT, CalcKey.UP -> commitBuf(state).let { s ->
            s.copy(inequalityState = s.inequalityState.copy(activeCol = (s.inequalityState.activeCol - 1).coerceAtLeast(0)))
          }
          CalcKey.RIGHT, CalcKey.DOWN -> commitBuf(state).let { s ->
            s.copy(inequalityState = s.inequalityState.copy(activeCol = (s.inequalityState.activeCol + 1).coerceAtMost(count - 1)))
          }
          CalcKey.OK -> {
            val s = commitBuf(state)
            if (s.inequalityState.activeCol + 1 < count) {
              s.copy(inequalityState = s.inequalityState.copy(activeCol = s.inequalityState.activeCol + 1))
            } else {
              val cs = s.inequalityState.coeffs.take(count).map { it.toDoubleOrNull() ?: 0.0 }
              val sol = AdvancedMathSolvers.solvePolynomialInequality(cs, s.inequalityState.op, state.settings)
              s.copy(inequalityState = s.inequalityState.copy(phase = InequalityState.Phase.RESULT, solutionText = sol))
            }
          }
          CalcKey.EXE -> {
            val s = commitBuf(state)
            val cs = s.inequalityState.coeffs.take(count).map { it.toDoubleOrNull() ?: 0.0 }
            val sol = AdvancedMathSolvers.solvePolynomialInequality(cs, s.inequalityState.op, state.settings)
            s.copy(inequalityState = s.inequalityState.copy(phase = InequalityState.Phase.RESULT, solutionText = sol))
          }
          CalcKey.BACK -> state.copy(inequalityState = ineq.copy(phase = InequalityState.Phase.SELECT_OP, activeCol = 0))
          else -> {
            val next = appendNumericKey(ineq.inputBuffer, key, isShift) ?: return state
            state.copy(inequalityState = ineq.copy(inputBuffer = next))
          }
        }
      }
      InequalityState.Phase.RESULT -> when (key) {
        CalcKey.BACK, CalcKey.AC, CalcKey.OK -> state.copy(inequalityState = ineq.copy(phase = InequalityState.Phase.COEFF_INPUT, activeCol = 0))
        else -> state
      }
    }
  }

  // --- MATRIX MODE ---
  private fun handleMatrixKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val ms = state.matrixState
    val names = listOf("MatA", "MatB", "MatC", "MatD")
    return when (ms.phase) {
      MatrixAppState.Phase.WORK -> null // Handled by standard expression editor + CATALOG/TOOLS
      MatrixAppState.Phase.PICK_MATRIX -> when (key) {
        CalcKey.UP -> state.copy(matrixState = ms.copy(editRow = (ms.editRow - 1).coerceAtLeast(0)))
        CalcKey.DOWN -> state.copy(matrixState = ms.copy(editRow = (ms.editRow + 1).coerceAtMost(3)))
        CalcKey.OK, CalcKey.EXE -> state.copy(matrixState = ms.copy(editingMatrixName = names[ms.editRow.coerceIn(0, 3)], phase = MatrixAppState.Phase.PICK_DIM, editRow = 0))
        CalcKey.BACK -> state.copy(matrixState = ms.copy(phase = MatrixAppState.Phase.WORK))
        else -> state
      }
      MatrixAppState.Phase.PICK_DIM -> when (key) {
        CalcKey.UP -> state.copy(matrixState = ms.copy(editRow = (ms.editRow - 1).coerceAtLeast(0)))
        CalcKey.DOWN -> state.copy(matrixState = ms.copy(editRow = (ms.editRow + 1).coerceAtMost(1)))
        CalcKey.D1, CalcKey.D2, CalcKey.D3, CalcKey.D4 -> {
          val n = when (key) { CalcKey.D1 -> 1; CalcKey.D2 -> 2; CalcKey.D3 -> 3; else -> 4 }
          val upd = if (ms.editRow == 0) ms.copy(rows = n, editRow = 1) else ms.copy(cols = n)
          state.copy(matrixState = upd)
        }
        CalcKey.OK, CalcKey.EXE -> {
          val existing = ms.matrices[ms.editingMatrixName]
          val data = List(ms.rows) { r -> List(ms.cols) { c -> existing?.data?.getOrNull(r)?.getOrNull(c) ?: 0.0 } }
          val newMap = ms.matrices + (ms.editingMatrixName to EvalResult.MatrixResult(ms.rows, ms.cols, data))
          state.copy(matrixState = ms.copy(phase = MatrixAppState.Phase.EDIT_GRID, matrices = newMap, editRow = 0, editCol = 0, inputBuffer = ""))
        }
        CalcKey.BACK -> state.copy(matrixState = ms.copy(phase = MatrixAppState.Phase.PICK_MATRIX, editRow = 0))
        else -> state
      }
      MatrixAppState.Phase.EDIT_GRID -> {
        fun commitCell(s: CalculatorUiState): CalculatorUiState {
          val m = s.matrixState
          if (m.inputBuffer.isEmpty()) return s
          val v = m.inputBuffer.toDoubleOrNull() ?: 0.0
          val curMat = m.matrices[m.editingMatrixName] ?: return s
          val newData = curMat.data.mapIndexed { r, row ->
            if (r == m.editRow) row.mapIndexed { c, old -> if (c == m.editCol) v else old } else row
          }
          return s.copy(matrixState = m.copy(matrices = m.matrices + (m.editingMatrixName to curMat.copy(data = newData)), inputBuffer = ""))
        }
        when (key) {
          CalcKey.LEFT -> commitCell(state).let { s -> s.copy(matrixState = s.matrixState.copy(editCol = (s.matrixState.editCol - 1).coerceAtLeast(0))) }
          CalcKey.RIGHT -> commitCell(state).let { s -> s.copy(matrixState = s.matrixState.copy(editCol = (s.matrixState.editCol + 1).coerceAtMost(ms.cols - 1))) }
          CalcKey.UP -> commitCell(state).let { s -> s.copy(matrixState = s.matrixState.copy(editRow = (s.matrixState.editRow - 1).coerceAtLeast(0))) }
          CalcKey.DOWN -> commitCell(state).let { s -> s.copy(matrixState = s.matrixState.copy(editRow = (s.matrixState.editRow + 1).coerceAtMost(ms.rows - 1))) }
          CalcKey.OK -> {
            val s = commitCell(state)
            val m = s.matrixState
            var nc = m.editCol + 1
            var nr = m.editRow
            if (nc >= m.cols) { nc = 0; nr++ }
            if (nr >= m.rows) nr = 0
            s.copy(matrixState = m.copy(editRow = nr, editCol = nc))
          }
          CalcKey.EXE, CalcKey.AC -> commitCell(state).let { s -> s.copy(matrixState = s.matrixState.copy(phase = MatrixAppState.Phase.WORK)) }
          CalcKey.BACK -> state.copy(matrixState = ms.copy(phase = MatrixAppState.Phase.WORK))
          else -> {
            val next = appendNumericKey(ms.inputBuffer, key, isShift) ?: return state
            state.copy(matrixState = ms.copy(inputBuffer = next))
          }
        }
      }
    }
  }

  // --- VECTOR MODE ---
  private fun handleVectorKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val vs = state.vectorState
    val names = listOf("VctA", "VctB", "VctC", "VctD")
    return when (vs.phase) {
      VectorAppState.Phase.WORK -> null
      VectorAppState.Phase.PICK_VECTOR -> when (key) {
        CalcKey.UP -> state.copy(vectorState = vs.copy(editIdx = (vs.editIdx - 1).coerceAtLeast(0)))
        CalcKey.DOWN -> state.copy(vectorState = vs.copy(editIdx = (vs.editIdx + 1).coerceAtMost(3)))
        CalcKey.OK, CalcKey.EXE -> state.copy(vectorState = vs.copy(editingVectorName = names[vs.editIdx.coerceIn(0, 3)], phase = VectorAppState.Phase.PICK_DIM, editIdx = 0))
        CalcKey.BACK -> state.copy(vectorState = vs.copy(phase = VectorAppState.Phase.WORK))
        else -> state
      }
      VectorAppState.Phase.PICK_DIM -> when (key) {
        CalcKey.UP -> state.copy(vectorState = vs.copy(dim = 2))
        CalcKey.DOWN -> state.copy(vectorState = vs.copy(dim = 3))
        CalcKey.D2 -> createVectorGrid(state, 2)
        CalcKey.D3 -> createVectorGrid(state, 3)
        CalcKey.OK, CalcKey.EXE -> createVectorGrid(state, vs.dim)
        CalcKey.BACK -> state.copy(vectorState = vs.copy(phase = VectorAppState.Phase.PICK_VECTOR, editIdx = 0))
        else -> state
      }
      VectorAppState.Phase.EDIT_GRID -> {
        fun commit(s: CalculatorUiState): CalculatorUiState {
          val v = s.vectorState
          if (v.inputBuffer.isEmpty()) return s
          val num = v.inputBuffer.toDoubleOrNull() ?: 0.0
          val cur = v.vectors[v.editingVectorName] ?: return s
          val newData = cur.data.mapIndexed { i, old -> if (i == v.editIdx) num else old }
          return s.copy(vectorState = v.copy(vectors = v.vectors + (v.editingVectorName to EvalResult.VectorResult(newData)), inputBuffer = ""))
        }
        when (key) {
          CalcKey.UP, CalcKey.LEFT -> commit(state).let { s -> s.copy(vectorState = s.vectorState.copy(editIdx = (s.vectorState.editIdx - 1).coerceAtLeast(0))) }
          CalcKey.DOWN, CalcKey.RIGHT -> commit(state).let { s -> s.copy(vectorState = s.vectorState.copy(editIdx = (s.vectorState.editIdx + 1).coerceAtMost(vs.dim - 1))) }
          CalcKey.OK -> commit(state).let { s ->
            val next = if (s.vectorState.editIdx + 1 < s.vectorState.dim) s.vectorState.editIdx + 1 else 0
            s.copy(vectorState = s.vectorState.copy(editIdx = next))
          }
          CalcKey.EXE, CalcKey.AC -> commit(state).let { s -> s.copy(vectorState = s.vectorState.copy(phase = VectorAppState.Phase.WORK)) }
          CalcKey.BACK -> state.copy(vectorState = vs.copy(phase = VectorAppState.Phase.WORK))
          else -> {
            val next = appendNumericKey(vs.inputBuffer, key, isShift) ?: return state
            state.copy(vectorState = vs.copy(inputBuffer = next))
          }
        }
      }
    }
  }

  private fun createVectorGrid(state: CalculatorUiState, dim: Int): CalculatorUiState {
    val vs = state.vectorState
    val existing = vs.vectors[vs.editingVectorName]
    val data = List(dim) { i -> existing?.data?.getOrNull(i) ?: 0.0 }
    return state.copy(
      vectorState = vs.copy(
        dim = dim,
        phase = VectorAppState.Phase.EDIT_GRID,
        vectors = vs.vectors + (vs.editingVectorName to EvalResult.VectorResult(data)),
        editIdx = 0,
        inputBuffer = ""
      )
    )
  }

  // --- STATISTICS MODE ---
  private fun handleStatsKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val st = state.statsState
    return when (st.phase) {
      StatsAppState.Phase.SELECT_MODEL -> {
        val digitPick = when (key) {
          CalcKey.D1 -> 0; CalcKey.D2 -> 1; CalcKey.D3 -> 2; CalcKey.D4 -> 3
          CalcKey.D5 -> 4; CalcKey.D6 -> 5; CalcKey.D7 -> 6; CalcKey.D8 -> 7
          else -> null
        }
        if (digitPick != null && digitPick <= StatModelType.entries.lastIndex) {
          val model = StatModelType.entries[digitPick]
          return state.copy(statsState = st.copy(model = model, phase = StatsAppState.Phase.DATA_GRID, activeRow = 0, activeCol = 0, inputBuffer = ""))
        }
        when (key) {
          CalcKey.UP -> state.copy(statsState = st.copy(activeRow = (st.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(statsState = st.copy(activeRow = (st.activeRow + 1).coerceAtMost(StatModelType.entries.lastIndex)))
          CalcKey.PAGE_UP -> state.copy(statsState = st.copy(activeRow = (st.activeRow - 4).coerceAtLeast(0)))
          CalcKey.PAGE_DOWN -> state.copy(statsState = st.copy(activeRow = (st.activeRow + 4).coerceAtMost(StatModelType.entries.lastIndex)))
          CalcKey.OK, CalcKey.EXE -> {
            val model = StatModelType.entries[st.activeRow.coerceIn(0, StatModelType.entries.lastIndex)]
            state.copy(statsState = st.copy(model = model, phase = StatsAppState.Phase.DATA_GRID, activeRow = 0, activeCol = 0, inputBuffer = ""))
          }
          else -> state
        }
      }
      StatsAppState.Phase.DATA_GRID -> {
        val showY = st.model != StatModelType.ONE_VAR
        val showFreq = state.settings.statFrequency
        val maxCol = (if (showY) 1 else 0) + (if (showFreq) 1 else 0)

        fun commitCell(s: CalculatorUiState): CalculatorUiState {
          val cur = s.statsState
          if (cur.inputBuffer.isEmpty()) return s
          val v = cur.inputBuffer.toDoubleOrNull() ?: 0.0
          val xs = cur.xValues.toMutableList()
          val ys = cur.yValues.toMutableList()
          val fs = cur.freqValues.toMutableList()
          while (xs.size <= cur.activeRow) {
            xs.add(0.0); ys.add(0.0); fs.add(1.0)
          }
          when {
            cur.activeCol == 0 -> xs[cur.activeRow] = v
            cur.activeCol == 1 && showY -> ys[cur.activeRow] = v
            else -> fs[cur.activeRow] = v.coerceAtLeast(0.0)
          }
          return s.copy(statsState = cur.copy(xValues = xs, yValues = ys, freqValues = fs, inputBuffer = ""))
        }

        when (key) {
          CalcKey.LEFT -> commitCell(state).let { s -> s.copy(statsState = s.statsState.copy(activeCol = (s.statsState.activeCol - 1).coerceAtLeast(0))) }
          CalcKey.RIGHT -> commitCell(state).let { s -> s.copy(statsState = s.statsState.copy(activeCol = (s.statsState.activeCol + 1).coerceAtMost(maxCol))) }
          CalcKey.UP -> commitCell(state).let { s -> s.copy(statsState = s.statsState.copy(activeRow = (s.statsState.activeRow - 1).coerceAtLeast(0))) }
          CalcKey.DOWN -> commitCell(state).let { s -> s.copy(statsState = s.statsState.copy(activeRow = (s.statsState.activeRow + 1).coerceAtMost(s.statsState.xValues.size))) }
          CalcKey.PAGE_UP -> commitCell(state).let { s -> s.copy(statsState = s.statsState.copy(activeRow = (s.statsState.activeRow - 4).coerceAtLeast(0))) }
          CalcKey.PAGE_DOWN -> commitCell(state).let { s -> s.copy(statsState = s.statsState.copy(activeRow = (s.statsState.activeRow + 4).coerceAtMost(s.statsState.xValues.size))) }
          CalcKey.AC -> {
            if (st.inputBuffer.isNotEmpty()) state.copy(statsState = st.copy(inputBuffer = ""))
            else state.copy(statsState = st.copy(xValues = emptyList(), yValues = emptyList(), freqValues = emptyList(), activeRow = 0, activeCol = 0))
          }
          CalcKey.OK -> commitCell(state).let { s ->
            s.copy(statsState = s.statsState.copy(activeRow = (s.statsState.activeRow + 1).coerceAtMost(s.statsState.xValues.size)))
          }
          CalcKey.EXE -> {
            val s = commitCell(state)
            val summary = AdvancedMathSolvers.computeStatistics(
              s.statsState.model, s.statsState.xValues, s.statsState.yValues,
              if (state.settings.statFrequency) s.statsState.freqValues else List(s.statsState.xValues.size) { 1.0 }
            )
            s.copy(statsState = s.statsState.copy(phase = StatsAppState.Phase.RESULTS, summary = summary, activeRow = 0))
          }
          CalcKey.BACK -> state.copy(statsState = st.copy(phase = StatsAppState.Phase.SELECT_MODEL, activeRow = 0))
          else -> {
            val next = appendNumericKey(st.inputBuffer, key, isShift) ?: return state
            state.copy(statsState = st.copy(inputBuffer = next))
          }
        }
      }
      StatsAppState.Phase.RESULTS -> {
        val maxResultRow = when (st.model) {
          StatModelType.ONE_VAR -> 10
          StatModelType.QUADRATIC -> 20
          else -> 19
        }
        when (key) {
          CalcKey.UP -> state.copy(statsState = st.copy(activeRow = (st.activeRow - 1).coerceAtLeast(0)))
          CalcKey.DOWN -> state.copy(statsState = st.copy(activeRow = (st.activeRow + 1).coerceAtMost(maxResultRow)))
          CalcKey.PAGE_UP -> state.copy(statsState = st.copy(activeRow = (st.activeRow - 4).coerceAtLeast(0)))
          CalcKey.PAGE_DOWN -> state.copy(statsState = st.copy(activeRow = (st.activeRow + 4).coerceAtMost(maxResultRow)))
          CalcKey.BACK, CalcKey.AC -> state.copy(statsState = st.copy(phase = StatsAppState.Phase.DATA_GRID, activeRow = 0))
          else -> state
        }
      }
      StatsAppState.Phase.STAT_CALC -> {
        when (key) {
          CalcKey.BACK -> state.copy(statsState = st.copy(phase = StatsAppState.Phase.DATA_GRID, activeRow = 0))
          CalcKey.EXE, CalcKey.OK -> {
            if (st.calcDoc.root.isEmpty()) return state
            val summary = AdvancedMathSolvers.computeStatistics(
              st.model, st.xValues, st.yValues,
              if (state.settings.statFrequency) st.freqValues else List(st.xValues.size) { 1.0 }
            )
            val ctx = EvalContext(
              settings = state.settings,
              mode = CalcMode.CALCULATE,
              variables = state.variables,
              ans = state.ans,
              statSummary = summary,
              statModel = st.model,
              fExpr = state.tableState.fDoc.root,
              gExpr = state.tableState.gDoc.root,
            )
            val res = ScientificEvaluator.evaluate(st.calcDoc.root, ctx)
            val newAns = if (res !is EvalResult.ErrorResult) res else state.ans
            state.copy(statsState = st.copy(summary = summary, calcResult = res), ans = newAns)
          }
          else -> null // Delegate expression typing to main natural math editor
        }
      }
    }
  }

  // --- DISTRIBUTION MODE ---
  private fun handleDistributionKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val ds = state.distributionState
    return when (ds.phase) {
      DistributionState.Phase.SELECT_TYPE -> when (key) {
        CalcKey.UP -> state.copy(distributionState = ds.copy(selectedParamIdx = (ds.selectedParamIdx - 1).coerceAtLeast(0)))
        CalcKey.DOWN -> state.copy(distributionState = ds.copy(selectedParamIdx = (ds.selectedParamIdx + 1).coerceAtMost(DistType.entries.lastIndex)))
        CalcKey.PAGE_UP -> state.copy(distributionState = ds.copy(selectedParamIdx = (ds.selectedParamIdx - 4).coerceAtLeast(0)))
        CalcKey.PAGE_DOWN -> state.copy(distributionState = ds.copy(selectedParamIdx = (ds.selectedParamIdx + 4).coerceAtMost(DistType.entries.lastIndex)))
        CalcKey.OK, CalcKey.EXE -> {
          val t = DistType.entries[ds.selectedParamIdx.coerceIn(0, DistType.entries.lastIndex)]
          state.copy(distributionState = ds.copy(type = t, phase = DistributionState.Phase.PARAM_INPUT, selectedParamIdx = 0, inputBuffer = ""))
        }
        else -> state
      }
      DistributionState.Phase.PARAM_INPUT -> {
        val keys = distParamKeys(ds.type)
        fun commit(s: CalculatorUiState): CalculatorUiState {
          val d = s.distributionState
          if (d.inputBuffer.isEmpty()) return s
          val k = keys[d.selectedParamIdx]
          return s.copy(distributionState = d.copy(params = d.params + (k to d.inputBuffer), inputBuffer = ""))
        }
        when (key) {
          CalcKey.UP -> commit(state).let { s -> s.copy(distributionState = s.distributionState.copy(selectedParamIdx = (s.distributionState.selectedParamIdx - 1).coerceAtLeast(0))) }
          CalcKey.DOWN -> commit(state).let { s -> s.copy(distributionState = s.distributionState.copy(selectedParamIdx = (s.distributionState.selectedParamIdx + 1).coerceAtMost(keys.lastIndex))) }
          CalcKey.OK -> {
            val s = commit(state)
            if (s.distributionState.selectedParamIdx + 1 < keys.size) {
              s.copy(distributionState = s.distributionState.copy(selectedParamIdx = s.distributionState.selectedParamIdx + 1))
            } else evalDistribution(s)
          }
          CalcKey.EXE -> evalDistribution(commit(state))
          CalcKey.BACK -> state.copy(distributionState = ds.copy(phase = DistributionState.Phase.SELECT_TYPE, selectedParamIdx = 0))
          else -> {
            val next = appendNumericKey(ds.inputBuffer, key, isShift) ?: return state
            state.copy(distributionState = ds.copy(inputBuffer = next))
          }
        }
      }
      DistributionState.Phase.RESULT -> when (key) {
        CalcKey.BACK, CalcKey.AC, CalcKey.OK -> state.copy(distributionState = ds.copy(phase = DistributionState.Phase.PARAM_INPUT, selectedParamIdx = 0))
        else -> state
      }
    }
  }

  fun distParamKeys(type: DistType): List<String> = when (type) {
    DistType.NORMAL_PD -> listOf("x", "σ", "μ")
    DistType.NORMAL_CD -> listOf("Lower", "Upper", "σ", "μ")
    DistType.INV_NORM -> listOf("Area", "σ", "μ")
    DistType.BINOMIAL_PD, DistType.BINOMIAL_CD -> listOf("x", "N", "p")
    DistType.POISSON_PD, DistType.POISSON_CD -> listOf("x", "λ")
  }

  private fun evalDistribution(state: CalculatorUiState): CalculatorUiState {
    val d = state.distributionState
    fun p(k: String, def: Double = 0.0) = d.params[k]?.toDoubleOrNull() ?: def
    val res = when (d.type) {
      DistType.NORMAL_PD -> AdvancedMathSolvers.normalPdf(p("x"), p("σ", 1.0).coerceAtLeast(1e-9), p("μ"))
      DistType.NORMAL_CD -> AdvancedMathSolvers.normalCdfRange(p("Lower"), p("Upper"), p("σ", 1.0).coerceAtLeast(1e-9), p("μ"))
      DistType.INV_NORM -> AdvancedMathSolvers.inverseNormalCdf(p("Area", 0.5).coerceIn(1e-9, 1 - 1e-9), p("σ", 1.0).coerceAtLeast(1e-9), p("μ"))
      DistType.BINOMIAL_PD -> AdvancedMathSolvers.binomialPd(p("x").toInt(), p("N", 10.0).toInt(), p("p", 0.5).coerceIn(0.0, 1.0))
      DistType.BINOMIAL_CD -> AdvancedMathSolvers.binomialCd(p("x").toInt(), p("N", 10.0).toInt(), p("p", 0.5).coerceIn(0.0, 1.0))
      DistType.POISSON_PD -> AdvancedMathSolvers.poissonPd(p("x").toInt(), p("λ", 1.0).coerceAtLeast(1e-9))
      DistType.POISSON_CD -> AdvancedMathSolvers.poissonCd(p("x").toInt(), p("λ", 1.0).coerceAtLeast(1e-9))
    }
    return state.copy(distributionState = d.copy(phase = DistributionState.Phase.RESULT, resultValue = res))
  }

  // --- SPREADSHEET MODE (A1:E45 with $, Fill Formula, Fill Value, Copy/Cut/Paste) ---
  private fun handleSpreadsheetKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val sh = state.spreadsheetState
    if (sh.subPhase != SpreadsheetState.SubPhase.GRID) {
      val isFormula = sh.subPhase == SpreadsheetState.SubPhase.FILL_FORMULA
      when (key) {
        CalcKey.UP -> return state.copy(spreadsheetState = sh.copy(fillField = 0))
        CalcKey.DOWN -> return state.copy(spreadsheetState = sh.copy(fillField = 1))
        CalcKey.BACK -> return state.copy(spreadsheetState = sh.copy(subPhase = SpreadsheetState.SubPhase.GRID))
        CalcKey.DEL -> {
          return if (sh.fillField == 0) {
            state.copy(spreadsheetState = sh.copy(fillSourceRaw = sh.fillSourceRaw.dropLast(1)))
          } else {
            state.copy(spreadsheetState = sh.copy(fillRange = sh.fillRange.dropLast(1)))
          }
        }
        CalcKey.AC -> {
          return if (sh.fillField == 0) state.copy(spreadsheetState = sh.copy(fillSourceRaw = ""))
          else state.copy(spreadsheetState = sh.copy(fillRange = ""))
        }
        CalcKey.OK -> {
          if (sh.fillField == 0) return state.copy(spreadsheetState = sh.copy(fillField = 1))
        }
        CalcKey.EXE -> {}
        else -> {
          val ch = when {
            key == CalcKey.PAREN_OPEN && isShift -> "="
            key == CalcKey.PAREN_CLOSE && isShift -> ":"
            key == CalcKey.D4 && isShift -> "A"
            key == CalcKey.D5 && isShift -> "B"
            key == CalcKey.D6 && isShift -> "C"
            key == CalcKey.D1 && isShift -> "D"
            key == CalcKey.D2 && isShift -> "E"
            key == CalcKey.EQ -> "="
            key == CalcKey.ADD -> "+"
            key == CalcKey.SUB || (key == CalcKey.NEG && !isShift) -> "-"
            key == CalcKey.MUL -> "*"
            key == CalcKey.DIV -> "/"
            key == CalcKey.PAREN_OPEN -> "("
            key == CalcKey.PAREN_CLOSE -> ")"
            else -> appendNumericKey("", key, isShift)
          } ?: return state
          return if (sh.fillField == 0) {
            state.copy(spreadsheetState = sh.copy(fillSourceRaw = sh.fillSourceRaw + ch))
          } else {
            state.copy(spreadsheetState = sh.copy(fillRange = sh.fillRange + ch.uppercase()))
          }
        }
      }
      // Execute Fill Formula or Fill Value on EXE / OK on field 1
      val newCells = if (isFormula) {
        val raw = if (sh.fillSourceRaw.startsWith("=")) sh.fillSourceRaw else "=${sh.fillSourceRaw}"
        AdvancedMathSolvers.fillSpreadsheetFormula(sh.cells, sh.activeCol, sh.activeRow, raw, sh.fillRange)
      } else {
        AdvancedMathSolvers.fillSpreadsheetValue(sh.cells, sh.fillSourceRaw, sh.fillRange)
      }
      return state.copy(
        spreadsheetState = sh.copy(
          cells = newCells,
          evaluated = AdvancedMathSolvers.evaluateSpreadsheet(newCells),
          subPhase = SpreadsheetState.SubPhase.GRID,
        )
      )
    }

    if (sh.editing) {
      return when (key) {
        CalcKey.OK, CalcKey.EXE -> {
          val keyName = sh.cellName()
          val newCells = if (sh.editBuffer.isEmpty()) sh.cells - keyName else sh.cells + (keyName to sh.editBuffer)
          val evald = AdvancedMathSolvers.evaluateSpreadsheet(newCells)
          val nextRow = if (key == CalcKey.EXE) (sh.activeRow + 1).coerceAtMost(44) else sh.activeRow
          state.copy(spreadsheetState = sh.copy(cells = newCells, evaluated = evald, editing = false, editBuffer = "", activeRow = nextRow))
        }
        CalcKey.BACK -> state.copy(spreadsheetState = sh.copy(editing = false, editBuffer = ""))
        CalcKey.DEL -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer.dropLast(1)))
        CalcKey.AC -> state.copy(spreadsheetState = sh.copy(editBuffer = ""))
        CalcKey.ADD -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "+"))
        CalcKey.SUB -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "-"))
        CalcKey.NEG -> if (isShift) state else state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "-"))
        CalcKey.MUL -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "*"))
        CalcKey.DIV -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "/"))
        CalcKey.PAREN_OPEN -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) "=" else "("))
        CalcKey.PAREN_CLOSE -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) ":" else ")"))
        CalcKey.EQ -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "="))
        CalcKey.D4 -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) "A" else "4"))
        CalcKey.D5 -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) "B" else "5"))
        CalcKey.D6 -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) "C" else "6"))
        CalcKey.D1 -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) "D" else "1"))
        CalcKey.D2 -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + if (isShift) "E" else "2"))
        CalcKey.VAR_X -> state.copy(spreadsheetState = sh.copy(editBuffer = sh.editBuffer + "A1"))
        else -> {
          val next = appendNumericKey(sh.editBuffer, key, isShift) ?: return state
          state.copy(spreadsheetState = sh.copy(editBuffer = next))
        }
      }
    }

    return when (key) {
      CalcKey.LEFT -> state.copy(spreadsheetState = sh.copy(activeCol = (sh.activeCol - 1).coerceAtLeast(0)))
      CalcKey.RIGHT -> state.copy(spreadsheetState = sh.copy(activeCol = (sh.activeCol + 1).coerceAtMost(4)))
      CalcKey.UP -> state.copy(spreadsheetState = sh.copy(activeRow = (sh.activeRow - 1).coerceAtLeast(0)))
      CalcKey.DOWN -> state.copy(spreadsheetState = sh.copy(activeRow = (sh.activeRow + 1).coerceAtMost(44)))
      CalcKey.PAGE_UP -> state.copy(spreadsheetState = sh.copy(activeRow = (sh.activeRow - 5).coerceAtLeast(0)))
      CalcKey.PAGE_DOWN -> state.copy(spreadsheetState = sh.copy(activeRow = (sh.activeRow + 5).coerceAtMost(44)))
      CalcKey.OK -> {
        val curRaw = sh.cells[sh.cellName()] ?: ""
        state.copy(spreadsheetState = sh.copy(editing = true, editBuffer = curRaw))
      }
      CalcKey.DEL -> {
        val newCells = sh.cells - sh.cellName()
        state.copy(spreadsheetState = sh.copy(cells = newCells, evaluated = AdvancedMathSolvers.evaluateSpreadsheet(newCells)))
      }
      CalcKey.EQ -> state.copy(spreadsheetState = sh.copy(editing = true, editBuffer = "="))
      CalcKey.PAREN_OPEN -> if (isShift) state.copy(spreadsheetState = sh.copy(editing = true, editBuffer = "=")) else state
      CalcKey.SUB -> if (isShift) state.copy(spreadsheetState = sh.copy(editing = true, editBuffer = "-")) else state
      CalcKey.NEG -> if (isShift) state else state.copy(spreadsheetState = sh.copy(editing = true, editBuffer = "-"))
      CalcKey.D0, CalcKey.D1, CalcKey.D2, CalcKey.D3, CalcKey.D4, CalcKey.D5, CalcKey.D6, CalcKey.D7, CalcKey.D8, CalcKey.D9, CalcKey.DOT -> {
        val ch = appendNumericKey("", key, isShift) ?: ""
        state.copy(spreadsheetState = sh.copy(editing = true, editBuffer = ch))
      }
      else -> state
    }
  }

  // --- RATIO MODE ---
  private fun handleRatioKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val rs = state.ratioState
    return when (rs.phase) {
      RatioState.Phase.SELECT_TYPE -> when (key) {
        CalcKey.UP, CalcKey.D1 -> state.copy(ratioState = rs.copy(xOnLeft = true, phase = if (key == CalcKey.D1) RatioState.Phase.INPUT else RatioState.Phase.SELECT_TYPE, activeField = 0, inputBuffer = ""))
        CalcKey.DOWN, CalcKey.D2 -> state.copy(ratioState = rs.copy(xOnLeft = false, phase = if (key == CalcKey.D2) RatioState.Phase.INPUT else RatioState.Phase.SELECT_TYPE, activeField = 0, inputBuffer = ""))
        CalcKey.OK, CalcKey.EXE -> state.copy(ratioState = rs.copy(phase = RatioState.Phase.INPUT, activeField = 0, inputBuffer = ""))
        else -> state
      }
      RatioState.Phase.INPUT -> {
        fun commit(s: CalculatorUiState): CalculatorUiState {
          val r = s.ratioState
          if (r.inputBuffer.isEmpty()) return s
          val upd = when (r.activeField) {
            0 -> r.copy(a = r.inputBuffer, inputBuffer = "")
            1 -> r.copy(b = r.inputBuffer, inputBuffer = "")
            else -> r.copy(cOrD = r.inputBuffer, inputBuffer = "")
          }
          return s.copy(ratioState = upd)
        }
        fun solve(s: CalculatorUiState): CalculatorUiState {
          val r = s.ratioState
          val av = Rational.fromDecimalString(r.a)
          val bv = Rational.fromDecimalString(r.b)
          val cv = Rational.fromDecimalString(r.cOrD)
          val sol: ExactReal = if (av != null && bv != null && cv != null) {
            if (r.xOnLeft) {
              if (bv.isZero) return s.copy(toastBanner = "Math ERROR")
              ExactReal.fromRational((av * cv) / bv)
            } else {
              if (av.isZero) return s.copy(toastBanner = "Math ERROR")
              ExactReal.fromRational((bv * cv) / av)
            }
          } else {
            val ad = r.a.toDoubleOrNull() ?: 1.0
            val bd = r.b.toDoubleOrNull() ?: 1.0
            val cd = r.cOrD.toDoubleOrNull() ?: 1.0
            val res = if (r.xOnLeft) (ad * cd) / bd else (bd * cd) / ad
            ExactReal.fromDouble(res)
          }
          return s.copy(ratioState = r.copy(phase = RatioState.Phase.RESULT, solution = sol))
        }
        when (key) {
          CalcKey.LEFT, CalcKey.UP -> commit(state).let { s -> s.copy(ratioState = s.ratioState.copy(activeField = (s.ratioState.activeField - 1).coerceAtLeast(0))) }
          CalcKey.RIGHT, CalcKey.DOWN -> commit(state).let { s -> s.copy(ratioState = s.ratioState.copy(activeField = (s.ratioState.activeField + 1).coerceAtMost(2))) }
          CalcKey.OK -> {
            val s = commit(state)
            if (s.ratioState.activeField < 2) s.copy(ratioState = s.ratioState.copy(activeField = s.ratioState.activeField + 1))
            else solve(s)
          }
          CalcKey.EXE -> solve(commit(state))
          CalcKey.BACK -> state.copy(ratioState = rs.copy(phase = RatioState.Phase.SELECT_TYPE))
          else -> {
            val next = appendNumericKey(rs.inputBuffer, key, isShift) ?: return state
            state.copy(ratioState = rs.copy(inputBuffer = next))
          }
        }
      }
      RatioState.Phase.RESULT -> when (key) {
        CalcKey.BACK, CalcKey.AC, CalcKey.OK -> state.copy(ratioState = rs.copy(phase = RatioState.Phase.INPUT, activeField = 0))
        else -> state
      }
    }
  }

  // --- MATH BOX MODE (All 4 official fx-991LA CW tools: Dice, Coin, Number Line, Circle) ---
  private fun handleMathBoxKey(state: CalculatorUiState, key: CalcKey, isShift: Boolean): CalculatorUiState? {
    val mb = state.mathBoxState
    return when (mb.phase) {
      MathBoxState.Phase.SELECT_TYPE -> {
        val total = MathBoxType.entries.size
        val curIdx = mb.type.ordinal
        when (key) {
          CalcKey.UP -> state.copy(mathBoxState = mb.copy(type = MathBoxType.entries[(curIdx - 1 + total) % total]))
          CalcKey.DOWN -> state.copy(mathBoxState = mb.copy(type = MathBoxType.entries[(curIdx + 1) % total]))
          CalcKey.D1 -> selectMathBoxType(state, MathBoxType.DICE)
          CalcKey.D2 -> selectMathBoxType(state, MathBoxType.COIN)
          CalcKey.D3 -> selectMathBoxType(state, MathBoxType.NUMBER_LINE)
          CalcKey.D4 -> selectMathBoxType(state, MathBoxType.CIRCLE)
          CalcKey.OK, CalcKey.EXE -> selectMathBoxType(state, mb.type)
          else -> state
        }
      }
      MathBoxState.Phase.PARAMS -> when (mb.type) {
        MathBoxType.DICE, MathBoxType.COIN -> {
          fun commit(s: CalculatorUiState): CalculatorUiState {
            val m = s.mathBoxState
            if (m.inputBuffer.isEmpty()) return s
            val num = m.inputBuffer.toIntOrNull() ?: 1
            return if (m.activeParam == 0) {
              s.copy(mathBoxState = m.copy(objectCount = num.coerceIn(1, 3), inputBuffer = ""))
            } else {
              s.copy(mathBoxState = m.copy(trials = num.coerceIn(1, 250), inputBuffer = ""))
            }
          }
          when (key) {
            CalcKey.UP -> commit(state).let { s -> s.copy(mathBoxState = s.mathBoxState.copy(activeParam = 0)) }
            CalcKey.DOWN -> commit(state).let { s -> s.copy(mathBoxState = s.mathBoxState.copy(activeParam = 1)) }
            CalcKey.OK -> {
              val s = commit(state)
              if (s.mathBoxState.activeParam == 0) s.copy(mathBoxState = s.mathBoxState.copy(activeParam = 1))
              else simulateDiceCoin(s)
            }
            CalcKey.EXE -> simulateDiceCoin(commit(state))
            CalcKey.BACK -> state.copy(mathBoxState = mb.copy(phase = MathBoxState.Phase.SELECT_TYPE))
            else -> {
              val next = appendNumericKey(mb.inputBuffer, key, isShift) ?: return state
              state.copy(mathBoxState = mb.copy(inputBuffer = next))
            }
          }
        }
        MathBoxType.NUMBER_LINE -> {
          val slot = mb.nlActiveSlot.coerceIn(0, 2)
          fun commitNlVal(s: CalculatorUiState): CalculatorUiState {
            val m = s.mathBoxState
            if (m.inputBuffer.isEmpty()) return s.copy(mathBoxState = m.copy(nlEditingValue = false))
            val parsed = m.inputBuffer.toDoubleOrNull() ?: m.nlEntries[slot].value
            val upd = m.nlEntries.mapIndexed { idx, e -> if (idx == slot) e.copy(value = parsed, enabled = true) else e }
            return s.copy(mathBoxState = m.copy(nlEntries = upd, inputBuffer = "", nlEditingValue = false))
          }
          when (key) {
            CalcKey.UP -> commitNlVal(state).let { s -> s.copy(mathBoxState = s.mathBoxState.copy(nlActiveSlot = (slot - 1).coerceAtLeast(0))) }
            CalcKey.DOWN -> commitNlVal(state).let { s -> s.copy(mathBoxState = s.mathBoxState.copy(nlActiveSlot = (slot + 1).coerceAtMost(2))) }
            CalcKey.LEFT, CalcKey.RIGHT -> {
              val s = commitNlVal(state)
              val curOp = s.mathBoxState.nlEntries[slot].op
              val ops = NumberLineOp.entries
              val dir = if (key == CalcKey.RIGHT) 1 else -1
              val nextOp = ops[(curOp.ordinal + dir + ops.size) % ops.size]
              val upd = s.mathBoxState.nlEntries.mapIndexed { idx, e -> if (idx == slot) e.copy(op = nextOp, enabled = true) else e }
              s.copy(mathBoxState = s.mathBoxState.copy(nlEntries = upd))
            }
            CalcKey.OK -> {
              val s = commitNlVal(state)
              val upd = s.mathBoxState.nlEntries.mapIndexed { idx, e -> if (idx == slot) e.copy(enabled = !e.enabled) else e }
              s.copy(mathBoxState = s.mathBoxState.copy(nlEntries = upd))
            }
            CalcKey.EXE -> {
              val s = commitNlVal(state)
              s.copy(mathBoxState = s.mathBoxState.copy(phase = MathBoxState.Phase.RESULTS))
            }
            CalcKey.BACK -> state.copy(mathBoxState = mb.copy(phase = MathBoxState.Phase.SELECT_TYPE, inputBuffer = ""))
            else -> {
              val next = appendNumericKey(mb.inputBuffer, key, isShift) ?: return state
              state.copy(mathBoxState = mb.copy(inputBuffer = next, nlEditingValue = true))
            }
          }
        }
        MathBoxType.CIRCLE -> {
          when (key) {
            CalcKey.LEFT -> {
              val modes = CircleModeType.entries
              val nextMode = modes[(mb.circleMode.ordinal - 1 + modes.size) % modes.size]
              state.copy(mathBoxState = mb.copy(circleMode = nextMode))
            }
            CalcKey.RIGHT -> {
              val modes = CircleModeType.entries
              val nextMode = modes[(mb.circleMode.ordinal + 1) % modes.size]
              state.copy(mathBoxState = mb.copy(circleMode = nextMode))
            }
            CalcKey.OK, CalcKey.EXE -> {
              val angle = mb.inputBuffer.toIntOrNull()?.let { ((it % 360) + 360) % 360 } ?: mb.circleAngleDeg
              state.copy(mathBoxState = mb.copy(circleAngleDeg = angle, inputBuffer = "", phase = MathBoxState.Phase.RESULTS))
            }
            CalcKey.BACK -> state.copy(mathBoxState = mb.copy(phase = MathBoxState.Phase.SELECT_TYPE, inputBuffer = ""))
            else -> {
              val next = appendNumericKey(mb.inputBuffer, key, isShift) ?: return state
              state.copy(mathBoxState = mb.copy(inputBuffer = next))
            }
          }
        }
      }
      MathBoxState.Phase.RESULTS -> when (mb.type) {
        MathBoxType.DICE, MathBoxType.COIN -> when (key) {
          CalcKey.EXE, CalcKey.OK -> simulateDiceCoin(state)
          CalcKey.LEFT, CalcKey.RIGHT -> state.copy(mathBoxState = mb.copy(diceShowList = !mb.diceShowList))
          CalcKey.BACK, CalcKey.AC -> state.copy(mathBoxState = mb.copy(phase = MathBoxState.Phase.PARAMS, activeParam = 0))
          else -> state
        }
        MathBoxType.NUMBER_LINE -> when (key) {
          CalcKey.LEFT -> state.copy(mathBoxState = mb.copy(nlPanOffset = mb.nlPanOffset - 1.0 / mb.nlZoom))
          CalcKey.RIGHT -> state.copy(mathBoxState = mb.copy(nlPanOffset = mb.nlPanOffset + 1.0 / mb.nlZoom))
          CalcKey.UP, CalcKey.ADD -> state.copy(mathBoxState = mb.copy(nlZoom = (mb.nlZoom * 1.5).coerceAtMost(8.0)))
          CalcKey.DOWN, CalcKey.SUB -> state.copy(mathBoxState = mb.copy(nlZoom = (mb.nlZoom / 1.5).coerceAtLeast(0.25)))
          CalcKey.BACK, CalcKey.AC -> state.copy(mathBoxState = mb.copy(phase = MathBoxState.Phase.PARAMS))
          else -> state
        }
        MathBoxType.CIRCLE -> {
          val maxDeg = if (mb.circleMode == CircleModeType.HALF) 180 else 360
          when (key) {
            CalcKey.LEFT -> {
              val next = if (mb.circleMode == CircleModeType.HALF) {
                (mb.circleAngleDeg + mb.circleStepDeg).coerceIn(0, 180)
              } else {
                (mb.circleAngleDeg + mb.circleStepDeg) % 360
              }
              state.copy(mathBoxState = mb.copy(circleAngleDeg = next))
            }
            CalcKey.RIGHT -> {
              val next = if (mb.circleMode == CircleModeType.HALF) {
                (mb.circleAngleDeg - mb.circleStepDeg).coerceIn(0, 180)
              } else {
                ((mb.circleAngleDeg - mb.circleStepDeg) + 360) % 360
              }
              state.copy(mathBoxState = mb.copy(circleAngleDeg = next))
            }
            CalcKey.UP, CalcKey.DOWN -> {
              val modes = CircleModeType.entries
              val dir = if (key == CalcKey.DOWN) 1 else -1
              val nextMode = modes[(mb.circleMode.ordinal + dir + modes.size) % modes.size]
              val clampedAngle = if (nextMode == CircleModeType.HALF) mb.circleAngleDeg.coerceIn(0, 180) else mb.circleAngleDeg % maxDeg
              state.copy(mathBoxState = mb.copy(circleMode = nextMode, circleAngleDeg = clampedAngle))
            }
            CalcKey.OK -> {
              val steps = listOf(15, 30, 45, 5)
              val nextStep = steps[(steps.indexOf(mb.circleStepDeg) + 1) % steps.size]
              state.copy(mathBoxState = mb.copy(circleStepDeg = nextStep), toastBanner = "Paso: ${nextStep}°")
            }
            CalcKey.BACK, CalcKey.AC -> state.copy(mathBoxState = mb.copy(phase = MathBoxState.Phase.PARAMS))
            else -> state
          }
        }
      }
    }
  }

  private fun selectMathBoxType(state: CalculatorUiState, type: MathBoxType): CalculatorUiState {
    val nextPhase = if (type == MathBoxType.CIRCLE) MathBoxState.Phase.RESULTS else MathBoxState.Phase.PARAMS
    return state.copy(mathBoxState = state.mathBoxState.copy(type = type, phase = nextPhase, activeParam = 0, inputBuffer = ""))
  }

  private fun simulateDiceCoin(state: CalculatorUiState): CalculatorUiState {
    val m = state.mathBoxState
    val counts = mutableMapOf<Int, Int>()
    val samples = mutableListOf<List<Int>>()
    repeat(m.trials) {
      val draw = List(m.objectCount) {
        if (m.type == MathBoxType.DICE) Random.nextInt(1, 7) else Random.nextInt(0, 2)
      }
      val sum = draw.sum()
      counts[sum] = (counts[sum] ?: 0) + 1
      samples.add(draw)
    }
    return state.copy(
      mathBoxState = m.copy(
        phase = MathBoxState.Phase.RESULTS,
        counts = counts.toSortedMap(),
        rollSamples = samples,
      )
    )
  }

  private fun formatNumTrim(d: Double): String {
    return if (abs(d - d.toLong()) < 1e-10) d.toLong().toString() else d.toString()
  }
}
