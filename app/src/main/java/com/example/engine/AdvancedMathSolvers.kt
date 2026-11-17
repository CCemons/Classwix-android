package com.example.engine

import kotlin.math.*

data class ComplexNum(val re: Double, val im: Double) {
  operator fun plus(o: ComplexNum) = ComplexNum(re + o.re, im + o.im)
  operator fun minus(o: ComplexNum) = ComplexNum(re - o.re, im - o.im)
  operator fun times(o: ComplexNum) = ComplexNum(re * o.re - im * o.im, re * o.im + im * o.re)
  operator fun div(o: ComplexNum): ComplexNum {
    val d = o.re * o.re + o.im * o.im
    if (d == 0.0) return ComplexNum(Double.NaN, Double.NaN)
    return ComplexNum((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d)
  }
  fun absVal(): Double = hypot(re, im)
}

enum class StatModelType(val labelEs: String, val labelEn: String, val formula: String) {
  ONE_VAR("1-Variable", "1-Variable", "x"),
  LINEAR("Lin. (y=a+bx)", "Lin. (y=a+bx)", "y=a+bx"),
  QUADRATIC("Cuad. (y=a+bx+cx²)", "Quad. (y=a+bx+cx²)", "y=a+bx+cx²"),
  LOGARITHMIC("Log. (y=a+bln(x))", "Log. (y=a+bln(x))", "y=a+b·ln(x)"),
  EXP_E("Exp e (y=ae^(bx))", "e Exp (y=ae^(bx))", "y=a·e^(bx)"),
  EXP_AB("Exp ab (y=ab^x)", "ab Exp (y=ab^x)", "y=a·b^x"),
  POWER("Pot. (y=ax^b)", "Power (y=ax^b)", "y=a·x^b"),
  INVERSE("Inv. (y=a+b/x)", "Inv. (y=a+b/x)", "y=a+b/x"),
}

enum class InequalityOp(val symbol: String) {
  GT(">"),
  LT("<"),
  GE("≥"),
  LE("≤"),
}

sealed class LinearSystemResult {
  data class Unique(val values: List<Double>) : LinearSystemResult()
  object NoSolution : LinearSystemResult()
  object InfiniteSolutions : LinearSystemResult()
}

object AdvancedMathSolvers {

  // --- Matrix Operations ---

  fun matrixDet(m: List<List<Double>>): Double {
    val n = m.size
    if (n == 0 || m.any { it.size != n }) throw CalcDimensionException()
    val a = Array(n) { r -> m[r].toDoubleArray() }
    var det = 1.0
    for (i in 0 until n) {
      var pivot = i
      for (r in i + 1 until n) {
        if (abs(a[r][i]) > abs(a[pivot][i])) pivot = r
      }
      if (abs(a[pivot][i]) < 1e-13) return 0.0
      if (pivot != i) {
        val tmp = a[i]
        a[i] = a[pivot]
        a[pivot] = tmp
        det = -det
      }
      det *= a[i][i]
      for (r in i + 1 until n) {
        val factor = a[r][i] / a[i][i]
        for (c in i until n) {
          a[r][c] -= factor * a[i][c]
        }
      }
    }
    return det
  }

  fun matrixInverse(m: List<List<Double>>): List<List<Double>> {
    val n = m.size
    if (n == 0 || m.any { it.size != n }) throw CalcDimensionException()
    val aug = Array(n) { r ->
      DoubleArray(2 * n) { c ->
        if (c < n) m[r][c] else if (c - n == r) 1.0 else 0.0
      }
    }
    for (i in 0 until n) {
      var pivot = i
      for (r in i + 1 until n) {
        if (abs(aug[r][i]) > abs(aug[pivot][i])) pivot = r
      }
      if (abs(aug[pivot][i]) < 1e-13) throw CalcMathException("Matriz singular")
      if (pivot != i) {
        val tmp = aug[i]
        aug[i] = aug[pivot]
        aug[pivot] = tmp
      }
      val div = aug[i][i]
      for (c in 0 until 2 * n) aug[i][c] /= div
      for (r in 0 until n) {
        if (r != i) {
          val factor = aug[r][i]
          for (c in 0 until 2 * n) {
            aug[r][c] -= factor * aug[i][c]
          }
        }
      }
    }
    return List(n) { r -> List(n) { c -> aug[r][n + c] } }
  }

  fun matrixTranspose(m: List<List<Double>>): List<List<Double>> {
    val rows = m.size
    val cols = m.firstOrNull()?.size ?: 0
    return List(cols) { c -> List(rows) { r -> m[r][c] } }
  }

  // --- Linear System Solver (2..4 unknowns) ---

  fun solveLinearSystem(coeffs: List<List<Double>>, rhs: List<Double>): LinearSystemResult {
    val n = coeffs.size
    val m = Array(n) { r ->
      DoubleArray(n + 1) { c -> if (c < n) coeffs[r][c] else rhs[r] }
    }
    var rank = 0
    val pivotCols = IntArray(n) { -1 }
    for (col in 0 until n) {
      var pivot = rank
      for (r in rank + 1 until n) {
        if (abs(m[r][col]) > abs(m[pivot][col])) pivot = r
      }
      if (pivot >= n || abs(m[pivot][col]) < 1e-11) continue
      val tmp = m[rank]
      m[rank] = m[pivot]
      m[pivot] = tmp
      val div = m[rank][col]
      for (c in col..n) m[rank][c] /= div
      for (r in 0 until n) {
        if (r != rank && abs(m[r][col]) > 1e-12) {
          val factor = m[r][col]
          for (c in col..n) m[r][c] -= factor * m[rank][c]
        }
      }
      pivotCols[rank] = col
      rank++
    }
    for (r in rank until n) {
      if (abs(m[r][n]) > 1e-9) return LinearSystemResult.NoSolution
    }
    if (rank < n) return LinearSystemResult.InfiniteSolutions
    val sol = DoubleArray(n)
    for (r in 0 until n) sol[pivotCols[r]] = m[r][n]
    return LinearSystemResult.Unique(sol.toList())
  }

  // --- Polynomial Roots (Degree 2, 3, 4) ---

  fun solvePolynomial(coeffsHighToLow: List<Double>): List<ComplexNum> {
    // Strip leading zeros
    var firstNonZero = coeffsHighToLow.indexOfFirst { abs(it) > 1e-14 }
    if (firstNonZero == -1) return emptyList()
    val c = coeffsHighToLow.drop(firstNonZero)
    val deg = c.size - 1
    if (deg <= 0) return emptyList()
    if (deg == 1) return listOf(ComplexNum(-c[1] / c[0], 0.0))
    if (deg == 2) {
      val a = c[0]
      val b = c[1]
      val d = c[2]
      val disc = b * b - 4.0 * a * d
      return if (disc >= 0.0) {
        val sq = sqrt(disc)
        listOf(ComplexNum((-b + sq) / (2 * a), 0.0), ComplexNum((-b - sq) / (2 * a), 0.0))
          .sortedBy { it.re }
      } else {
        val sq = sqrt(-disc)
        listOf(ComplexNum(-b / (2 * a), sq / (2 * a)), ComplexNum(-b / (2 * a), -sq / (2 * a)))
      }
    }

    // Normalize monic polynomial and apply Durand-Kerner
    val lead = c[0]
    val norm = c.map { it / lead }
    val roots = Array(deg) { i ->
      val angle = 2.0 * PI * i / deg + 0.37
      val radius = 1.0 + abs(norm.last()).pow(1.0 / deg) * 0.5
      ComplexNum(radius * cos(angle), radius * sin(angle))
    }

    fun evalPoly(z: ComplexNum): ComplexNum {
      var acc = ComplexNum(1.0, 0.0)
      for (i in 1..deg) {
        acc = acc * z + ComplexNum(norm[i], 0.0)
      }
      return acc
    }

    repeat(120) {
      var maxDelta = 0.0
      for (i in 0 until deg) {
        var denom = ComplexNum(1.0, 0.0)
        for (j in 0 until deg) {
          if (i != j) denom *= (roots[i] - roots[j])
        }
        val delta = evalPoly(roots[i]) / denom
        roots[i] = roots[i] - delta
        maxDelta = max(maxDelta, delta.absVal())
      }
      if (maxDelta < 1e-13) return@repeat
    }

    return roots.map { r ->
      if (abs(r.im) < 1e-9) ComplexNum(r.re, 0.0) else r
    }.sortedWith(compareBy({ abs(it.im) > 1e-9 }, { it.re }, { -it.im }))
  }

  // --- Numerical Equation Solver (f(x) = g(x)) ---

  fun solveNumerical(lhs: MathRow, rhs: MathRow, initialX: Double, ctx: EvalContext): Pair<Double, Double>? {
    fun f(x: Double): Double {
      val sub = ctx.copy(variables = ctx.variables + ("x" to EvalResult.RealResult(ExactReal.fromDouble(x))))
      return ScientificEvaluator.evaluateReal(lhs, sub) - ScientificEvaluator.evaluateReal(rhs, sub)
    }

    var x = initialX
    repeat(80) {
      val fx = try {
        f(x)
      } catch (_: Exception) {
        x += 0.37
        return@repeat
      }
      if (fx.isNaN() || fx.isInfinite()) {
        x += 0.37
        return@repeat
      }
      if (abs(fx) < 1e-11) return x to fx
      val h = max(1e-6, abs(x) * 1e-6)
      val fp = try {
        (f(x + h) - f(x - h)) / (2.0 * h)
      } catch (_: Exception) {
        x += 0.19
        return@repeat
      }
      if (abs(fp) < 1e-14 || fp.isNaN() || fp.isInfinite()) {
        x += 0.1
      } else {
        val next = x - fx / fp
        if (abs(next - x) < 1e-12) {
          val finalFx = runCatching { f(next) }.getOrNull() ?: fx
          return next to finalFx
        }
        x = next.coerceIn(-1e12, 1e12)
      }
    }
    return try {
      val residual = f(x)
      if (!residual.isNaN() && abs(residual) < 1e-4) x to residual else null
    } catch (_: Exception) {
      null
    }
  }

  // --- Polynomial Inequality Solver ---

  fun solvePolynomialInequality(
    coeffsHighToLow: List<Double>,
    op: InequalityOp,
    settings: CalcSettings,
  ): String {
    val lead = coeffsHighToLow.firstOrNull { abs(it) > 1e-14 } ?: return settings.tr("Sin solución", "No solution")
    val roots = solvePolynomial(coeffsHighToLow)
      .filter { abs(it.im) < 1e-8 }
      .map { it.re }
      .sorted()

    // Deduplicate near-identical roots
    val uniqueRoots = mutableListOf<Double>()
    for (r in roots) {
      if (uniqueRoots.isEmpty() || abs(r - uniqueRoots.last()) > 1e-7) uniqueRoots.add(r)
    }

    fun evalP(x: Double): Double {
      var acc = 0.0
      for (c in coeffsHighToLow) acc = acc * x + c
      return acc
    }

    fun satisfies(v: Double): Boolean = when (op) {
      InequalityOp.GT -> v > 1e-10
      InequalityOp.LT -> v < -1e-10
      InequalityOp.GE -> v >= -1e-10
      InequalityOp.LE -> v <= 1e-10
    }

    val inclusive = op == InequalityOp.GE || op == InequalityOp.LE
    val points = mutableListOf<Double>()
    if (uniqueRoots.isEmpty()) {
      val holds = satisfies(evalP(0.0))
      return if (holds) settings.tr("Todos los reales", "All Real Numbers") else settings.tr("Sin solución", "No solution")
    }

    // Build intervals (-inf, r0), (r0, r1), ..., (rn-1, +inf)
    val intervals = mutableListOf<String>()
    val testPoints = mutableListOf<Double>()
    testPoints.add(uniqueRoots.first() - 1.0)
    for (i in 0 until uniqueRoots.size - 1) {
      testPoints.add(0.5 * (uniqueRoots[i] + uniqueRoots[i + 1]))
    }
    testPoints.add(uniqueRoots.last() + 1.0)

    val intervalHolds = testPoints.map { satisfies(evalP(it)) }
    val rootHolds = uniqueRoots.map { inclusive }

    // Merge adjacent intervals if root holds
    var i = 0
    while (i < intervalHolds.size) {
      if (!intervalHolds[i]) {
        if (i < uniqueRoots.size && rootHolds[i] && (i == 0 || !intervalHolds[i])) {
          // Check isolated root
          val prevFalse = i == 0 || !intervalHolds[i]
          val nextFalse = i + 1 >= intervalHolds.size || !intervalHolds[i + 1]
          if (prevFalse && nextFalse) {
            intervals.add("x=${formatDoubleClassWiz(uniqueRoots[i], settings)}")
          }
        }
        i++
        continue
      }
      val startRootIdx = i - 1
      var endIdx = i
      while (endIdx + 1 < intervalHolds.size && intervalHolds[endIdx + 1] && rootHolds[endIdx]) {
        endIdx++
      }
      val endRootIdx = endIdx
      val part = when {
        startRootIdx < 0 && endRootIdx >= uniqueRoots.size -> return settings.tr("Todos los reales", "All Real Numbers")
        startRootIdx < 0 -> {
          val sym = if (inclusive) "≤" else "<"
          "x $sym ${formatDoubleClassWiz(uniqueRoots[endRootIdx], settings)}"
        }
        endRootIdx >= uniqueRoots.size -> {
          val sym = if (inclusive) "≥" else ">"
          "x $sym ${formatDoubleClassWiz(uniqueRoots[startRootIdx], settings)}"
        }
        else -> {
          val sym = if (inclusive) "≤" else "<"
          "${formatDoubleClassWiz(uniqueRoots[startRootIdx], settings)} $sym x $sym ${formatDoubleClassWiz(uniqueRoots[endRootIdx], settings)}"
        }
      }
      intervals.add(part)
      i = endIdx + 1
    }

    if (intervals.isEmpty()) return settings.tr("Sin solución", "No solution")
    return intervals.joinToString(", ")
  }

  // --- Statistics & Regressions ---

  fun computeStatistics(
    model: StatModelType,
    xList: List<Double>,
    yList: List<Double>,
    freqList: List<Double>,
  ): StatSummary {
    if (xList.isEmpty()) return StatSummary()
    val weights = if (freqList.size == xList.size) freqList else List(xList.size) { 1.0 }
    val expandedX = mutableListOf<Double>()
    var nTotal = 0.0
    var sumX = 0.0
    var sumX2 = 0.0
    for (i in xList.indices) {
      val f = weights[i].coerceAtLeast(0.0)
      nTotal += f
      sumX += xList[i] * f
      sumX2 += xList[i] * xList[i] * f
      val count = f.roundToInt().coerceIn(0, 500)
      repeat(count) { expandedX.add(xList[i]) }
    }
    if (nTotal <= 0.0) return StatSummary()
    val meanX = sumX / nTotal
    val varPopX = max(0.0, sumX2 / nTotal - meanX * meanX)
    val sigmaX = sqrt(varPopX)
    val sX = if (nTotal > 1.0) sqrt(max(0.0, (sumX2 - sumX * meanX) / (nTotal - 1.0))) else 0.0

    val sortedX = if (expandedX.isNotEmpty()) expandedX.sorted() else xList.sorted()
    fun medianOf(list: List<Double>): Double {
      if (list.isEmpty()) return 0.0
      val m = list.size / 2
      return if (list.size % 2 == 1) list[m] else 0.5 * (list[m - 1] + list[m])
    }
    val med = medianOf(sortedX)
    val mid = sortedX.size / 2
    val q1 = medianOf(sortedX.subList(0, mid.coerceAtLeast(1)))
    val q3 = medianOf(sortedX.subList(if (sortedX.size % 2 == 0) mid else (mid + 1).coerceAtMost(sortedX.size), sortedX.size).ifEmpty { sortedX })

    var meanY = 0.0
    var sumY = 0.0
    var sumY2 = 0.0
    var sumXY = 0.0
    var sigmaY = 0.0
    var sY = 0.0
    var regA = 0.0
    var regB = 0.0
    var regC = 0.0
    var regR = 0.0
    var regR2 = 0.0

    if (model != StatModelType.ONE_VAR && yList.size == xList.size) {
      for (i in yList.indices) {
        val f = weights[i].coerceAtLeast(0.0)
        sumY += yList[i] * f
        sumY2 += yList[i] * yList[i] * f
        sumXY += xList[i] * yList[i] * f
      }
      meanY = sumY / nTotal
      sigmaY = sqrt(max(0.0, sumY2 / nTotal - meanY * meanY))
      sY = if (nTotal > 1.0) sqrt(max(0.0, (sumY2 - sumY * meanY) / (nTotal - 1.0))) else 0.0

      when (model) {
        StatModelType.LINEAR -> {
          val lin = fitLinear(xList, yList, weights)
          regA = lin.first; regB = lin.second; regR = lin.third; regR2 = regR * regR
        }
        StatModelType.QUADRATIC -> {
          val quad = fitQuadratic(xList, yList, weights)
          regA = quad[0]; regB = quad[1]; regC = quad[2]; regR2 = quad[3]; regR = sqrt(max(0.0, regR2))
        }
        StatModelType.LOGARITHMIC -> {
          val tx = xList.map { ln(it.coerceAtLeast(1e-15)) }
          val lin = fitLinear(tx, yList, weights)
          regA = lin.first; regB = lin.second; regR = lin.third; regR2 = regR * regR
        }
        StatModelType.EXP_E -> {
          val ty = yList.map { ln(it.coerceAtLeast(1e-15)) }
          val lin = fitLinear(xList, ty, weights)
          regA = exp(lin.first); regB = lin.second; regR = lin.third; regR2 = regR * regR
        }
        StatModelType.EXP_AB -> {
          val ty = yList.map { ln(it.coerceAtLeast(1e-15)) }
          val lin = fitLinear(xList, ty, weights)
          regA = exp(lin.first); regB = exp(lin.second); regR = lin.third; regR2 = regR * regR
        }
        StatModelType.POWER -> {
          val tx = xList.map { ln(it.coerceAtLeast(1e-15)) }
          val ty = yList.map { ln(it.coerceAtLeast(1e-15)) }
          val lin = fitLinear(tx, ty, weights)
          regA = exp(lin.first); regB = lin.second; regR = lin.third; regR2 = regR * regR
        }
        StatModelType.INVERSE -> {
          val tx = xList.map { if (abs(it) < 1e-15) 1e15 else 1.0 / it }
          val lin = fitLinear(tx, yList, weights)
          regA = lin.first; regB = lin.second; regR = lin.third; regR2 = regR * regR
        }
        StatModelType.ONE_VAR -> {}
      }
    }

    return StatSummary(
      n = nTotal.roundToInt(),
      meanX = meanX, sumX = sumX, sumX2 = sumX2, sigmaX = sigmaX, sX = sX,
      minX = sortedX.first(), q1 = q1, med = med, q3 = q3, maxX = sortedX.last(),
      meanY = meanY, sumY = sumY, sumY2 = sumY2, sumXY = sumXY, sigmaY = sigmaY, sY = sY,
      regA = regA, regB = regB, regC = regC, regR = regR, regR2 = regR2,
    )
  }

  private fun fitLinear(xs: List<Double>, ys: List<Double>, ws: List<Double>): Triple<Double, Double, Double> {
    var sw = 0.0; var sx = 0.0; var sy = 0.0; var sxx = 0.0; var syy = 0.0; var sxy = 0.0
    for (i in xs.indices) {
      val w = ws[i]
      sw += w; sx += w * xs[i]; sy += w * ys[i]
      sxx += w * xs[i] * xs[i]; syy += w * ys[i] * ys[i]; sxy += w * xs[i] * ys[i]
    }
    val den = sw * sxx - sx * sx
    if (abs(den) < 1e-15) return Triple(0.0, 0.0, 0.0)
    val b = (sw * sxy - sx * sy) / den
    val a = (sy - b * sx) / sw
    val denR = sqrt(max(0.0, (sw * sxx - sx * sx) * (sw * syy - sy * sy)))
    val r = if (denR < 1e-15) 0.0 else (sw * sxy - sx * sy) / denR
    return Triple(a, b, r)
  }

  private fun fitQuadratic(xs: List<Double>, ys: List<Double>, ws: List<Double>): DoubleArray {
    var sw = 0.0; var sx = 0.0; var sx2 = 0.0; var sx3 = 0.0; var sx4 = 0.0
    var sy = 0.0; var sxy = 0.0; var sx2y = 0.0
    for (i in xs.indices) {
      val w = ws[i]; val x = xs[i]; val y = ys[i]; val x2 = x * x
      sw += w; sx += w * x; sx2 += w * x2; sx3 += w * x2 * x; sx4 += w * x2 * x2
      sy += w * y; sxy += w * x * y; sx2y += w * x2 * y
    }
    val mat = listOf(
      listOf(sw, sx, sx2),
      listOf(sx, sx2, sx3),
      listOf(sx2, sx3, sx4)
    )
    val rhs = listOf(sy, sxy, sx2y)
    val sol = solveLinearSystem(mat, rhs)
    val abc = (sol as? LinearSystemResult.Unique)?.values ?: return doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    val meanY = sy / sw
    var ssTot = 0.0; var ssRes = 0.0
    for (i in xs.indices) {
      val pred = abc[0] + abc[1] * xs[i] + abc[2] * xs[i] * xs[i]
      ssRes += ws[i] * (ys[i] - pred).pow(2)
      ssTot += ws[i] * (ys[i] - meanY).pow(2)
    }
    val r2 = if (ssTot < 1e-15) 1.0 else (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)
    return doubleArrayOf(abc[0], abc[1], abc[2], r2)
  }

  // --- Probability Distributions ---

  fun normalPdf(x: Double, sigma: Double, mu: Double): Double {
    val z = (x - mu) / sigma
    return exp(-0.5 * z * z) / (sigma * sqrt(2.0 * PI))
  }

  fun normalCdf(x: Double): Double = 0.5 * (1.0 + erf(x / sqrt(2.0)))

  fun normalCdfRange(low: Double, high: Double, sigma: Double, mu: Double): Double {
    val z1 = (low - mu) / sigma
    val z2 = (high - mu) / sigma
    return normalCdf(z2) - normalCdf(z1)
  }

  private fun erf(x: Double): Double {
    val sign = if (x < 0) -1.0 else 1.0
    val ax = abs(x)
    val t = 1.0 / (1.0 + 0.3275911 * ax)
    val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-ax * ax)
    return sign * y
  }

  fun inverseNormalCdf(p: Double, sigma: Double, mu: Double): Double {
    // Acklam rational approximation
    val a = doubleArrayOf(-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00)
    val b = doubleArrayOf(-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01)
    val c = doubleArrayOf(-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00)
    val d = doubleArrayOf(7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00)
    val pLow = 0.02425
    val pHigh = 1.0 - pLow
    val z = when {
      p < pLow -> {
        val q = sqrt(-2.0 * ln(p))
        (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
          ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
      }
      p <= pHigh -> {
        val q = p - 0.5
        val r = q * q
        (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
          (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0)
      }
      else -> {
        val q = sqrt(-2.0 * ln(1.0 - p))
        -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
          ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
      }
    }
    return mu + sigma * z
  }

  fun binomialPd(x: Int, n: Int, p: Double): Double {
    if (x < 0 || x > n || n < 0 || p < 0.0 || p > 1.0) return 0.0
    if (p == 0.0) return if (x == 0) 1.0 else 0.0
    if (p == 1.0) return if (x == n) 1.0 else 0.0
    val logCoeff = lgamma(n + 1.0) - lgamma(x + 1.0) - lgamma(n - x + 1.0)
    return exp(logCoeff + x * ln(p) + (n - x) * ln(1.0 - p))
  }

  fun binomialCd(x: Int, n: Int, p: Double): Double {
    var sum = 0.0
    for (k in 0..min(x, n)) sum += binomialPd(k, n, p)
    return sum.coerceIn(0.0, 1.0)
  }

  fun poissonPd(x: Int, lambda: Double): Double {
    if (x < 0 || lambda <= 0.0) return 0.0
    return exp(x * ln(lambda) - lambda - lgamma(x + 1.0))
  }

  fun poissonCd(x: Int, lambda: Double): Double {
    var sum = 0.0
    for (k in 0..x.coerceAtLeast(0)) sum += poissonPd(k, lambda)
    return sum.coerceIn(0.0, 1.0)
  }

  private fun lgamma(x: Double): Double {
    val cof = doubleArrayOf(
      76.18009172947146, -86.50532032941677, 24.01409824083091,
      -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5
    )
    var y = x
    var tmp = x + 5.5
    tmp -= (x + 0.5) * ln(tmp)
    var ser = 1.000000000190015
    for (j in 0..5) {
      y += 1.0
      ser += cof[j] / y
    }
    return -tmp + ln(2.5066282746310005 * ser / x)
  }

  // --- Spreadsheet Formula Evaluator ---

  fun evaluateSpreadsheet(cells: Map<String, String>): Map<String, Double?> {
    val results = mutableMapOf<String, Double?>()
    val visiting = mutableSetOf<String>()

    fun evalCell(key: String): Double? {
      if (results.containsKey(key)) return results[key]
      val raw = cells[key]?.trim()
      if (raw.isNullOrEmpty()) return null
      if (!raw.startsWith("=")) {
        val v = raw.toDoubleOrNull()
        results[key] = v
        return v
      }
      if (!visiting.add(key)) return Double.NaN
      val expr = raw.drop(1).uppercase()
      val v = try {
        evalSheetExpr(expr, ::evalCell)
      } catch (_: Exception) {
        Double.NaN
      }
      visiting.remove(key)
      results[key] = v
      return v
    }

    for (k in cells.keys) evalCell(k)
    return results
  }

  private fun evalSheetExpr(expr: String, lookup: (String) -> Double?): Double {
    // Expand SUM(A1:B3), MEAN(...), MIN(...), MAX(...)
    var expanded = expr.replace(" ", "")
    val funcRegex = Regex("(SUM|MEAN|MIN|MAX)\\(([A-E]\\d+):([A-E]\\d+)\\)")
    while (true) {
      val match = funcRegex.find(expanded) ?: break
      val fn = match.groupValues[1]
      val start = match.groupValues[2]
      val end = match.groupValues[3]
      val vals = expandRange(start, end).mapNotNull(lookup).filter { !it.isNaN() }
      val agg = when (fn) {
        "SUM" -> vals.sum()
        "MEAN" -> if (vals.isEmpty()) 0.0 else vals.average()
        "MIN" -> vals.minOrNull() ?: 0.0
        "MAX" -> vals.maxOrNull() ?: 0.0
        else -> 0.0
      }
      expanded = expanded.replaceRange(match.range, agg.toString())
    }
    val cellRegex = Regex("[A-E]\\d+")
    var circError = false
    expanded = cellRegex.replace(expanded) { m ->
      val v = lookup(m.value) ?: 0.0
      if (v.isNaN()) {
        circError = true
        "0"
      } else {
        "($v)"
      }
    }
    if (circError) return Double.NaN
    return PlainArithmeticParser(expanded).parseExpr()
  }

  private fun expandRange(start: String, end: String): List<String> {
    val c1 = start[0]
    val r1 = start.drop(1).toIntOrNull() ?: return emptyList()
    val c2 = end[0]
    val r2 = end.drop(1).toIntOrNull() ?: return emptyList()
    val out = mutableListOf<String>()
    for (c in min(c1.code, c2.code)..max(c1.code, c2.code)) {
      for (r in min(r1, r2)..max(r1, r2)) {
        out.add("${c.toChar()}$r")
      }
    }
    return out
  }

  private class PlainArithmeticParser(private val s: String) {
    private var pos = 0
    private fun peek(): Char? = s.getOrNull(pos)

    fun parseExpr(): Double {
      var v = parseTerm()
      while (peek() == '+' || peek() == '-') {
        val op = s[pos++]
        val r = parseTerm()
        v = if (op == '+') v + r else v - r
      }
      return v
    }

    private fun parseTerm(): Double {
      var v = parsePow()
      while (peek() == '*' || peek() == '×' || peek() == '/' || peek() == '÷') {
        val op = s[pos++]
        val r = parsePow()
        v = if (op == '*' || op == '×') v * r else v / r
      }
      return v
    }

    private fun parsePow(): Double {
      val base = parseFactor()
      if (peek() == '^') {
        pos++
        return base.pow(parsePow())
      }
      return base
    }

    private fun parseFactor(): Double {
      if (peek() == '-') { pos++; return -parseFactor() }
      if (peek() == '+') { pos++; return parseFactor() }
      if (peek() == '(') {
        pos++
        val v = parseExpr()
        if (peek() == ')') pos++
        return v
      }
      val start = pos
      while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
      return s.substring(start, pos).toDouble()
    }
  }
}
