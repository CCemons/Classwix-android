package com.example.viewmodel

import com.example.engine.*

enum class CalcKey {
  ON, HOME, SETTINGS, BACK,
  UP, DOWN, LEFT, RIGHT, OK,
  PAGE_UP, PAGE_DOWN,
  SHIFT, VARIABLE, FX, CATALOG, TOOLS,
  VAR_X, FRAC, SQRT, SQUARE, POWER, LOG_BASE,
  LOG10, LN, SIN, COS, TAN, NEG,
  DMS, PAREN_OPEN, PAREN_CLOSE, DERIV, EQ, ANS,
  D7, D8, D9, DEL, AC,
  D4, D5, D6, MUL, DIV,
  D1, D2, D3, ADD, SUB,
  D0, DOT, EXP10, FORMAT, EXE,
}

enum class OverlayScreen {
  NONE,
  HOME_GRID,
  CATALOG_MENU,
  TOOLS_MENU,
  SETTINGS_MENU,
  FORMAT_MENU,
  VARIABLE_MENU,
  FX_MENU,
  QR_VIEW,
}

data class CatalogCategory(
  val id: String,
  val titleEs: String,
  val titleEn: String,
  val items: List<CatalogEntry>,
)

sealed class CatalogEntry {
  abstract val labelEs: String
  abstract val labelEn: String
  abstract val subtitle: String

  data class InsertAction(
    override val labelEs: String,
    override val labelEn: String,
    override val subtitle: String = "",
    val actionId: String,
  ) : CatalogEntry()

  data class SubCategory(
    override val labelEs: String,
    override val labelEn: String,
    override val subtitle: String = "",
    val children: List<CatalogEntry>,
  ) : CatalogEntry()
}

data class HistoryEntry(
  val expression: MathRow,
  val result: EvalResult,
  val mode: CalcMode,
)

data class TableState(
  val fDoc: EditorDocument = EditorDocument(),
  val gDoc: EditorDocument = EditorDocument(),
  val editingG: Boolean = false,
  val showG: Boolean = true,
  val start: Double = 1.0,
  val end: Double = 5.0,
  val step: Double = 1.0,
  val phase: Phase = Phase.EDIT_F,
  val rows: List<TableRow> = emptyList(),
  val selectedRow: Int = 0,
  val selectedCol: Int = 0,
  val editingXRow: Int? = null,
  val editingXBuffer: String = "",
) {
  enum class Phase { EDIT_F, EDIT_G, RANGE_SETUP, RESULT_TABLE }
  data class TableRow(val x: Double, val fx: Double?, val gx: Double?)
}

data class EquationState(
  val subType: SubType = SubType.MENU,
  val unknowns: Int = 2,
  val degree: Int = 2,
  val linearCoeffs: List<List<String>> = List(4) { List(5) { "0" } },
  val polyCoeffs: List<String> = List(5) { "0" },
  val activeRow: Int = 0,
  val activeCol: Int = 0,
  val inputBuffer: String = "",
  val linearResult: LinearSystemResult? = null,
  val polyRoots: List<ComplexNum> = emptyList(),
  val polyVertex: PolyVertex? = null,
  val solverDoc: EditorDocument = EditorDocument(),
  val solverInitialX: Double = 0.0,
  val solverResult: Pair<Double, Double>? = null,
  val solverError: String? = null,
) {
  enum class SubType {
    MENU,
    SIMUL_DIM,
    POLY_DEGREE,
    LINEAR_INPUT,
    LINEAR_RESULT,
    POLY_INPUT,
    POLY_RESULT,
    SOLVER_EQ,
    SOLVER_GUESS,
    SOLVER_RESULT,
  }
}

data class InequalityState(
  val phase: Phase = Phase.SELECT_DEGREE,
  val degree: Int = 2,
  val op: InequalityOp = InequalityOp.GT,
  val coeffs: List<String> = List(5) { "0" },
  val activeCol: Int = 0,
  val inputBuffer: String = "",
  val solutionText: String = "",
) {
  enum class Phase { SELECT_DEGREE, SELECT_OP, COEFF_INPUT, RESULT }
}

data class MatrixAppState(
  val phase: Phase = Phase.WORK,
  val editingMatrixName: String = "MatA",
  val rows: Int = 2,
  val cols: Int = 2,
  val editRow: Int = 0,
  val editCol: Int = 0,
  val inputBuffer: String = "",
  val matrices: Map<String, EvalResult.MatrixResult> = emptyMap(),
) {
  enum class Phase { WORK, PICK_MATRIX, PICK_DIM, EDIT_GRID }
}

data class VectorAppState(
  val phase: Phase = Phase.WORK,
  val editingVectorName: String = "VctA",
  val dim: Int = 3,
  val editIdx: Int = 0,
  val inputBuffer: String = "",
  val vectors: Map<String, EvalResult.VectorResult> = emptyMap(),
) {
  enum class Phase { WORK, PICK_VECTOR, PICK_DIM, EDIT_GRID }
}

data class StatsAppState(
  val phase: Phase = Phase.SELECT_MODEL,
  val model: StatModelType = StatModelType.ONE_VAR,
  val xValues: List<Double> = listOf(1.0, 2.0, 3.0, 4.0, 5.0),
  val yValues: List<Double> = listOf(2.0, 4.0, 5.0, 4.0, 5.0),
  val freqValues: List<Double> = listOf(1.0, 1.0, 1.0, 1.0, 1.0),
  val activeRow: Int = 0,
  val activeCol: Int = 0,
  val inputBuffer: String = "",
  val summary: StatSummary = StatSummary(),
  val calcDoc: EditorDocument = EditorDocument(),
  val calcResult: EvalResult? = null,
) {
  enum class Phase { SELECT_MODEL, DATA_GRID, RESULTS, STAT_CALC }
}

enum class DistType(val labelEs: String, val labelEn: String) {
  NORMAL_PD("Normal PD", "Normal PD"),
  NORMAL_CD("Normal CD", "Normal CD"),
  INV_NORM("Normal inversa", "Inverse Normal"),
  BINOMIAL_PD("Binomial PD", "Binomial PD"),
  BINOMIAL_CD("Binomial CD", "Binomial CD"),
  POISSON_PD("Poisson PD", "Poisson PD"),
  POISSON_CD("Poisson CD", "Poisson CD"),
}

data class DistributionState(
  val phase: Phase = Phase.SELECT_TYPE,
  val type: DistType = DistType.NORMAL_CD,
  val params: Map<String, String> = mapOf(
    "x" to "0", "Lower" to "-1", "Upper" to "1", "σ" to "1", "μ" to "0",
    "Area" to "0.95", "N" to "10", "p" to "0.5", "λ" to "3"
  ),
  val selectedParamIdx: Int = 0,
  val inputBuffer: String = "",
  val resultValue: Double? = null,
) {
  enum class Phase { SELECT_TYPE, PARAM_INPUT, RESULT }
}

data class SpreadsheetState(
  val cells: Map<String, String> = emptyMap(),
  val evaluated: Map<String, Double?> = emptyMap(),
  val activeCol: Int = 0, // 0..4 -> A..E
  val activeRow: Int = 0, // 0..44 -> 1..45
  val editing: Boolean = false,
  val editBuffer: String = "",
  val clipboardRaw: String? = null,
  val clipboardSourceCol: Int = 0,
  val clipboardSourceRow: Int = 0,
  val clipboardIsCut: Boolean = false,
  val subPhase: SubPhase = SubPhase.GRID,
  val fillSourceRaw: String = "",
  val fillRange: String = "A1:A5",
  val fillField: Int = 0,
  val fillBuffer: String = "",
) {
  enum class SubPhase { GRID, FILL_FORMULA, FILL_VALUE }
  fun cellName(col: Int = activeCol, row: Int = activeRow): String = "${'A' + col}${row + 1}"
}

data class RatioState(
  val phase: Phase = Phase.SELECT_TYPE,
  val xOnLeft: Boolean = true, // true: A:B = X:D, false: A:B = C:X
  val a: String = "2",
  val b: String = "3",
  val cOrD: String = "12",
  val activeField: Int = 0,
  val inputBuffer: String = "",
  val solution: ExactReal? = null,
) {
  enum class Phase { SELECT_TYPE, INPUT, RESULT }
}

enum class MathBoxType(val labelEs: String, val labelEn: String) {
  DICE("Lanzam. dado", "Dice Roll"),
  COIN("Lanzam. moneda", "Coin Toss"),
  NUMBER_LINE("Recta numérica", "Number Line"),
  CIRCLE("Círculo", "Circle"),
}

enum class NumberLineOp(val symbol: String) {
  LT("<"),
  LE("≤"),
  GT(">"),
  GE("≥"),
  EQ("="),
}

data class NumberLineEntry(
  val label: String,
  val enabled: Boolean,
  val op: NumberLineOp,
  val value: Double,
)

enum class CircleModeType(val labelEs: String, val labelEn: String) {
  UNIT("Círculo unitario", "Unit Circle"),
  HALF("Semicírculo", "Half Circle"),
  CLOCK("Reloj", "Clock"),
}

data class MathBoxState(
  val phase: Phase = Phase.SELECT_TYPE,
  val type: MathBoxType = MathBoxType.DICE,
  val objectCount: Int = 1, // 1..3
  val trials: Int = 20,     // 1..250
  val activeParam: Int = 0,
  val inputBuffer: String = "",
  val counts: Map<Int, Int> = emptyMap(),
  val diceShowList: Boolean = false,
  val rollSamples: List<List<Int>> = emptyList(),
  val nlEntries: List<NumberLineEntry> = listOf(
    NumberLineEntry("A", true, NumberLineOp.LE, -1.5),
    NumberLineEntry("B", true, NumberLineOp.GT, 1.0),
    NumberLineEntry("C", false, NumberLineOp.EQ, 0.0),
  ),
  val nlActiveSlot: Int = 0,
  val nlEditingValue: Boolean = false,
  val nlPanOffset: Double = 0.0,
  val nlZoom: Double = 1.0,
  val circleMode: CircleModeType = CircleModeType.UNIT,
  val circleAngleDeg: Int = 30,
  val circleStepDeg: Int = 15,
) {
  enum class Phase { SELECT_TYPE, PARAMS, RESULTS }
}

data class CalculatorUiState(
  val isPoweredOn: Boolean = true,
  val isShiftActive: Boolean = false,
  val insertOverwriteMode: Boolean = false,
  val storePending: Boolean = false,
  val verifyModeEnabled: Boolean = false,
  val mode: CalcMode = CalcMode.CALCULATE,
  val overlay: OverlayScreen = OverlayScreen.NONE,
  val overlayCursor: Int = 0,
  val overlayStack: List<Pair<String, List<CatalogEntry>>> = emptyList(),
  val settingsPage: Int = 0, // 0=root, 1=Calc Settings, 2=System Settings, 3=Reset, 4=Sub-option, 5=Fix/Sci digits
  val settingsDetailKey: String = "",
  val settings: CalcSettings = CalcSettings(),
  val doc: EditorDocument = EditorDocument(),
  val undoStack: List<EditorDocument> = emptyList(),
  val fxUndoStack: List<EditorDocument> = emptyList(),
  val solverUndoStack: List<EditorDocument> = emptyList(),
  val statCalcUndoStack: List<EditorDocument> = emptyList(),
  val currentResult: EvalResult? = null,
  val resultFormatOverride: ResultFormatMode? = null,
  val history: List<HistoryEntry> = emptyList(),
  val historyBrowseIdx: Int = -1,
  val variables: Map<String, EvalResult> = mapOf(
    "A" to EvalResult.RealResult(ExactReal.ZERO),
    "B" to EvalResult.RealResult(ExactReal.ZERO),
    "C" to EvalResult.RealResult(ExactReal.ZERO),
    "D" to EvalResult.RealResult(ExactReal.ZERO),
    "E" to EvalResult.RealResult(ExactReal.ZERO),
    "F" to EvalResult.RealResult(ExactReal.ZERO),
    "x" to EvalResult.RealResult(ExactReal.ZERO),
    "y" to EvalResult.RealResult(ExactReal.ZERO),
    "z" to EvalResult.RealResult(ExactReal.ZERO),
  ),
  val ans: EvalResult? = null,
  val preAns: EvalResult? = null,
  val baseNMode: BaseNMode = BaseNMode.DEC,
  val tableState: TableState = TableState(),
  val equationState: EquationState = EquationState(),
  val inequalityState: InequalityState = InequalityState(),
  val matrixState: MatrixAppState = MatrixAppState(),
  val vectorState: VectorAppState = VectorAppState(),
  val statsState: StatsAppState = StatsAppState(),
  val distributionState: DistributionState = DistributionState(),
  val spreadsheetState: SpreadsheetState = SpreadsheetState(),
  val ratioState: RatioState = RatioState(),
  val editingFxTarget: Int? = null, // 0 = f(x), 1 = g(x)
  val qrTitle: String = "",
  val qrSubtitle: String = "",
  val qrMatrix: List<List<Boolean>> = emptyList(),
  val mathBoxState: MathBoxState = MathBoxState(),
  val toastBanner: String? = null,
)
