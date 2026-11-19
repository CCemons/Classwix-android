package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.engine.*
import com.example.viewmodel.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClassWizShiftBehaviorTest {

  private fun newVm(): CalculatorViewModel {
    val app = ApplicationProvider.getApplicationContext<Application>()
    return CalculatorViewModel(app)
  }

  @Test
  fun shiftNumericConstantsAndPowerOff() {
    val vm = newVm()

    // SHIFT + 7 -> π
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.D7)
    vm.onKey(CalcKey.EXE)
    val piRes = vm.uiState.value.currentResult as EvalResult.RealResult
    assertTrue(piRes.value is ExactReal.PiMultiple)
    assertEquals(Math.PI, piRes.value.toDouble(), 1e-10)

    // SHIFT + 8 -> e
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.D8)
    vm.onKey(CalcKey.EXE)
    val eRes = vm.uiState.value.currentResult as EvalResult.RealResult
    assertEquals(Math.E, eRes.value.toDouble(), 1e-10)

    // Complex mode: SHIFT + 9 -> i
    vm.selectHomeApp(CalcMode.COMPLEX)
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.D9)
    vm.onKey(CalcKey.EXE)
    val cRes = vm.uiState.value.currentResult as EvalResult.ComplexResult
    assertEquals(2.0, cRes.re.toDouble(), 1e-10)
    assertEquals(3.0, cRes.im.toDouble(), 1e-10)

    // SHIFT + AC -> OFF, ON -> ON
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.AC)
    assertFalse(vm.uiState.value.isPoweredOn)
    vm.onKey(CalcKey.ON)
    assertTrue(vm.uiState.value.isPoweredOn)
  }

  @Test
  fun shiftExeApproxAndFormatSdAndAnsPreAns() {
    val vm = newVm()

    // 1 ÷ 2 -> EXE = 1/2, then SHIFT + EXE (≈) switches to DECIMAL without duplicate history
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.DIV)
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.EXE)
    assertNull(vm.uiState.value.resultFormatOverride)
    val histCount = vm.uiState.value.history.size

    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.EXE)
    assertEquals(ResultFormatMode.DECIMAL, vm.uiState.value.resultFormatOverride)
    assertEquals(histCount, vm.uiState.value.history.size)

    // SHIFT + FORMAT toggles back to STANDARD
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.FORMAT)
    assertEquals(ResultFormatMode.STANDARD, vm.uiState.value.resultFormatOverride)

    // 5 EXE, then SHIFT + DMS (!) -> Ans! = 120
    vm.onKey(CalcKey.D5)
    vm.onKey(CalcKey.EXE)
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.DMS)
    vm.onKey(CalcKey.EXE)
    val factRes = vm.uiState.value.currentResult as EvalResult.RealResult
    assertEquals(120.0, factRes.value.toDouble(), 1e-9)

    // Now Ans=120, PreAns=5. Evaluate PreAns -> 5
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.ANS)
    vm.onKey(CalcKey.EXE)
    val preRes = vm.uiState.value.currentResult as EvalResult.RealResult
    assertEquals(5.0, preRes.value.toDouble(), 1e-9)

    // Ans is now 5: SHIFT + POWER (■⁻¹) -> Ans⁻¹ = 0.2
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.POWER)
    vm.onKey(CalcKey.EXE)
    val invRes = vm.uiState.value.currentResult as EvalResult.RealResult
    assertEquals(0.2, invRes.value.toDouble(), 1e-9)
  }

  @Test
  fun fxMenuQrVerifyAndHomeGridNavigation() {
    val vm = newVm()

    // Press FX -> opens FX_MENU, select Definir f(x) (0)
    vm.onKey(CalcKey.FX)
    assertEquals(OverlayScreen.FX_MENU, vm.uiState.value.overlay)
    vm.selectOverlayItem(0)
    assertEquals(0, vm.uiState.value.editingFxTarget)

    // Define f(x) = 2x + 1 and press OK
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.VAR_X)
    vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.OK)
    assertNull(vm.uiState.value.editingFxTarget)

    // Insert f( ) from FX_MENU (index 2) and evaluate f(4) = 9
    vm.onKey(CalcKey.FX)
    vm.selectOverlayItem(2)
    vm.onKey(CalcKey.D4)
    vm.onKey(CalcKey.PAREN_CLOSE)
    vm.onKey(CalcKey.EXE)
    val fxRes = vm.uiState.value.currentResult as EvalResult.RealResult
    assertEquals(9.0, fxRes.value.toDouble(), 1e-9)

    // SHIFT + FX -> quick edit g(x)
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.FX)
    assertEquals(1, vm.uiState.value.editingFxTarget)
    vm.onKey(CalcKey.BACK)
    assertNull(vm.uiState.value.editingFxTarget)

    // Verify 2+3≠6 -> TRUE
    vm.onKey(CalcKey.AC)
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.EQ)
    vm.onKey(CalcKey.D6)
    vm.onKey(CalcKey.EXE)
    val vRes = vm.uiState.value.currentResult as EvalResult.VerifyResult
    assertTrue(vRes.isTrue)

    // SHIFT + x -> QR overlay
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.VAR_X)
    assertEquals(OverlayScreen.QR_VIEW, vm.uiState.value.overlay)
    assertTrue(vm.uiState.value.qrMatrix.size >= 21)
    vm.onKey(CalcKey.BACK)
    assertEquals(OverlayScreen.NONE, vm.uiState.value.overlay)

    // HOME 4-column navigation: cursor 0 + DOWN -> cursor 4
    vm.onKey(CalcKey.HOME)
    assertEquals(OverlayScreen.HOME_GRID, vm.uiState.value.overlay)
    assertEquals(0, vm.uiState.value.overlayCursor)
    vm.onKey(CalcKey.DOWN)
    assertEquals(4, vm.uiState.value.overlayCursor)
  }

  @Test
  fun shiftConstantsInNumericModes() {
    val vm = newVm()
    vm.selectHomeApp(CalcMode.EQUATION)
    vm.onKey(CalcKey.D1) // 1. Simul Equation
    assertEquals(EquationState.SubType.SIMUL_DIM, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.D2) // 2 Unknowns
    assertEquals(EquationState.SubType.LINEAR_INPUT, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.D7) // inserts π numeric approximation
    assertTrue(vm.uiState.value.equationState.inputBuffer.startsWith("3.14159"))
  }

  @Test
  fun equationSolverAndSubmenus() {
    val vm = newVm()
    vm.selectHomeApp(CalcMode.EQUATION)
    assertEquals(EquationState.SubType.MENU, vm.uiState.value.equationState.subType)

    // Navigate down to 3. Solver and enter with OK
    vm.onKey(CalcKey.DOWN)
    vm.onKey(CalcKey.DOWN)
    assertEquals(2, vm.uiState.value.equationState.activeRow)
    vm.onKey(CalcKey.OK)
    assertEquals(EquationState.SubType.SOLVER_EQ, vm.uiState.value.equationState.subType)

    // Type 2x + 3 = 11 using CalcKey.EQ
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.VAR_X)
    vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.EQ)
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.EXE)
    assertEquals(EquationState.SubType.SOLVER_GUESS, vm.uiState.value.equationState.subType)

    // Enter x0 = 1, OK -> [Execute], OK -> Solve -> x = 4.0
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.OK)
    assertEquals(1, vm.uiState.value.equationState.activeRow)
    vm.onKey(CalcKey.OK)
    assertEquals(EquationState.SubType.SOLVER_RESULT, vm.uiState.value.equationState.subType)
    val res = vm.uiState.value.equationState.solverResult!!
    assertEquals(4.0, res.first, 1e-8)
    assertEquals(0.0, res.second, 1e-8)
    val storedX = (vm.uiState.value.variables["x"] as EvalResult.RealResult).value.toDouble()
    assertEquals(4.0, storedX, 1e-8)

    // AC starts new equation, solve x^2 = 9 from x0 = 1 -> x = 3.0
    vm.onKey(CalcKey.AC)
    assertEquals(EquationState.SubType.SOLVER_EQ, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.AC)
    vm.onKey(CalcKey.VAR_X)
    vm.onKey(CalcKey.SQUARE)
    vm.onKey(CalcKey.EQ)
    vm.onKey(CalcKey.D9)
    vm.onKey(CalcKey.EXE)
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.EXE)
    val res2 = vm.uiState.value.equationState.solverResult!!
    assertEquals(3.0, res2.first, 1e-8)

    // BACK -> SOLVER_GUESS -> BACK -> SOLVER_EQ -> BACK -> MENU
    vm.onKey(CalcKey.BACK)
    assertEquals(EquationState.SubType.SOLVER_GUESS, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.BACK)
    assertEquals(EquationState.SubType.SOLVER_EQ, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.BACK)
    assertEquals(EquationState.SubType.MENU, vm.uiState.value.equationState.subType)

    // Polynomial degree 2 -> x^2 - 5x + 6 = 0 -> roots 2, 3
    vm.onKey(CalcKey.D2)
    assertEquals(EquationState.SubType.POLY_DEGREE, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.D2)
    assertEquals(EquationState.SubType.POLY_INPUT, vm.uiState.value.equationState.subType)
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.NEG); vm.onKey(CalcKey.D5); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.D6); vm.onKey(CalcKey.EXE)
    assertEquals(EquationState.SubType.POLY_RESULT, vm.uiState.value.equationState.subType)
    assertEquals(2.0, vm.uiState.value.equationState.polyRoots[0].re, 1e-8)
    assertEquals(3.0, vm.uiState.value.equationState.polyRoots[1].re, 1e-8)
  }

  @Test
  fun longMenuPageNavigationAndScrollBounds() {
    val vm = newVm()

    // FORMAT has 8 items: DOWN / PAGE_DOWN should move cursor to bottom cleanly
    vm.onKey(CalcKey.FORMAT)
    assertEquals(OverlayScreen.FORMAT_MENU, vm.uiState.value.overlay)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(4, vm.uiState.value.overlayCursor)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(7, vm.uiState.value.overlayCursor)
    vm.onKey(CalcKey.PAGE_UP)
    assertEquals(3, vm.uiState.value.overlayCursor)
    vm.onKey(CalcKey.BACK)

    // SETTINGS -> Calc Settings has 6 items (indices 0..5)
    vm.onKey(CalcKey.SETTINGS)
    vm.onKey(CalcKey.OK)
    assertEquals(1, vm.uiState.value.settingsPage)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(4, vm.uiState.value.overlayCursor)
    vm.onKey(CalcKey.DOWN)
    assertEquals(5, vm.uiState.value.overlayCursor)
    vm.onKey(CalcKey.BACK)
    vm.onKey(CalcKey.BACK)

    // STATISTICS -> 8 models (indices 0..7)
    vm.selectHomeApp(CalcMode.STATISTICS)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(4, vm.uiState.value.statsState.activeRow)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(7, vm.uiState.value.statsState.activeRow)

    // DISTRIBUTION -> 7 types (indices 0..6)
    vm.selectHomeApp(CalcMode.DISTRIBUTION)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(4, vm.uiState.value.distributionState.selectedParamIdx)
    vm.onKey(CalcKey.PAGE_DOWN)
    assertEquals(6, vm.uiState.value.distributionState.selectedParamIdx)
  }

  @Test
  fun settingsFixDigitsPickerAndManualSimplifyTools() {
    val vm = newVm()

    // SETTINGS -> Calc Settings (page 1, 8 items: 0..7) -> Number Format (idx 2) -> FIX (idx 0) -> Page 5 -> D4 -> fixDigits=4
    vm.onKey(CalcKey.SETTINGS)
    vm.onKey(CalcKey.OK)
    assertEquals(1, vm.uiState.value.settingsPage)
    vm.onKey(CalcKey.DOWN)
    vm.onKey(CalcKey.DOWN)
    vm.onKey(CalcKey.OK)
    assertEquals(4, vm.uiState.value.settingsPage)
    assertEquals("NUMFMT", vm.uiState.value.settingsDetailKey)
    vm.selectOverlayItem(NumberFormatMode.FIX.ordinal) // FIX (index 2)
    assertEquals(5, vm.uiState.value.settingsPage)
    vm.onKey(CalcKey.D4)
    assertEquals(1, vm.uiState.value.settingsPage)
    assertEquals(NumberFormatMode.FIX, vm.uiState.value.settings.numberFormat)
    assertEquals(4, vm.uiState.value.settings.fixDigits)
    vm.onKey(CalcKey.BACK)
    vm.onKey(CalcKey.BACK)

    // Enable Manual Simplify via TOOLS (index 1 in Calculate TOOLS when no UnreducedFraction is active)
    vm.onKey(CalcKey.TOOLS)
    vm.selectOverlayItem(1) // Simplificar: Auto -> Manual
    assertEquals(SimplifyMode.MANUAL, vm.uiState.value.settings.simplifyMode)

    // Evaluate 12 ÷ 18 -> UnreducedFraction(12, 18), then TOOLS -> Simplificar (idx 0) -> 6/9 -> 2/3
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.DIV)
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D8)
    vm.onKey(CalcKey.EXE)
    val unred = vm.uiState.value.currentResult as EvalResult.UnreducedFraction
    assertEquals(12L, unred.num.toLong())
    assertEquals(18L, unred.den.toLong())

    vm.onKey(CalcKey.TOOLS)
    vm.selectOverlayItem(0) // tool:simplify_step
    val step1 = vm.uiState.value.currentResult as EvalResult.UnreducedFraction
    assertEquals(6L, step1.num.toLong())
    assertEquals(9L, step1.den.toLong())
  }

  @Test
  fun tableDirectXEditEquationVertexAndStatCalc() {
    val vm = newVm()

    // TABLE: f(x) = 2x, toggle Tipo tabla to hide g(x), generate, edit row 0 x -> 10 -> f(x)=20
    vm.selectHomeApp(CalcMode.TABLE)
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.VAR_X)
    vm.onKey(CalcKey.TOOLS)
    vm.selectOverlayItem(3) // Tipo tabla -> showG = false
    assertFalse(vm.uiState.value.tableState.showG)
    vm.onKey(CalcKey.EXE) // RANGE_SETUP
    vm.onKey(CalcKey.EXE) // RESULT_TABLE
    assertEquals(TableState.Phase.RESULT_TABLE, vm.uiState.value.tableState.phase)
    assertEquals(2.0, vm.uiState.value.tableState.rows[0].fx!!, 1e-9)

    // Direct x-value override in row 0: type 10 and press OK
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.D0)
    vm.onKey(CalcKey.OK)
    assertEquals(10.0, vm.uiState.value.tableState.rows[0].x, 1e-9)
    assertEquals(20.0, vm.uiState.value.tableState.rows[0].fx!!, 1e-9)

    // EQUATION: quadratic x^2 - 4x + 3 = 0 has polyVertex min(2, -1)
    vm.selectHomeApp(CalcMode.EQUATION)
    vm.onKey(CalcKey.D2) // Poly
    vm.onKey(CalcKey.D2) // Degree 2
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.NEG); vm.onKey(CalcKey.D4); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.D3); vm.onKey(CalcKey.EXE)
    val vertex = vm.uiState.value.equationState.polyVertex!!
    assertTrue(vertex.isMinimum)
    assertEquals(2.0, vertex.x, 1e-8)
    assertEquals(-1.0, vertex.y, 1e-8)

    // STATISTICS: 1-Var data [2, 4, 6], open STAT_CALC via TOOLS -> Cálc. Est., evaluate x̄ * 2 = 8
    vm.selectHomeApp(CalcMode.STATISTICS)
    vm.onKey(CalcKey.D1) // 1-Variable
    vm.onKey(CalcKey.AC) // Clear pre-populated sample data
    vm.onKey(CalcKey.D2); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.D4); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.D6); vm.onKey(CalcKey.OK)
    vm.onKey(CalcKey.TOOLS)
    vm.selectOverlayItem(0) // tool:stat_calc
    assertEquals(StatsAppState.Phase.STAT_CALC, vm.uiState.value.statsState.phase)
    vm.onKey(CalcKey.CATALOG)
    vm.selectOverlayItem(0) // Variables Est.
    vm.selectOverlayItem(0) // x̄
    vm.onKey(CalcKey.MUL)
    vm.onKey(CalcKey.D2)
    vm.onKey(CalcKey.EXE)
    val statCalcRes = vm.uiState.value.statsState.calcResult as EvalResult.RealResult
    assertEquals(8.0, statCalcRes.value.toDouble(), 1e-9)
  }

  @Test
  fun spreadsheet45RowsTapRefAndMathBoxNumberLineCircle() {
    val vm = newVm()

    // SPREADSHEET: A1=5, tap B1, enter formula =A1*2 via tap-to-reference, copy & paste to B2
    vm.selectHomeApp(CalcMode.SPREADSHEET)
    vm.onKey(CalcKey.D5); vm.onKey(CalcKey.OK) // A1 = 5
    vm.selectSpreadsheetCell(1, 0) // B1
    vm.onKey(CalcKey.EQ) // starts editing "="
    vm.selectSpreadsheetCell(0, 0) // taps A1 -> "=A1"
    vm.onKey(CalcKey.MUL); vm.onKey(CalcKey.D2); vm.onKey(CalcKey.OK)
    assertEquals("=A1*2", vm.uiState.value.spreadsheetState.cells["B1"])
    assertEquals(10.0, vm.uiState.value.spreadsheetState.evaluated["B1"]!!, 1e-9)

    // Can navigate down to row 45 (index 44)
    vm.selectSpreadsheetCell(4, 44)
    assertEquals("E45", vm.uiState.value.spreadsheetState.cellName())

    // MATH BOX: Number Line (D3) + Circle (D4)
    vm.selectHomeApp(CalcMode.MATH_BOX)
    vm.onKey(CalcKey.D3) // NUMBER_LINE -> PARAMS
    assertEquals(MathBoxType.NUMBER_LINE, vm.uiState.value.mathBoxState.type)
    assertEquals(MathBoxState.Phase.PARAMS, vm.uiState.value.mathBoxState.phase)
    vm.onKey(CalcKey.EXE) // RESULTS
    assertEquals(MathBoxState.Phase.RESULTS, vm.uiState.value.mathBoxState.phase)
    vm.onKey(CalcKey.RIGHT) // Pan right
    assertTrue(vm.uiState.value.mathBoxState.nlPanOffset > 0.0)

    vm.onKey(CalcKey.BACK)
    vm.onKey(CalcKey.BACK)
    vm.onKey(CalcKey.D4) // CIRCLE -> jumps directly to RESULTS
    assertEquals(MathBoxType.CIRCLE, vm.uiState.value.mathBoxState.type)
    assertEquals(MathBoxState.Phase.RESULTS, vm.uiState.value.mathBoxState.phase)
    val startDeg = vm.uiState.value.mathBoxState.circleAngleDeg
    vm.onKey(CalcKey.LEFT) // +15 deg
    assertEquals(startDeg + 15, vm.uiState.value.mathBoxState.circleAngleDeg)
    vm.onKey(CalcKey.DOWN) // UNIT -> HALF
    assertEquals(CircleModeType.HALF, vm.uiState.value.mathBoxState.circleMode)
  }

  @Test
  fun authenticPhysicalCwKeypadShiftBindings() {
    val vm = newVm()

    // 1. SHIFT + SQUARE -> log(100) = 2
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.SQUARE)
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D0); vm.onKey(CalcKey.D0)
    vm.onKey(CalcKey.PAREN_CLOSE)
    vm.onKey(CalcKey.EXE)
    assertEquals(2.0, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)

    // 2. SHIFT + LOG_BASE -> ln(e) = 1
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.LOG_BASE)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.D8) // e
    vm.onKey(CalcKey.PAREN_CLOSE)
    vm.onKey(CalcKey.EXE)
    assertEquals(1.0, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)

    // 3. SHIFT + SQRT -> ˣ√□: ³√27 = 3
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.SQRT)
    vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.RIGHT)
    vm.onKey(CalcKey.D2); vm.onKey(CalcKey.D7)
    vm.onKey(CalcKey.EXE)
    assertEquals(3.0, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)

    // 4. SHIFT + ÷ (└ / ÷R) -> 17 ÷R 5 = 3, R=2
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D7)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.DIV)
    vm.onKey(CalcKey.D5)
    vm.onKey(CalcKey.EXE)
    val remRes = vm.uiState.value.currentResult as EvalResult.RemainderResult
    assertEquals(3L, remRes.quotient)
    assertEquals(2L, remRes.remainder)

    // 5. SHIFT + + (° ' ") -> 1°30' = 1.5
    vm.onKey(CalcKey.D1)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.D3); vm.onKey(CalcKey.D0)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.EXE)
    assertEquals(1.5, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)

    // 6. SHIFT + − ((−)) -> (−)5 + 8 = 3
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.SUB)
    vm.onKey(CalcKey.D5); vm.onKey(CalcKey.ADD); vm.onKey(CalcKey.D8)
    vm.onKey(CalcKey.EXE)
    assertEquals(3.0, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)

    // 7. Store 10 in A, then SHIFT + 4 (A) + SHIFT + 0 (x=0) = 10
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D0); vm.onKey(CalcKey.EXE)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.VARIABLE) // STO
    vm.selectOverlayItem(0) // A = 10
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.D4) // A
    vm.onKey(CalcKey.ADD)
    vm.onKey(CalcKey.D5)
    vm.onKey(CalcKey.EXE)
    assertEquals(15.0, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)

    // 8. SHIFT + DEL (INS) toggles overwrite mode cleanly
    vm.onKey(CalcKey.AC)
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D2); vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.LEFT); vm.onKey(CalcKey.LEFT) // cursor before '2'
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.DEL) // enable INS overwrite
    assertTrue(vm.uiState.value.insertOverwriteMode)
    vm.onKey(CalcKey.D9) // overwrites '2' -> 193
    vm.onKey(CalcKey.EXE)
    assertEquals(193.0, (vm.uiState.value.currentResult as EvalResult.RealResult).value.toDouble(), 1e-9)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.DEL) // restore insert mode
    assertFalse(vm.uiState.value.insertOverwriteMode)

    // 9. SHIFT + ( -> '=' in Verify: 2 + 3 = 5 -> TRUE
    vm.onKey(CalcKey.AC)
    vm.onKey(CalcKey.D2); vm.onKey(CalcKey.ADD); vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.PAREN_OPEN)
    vm.onKey(CalcKey.D5)
    vm.onKey(CalcKey.EXE)
    assertTrue((vm.uiState.value.currentResult as EvalResult.VerifyResult).isTrue)

    // 10. SHIFT + ( -> '=' in Equation Solver: 2x + 3 = 11 -> x = 4
    vm.selectHomeApp(CalcMode.EQUATION)
    vm.onKey(CalcKey.D3) // Solver
    vm.onKey(CalcKey.D2); vm.onKey(CalcKey.VAR_X); vm.onKey(CalcKey.ADD); vm.onKey(CalcKey.D3)
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.PAREN_OPEN)
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.D1); vm.onKey(CalcKey.EXE)
    vm.onKey(CalcKey.D1); vm.onKey(CalcKey.EXE)
    assertEquals(4.0, vm.uiState.value.equationState.solverResult!!.first, 1e-8)

    // 11. Spreadsheet: SHIFT + ( starts "=", SHIFT + 4 types "A" -> =A1*3 = 21
    vm.selectHomeApp(CalcMode.SPREADSHEET)
    vm.onKey(CalcKey.D7); vm.onKey(CalcKey.OK) // A1 = 7
    vm.selectSpreadsheetCell(1, 0) // B1
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.PAREN_OPEN) // "="
    vm.onKey(CalcKey.SHIFT); vm.onKey(CalcKey.D4) // "A"
    vm.onKey(CalcKey.D1) // "1"
    vm.onKey(CalcKey.MUL); vm.onKey(CalcKey.D3); vm.onKey(CalcKey.OK)
    assertEquals("=A1*3", vm.uiState.value.spreadsheetState.cells["B1"])
    assertEquals(21.0, vm.uiState.value.spreadsheetState.evaluated["B1"]!!, 1e-9)
  }
}

