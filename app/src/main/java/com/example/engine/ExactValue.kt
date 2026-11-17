package com.example.engine

import java.math.BigInteger
import kotlin.math.*

data class Rational(val num: BigInteger, val den: BigInteger) {
  companion object {
    val ZERO = Rational(BigInteger.ZERO, BigInteger.ONE)
    val ONE = Rational(BigInteger.ONE, BigInteger.ONE)
    val MINUS_ONE = Rational(-BigInteger.ONE, BigInteger.ONE)

    fun of(n: Long, d: Long = 1L): Rational =
      normalize(BigInteger.valueOf(n), BigInteger.valueOf(d))

    fun of(n: Int, d: Int = 1): Rational =
      normalize(BigInteger.valueOf(n.toLong()), BigInteger.valueOf(d.toLong()))

    fun normalize(n: BigInteger, d: BigInteger): Rational {
      require(d != BigInteger.ZERO) { "Division by zero" }
      if (n == BigInteger.ZERO) return ZERO
      var num = n
      var den = d
      if (den < BigInteger.ZERO) {
        num = -num
        den = -den
      }
      val g = num.abs().gcd(den)
      return Rational(num / g, den / g)
    }

    fun fromDecimalString(text: String): Rational? {
      val trimmed = text.trim()
      if (trimmed.isEmpty()) return null
      if (!trimmed.contains('.')) {
        val bi = trimmed.toBigIntegerOrNull() ?: return null
        return Rational(bi, BigInteger.ONE)
      }
      val parts = trimmed.split('.')
      if (parts.size != 2) return null
      val intPart = if (parts[0].isEmpty() || parts[0] == "-") "${parts[0]}0" else parts[0]
      val fracPart = parts[1]
      if (fracPart.length > 12) return null
      val sign = if (intPart.startsWith("-")) -1 else 1
      val absInt = intPart.removePrefix("-").toBigIntegerOrNull() ?: return null
      val fracVal = if (fracPart.isEmpty()) BigInteger.ZERO else fracPart.toBigIntegerOrNull() ?: return null
      val scale = BigInteger.TEN.pow(fracPart.length)
      val combined = absInt * scale + fracVal
      return normalize(if (sign < 0) -combined else combined, scale)
    }
  }

  val isZero: Boolean get() = num == BigInteger.ZERO
  val isInteger: Boolean get() = den == BigInteger.ONE
  val isNegative: Boolean get() = num < BigInteger.ZERO

  operator fun plus(other: Rational): Rational =
    normalize(num * other.den + other.num * den, den * other.den)

  operator fun minus(other: Rational): Rational =
    normalize(num * other.den - other.num * den, den * other.den)

  operator fun times(other: Rational): Rational =
    normalize(num * other.num, den * other.den)

  operator fun div(other: Rational): Rational {
    require(!other.isZero) { "Division by zero" }
    return normalize(num * other.den, den * other.num)
  }

  operator fun unaryMinus(): Rational = Rational(-num, den)

  fun abs(): Rational = Rational(num.abs(), den)

  fun toDouble(): Double = num.toDouble() / den.toDouble()

  fun fitsReasonably(): Boolean =
    num.abs().bitLength() <= 50 && den.bitLength() <= 45
}

/**
 * Exact sum of rational multiples of square-free surds: sum_k (coeff_k * sqrt(k)),
 * where key 1 represents the rational term.
 */
data class SurdSum(val terms: Map<Int, Rational>) {
  companion object {
    val ZERO = SurdSum(emptyMap())
    val ONE = SurdSum(mapOf(1 to Rational.ONE))

    fun rational(r: Rational): SurdSum =
      if (r.isZero) ZERO else SurdSum(mapOf(1 to r))

    fun surd(coeff: Rational, radicand: Int): SurdSum? {
      if (coeff.isZero || radicand == 0) return ZERO
      if (radicand < 0) return null
      val (outer, inner) = factorSquare(radicand)
      val scaled = coeff * Rational.of(outer)
      return if (scaled.isZero) ZERO else SurdSum(mapOf(inner to scaled))
    }

    fun factorSquare(n: Int): Pair<Int, Int> {
      if (n <= 1) return 1 to n
      var rem = n
      var out = 1
      var d = 2
      while (d * d <= rem) {
        while (rem % (d * d) == 0) {
          out *= d
          rem /= (d * d)
        }
        d++
      }
      return out to rem
    }

    fun fromRationalSqrt(r: Rational): SurdSum? {
      if (r.isNegative) return null
      if (r.isZero) return ZERO
      if (r.num.bitLength() > 30 || r.den.bitLength() > 30) return null
      val n = r.num.toLong()
      val d = r.den.toLong()
      if (n > 1_000_000L || d > 1_000_000L) return null
      val (outN, inN) = factorSquare(n.toInt())
      val (outD, inD) = factorSquare(d.toInt())
      val combinedRadicand = inN.toLong() * inD.toLong()
      if (combinedRadicand > 1_000_000L) return null
      val (outC, inC) = factorSquare(combinedRadicand.toInt())
      val coeff = Rational.of(outN.toLong() * outC.toLong(), outD.toLong() * inD.toLong())
      return SurdSum(mapOf(inC to coeff)).clean()
    }
  }

  fun clean(): SurdSum = SurdSum(terms.filterValues { !it.isZero })

  val isZero: Boolean get() = terms.isEmpty()

  fun asRationalOrNull(): Rational? {
    if (terms.isEmpty()) return Rational.ZERO
    if (terms.size == 1 && terms.containsKey(1)) return terms[1]
    return null
  }

  fun toDouble(): Double =
    terms.entries.sumOf { (k, c) -> c.toDouble() * sqrt(k.toDouble()) }

  operator fun plus(other: SurdSum): SurdSum? {
    val merged = terms.toMutableMap()
    for ((k, v) in other.terms) {
      val sum = (merged[k] ?: Rational.ZERO) + v
      if (sum.isZero) merged.remove(k) else merged[k] = sum
    }
    if (merged.size > 4 || merged.values.any { !it.fitsReasonably() }) return null
    return SurdSum(merged)
  }

  operator fun minus(other: SurdSum): SurdSum? = plus(-other)

  operator fun unaryMinus(): SurdSum = SurdSum(terms.mapValues { -it.value })

  operator fun times(other: SurdSum): SurdSum? {
    if (isZero || other.isZero) return ZERO
    val acc = mutableMapOf<Int, Rational>()
    for ((k1, c1) in terms) {
      for ((k2, c2) in other.terms) {
        val prod = k1.toLong() * k2.toLong()
        if (prod > 1_000_000L) return null
        val (out, inner) = factorSquare(prod.toInt())
        val c = c1 * c2 * Rational.of(out)
        val cur = (acc[inner] ?: Rational.ZERO) + c
        if (cur.isZero) acc.remove(inner) else acc[inner] = cur
      }
    }
    if (acc.size > 4 || acc.values.any { !it.fitsReasonably() }) return null
    return SurdSum(acc)
  }

  fun divideBy(other: SurdSum): SurdSum? {
    if (other.isZero) return null
    val r = other.asRationalOrNull()
    if (r != null) {
      val inv = Rational.ONE / r
      return this * rational(inv)
    }
    // Rationalize single surd: c * sqrt(k)
    if (other.terms.size == 1) {
      val (k, c) = other.terms.entries.first()
      val conj = SurdSum(mapOf(k to Rational.ONE))
      val num = this * conj ?: return null
      val den = c * Rational.of(k)
      return num * rational(Rational.ONE / den)
    }
    // Rationalize binomial: a*sqrt(k1) + b*sqrt(k2)
    if (other.terms.size == 2) {
      val entries = other.terms.entries.toList()
      val (k1, c1) = entries[0]
      val (k2, c2) = entries[1]
      val conj = SurdSum(mapOf(k1 to c1, k2 to -c2))
      val denom = c1 * c1 * Rational.of(k1) - c2 * c2 * Rational.of(k2)
      if (denom.isZero) return null
      val num = this * conj ?: return null
      return num * rational(Rational.ONE / denom)
    }
    return null
  }

  fun formatLatexLike(fractionFormat: FractionFormat = FractionFormat.IMPROPER): String {
    if (isZero) return "0"
    val r = asRationalOrNull()
    if (r != null) return formatRational(r, fractionFormat)

    // Try factoring common denominator so (√6 + √2)/4 renders neatly
    val lcmDen = terms.values.fold(BigInteger.ONE) { acc, rat ->
      val g = acc.gcd(rat.den)
      (acc / g) * rat.den
    }
    val sorted = terms.entries.sortedBy { it.key }
    val numParts = StringBuilder()
    sorted.forEachIndexed { idx, (radicand, rat) ->
      val scaled = rat.num * (lcmDen / rat.den)
      val absScaled = scaled.abs()
      val sign = if (scaled < BigInteger.ZERO) "-" else if (idx > 0) "+" else ""
      val termStr = if (radicand == 1) {
        absScaled.toString()
      } else {
        if (absScaled == BigInteger.ONE) "√$radicand" else "${absScaled}√$radicand"
      }
      numParts.append(sign).append(termStr)
    }
    return if (lcmDen == BigInteger.ONE) {
      numParts.toString()
    } else {
      "($numParts)/$lcmDen"
    }
  }
}

sealed class ExactReal {
  abstract fun toDouble(): Double

  data class Surd(val value: SurdSum) : ExactReal() {
    override fun toDouble(): Double = value.toDouble()
  }

  data class PiMultiple(val coeff: SurdSum) : ExactReal() {
    override fun toDouble(): Double = coeff.toDouble() * PI
  }

  data class Approx(val value: Double) : ExactReal() {
    override fun toDouble(): Double = value
  }

  companion object {
    val ZERO = Surd(SurdSum.ZERO)
    val ONE = Surd(SurdSum.ONE)

    fun fromRational(r: Rational): ExactReal = Surd(SurdSum.rational(r))
    fun fromDouble(d: Double): ExactReal = Approx(d)
  }

  val isZero: Boolean
    get() = when (this) {
      is Surd -> value.isZero
      is PiMultiple -> coeff.isZero
      is Approx -> abs(value) < 1e-15
    }

  operator fun plus(other: ExactReal): ExactReal {
    if (this is Surd && other is Surd) {
      val s = value + other.value
      if (s != null) return Surd(s)
    }
    if (this is PiMultiple && other is PiMultiple) {
      val s = coeff + other.coeff
      if (s != null) return if (s.isZero) ZERO else PiMultiple(s)
    }
    if (this.isZero) return other
    if (other.isZero) return this
    return Approx(toDouble() + other.toDouble())
  }

  operator fun minus(other: ExactReal): ExactReal = this + (-other)

  operator fun unaryMinus(): ExactReal = when (this) {
    is Surd -> Surd(-value)
    is PiMultiple -> PiMultiple(-coeff)
    is Approx -> Approx(-value)
  }

  operator fun times(other: ExactReal): ExactReal {
    if (this.isZero || other.isZero) return ZERO
    if (this is Surd && other is Surd) {
      val p = value * other.value
      if (p != null) return Surd(p)
    }
    if (this is Surd && other is PiMultiple) {
      val p = value * other.coeff
      if (p != null) return if (p.isZero) ZERO else PiMultiple(p)
    }
    if (this is PiMultiple && other is Surd) {
      val p = coeff * other.value
      if (p != null) return if (p.isZero) ZERO else PiMultiple(p)
    }
    return Approx(toDouble() * other.toDouble())
  }

  operator fun div(other: ExactReal): ExactReal {
    if (other.isZero) throw ArithmeticException("Math Error")
    if (this.isZero) return ZERO
    if (this is Surd && other is Surd) {
      val q = value.divideBy(other.value)
      if (q != null) return Surd(q)
    }
    if (this is PiMultiple && other is Surd) {
      val q = coeff.divideBy(other.value)
      if (q != null) return PiMultiple(q)
    }
    if (this is PiMultiple && other is PiMultiple) {
      val q = coeff.divideBy(other.coeff)
      if (q != null) return Surd(q)
    }
    return Approx(toDouble() / other.toDouble())
  }
}

sealed class EvalResult {
  data class RealResult(val value: ExactReal) : EvalResult()
  data class ComplexResult(val re: ExactReal, val im: ExactReal) : EvalResult()
  data class PolarPoint(val r: Double, val theta: Double, val isPol: Boolean) : EvalResult()
  data class MatrixResult(val rows: Int, val cols: Int, val data: List<List<Double>>) : EvalResult()
  data class VectorResult(val data: List<Double>) : EvalResult()
  data class BaseNResult(val value: Int, val base: BaseNMode) : EvalResult()
  data class VerifyResult(val isTrue: Boolean, val lhsSummary: String, val rhsSummary: String) : EvalResult()
  data class ErrorResult(val title: String, val message: String) : EvalResult()
}

enum class ResultFormatMode {
  STANDARD,
  DECIMAL,
  MIXED_FRACTION,
  IMPROPER_FRACTION,
  PRIME_FACTOR,
  SEXAGESIMAL,
  SCIENTIFIC,
  ENGINEERING,
  POLAR,
  RECTANGULAR,
}

fun formatRational(r: Rational, fractionFormat: FractionFormat): String {
  if (r.isInteger) return r.num.toString()
  if (fractionFormat == FractionFormat.MIXED && r.num.abs() > r.den) {
    val neg = if (r.isNegative) "-" else ""
    val whole = r.num.abs() / r.den
    val rem = r.num.abs() % r.den
    return "$neg$whole $rem/${r.den}"
  }
  return "${r.num}/${r.den}"
}

fun primeFactorize(n: Long): String? {
  if (n < 2L || n > 10_000_000_000L) return null
  var rem = n
  val factors = mutableListOf<Pair<Long, Int>>()
  var d = 2L
  while (d * d <= rem) {
    if (rem % d == 0L) {
      var count = 0
      while (rem % d == 0L) {
        rem /= d
        count++
      }
      factors.add(d to count)
    }
    d += if (d == 2L) 1L else 2L
  }
  if (rem > 1L) factors.add(rem to 1)
  return factors.joinToString("×") { (p, e) ->
    if (e == 1) "$p" else "$p${toSuperscript(e)}"
  }
}

fun toSuperscript(n: Int): String {
  val map = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '-' to '⁻'
  )
  return n.toString().map { map[it] ?: it }.joinToString("")
}

fun formatSexagesimal(degVal: Double): String {
  val neg = if (degVal < 0) "-" else ""
  val absVal = abs(degVal) + 1e-11
  val d = floor(absVal).toInt()
  val remMin = (absVal - d) * 60.0
  val m = floor(remMin).toInt()
  val s = (remMin - m) * 60.0
  val sClean = if (abs(s - round(s)) < 1e-5) round(s).toInt().toString() else String.format(java.util.Locale.US, "%.2f", s).trimEnd('0').trimEnd('.')
  return "$neg$d°$m'$sClean\""
}

fun formatDoubleClassWiz(v: Double, settings: CalcSettings, forceMode: ResultFormatMode? = null): String {
  if (v.isNaN() || v.isInfinite()) return "Math ERROR"
  val clean = if (abs(v) < 1e-14) 0.0 else v
  if (forceMode == ResultFormatMode.SEXAGESIMAL) return formatSexagesimal(clean)
  if (forceMode == ResultFormatMode.ENGINEERING) {
    if (clean == 0.0) return "0×10⁰"
    val exp = floor(log10(abs(clean))).toInt()
    val engExp = Math.floorDiv(exp, 3) * 3
    val mant = clean / 10.0.pow(engExp.toDouble())
    val mantStr = String.format(java.util.Locale.US, "%.6f", mant).trimEnd('0').trimEnd('.')
    return "$mantStr×10${toSuperscript(engExp)}"
  }
  if (forceMode == ResultFormatMode.SCIENTIFIC || settings.numberFormat == NumberFormatMode.SCI) {
    val digits = settings.sciDigits.coerceIn(0, 9)
    if (clean == 0.0) return "0"
    val exp = floor(log10(abs(clean))).toInt()
    val mant = clean / 10.0.pow(exp.toDouble())
    val mantStr = String.format(java.util.Locale.US, "%.${digits}f", mant)
    return "$mantStr×10${toSuperscript(exp)}"
  }
  if (settings.numberFormat == NumberFormatMode.FIX) {
    val digits = settings.fixDigits.coerceIn(0, 9)
    return String.format(java.util.Locale.US, "%.${digits}f", clean)
  }
  // Norm 1 / Norm 2
  val normLimit = if (settings.numberFormat == NumberFormatMode.NORM_2) 1e-9 else 1e-2
  if (clean != 0.0 && (abs(clean) >= 1e10 || abs(clean) < normLimit)) {
    val exp = floor(log10(abs(clean))).toInt()
    val mant = clean / 10.0.pow(exp.toDouble())
    val mantStr = String.format(java.util.Locale.US, "%.9f", mant).trimEnd('0').trimEnd('.')
    return "$mantStr×10${toSuperscript(exp)}"
  }
  val rounded = String.format(java.util.Locale.US, "%.10g", clean).toDouble()
  if (abs(rounded - round(rounded)) < 1e-11 && abs(rounded) < 1e12) {
    return round(rounded).toLong().toString()
  }
  val s = String.format(java.util.Locale.US, "%.10f", clean).trimEnd('0').trimEnd('.')
  return if (s == "-0") "0" else s
}

/**
 * Exact trig lookup for multiples of 15 degrees (mod 360).
 */
object ExactTrig {
  fun sinDegExact(degDouble: Double): ExactReal? {
    val m = degDouble / 15.0
    val r = round(m)
    if (abs(m - r) > 1e-11) return null
    val idx = ((r.toLong() % 24L) + 24L).toInt() % 24
    val sign = if (idx >= 12) -1 else 1
    val q = idx % 12
    val sym = if (q > 6) 12 - q else q
    val base: SurdSum = when (sym) {
      0 -> SurdSum.ZERO
      1 -> SurdSum(mapOf(6 to Rational.of(1, 4), 2 to Rational.of(-1, 4))) // (√6 - √2)/4
      2 -> SurdSum(mapOf(1 to Rational.of(1, 2)))                          // 1/2
      3 -> SurdSum(mapOf(2 to Rational.of(1, 4)))                          // √2/2 = 2/4 √2 -> wait 1/2 √2!
      4 -> SurdSum(mapOf(3 to Rational.of(1, 2)))                          // √3/2
      5 -> SurdSum(mapOf(6 to Rational.of(1, 4), 2 to Rational.of(1, 4)))  // (√6 + √2)/4
      6 -> SurdSum.ONE
      else -> return null
    }.let {
      if (sym == 3) SurdSum(mapOf(2 to Rational.of(1, 2))) else it
    }
    return ExactReal.Surd(if (sign < 0) -base else base)
  }

  fun cosDegExact(degDouble: Double): ExactReal? = sinDegExact(degDouble + 90.0)

  fun tanDegExact(degDouble: Double): ExactReal? {
    val s = sinDegExact(degDouble) ?: return null
    val c = cosDegExact(degDouble) ?: return null
    if (c.isZero) throw ArithmeticException("Math ERROR")
    return s / c
  }
}
