package com.example

import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ClassWizEngineTest {

  private fun tok(s: String, kind: TokenKind = TokenKind.DIGIT) = MathElement.Token(s, kind, s)

  @Test
  fun exactFractionsAndSurds() {
    val half = MathElement.Fraction(MathRow(listOf(tok("1"))), MathRow(listOf(tok("2"))))
    val third = MathElement.Fraction(MathRow(listOf(tok("1"))), MathRow(listOf(tok("3"))))
    val expr = MathRow(listOf(half, tok("+", TokenKind.OPERATOR), third))
    val res = ScientificEvaluator.evaluate(expr, EvalContext()) as EvalResult.RealResult
    val surd = (res.value as ExactReal.Surd).value.asRationalOrNull()!!
    assertEquals(Rational.of(5, 6), surd)

    val sqrt12 = MathRow(listOf(MathElement.Sqrt(MathRow(listOf(tok("1"), tok("2"))))))
    val r12 = ScientificEvaluator.evaluate(sqrt12, EvalContext()) as EvalResult.RealResult
    assertEquals("2√3", (r12.value as ExactReal.Surd).value.formatLatexLike())
  }

  @Test
  fun exactTrigonometryAndCalculus() {
    val sin60 = MathRow(listOf(
      tok("sin(", TokenKind.FUNC_OPEN), tok("6"), tok("0"), tok(")", TokenKind.PAREN_CLOSE)
    ))
    val res = ScientificEvaluator.evaluate(sin60, EvalContext(settings = CalcSettings(angleUnit = AngleUnit.DEG))) as EvalResult.RealResult
    assertEquals(1, (res.value as ExactReal.Surd).value.terms.size)
    assertEquals(Rational.of(1, 2), res.value.value.terms[3])

    // Integral of x^2 from 0 to 1 == 1/3
    val integrand = MathRow(listOf(
      tok("x", TokenKind.VARIABLE),
      MathElement.Superscript(MathRow(listOf(tok("2"))))
    ))
    val intRow = MathRow(listOf(
      MathElement.Integral(
        integrand = integrand,
        lower = MathRow(listOf(tok("0"))),
        upper = MathRow(listOf(tok("1")))
      )
    ))
    val intRes = ScientificEvaluator.evaluate(intRow, EvalContext()) as EvalResult.RealResult
    assertEquals(1.0 / 3.0, intRes.value.toDouble(), 1e-8)

    // Derivative of x^3 at x=2 == 12
    val derivRow = MathRow(listOf(
      MathElement.Derivative(
        expr = MathRow(listOf(tok("x", TokenKind.VARIABLE), MathElement.Superscript(MathRow(listOf(tok("3")))))),
        atValue = MathRow(listOf(tok("2")))
      )
    ))
    val derRes = ScientificEvaluator.evaluate(derivRow, EvalContext()) as EvalResult.RealResult
    assertEquals(12.0, derRes.value.toDouble(), 1e-6)
  }

  @Test
  fun complexAndMatrixAndSolvers() {
    // (2 + 3i) * (1 - i) = 5 + i
    val cRow = MathRow(listOf(
      tok("(", TokenKind.PAREN_OPEN), tok("2"), tok("+", TokenKind.OPERATOR), tok("3"), tok("i", TokenKind.CONSTANT), tok(")", TokenKind.PAREN_CLOSE),
      tok("(", TokenKind.PAREN_OPEN), tok("1"), tok("-", TokenKind.OPERATOR), tok("i", TokenKind.CONSTANT), tok(")", TokenKind.PAREN_CLOSE),
    ))
    val cRes = ScientificEvaluator.evaluate(cRow, EvalContext(mode = CalcMode.COMPLEX)) as EvalResult.ComplexResult
    assertEquals(5.0, cRes.re.toDouble(), 1e-10)
    assertEquals(1.0, cRes.im.toDouble(), 1e-10)

    // Linear system: 2x + y = 5, x - y = 1 -> x=2, y=1
    val lin = AdvancedMathSolvers.solveLinearSystem(
      listOf(listOf(2.0, 1.0), listOf(1.0, -1.0)),
      listOf(5.0, 1.0)
    ) as LinearSystemResult.Unique
    assertEquals(2.0, lin.values[0], 1e-9)
    assertEquals(1.0, lin.values[1], 1e-9)

    // Quadratic: x^2 - 5x + 6 = 0 -> roots 2, 3
    val roots = AdvancedMathSolvers.solvePolynomial(listOf(1.0, -5.0, 6.0))
    assertEquals(2, roots.size)
    assertTrue(abs(roots[0].re - 2.0) < 1e-9)
    assertTrue(abs(roots[1].re - 3.0) < 1e-9)

    // Matrix inverse & det
    val mat = listOf(listOf(4.0, 7.0), listOf(2.0, 6.0))
    assertEquals(10.0, AdvancedMathSolvers.matrixDet(mat), 1e-9)
    val inv = AdvancedMathSolvers.matrixInverse(mat)
    assertEquals(0.6, inv[0][0], 1e-9)
    assertEquals(-0.7, inv[0][1], 1e-9)

    // Normal CDF(-1, 1, 1, 0) ~ 0.68268949
    val p = AdvancedMathSolvers.normalCdfRange(-1.0, 1.0, 1.0, 0.0)
    assertEquals(0.68268949, p, 1e-6)
  }

  @Test
  fun verifyAndUserFunctionsAndQr() {
    // Verify 2 + 3 = 5 -> true
    val eqRow = MathRow(listOf(tok("2"), tok("+", TokenKind.OPERATOR), tok("3"), tok("=", TokenKind.OPERATOR), tok("5")))
    val eqRes = ScientificEvaluator.evaluate(eqRow, EvalContext()) as EvalResult.VerifyResult
    assertTrue(eqRes.isTrue)

    // Verify 2 + 3 ≠ 5 -> false
    val neqRow = MathRow(listOf(tok("2"), tok("+", TokenKind.OPERATOR), tok("3"), tok("≠", TokenKind.OPERATOR), tok("5")))
    val neqRes = ScientificEvaluator.evaluate(neqRow, EvalContext()) as EvalResult.VerifyResult
    assertFalse(neqRes.isTrue)

    // f(x) = x^2 + 1, evaluate f(3) == 10
    val fBody = MathRow(listOf(tok("x", TokenKind.VARIABLE), MathElement.Superscript(MathRow(listOf(tok("2")))), tok("+", TokenKind.OPERATOR), tok("1")))
    val callF = MathRow(listOf(tok("f(", TokenKind.FUNC_OPEN), tok("3"), tok(")", TokenKind.PAREN_CLOSE)))
    val fRes = ScientificEvaluator.evaluate(callF, EvalContext(fExpr = fBody)) as EvalResult.RealResult
    assertEquals(10.0, fRes.value.toDouble(), 1e-10)

    // QR matrix dimensions should be odd >= 21 and non-empty
    val qr = QrEncoder.encode("CASIO:991LACW:CALCULATE:2+3=5", QrVersion.V3)
    assertTrue(qr.size >= 29 && qr.size % 4 == 1)
    assertEquals(qr.size, qr.first().size)
  }

  @Test
  fun chainedVerifyManualSimplifyLinearAndVertex() {
    // Chained inequality: 3 < π < 4 -> TRUE
    val chain = MathRow(listOf(
      tok("3"), tok("<", TokenKind.OPERATOR), tok("π", TokenKind.CONSTANT),
      tok("<", TokenKind.OPERATOR), tok("4")
    ))
    val chainRes = ScientificEvaluator.evaluate(chain, EvalContext()) as EvalResult.VerifyResult
    assertTrue(chainRes.isTrue)
    assertEquals("<..<", chainRes.relationOp)

    // 5 ≥ 5 -> TRUE, 2 > 3 -> FALSE
    val geRes = ScientificEvaluator.evaluate(
      MathRow(listOf(tok("5"), tok("≥", TokenKind.OPERATOR), tok("5"))),
      EvalContext()
    ) as EvalResult.VerifyResult
    assertTrue(geRes.isTrue)

    // Manual simplification: 12/18 -> UnreducedFraction(12, 18) -> step ÷2 -> 6/9 -> step ÷3 -> 2/3
    val fracRow = MathRow(listOf(
      MathElement.Fraction(MathRow(listOf(tok("1"), tok("2"))), MathRow(listOf(tok("1"), tok("8"))))
    ))
    val manCtx = EvalContext(settings = CalcSettings(simplifyMode = SimplifyMode.MANUAL))
    val u1 = ScientificEvaluator.evaluate(fracRow, manCtx) as EvalResult.UnreducedFraction
    assertEquals(12L, u1.num.toLong())
    assertEquals(18L, u1.den.toLong())

    val u2 = ScientificEvaluator.simplifyFractionStep(u1) as EvalResult.UnreducedFraction
    assertEquals(6L, u2.num.toLong())
    assertEquals(9L, u2.den.toLong())
    assertEquals(2L, u2.lastDivisor)

    val finalRes = ScientificEvaluator.simplifyFractionStep(u2) as EvalResult.RealResult
    assertEquals(Rational.of(2, 3), (finalRes.value as ExactReal.Surd).value.asRationalOrNull())

    // Linear fraction output uses ClassWiz ⌟ symbol + digit grouping
    val linearStr = formatExactRealLinear(
      ExactReal.fromRational(Rational.of(7, 3)),
      CalcSettings(fractionFormat = FractionFormat.MIXED)
    )
    assertEquals("2⌟1⌟3", linearStr)
    assertEquals("1 234 567.89", applyDigitGrouping("1234567.89", true))

    // Quadratic vertex: x^2 - 4x + 3 -> minimum at (2, -1)
    val vertex = AdvancedMathSolvers.quadraticVertex(1.0, -4.0, 3.0)!!
    assertTrue(vertex.isMinimum)
    assertEquals(2.0, vertex.x, 1e-10)
    assertEquals(-1.0, vertex.y, 1e-10)
  }

  @Test
  fun spreadsheetAbsoluteRefsAndFillFormulaUpToRow45() {
    val shifted = AdvancedMathSolvers.shiftFormulaReferences("=\$A\$1+B2+C\$3+\$D4", dCol = 1, dRow = 2)
    assertEquals("=\$A\$1+C4+D\$3+\$D6", shifted)

    val initial = mapOf("A1" to "10", "A2" to "20", "A3" to "30", "B1" to "2")
    val filled = AdvancedMathSolvers.fillSpreadsheetFormula(
      cells = initial,
      sourceCol = 2,
      sourceRow = 0,
      formula = "=A1*\$B\$1",
      rangeStr = "C1:C3",
    )
    assertEquals("=A1*\$B\$1", filled["C1"])
    assertEquals("=A2*\$B\$1", filled["C2"])
    assertEquals("=A3*\$B\$1", filled["C3"])

    val evald = AdvancedMathSolvers.evaluateSpreadsheet(filled + ("E45" to "=SUM(C1:C3)"))
    assertEquals(20.0, evald["C1"]!!, 1e-9)
    assertEquals(40.0, evald["C2"]!!, 1e-9)
    assertEquals(60.0, evald["C3"]!!, 1e-9)
    assertEquals(120.0, evald["E45"]!!, 1e-9)
  }
}
