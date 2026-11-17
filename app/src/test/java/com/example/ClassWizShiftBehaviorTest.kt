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

    // Ans is now 5: SHIFT + SQUARE (⁻¹) -> Ans⁻¹ = 0.2
    vm.onKey(CalcKey.SHIFT)
    vm.onKey(CalcKey.SQUARE)
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
}
