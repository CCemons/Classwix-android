package com.example.engine

import java.math.BigInteger
import kotlin.math.*
import kotlin.random.Random

class CalcSyntaxException(message: String = "Syntax ERROR") : RuntimeException(message)
class CalcMathException(message: String = "Math ERROR") : RuntimeException(message)
class CalcDimensionException(message: String = "Dimension ERROR") : RuntimeException(message)
class CalcArgumentException(message: String = "Argument ERROR") : RuntimeException(message)

data class EvalContext(
  val settings: CalcSettings = CalcSettings(),
  val mode: CalcMode = CalcMode.CALCULATE,
  val baseNMode: BaseNMode = BaseNMode.DEC,
  val variables: Map<String, EvalResult> = emptyMap(),
  val matrices: Map<String, EvalResult.MatrixResult> = emptyMap(),
  val vectors: Map<String, EvalResult.VectorResult> = emptyMap(),
  val ans: EvalResult? = null,
  val preAns: EvalResult? = null,
  val statSummary: StatSummary? = null,
  val fExpr: MathRow? = null,
  val gExpr: MathRow? = null,
  val recursionDepth: Int = 0,
)

data class StatSummary(
  val n: Int = 0,
  val meanX: Double = 0.0,
  val sumX: Double = 0.0,
  val sumX2: Double = 0.0,
  val sigmaX: Double = 0.0,
  val sX: Double = 0.0,
  val minX: Double = 0.0,
  val q1: Double = 0.0,
  val med: Double = 0.0,
  val q3: Double = 0.0,
  val maxX: Double = 0.0,
  val meanY: Double = 0.0,
  val sumY: Double = 0.0,
  val sumY2: Double = 0.0,
  val sumXY: Double = 0.0,
  val sigmaY: Double = 0.0,
  val sY: Double = 0.0,
  val regA: Double = 0.0,
  val regB: Double = 0.0,
  val regC: Double = 0.0,
  val regR: Double = 0.0,
  val regR2: Double = 0.0,
)

object ScientificEvaluator {

  fun evaluate(row: MathRow, ctx: EvalContext): EvalResult {
    return try {
      if (row.isEmpty()) return EvalResult.ErrorResult("Syntax ERROR", "Expresión vacía")
      if (ctx.recursionDepth > 16) return EvalResult.ErrorResult("Stack ERROR", "Límite de recursión")
      if (ctx.mode == CalcMode.BASE_N) {
        val v = evalBaseNRow(row, ctx)
        return EvalResult.BaseNResult(v, ctx.baseNMode)
      }
      val verifyOp = findTopLevelVerifyOp(row)
      if (verifyOp != null) {
        val (opIdx, opCode) = verifyOp
        val lhs = MathRow(row.items.subList(0, opIdx))
        val rhs = MathRow(row.items.subList(opIdx + 1, row.items.size))
        if (lhs.isEmpty() || rhs.isEmpty()) throw CalcSyntaxException()
        val lhsRes = evaluate(lhs, ctx)
        if (lhsRes is EvalResult.ErrorResult) return lhsRes
        val rhsRes = evaluate(rhs, ctx)
        if (rhsRes is EvalResult.ErrorResult) return rhsRes
        val isEq = areResultsEqual(lhsRes, rhsRes)
        val isTrue = if (opCode == "=") isEq else !isEq
        return EvalResult.VerifyResult(
          isTrue = isTrue,
          lhsSummary = summarizeEvalResult(lhsRes, ctx.settings),
          rhsSummary = summarizeEvalResult(rhsRes, ctx.settings),
        )
      }
      val parser = RowParser(row, ctx)
      val res = parser.parseExpression()
      if (!parser.isAtEnd()) throw CalcSyntaxException()
      res
    } catch (e: CalcSyntaxException) {
      EvalResult.ErrorResult("Syntax ERROR", e.message ?: "Syntax ERROR")
    } catch (e: CalcMathException) {
      EvalResult.ErrorResult("Math ERROR", e.message ?: "Math ERROR")
    } catch (e: CalcDimensionException) {
      EvalResult.ErrorResult("Dimension ERROR", e.message ?: "Dimension ERROR")
    } catch (e: CalcArgumentException) {
      EvalResult.ErrorResult("Argument ERROR", e.message ?: "Argument ERROR")
    } catch (e: ArithmeticException) {
      EvalResult.ErrorResult("Math ERROR", e.message ?: "Math ERROR")
    } catch (e: Exception) {
      EvalResult.ErrorResult("Syntax ERROR", "Error de expresión")
    }
  }

  fun evaluateReal(row: MathRow, ctx: EvalContext): Double {
    return when (val r = evaluate(row, ctx)) {
      is EvalResult.RealResult -> r.value.toDouble()
      is EvalResult.ComplexResult -> {
        if (abs(r.im.toDouble()) < 1e-12) r.re.toDouble() else throw CalcMathException()
      }
      is EvalResult.ErrorResult -> throw CalcMathException(r.title)
      else -> throw CalcMathException()
    }
  }

  private fun evalBaseNRow(row: MathRow, ctx: EvalContext): Int {
    val tokens = flattenBaseNTokens(row)
    return BaseNParser(tokens, ctx).parseAll()
  }

  private class BaseNParser(
    private val tokens: List<String>,
    private val ctx: EvalContext,
  ) {
    private var pos = 0
    private fun peek(): String? = tokens.getOrNull(pos)
    private fun consume(): String = tokens[pos++]

    fun parseAll(): Int {
      val res = parseXnor()
      if (pos != tokens.size) throw CalcSyntaxException()
      return res
    }

    private fun parsePrimary(): Int {
      val t = peek() ?: throw CalcSyntaxException()
      return when {
        t == "(" -> {
          consume()
          val v = parseXnor()
          if (peek() == ")") consume()
          v
        }
        t == "-" || t == "−" || t == "(-)" -> {
          consume()
          -parsePrimary()
        }
        t == "Not(" -> {
          consume()
          val v = parseXnor()
          if (peek() == ")") consume()
          v.inv()
        }
        t == "Neg(" -> {
          consume()
          val v = parseXnor()
          if (peek() == ")") consume()
          -v
        }
        t == "Ans" -> {
          consume()
          (ctx.ans as? EvalResult.BaseNResult)?.value
            ?: (ctx.ans as? EvalResult.RealResult)?.value?.toDouble()?.toInt()
            ?: 0
        }
        else -> {
          consume()
          parseBaseNLiteral(t, ctx.baseNMode)
        }
      }
    }

    private fun parseMul(): Int {
      var left = parsePrimary()
      while (peek() == "×" || peek() == "*" || peek() == "÷" || peek() == "/") {
        val op = consume()
        val right = parsePrimary()
        left = if (op == "×" || op == "*") left * right else {
          if (right == 0) throw CalcMathException()
          left / right
        }
      }
      return left
    }

    private fun parseAdd(): Int {
      var left = parseMul()
      while (peek() == "+" || peek() == "-" || peek() == "−") {
        val op = consume()
        val right = parseMul()
        left = if (op == "+") left + right else left - right
      }
      return left
    }

    private fun parseAnd(): Int {
      var left = parseAdd()
      while (peek() == "and") {
        consume()
        left = left and parseAdd()
      }
      return left
    }

    private fun parseOr(): Int {
      var left = parseAnd()
      while (peek() == "or") {
        consume()
        left = left or parseAnd()
      }
      return left
    }

    private fun parseXor(): Int {
      var left = parseOr()
      while (peek() == "xor") {
        consume()
        left = left xor parseOr()
      }
      return left
    }

    private fun parseXnor(): Int {
      var left = parseXor()
      while (peek() == "xnor") {
        consume()
        left = (left xor parseXor()).inv()
      }
      return left
    }
  }

  private fun flattenBaseNTokens(row: MathRow): List<String> {
    val out = mutableListOf<String>()
    val numBuf = StringBuilder()
    fun flushNum() {
      if (numBuf.isNotEmpty()) {
        out.add(numBuf.toString())
        numBuf.clear()
      }
    }
    for (el in row.items) {
      if (el !is MathElement.Token) throw CalcSyntaxException()
      val t = el.evalCode
      if (el.kind == TokenKind.DIGIT || t in setOf("A", "B", "C", "D", "E", "F", "d", "h", "b", "o")) {
        numBuf.append(t)
      } else {
        flushNum()
        out.add(t)
      }
    }
    flushNum()
    return out
  }

  private fun parseBaseNLiteral(s: String, defaultBase: BaseNMode): Int {
    val (radix, digits) = when {
      s.startsWith("d") && s.length > 1 -> 10 to s.drop(1)
      s.startsWith("h") && s.length > 1 -> 16 to s.drop(1)
      s.startsWith("b") && s.length > 1 -> 2 to s.drop(1)
      s.startsWith("o") && s.length > 1 -> 8 to s.drop(1)
      else -> defaultBase.radix to s
    }
    return try {
      java.lang.Long.parseLong(digits, radix).toInt()
    } catch (_: NumberFormatException) {
      throw CalcSyntaxException()
    }
  }

  private class RowParser(
    private val row: MathRow,
    private val ctx: EvalContext,
  ) {
    private var idx = 0

    fun isAtEnd(): Boolean = idx >= row.items.size

    private fun peek(): MathElement? = row.items.getOrNull(idx)
    private fun consume(): MathElement = row.items[idx++]

    private fun peekTokenCode(): String? = (peek() as? MathElement.Token)?.evalCode

    fun parseExpression(): EvalResult {
      var left = parseAddSub()
      // Check for trailing unit conversion token like in▶cm
      while (peek() is MathElement.Token && (peek() as MathElement.Token).kind == TokenKind.UNIT_CONVERSION) {
        val tok = consume() as MathElement.Token
        val conv = CatalogData.findConversion(tok.evalCode) ?: throw CalcSyntaxException()
        val x = expectRealDouble(left)
        left = EvalResult.RealResult(ExactReal.fromDouble(conv.convert(x)))
      }
      return left
    }

    private fun parseAddSub(): EvalResult {
      var left = parseMulDiv()
      while (true) {
        val code = peekTokenCode()
        if (code == "+" || code == "-" || code == "−") {
          consume()
          val right = parseMulDiv()
          left = if (code == "+") addResults(left, right) else subResults(left, right)
        } else break
      }
      return left
    }

    private fun parseMulDiv(): EvalResult {
      var left = parsePermComb()
      while (true) {
        val code = peekTokenCode()
        when {
          code == "×" || code == "*" -> {
            consume()
            val right = parsePermComb()
            left = mulResults(left, right)
          }
          code == "÷" || code == "/" -> {
            consume()
            val right = parsePermComb()
            left = divResults(left, right)
          }
          code == "Mod" -> {
            consume()
            val right = parsePermComb()
            val a = expectRealDouble(left)
            val b = expectRealDouble(right)
            if (b == 0.0) throw CalcMathException()
            left = EvalResult.RealResult(ExactReal.fromDouble(a % b))
          }
          code == "·" -> {
            consume()
            val right = parsePermComb()
            left = dotProduct(left, right)
          }
          code == "∠" -> {
            consume()
            val right = parsePermComb()
            val r = expectRealDouble(left)
            val thetaDeg = toRadians(expectRealDouble(right), ctx.settings.angleUnit)
            left = EvalResult.ComplexResult(
              ExactReal.fromDouble(r * cos(thetaDeg)),
              ExactReal.fromDouble(r * sin(thetaDeg))
            )
          }
          isImplicitMulStart(peek()) -> {
            val right = parsePermComb()
            left = mulResults(left, right)
          }
          else -> break
        }
      }
      return left
    }

    private fun parsePermComb(): EvalResult {
      var left = parseUnary()
      while (true) {
        val code = peekTokenCode()
        if (code == "nPr" || code == "nCr" || code == "P" || code == "C") {
          consume()
          val right = parseUnary()
          val n = expectNonNegInt(left)
          val r = expectNonNegInt(right)
          if (r > n || n > 170) throw CalcMathException()
          val res = if (code == "nPr" || code == "P") perm(n, r) else comb(n, r)
          left = EvalResult.RealResult(res)
        } else break
      }
      return left
    }

    private fun parseUnary(): EvalResult {
      val code = peekTokenCode()
      if (code == "-" || code == "−" || code == "(-)") {
        consume()
        return negResult(parseUnary())
      }
      if (code == "+") {
        consume()
        return parseUnary()
      }
      return parsePowerPostfix()
    }

    private fun parsePowerPostfix(): EvalResult {
      var base = parseAtom()
      while (!isAtEnd()) {
        val next = peek()
        when {
          next is MathElement.Superscript -> {
            consume()
            val expRes = evaluate(next.exponent, ctx)
            base = powResult(base, expRes)
          }
          next is MathElement.Token && next.evalCode == "^" -> {
            consume()
            val expRes = parseUnary()
            base = powResult(base, expRes)
          }
          next is MathElement.Token && next.evalCode == "²" -> {
            consume()
            base = powResult(base, EvalResult.RealResult(ExactReal.fromRational(Rational.of(2))))
          }
          next is MathElement.Token && next.evalCode == "⁻¹" -> {
            consume()
            base = powResult(base, EvalResult.RealResult(ExactReal.fromRational(Rational.of(-1))))
          }
          next is MathElement.Token && next.evalCode == "!" -> {
            consume()
            val n = expectNonNegInt(base)
            if (n > 170) throw CalcMathException()
            base = EvalResult.RealResult(factorialExact(n))
          }
          next is MathElement.Token && next.evalCode == "%" -> {
            consume()
            val div = EvalResult.RealResult(ExactReal.fromRational(Rational.of(100)))
            base = divResults(base, div)
          }
          next is MathElement.Token && next.evalCode == "°" -> {
            consume()
            // Check for DMS chain: deg° min' sec"
            val d = expectRealDouble(base)
            if (isDmsFollower(peekTokenCode())) {
              base = parseDmsChain(d)
            } else {
              // Explicit degree override -> convert degrees to current angle unit
              val inRad = Math.toRadians(d)
              val inCurrent = fromRadians(inRad, ctx.settings.angleUnit)
              base = EvalResult.RealResult(ExactReal.fromDouble(inCurrent))
            }
          }
          next is MathElement.Token && next.evalCode == "ʳ" -> {
            consume()
            val r = expectRealDouble(base)
            val inCurrent = fromRadians(r, ctx.settings.angleUnit)
            base = EvalResult.RealResult(ExactReal.fromDouble(inCurrent))
          }
          next is MathElement.Token && next.evalCode == "ᵍ" -> {
            consume()
            val g = expectRealDouble(base)
            val inRad = g * PI / 200.0
            val inCurrent = fromRadians(inRad, ctx.settings.angleUnit)
            base = EvalResult.RealResult(ExactReal.fromDouble(inCurrent))
          }
          else -> break
        }
      }
      return base
    }

    private fun isDmsFollower(code: String?): Boolean =
      code != null && (code.firstOrNull()?.isDigit() == true || code == "'" || code == "\"")

    private fun parseDmsChain(degrees: Double): EvalResult {
      val sign = if (degrees < 0) -1.0 else 1.0
      var total = abs(degrees)
      if (peek() is MathElement.Token && (peek() as MathElement.Token).kind == TokenKind.DIGIT) {
        val m = expectRealDouble(parseNumberLiteral())
        if (peekTokenCode() == "'" || peekTokenCode() == "°") consume()
        total += m / 60.0
        if (peek() is MathElement.Token && (peek() as MathElement.Token).kind == TokenKind.DIGIT) {
          val s = expectRealDouble(parseNumberLiteral())
          if (peekTokenCode() == "\"" || peekTokenCode() == "°") consume()
          total += s / 3600.0
        }
      }
      return EvalResult.RealResult(ExactReal.fromDouble(sign * total))
    }

    private fun parseAtom(): EvalResult {
      val el = peek() ?: throw CalcSyntaxException()
      return when (el) {
        is MathElement.Fraction -> {
          consume()
          val num = evaluate(el.numerator, ctx)
          val den = evaluate(el.denominator, ctx)
          divResults(num, den)
        }
        is MathElement.MixedFraction -> {
          consume()
          val w = evaluate(el.whole, ctx)
          val n = evaluate(el.numerator, ctx)
          val d = evaluate(el.denominator, ctx)
          val frac = divResults(n, d)
          val wVal = expectRealDouble(w)
          if (wVal < 0) subResults(w, frac) else addResults(w, frac)
        }
        is MathElement.Sqrt -> {
          consume()
          val arg = evaluate(el.radicand, ctx)
          sqrtResult(arg, ctx.mode == CalcMode.COMPLEX)
        }
        is MathElement.NthRoot -> {
          consume()
          val nRes = evaluate(el.index, ctx)
          val aRes = evaluate(el.radicand, ctx)
          val n = expectRealDouble(nRes)
          if (n == 0.0) throw CalcMathException()
          if (aRes is EvalResult.RealResult) {
            val a = aRes.value.toDouble()
            if (a < 0.0) {
              val nInt = round(n).toInt()
              if (abs(n - nInt) < 1e-12 && nInt % 2 != 0) {
                return EvalResult.RealResult(ExactReal.fromDouble(-(-a).pow(1.0 / n)))
              }
              if (ctx.mode == CalcMode.COMPLEX) {
                return powResult(aRes, EvalResult.RealResult(ExactReal.fromDouble(1.0 / n)))
              }
              throw CalcMathException()
            }
          }
          powResult(aRes, divResults(EvalResult.RealResult(ExactReal.ONE), nRes))
        }
        is MathElement.LogBase -> {
          consume()
          val baseVal = expectRealDouble(evaluate(el.base, ctx))
          val argVal = expectRealDouble(evaluate(el.argument, ctx))
          if (baseVal <= 0.0 || baseVal == 1.0 || argVal <= 0.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(ln(argVal) / ln(baseVal)))
        }
        is MathElement.AbsBox -> {
          consume()
          val inner = evaluate(el.inner, ctx)
          absResult(inner)
        }
        is MathElement.Integral -> {
          consume()
          val a = expectRealDouble(evaluate(el.lower, ctx))
          val b = expectRealDouble(evaluate(el.upper, ctx))
          val integral = integrateAdaptive(el.integrand, a, b, ctx)
          EvalResult.RealResult(ExactReal.fromDouble(integral))
        }
        is MathElement.Derivative -> {
          consume()
          val x0 = expectRealDouble(evaluate(el.atValue, ctx))
          val deriv = numericalDerivative(el.expr, x0, ctx)
          EvalResult.RealResult(ExactReal.fromDouble(deriv))
        }
        is MathElement.Summation -> {
          consume()
          val start = round(expectRealDouble(evaluate(el.lower, ctx))).toInt()
          val end = round(expectRealDouble(evaluate(el.upper, ctx))).toInt()
          if (end - start > 50_000) throw CalcMathException()
          var sum = 0.0
          for (k in start..end) {
            val subCtx = ctx.copy(variables = ctx.variables + ("x" to EvalResult.RealResult(ExactReal.fromRational(Rational.of(k)))))
            sum += evaluateReal(el.expr, subCtx)
          }
          EvalResult.RealResult(ExactReal.fromDouble(sum))
        }
        is MathElement.ProductOp -> {
          consume()
          val start = round(expectRealDouble(evaluate(el.lower, ctx))).toInt()
          val end = round(expectRealDouble(evaluate(el.upper, ctx))).toInt()
          if (end - start > 10_000) throw CalcMathException()
          var prod = 1.0
          for (k in start..end) {
            val subCtx = ctx.copy(variables = ctx.variables + ("x" to EvalResult.RealResult(ExactReal.fromRational(Rational.of(k)))))
            prod *= evaluateReal(el.expr, subCtx)
          }
          EvalResult.RealResult(ExactReal.fromDouble(prod))
        }
        is MathElement.Superscript -> throw CalcSyntaxException()
        is MathElement.Token -> parseTokenAtom(el)
      }
    }

    private fun parseTokenAtom(tok: MathElement.Token): EvalResult {
      if (tok.kind == TokenKind.DIGIT || tok.kind == TokenKind.DECIMAL) {
        return parseNumberLiteral()
      }
      consume()
      return when (tok.kind) {
        TokenKind.PAREN_OPEN -> {
          val inner = parseExpression()
          if (peekTokenCode() == ")") consume()
          inner
        }
        TokenKind.FUNC_OPEN -> parseFunctionCall(tok.evalCode)
        TokenKind.CONSTANT -> resolveConstant(tok.evalCode)
        TokenKind.VARIABLE -> resolveVariable(tok.evalCode)
        else -> throw CalcSyntaxException()
      }
    }

    private fun parseNumberLiteral(): EvalResult {
      val sb = StringBuilder()
      while (peek() is MathElement.Token) {
        val t = peek() as MathElement.Token
        if (t.kind == TokenKind.DIGIT || t.kind == TokenKind.DECIMAL) {
          sb.append(t.evalCode)
          consume()
        } else break
      }
      var s = sb.toString()
      if (s == "." || s.count { it == '.' } > 1) throw CalcSyntaxException()
      // Support ×10^ inline scientific literal token if present
      if (peekTokenCode() == "×10^" || peekTokenCode() == "E") {
        consume()
        val expSign = if (peekTokenCode() == "-" || peekTokenCode() == "−" || peekTokenCode() == "(-)") {
          consume()
          "-"
        } else ""
        val expDigits = StringBuilder()
        while (peek() is MathElement.Token && (peek() as MathElement.Token).kind == TokenKind.DIGIT) {
          expDigits.append((consume() as MathElement.Token).evalCode)
        }
        if (expDigits.isEmpty()) throw CalcSyntaxException()
        val valDouble = (s + "e" + expSign + expDigits.toString()).toDoubleOrNull()
          ?: throw CalcSyntaxException()
        return EvalResult.RealResult(ExactReal.fromDouble(valDouble))
      }

      if (s.startsWith(".")) s = "0$s"
      val rat = Rational.fromDecimalString(s)
      return if (rat != null && rat.fitsReasonably()) {
        EvalResult.RealResult(ExactReal.fromRational(rat))
      } else {
        val d = s.toDoubleOrNull() ?: throw CalcSyntaxException()
        EvalResult.RealResult(ExactReal.fromDouble(d))
      }
    }

    private fun parseFunctionCall(funcCode: String): EvalResult {
      val args = mutableListOf<EvalResult>()
      if (peekTokenCode() != ")") {
        args.add(parseExpression())
        while (peekTokenCode() == "," || peekTokenCode() == ";") {
          consume()
          args.add(parseExpression())
        }
      }
      if (peekTokenCode() == ")") consume()

      return evalNamedFunction(funcCode, args)
    }

    private fun evalNamedFunction(code: String, args: List<EvalResult>): EvalResult {
      fun need1(): EvalResult = args.singleOrNull() ?: throw CalcArgumentException()
      fun need2(): Pair<EvalResult, EvalResult> =
        if (args.size == 2) args[0] to args[1] else throw CalcArgumentException()

      return when (code) {
        "sin(" -> {
          val a = need1()
          if (a is EvalResult.ComplexResult) complexSin(a)
          else evalSin(expectReal(a), ctx.settings.angleUnit)
        }
        "cos(" -> {
          val a = need1()
          if (a is EvalResult.ComplexResult) complexCos(a)
          else evalCos(expectReal(a), ctx.settings.angleUnit)
        }
        "tan(" -> {
          val a = need1()
          if (a is EvalResult.ComplexResult) complexTan(a)
          else evalTan(expectReal(a), ctx.settings.angleUnit)
        }
        "sin⁻¹(" -> {
          val x = expectRealDouble(need1())
          if (abs(x) > 1.0) throw CalcMathException()
          evalAsin(x, ctx.settings.angleUnit)
        }
        "cos⁻¹(" -> {
          val x = expectRealDouble(need1())
          if (abs(x) > 1.0) throw CalcMathException()
          evalAcos(x, ctx.settings.angleUnit)
        }
        "tan⁻¹(" -> {
          val x = expectRealDouble(need1())
          evalAtan(x, ctx.settings.angleUnit)
        }
        "sinh(" -> EvalResult.RealResult(ExactReal.fromDouble(sinh(expectRealDouble(need1()))))
        "cosh(" -> EvalResult.RealResult(ExactReal.fromDouble(cosh(expectRealDouble(need1()))))
        "tanh(" -> EvalResult.RealResult(ExactReal.fromDouble(tanh(expectRealDouble(need1()))))
        "sinh⁻¹(" -> EvalResult.RealResult(ExactReal.fromDouble(asinh(expectRealDouble(need1()))))
        "cosh⁻¹(" -> {
          val x = expectRealDouble(need1())
          if (x < 1.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(acosh(x)))
        }
        "tanh⁻¹(" -> {
          val x = expectRealDouble(need1())
          if (abs(x) >= 1.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(atanh(x)))
        }
        "ln(" -> {
          val a = need1()
          if (a is EvalResult.ComplexResult) complexLn(a)
          else {
            val x = expectRealDouble(a)
            if (x <= 0.0) {
              if (x < 0.0 && ctx.mode == CalcMode.COMPLEX) complexLn(EvalResult.ComplexResult(ExactReal.fromDouble(x), ExactReal.ZERO))
              else throw CalcMathException()
            } else EvalResult.RealResult(ExactReal.fromDouble(ln(x)))
          }
        }
        "log(" -> {
          val x = expectRealDouble(need1())
          if (x <= 0.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(log10(x)))
        }
        "Abs(" -> absResult(need1())
        "Arg(" -> {
          val c = toComplexDouble(need1())
          val angRad = atan2(c.second, c.first)
          EvalResult.RealResult(ExactReal.fromDouble(fromRadians(angRad, ctx.settings.angleUnit)))
        }
        "Conjg(" -> {
          val c = toComplex(need1())
          EvalResult.ComplexResult(c.re, -c.im)
        }
        "ReP(" -> EvalResult.RealResult(toComplex(need1()).re)
        "ImP(" -> EvalResult.RealResult(toComplex(need1()).im)
        "Int(" -> EvalResult.RealResult(ExactReal.fromDouble(truncate(expectRealDouble(need1()))))
        "Intg(" -> EvalResult.RealResult(ExactReal.fromDouble(floor(expectRealDouble(need1()))))
        "Rnd(" -> EvalResult.RealResult(ExactReal.fromDouble(round(expectRealDouble(need1()))))
        "GCD(" -> {
          val (a, b) = need2()
          val ia = BigInteger.valueOf(round(expectRealDouble(a)).toLong()).abs()
          val ib = BigInteger.valueOf(round(expectRealDouble(b)).toLong()).abs()
          EvalResult.RealResult(ExactReal.fromRational(Rational(ia.gcd(ib), BigInteger.ONE)))
        }
        "LCM(" -> {
          val (a, b) = need2()
          val ia = BigInteger.valueOf(round(expectRealDouble(a)).toLong()).abs()
          val ib = BigInteger.valueOf(round(expectRealDouble(b)).toLong()).abs()
          if (ia == BigInteger.ZERO || ib == BigInteger.ZERO) EvalResult.RealResult(ExactReal.ZERO)
          else {
            val lcm = (ia / ia.gcd(ib)) * ib
            EvalResult.RealResult(ExactReal.fromRational(Rational(lcm, BigInteger.ONE)))
          }
        }
        "Pol(" -> {
          val (xR, yR) = need2()
          val x = expectRealDouble(xR)
          val y = expectRealDouble(yR)
          val r = hypot(x, y)
          val theta = fromRadians(atan2(y, x), ctx.settings.angleUnit)
          EvalResult.PolarPoint(r, theta, isPol = true)
        }
        "Rec(" -> {
          val (rR, tR) = need2()
          val r = expectRealDouble(rR)
          val thetaRad = toRadians(expectRealDouble(tR), ctx.settings.angleUnit)
          EvalResult.PolarPoint(r * cos(thetaRad), r * sin(thetaRad), isPol = false)
        }
        "RanInt#(" -> {
          val (aR, bR) = need2()
          val lo = round(expectRealDouble(aR)).toInt()
          val hi = round(expectRealDouble(bR)).toInt()
          if (lo > hi) throw CalcArgumentException()
          val v = Random.nextInt(lo, hi + 1)
          EvalResult.RealResult(ExactReal.fromRational(Rational.of(v)))
        }
        "Det(" -> {
          val m = need1() as? EvalResult.MatrixResult ?: throw CalcDimensionException()
          if (m.rows != m.cols) throw CalcDimensionException()
          EvalResult.RealResult(ExactReal.fromDouble(AdvancedMathSolvers.matrixDet(m.data)))
        }
        "Trn(" -> {
          val m = need1() as? EvalResult.MatrixResult ?: throw CalcDimensionException()
          val t = AdvancedMathSolvers.matrixTranspose(m.data)
          EvalResult.MatrixResult(m.cols, m.rows, t)
        }
        "Identity(" -> {
          val n = round(expectRealDouble(need1())).toInt()
          if (n !in 1..4) throw CalcDimensionException()
          val id = List(n) { r -> List(n) { c -> if (r == c) 1.0 else 0.0 } }
          EvalResult.MatrixResult(n, n, id)
        }
        "UnitVct(" -> {
          val v = need1() as? EvalResult.VectorResult ?: throw CalcDimensionException()
          val norm = sqrt(v.data.sumOf { it * it })
          if (norm == 0.0) throw CalcMathException()
          EvalResult.VectorResult(v.data.map { it / norm })
        }
        "Angle(" -> {
          val (a, b) = need2()
          val va = a as? EvalResult.VectorResult ?: throw CalcDimensionException()
          val vb = b as? EvalResult.VectorResult ?: throw CalcDimensionException()
          if (va.data.size != vb.data.size) throw CalcDimensionException()
          val na = sqrt(va.data.sumOf { it * it })
          val nb = sqrt(vb.data.sumOf { it * it })
          if (na == 0.0 || nb == 0.0) throw CalcMathException()
          val dot = va.data.zip(vb.data).sumOf { (x, y) -> x * y }
          val cosT = (dot / (na * nb)).coerceIn(-1.0, 1.0)
          EvalResult.RealResult(ExactReal.fromDouble(fromRadians(acos(cosT), ctx.settings.angleUnit)))
        }
        "NormPD(" -> {
          val x = expectRealDouble(args.getOrElse(0) { throw CalcArgumentException() })
          val sigma = if (args.size >= 2) expectRealDouble(args[1]) else 1.0
          val mu = if (args.size >= 3) expectRealDouble(args[2]) else 0.0
          if (sigma <= 0.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(AdvancedMathSolvers.normalPdf(x, sigma, mu)))
        }
        "NormCD(" -> {
          if (args.size < 2) throw CalcArgumentException()
          val low = expectRealDouble(args[0])
          val high = expectRealDouble(args[1])
          val sigma = if (args.size >= 3) expectRealDouble(args[2]) else 1.0
          val mu = if (args.size >= 4) expectRealDouble(args[3]) else 0.0
          if (sigma <= 0.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(AdvancedMathSolvers.normalCdfRange(low, high, sigma, mu)))
        }
        "InvNorm(" -> {
          val area = expectRealDouble(args.getOrElse(0) { throw CalcArgumentException() })
          val sigma = if (args.size >= 2) expectRealDouble(args[1]) else 1.0
          val mu = if (args.size >= 3) expectRealDouble(args[2]) else 0.0
          if (area <= 0.0 || area >= 1.0 || sigma <= 0.0) throw CalcMathException()
          EvalResult.RealResult(ExactReal.fromDouble(AdvancedMathSolvers.inverseNormalCdf(area, sigma, mu)))
        }
        "f(" -> {
          val arg = need1()
          val body = ctx.fExpr?.takeIf { !it.isEmpty() } ?: throw CalcSyntaxException("f(x) sin definir")
          val subCtx = ctx.copy(variables = ctx.variables + ("x" to arg), recursionDepth = ctx.recursionDepth + 1)
          when (val r = evaluate(body, subCtx)) {
            is EvalResult.ErrorResult -> throw CalcMathException(r.title)
            else -> r
          }
        }
        "g(" -> {
          val arg = need1()
          val body = ctx.gExpr?.takeIf { !it.isEmpty() } ?: throw CalcSyntaxException("g(x) sin definir")
          val subCtx = ctx.copy(variables = ctx.variables + ("x" to arg), recursionDepth = ctx.recursionDepth + 1)
          when (val r = evaluate(body, subCtx)) {
            is EvalResult.ErrorResult -> throw CalcMathException(r.title)
            else -> r
          }
        }
        else -> throw CalcSyntaxException()
      }
    }

    private fun resolveConstant(code: String): EvalResult {
      return when (code) {
        "π" -> EvalResult.RealResult(ExactReal.PiMultiple(SurdSum.ONE))
        "e" -> EvalResult.RealResult(ExactReal.fromDouble(E))
        "i" -> {
          if (ctx.mode != CalcMode.COMPLEX) throw CalcSyntaxException("Modo Complejos requerido")
          EvalResult.ComplexResult(ExactReal.ZERO, ExactReal.ONE)
        }
        "Ran#" -> {
          val r = (Random.nextInt(0, 1000)) / 1000.0
          EvalResult.RealResult(ExactReal.fromDouble(r))
        }
        else -> {
          val sciConst = CatalogData.findConstant(code) ?: throw CalcSyntaxException()
          EvalResult.RealResult(ExactReal.fromDouble(sciConst.value))
        }
      }
    }

    private fun resolveVariable(code: String): EvalResult {
      return when (code) {
        "Ans" -> ctx.ans ?: EvalResult.RealResult(ExactReal.ZERO)
        "PreAns" -> ctx.preAns ?: EvalResult.RealResult(ExactReal.ZERO)
        "MatA", "MatB", "MatC", "MatD", "MatAns" ->
          ctx.matrices[code] ?: throw CalcDimensionException("Matriz vacía")
        "VctA", "VctB", "VctC", "VctD", "VctAns" ->
          ctx.vectors[code] ?: throw CalcDimensionException("Vector vacío")
        "x̄" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.meanX ?: 0.0))
        "Σx" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.sumX ?: 0.0))
        "Σx²" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.sumX2 ?: 0.0))
        "σx" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.sigmaX ?: 0.0))
        "sx" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.sX ?: 0.0))
        "n" -> EvalResult.RealResult(ExactReal.fromDouble((ctx.statSummary?.n ?: 0).toDouble()))
        "ȳ" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.meanY ?: 0.0))
        "Σy" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.sumY ?: 0.0))
        "Σxy" -> EvalResult.RealResult(ExactReal.fromDouble(ctx.statSummary?.sumXY ?: 0.0))
        else -> ctx.variables[code] ?: EvalResult.RealResult(ExactReal.ZERO)
      }
    }

    private fun isImplicitMulStart(el: MathElement?): Boolean {
      if (el == null) return false
      return when (el) {
        is MathElement.Fraction,
        is MathElement.MixedFraction,
        is MathElement.Sqrt,
        is MathElement.NthRoot,
        is MathElement.LogBase,
        is MathElement.AbsBox,
        is MathElement.Integral,
        is MathElement.Derivative,
        is MathElement.Summation,
        is MathElement.ProductOp -> true
        is MathElement.Token -> el.kind == TokenKind.PAREN_OPEN ||
          el.kind == TokenKind.FUNC_OPEN ||
          el.kind == TokenKind.CONSTANT ||
          el.kind == TokenKind.VARIABLE
        is MathElement.Superscript -> false
      }
    }
  }

  // --- Result Arithmetic Helpers ---

  private fun expectReal(res: EvalResult): ExactReal = when (res) {
    is EvalResult.RealResult -> res.value
    is EvalResult.ComplexResult -> {
      if (res.im.isZero) res.re else throw CalcMathException()
    }
    else -> throw CalcMathException()
  }

  private fun expectRealDouble(res: EvalResult): Double = expectReal(res).toDouble()

  private fun expectNonNegInt(res: EvalResult): Int {
    val d = expectRealDouble(res)
    val r = round(d)
    if (d < -1e-9 || abs(d - r) > 1e-9 || r > Int.MAX_VALUE) throw CalcMathException()
    return r.toInt()
  }

  private fun toComplex(res: EvalResult): EvalResult.ComplexResult = when (res) {
    is EvalResult.RealResult -> EvalResult.ComplexResult(res.value, ExactReal.ZERO)
    is EvalResult.ComplexResult -> res
    else -> throw CalcMathException()
  }

  private fun toComplexDouble(res: EvalResult): Pair<Double, Double> {
    val c = toComplex(res)
    return c.re.toDouble() to c.im.toDouble()
  }

  private fun addResults(a: EvalResult, b: EvalResult): EvalResult {
    if (a is EvalResult.MatrixResult && b is EvalResult.MatrixResult) {
      if (a.rows != b.rows || a.cols != b.cols) throw CalcDimensionException()
      val d = List(a.rows) { r -> List(a.cols) { c -> a.data[r][c] + b.data[r][c] } }
      return EvalResult.MatrixResult(a.rows, a.cols, d)
    }
    if (a is EvalResult.VectorResult && b is EvalResult.VectorResult) {
      if (a.data.size != b.data.size) throw CalcDimensionException()
      return EvalResult.VectorResult(a.data.zip(b.data).map { (x, y) -> x + y })
    }
    if (a is EvalResult.ComplexResult || b is EvalResult.ComplexResult) {
      val ca = toComplex(a)
      val cb = toComplex(b)
      return normalizeComplex(ca.re + cb.re, ca.im + cb.im)
    }
    return EvalResult.RealResult(expectReal(a) + expectReal(b))
  }

  private fun subResults(a: EvalResult, b: EvalResult): EvalResult {
    if (a is EvalResult.MatrixResult && b is EvalResult.MatrixResult) {
      if (a.rows != b.rows || a.cols != b.cols) throw CalcDimensionException()
      val d = List(a.rows) { r -> List(a.cols) { c -> a.data[r][c] - b.data[r][c] } }
      return EvalResult.MatrixResult(a.rows, a.cols, d)
    }
    if (a is EvalResult.VectorResult && b is EvalResult.VectorResult) {
      if (a.data.size != b.data.size) throw CalcDimensionException()
      return EvalResult.VectorResult(a.data.zip(b.data).map { (x, y) -> x - y })
    }
    if (a is EvalResult.ComplexResult || b is EvalResult.ComplexResult) {
      val ca = toComplex(a)
      val cb = toComplex(b)
      return normalizeComplex(ca.re - cb.re, ca.im - cb.im)
    }
    return EvalResult.RealResult(expectReal(a) - expectReal(b))
  }

  private fun mulResults(a: EvalResult, b: EvalResult): EvalResult {
    if (a is EvalResult.MatrixResult && b is EvalResult.MatrixResult) {
      if (a.cols != b.rows) throw CalcDimensionException()
      val d = List(a.rows) { r ->
        List(b.cols) { c ->
          var s = 0.0
          for (k in 0 until a.cols) s += a.data[r][k] * b.data[k][c]
          s
        }
      }
      return EvalResult.MatrixResult(a.rows, b.cols, d)
    }
    if (a is EvalResult.RealResult && b is EvalResult.MatrixResult) {
      val k = a.value.toDouble()
      return EvalResult.MatrixResult(b.rows, b.cols, b.data.map { row -> row.map { it * k } })
    }
    if (a is EvalResult.MatrixResult && b is EvalResult.RealResult) {
      val k = b.value.toDouble()
      return EvalResult.MatrixResult(a.rows, a.cols, a.data.map { row -> row.map { it * k } })
    }
    if (a is EvalResult.RealResult && b is EvalResult.VectorResult) {
      val k = a.value.toDouble()
      return EvalResult.VectorResult(b.data.map { it * k })
    }
    if (a is EvalResult.VectorResult && b is EvalResult.RealResult) {
      val k = b.value.toDouble()
      return EvalResult.VectorResult(a.data.map { it * k })
    }
    if (a is EvalResult.VectorResult && b is EvalResult.VectorResult) {
      // Cross product in ClassWiz Vector mode with '×'
      if (a.data.size != 3 || b.data.size != 3) throw CalcDimensionException()
      val (ax, ay, az) = a.data
      val (bx, by, bz) = b.data
      return EvalResult.VectorResult(listOf(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx))
    }
    if (a is EvalResult.ComplexResult || b is EvalResult.ComplexResult) {
      val ca = toComplex(a)
      val cb = toComplex(b)
      val re = ca.re * cb.re - ca.im * cb.im
      val im = ca.re * cb.im + ca.im * cb.re
      return normalizeComplex(re, im)
    }
    return EvalResult.RealResult(expectReal(a) * expectReal(b))
  }

  private fun divResults(a: EvalResult, b: EvalResult): EvalResult {
    if (a is EvalResult.MatrixResult && b is EvalResult.RealResult) {
      val k = b.value.toDouble()
      if (k == 0.0) throw CalcMathException()
      return EvalResult.MatrixResult(a.rows, a.cols, a.data.map { row -> row.map { it / k } })
    }
    if (a is EvalResult.VectorResult && b is EvalResult.RealResult) {
      val k = b.value.toDouble()
      if (k == 0.0) throw CalcMathException()
      return EvalResult.VectorResult(a.data.map { it / k })
    }
    if (a is EvalResult.ComplexResult || b is EvalResult.ComplexResult) {
      val ca = toComplex(a)
      val cb = toComplex(b)
      val denom = cb.re * cb.re + cb.im * cb.im
      if (denom.isZero) throw CalcMathException()
      val re = (ca.re * cb.re + ca.im * cb.im) / denom
      val im = (ca.im * cb.re - ca.re * cb.im) / denom
      return normalizeComplex(re, im)
    }
    return EvalResult.RealResult(expectReal(a) / expectReal(b))
  }

  private fun dotProduct(a: EvalResult, b: EvalResult): EvalResult {
    val va = a as? EvalResult.VectorResult ?: throw CalcDimensionException()
    val vb = b as? EvalResult.VectorResult ?: throw CalcDimensionException()
    if (va.data.size != vb.data.size) throw CalcDimensionException()
    return EvalResult.RealResult(ExactReal.fromDouble(va.data.zip(vb.data).sumOf { (x, y) -> x * y }))
  }

  private fun negResult(a: EvalResult): EvalResult = when (a) {
    is EvalResult.RealResult -> EvalResult.RealResult(-a.value)
    is EvalResult.ComplexResult -> EvalResult.ComplexResult(-a.re, -a.im)
    is EvalResult.MatrixResult -> EvalResult.MatrixResult(a.rows, a.cols, a.data.map { r -> r.map { -it } })
    is EvalResult.VectorResult -> EvalResult.VectorResult(a.data.map { -it })
    else -> throw CalcMathException()
  }

  private fun powResult(base: EvalResult, exp: EvalResult): EvalResult {
    if (base is EvalResult.MatrixResult) {
      if (base.rows != base.cols) throw CalcDimensionException()
      val p = round(expectRealDouble(exp)).toInt()
      if (abs(expectRealDouble(exp) - p) > 1e-9) throw CalcMathException()
      return when {
        p == -1 -> EvalResult.MatrixResult(base.rows, base.cols, AdvancedMathSolvers.matrixInverse(base.data))
        p == 0 -> EvalResult.MatrixResult(base.rows, base.cols, List(base.rows) { r -> List(base.cols) { c -> if (r == c) 1.0 else 0.0 } })
        p in 1..20 -> {
          var acc = base
          repeat(p - 1) { acc = mulResults(acc, base) as EvalResult.MatrixResult }
          acc
        }
        else -> throw CalcMathException()
      }
    }

    if (base is EvalResult.ComplexResult || exp is EvalResult.ComplexResult) {
      val cb = toComplexDouble(base)
      val ce = toComplexDouble(exp)
      val r = hypot(cb.first, cb.second)
      if (r == 0.0) {
        if (ce.first <= 0.0) throw CalcMathException()
        return EvalResult.RealResult(ExactReal.ZERO)
      }
      val theta = atan2(cb.second, cb.first)
      val lnR = ln(r)
      val mag = exp(ce.first * lnR - ce.second * theta)
      val ang = ce.second * lnR + ce.first * theta
      return normalizeComplex(ExactReal.fromDouble(mag * cos(ang)), ExactReal.fromDouble(mag * sin(ang)))
    }

    val bExact = expectReal(base)
    val eExact = expectReal(exp)
    if (bExact is ExactReal.Surd && eExact is ExactReal.Surd) {
      val er = eExact.value.asRationalOrNull()
      if (er != null && er.isInteger) {
        val n = er.num.toLong()
        if (n in -12L..12L) {
          if (n == 0L) return EvalResult.RealResult(ExactReal.ONE)
          val absN = abs(n).toInt()
          var acc: ExactReal = ExactReal.ONE
          repeat(absN) { acc *= bExact }
          return EvalResult.RealResult(if (n < 0) ExactReal.ONE / acc else acc)
        }
      }
    }
    val bd = bExact.toDouble()
    val ed = eExact.toDouble()
    if (bd == 0.0 && ed <= 0.0) throw CalcMathException()
    val res = bd.pow(ed)
    if (res.isNaN() || res.isInfinite()) throw CalcMathException()
    return EvalResult.RealResult(ExactReal.fromDouble(res))
  }

  private fun sqrtResult(arg: EvalResult, allowComplex: Boolean): EvalResult {
    if (arg is EvalResult.ComplexResult) {
      val (re, im) = toComplexDouble(arg)
      val r = hypot(re, im)
      val sr = sqrt((r + re) / 2.0)
      val si = (if (im >= 0) 1.0 else -1.0) * sqrt(max(0.0, (r - re) / 2.0))
      return normalizeComplex(ExactReal.fromDouble(sr), ExactReal.fromDouble(si))
    }
    val r = expectReal(arg)
    if (r is ExactReal.Surd) {
      val rat = r.value.asRationalOrNull()
      if (rat != null) {
        if (rat.isNegative) {
          if (!allowComplex) throw CalcMathException()
          val s = SurdSum.fromRationalSqrt(-rat)
          val im = if (s != null) ExactReal.Surd(s) else ExactReal.fromDouble(sqrt(-rat.toDouble()))
          return EvalResult.ComplexResult(ExactReal.ZERO, im)
        }
        val s = SurdSum.fromRationalSqrt(rat)
        if (s != null) return EvalResult.RealResult(ExactReal.Surd(s))
      }
    }
    val d = r.toDouble()
    if (d < 0.0) {
      if (!allowComplex) throw CalcMathException()
      return EvalResult.ComplexResult(ExactReal.ZERO, ExactReal.fromDouble(sqrt(-d)))
    }
    return EvalResult.RealResult(ExactReal.fromDouble(sqrt(d)))
  }

  private fun absResult(arg: EvalResult): EvalResult = when (arg) {
    is EvalResult.RealResult -> {
      val d = arg.value.toDouble()
      EvalResult.RealResult(if (d < 0) -arg.value else arg.value)
    }
    is EvalResult.ComplexResult -> {
      val (re, im) = toComplexDouble(arg)
      EvalResult.RealResult(ExactReal.fromDouble(hypot(re, im)))
    }
    is EvalResult.VectorResult -> {
      EvalResult.RealResult(ExactReal.fromDouble(sqrt(arg.data.sumOf { it * it })))
    }
    else -> throw CalcMathException()
  }

  private fun normalizeComplex(re: ExactReal, im: ExactReal): EvalResult {
    return if (im.isZero) EvalResult.RealResult(re) else EvalResult.ComplexResult(re, im)
  }

  // --- Trig & Angle Helpers ---

  private fun toRadians(value: Double, unit: AngleUnit): Double = when (unit) {
    AngleUnit.DEG -> Math.toRadians(value)
    AngleUnit.RAD -> value
    AngleUnit.GRAD -> value * PI / 200.0
  }

  private fun fromRadians(rad: Double, unit: AngleUnit): Double = when (unit) {
    AngleUnit.DEG -> Math.toDegrees(rad)
    AngleUnit.RAD -> rad
    AngleUnit.GRAD -> rad * 200.0 / PI
  }

  private fun exactAngleInDegreesOrNull(x: ExactReal, unit: AngleUnit): Double? = when (unit) {
    AngleUnit.DEG -> if (x is ExactReal.Surd) x.value.toDouble() else null
    AngleUnit.RAD -> if (x is ExactReal.PiMultiple) x.coeff.toDouble() * 180.0 else if (x.isZero) 0.0 else null
    AngleUnit.GRAD -> if (x is ExactReal.Surd) x.value.toDouble() * 0.9 else null
  }

  private fun evalSin(x: ExactReal, unit: AngleUnit): EvalResult {
    exactAngleInDegreesOrNull(x, unit)?.let { deg ->
      ExactTrig.sinDegExact(deg)?.let { return EvalResult.RealResult(it) }
    }
    return EvalResult.RealResult(ExactReal.fromDouble(sin(toRadians(x.toDouble(), unit))))
  }

  private fun evalCos(x: ExactReal, unit: AngleUnit): EvalResult {
    exactAngleInDegreesOrNull(x, unit)?.let { deg ->
      ExactTrig.cosDegExact(deg)?.let { return EvalResult.RealResult(it) }
    }
    return EvalResult.RealResult(ExactReal.fromDouble(cos(toRadians(x.toDouble(), unit))))
  }

  private fun evalTan(x: ExactReal, unit: AngleUnit): EvalResult {
    exactAngleInDegreesOrNull(x, unit)?.let { deg ->
      ExactTrig.tanDegExact(deg)?.let { return EvalResult.RealResult(it) }
    }
    val rad = toRadians(x.toDouble(), unit)
    if (abs(cos(rad)) < 1e-14) throw CalcMathException()
    return EvalResult.RealResult(ExactReal.fromDouble(tan(rad)))
  }

  private fun evalAsin(x: Double, unit: AngleUnit): EvalResult {
    val exactDeg: Int? = when {
      abs(x) < 1e-13 -> 0
      abs(x - 0.5) < 1e-13 -> 30
      abs(x + 0.5) < 1e-13 -> -30
      abs(x - 1.0) < 1e-13 -> 90
      abs(x + 1.0) < 1e-13 -> -90
      abs(x - sqrt(2.0) / 2.0) < 1e-13 -> 45
      abs(x + sqrt(2.0) / 2.0) < 1e-13 -> -45
      abs(x - sqrt(3.0) / 2.0) < 1e-13 -> 60
      abs(x + sqrt(3.0) / 2.0) < 1e-13 -> -60
      else -> null
    }
    if (exactDeg != null) {
      return when (unit) {
        AngleUnit.DEG -> EvalResult.RealResult(ExactReal.fromRational(Rational.of(exactDeg)))
        AngleUnit.RAD -> EvalResult.RealResult(ExactReal.PiMultiple(SurdSum.rational(Rational.of(exactDeg, 180))))
        AngleUnit.GRAD -> EvalResult.RealResult(ExactReal.fromRational(Rational.of(exactDeg * 10, 9)))
      }
    }
    return EvalResult.RealResult(ExactReal.fromDouble(fromRadians(asin(x), unit)))
  }

  private fun evalAcos(x: Double, unit: AngleUnit): EvalResult {
    val exactDeg: Int? = when {
      abs(x) < 1e-13 -> 90
      abs(x - 0.5) < 1e-13 -> 60
      abs(x + 0.5) < 1e-13 -> 120
      abs(x - 1.0) < 1e-13 -> 0
      abs(x + 1.0) < 1e-13 -> 180
      abs(x - sqrt(2.0) / 2.0) < 1e-13 -> 45
      abs(x + sqrt(2.0) / 2.0) < 1e-13 -> 135
      abs(x - sqrt(3.0) / 2.0) < 1e-13 -> 30
      abs(x + sqrt(3.0) / 2.0) < 1e-13 -> 150
      else -> null
    }
    if (exactDeg != null) {
      return when (unit) {
        AngleUnit.DEG -> EvalResult.RealResult(ExactReal.fromRational(Rational.of(exactDeg)))
        AngleUnit.RAD -> EvalResult.RealResult(ExactReal.PiMultiple(SurdSum.rational(Rational.of(exactDeg, 180))))
        AngleUnit.GRAD -> EvalResult.RealResult(ExactReal.fromRational(Rational.of(exactDeg * 10, 9)))
      }
    }
    return EvalResult.RealResult(ExactReal.fromDouble(fromRadians(acos(x), unit)))
  }

  private fun evalAtan(x: Double, unit: AngleUnit): EvalResult {
    val exactDeg: Int? = when {
      abs(x) < 1e-13 -> 0
      abs(x - 1.0) < 1e-13 -> 45
      abs(x + 1.0) < 1e-13 -> -45
      abs(x - sqrt(3.0)) < 1e-13 -> 60
      abs(x + sqrt(3.0)) < 1e-13 -> -60
      abs(x - 1.0 / sqrt(3.0)) < 1e-13 -> 30
      abs(x + 1.0 / sqrt(3.0)) < 1e-13 -> -30
      else -> null
    }
    if (exactDeg != null) {
      return when (unit) {
        AngleUnit.DEG -> EvalResult.RealResult(ExactReal.fromRational(Rational.of(exactDeg)))
        AngleUnit.RAD -> EvalResult.RealResult(ExactReal.PiMultiple(SurdSum.rational(Rational.of(exactDeg, 180))))
        AngleUnit.GRAD -> EvalResult.RealResult(ExactReal.fromRational(Rational.of(exactDeg * 10, 9)))
      }
    }
    return EvalResult.RealResult(ExactReal.fromDouble(fromRadians(atan(x), unit)))
  }

  private fun complexSin(c: EvalResult.ComplexResult): EvalResult {
    val a = c.re.toDouble()
    val b = c.im.toDouble()
    return normalizeComplex(ExactReal.fromDouble(sin(a) * cosh(b)), ExactReal.fromDouble(cos(a) * sinh(b)))
  }

  private fun complexCos(c: EvalResult.ComplexResult): EvalResult {
    val a = c.re.toDouble()
    val b = c.im.toDouble()
    return normalizeComplex(ExactReal.fromDouble(cos(a) * cosh(b)), ExactReal.fromDouble(-sin(a) * sinh(b)))
  }

  private fun complexTan(c: EvalResult.ComplexResult): EvalResult =
    divResults(complexSin(c), complexCos(c))

  private fun complexLn(c: EvalResult.ComplexResult): EvalResult {
    val a = c.re.toDouble()
    val b = c.im.toDouble()
    val r = hypot(a, b)
    if (r == 0.0) throw CalcMathException()
    return normalizeComplex(ExactReal.fromDouble(ln(r)), ExactReal.fromDouble(atan2(b, a)))
  }

  private fun factorialExact(n: Int): ExactReal {
    if (n <= 20) {
      var res = java.math.BigInteger.ONE
      for (i in 2..n) res = res.multiply(java.math.BigInteger.valueOf(i.toLong()))
      return ExactReal.fromRational(Rational(res, java.math.BigInteger.ONE))
    }
    var d = 1.0
    for (i in 2..n) d *= i.toDouble()
    return ExactReal.fromDouble(d)
  }

  private fun perm(n: Int, r: Int): ExactReal {
    if (n <= 20) {
      var res = java.math.BigInteger.ONE
      for (i in (n - r + 1)..n) res = res.multiply(java.math.BigInteger.valueOf(i.toLong()))
      return ExactReal.fromRational(Rational(res, java.math.BigInteger.ONE))
    }
    var d = 1.0
    for (i in (n - r + 1)..n) d *= i.toDouble()
    return ExactReal.fromDouble(d)
  }

  private fun comb(n: Int, r: Int): ExactReal {
    val k = min(r, n - r)
    if (n <= 30) {
      var res = java.math.BigInteger.ONE
      for (i in 1..k) {
        res = res.multiply(java.math.BigInteger.valueOf((n - k + i).toLong()))
          .divide(java.math.BigInteger.valueOf(i.toLong()))
      }
      return ExactReal.fromRational(Rational(res, java.math.BigInteger.ONE))
    }
    var d = 1.0
    for (i in 1..k) d = d * (n - k + i).toDouble() / i.toDouble()
    return ExactReal.fromDouble(d)
  }

  // --- Numerical Calculus ---

  private val gkNodes = doubleArrayOf(
    0.0,
    0.20778495500789848, 0.40584515137739717, 0.5860872354676911,
    0.7415311855993945, 0.8648644233597691, 0.9491079123427585, 0.9914553711208126
  )
  private val gkWeights = doubleArrayOf(
    0.20948214108472782,
    0.20443294007529889, 0.19035057806478542, 0.1690047266392679,
    0.14065325971552592, 0.10479001032225018, 0.06309209262997855, 0.022935322010529224
  )
  private val gWeights = doubleArrayOf(
    0.4179591836734694,
    0.0, 0.3818300505051189, 0.0,
    0.27970539148927664, 0.0, 0.1294849661688697, 0.0
  )

  private fun evalIntegrandAt(expr: MathRow, x: Double, ctx: EvalContext): Double {
    val subCtx = ctx.copy(
      variables = ctx.variables + ("x" to EvalResult.RealResult(ExactReal.fromDouble(x)))
    )
    return evaluateReal(expr, subCtx)
  }

  private fun integrateAdaptive(expr: MathRow, a: Double, b: Double, ctx: EvalContext): Double {
    if (a == b) return 0.0
    fun gk15(lo: Double, hi: Double): Pair<Double, Double> {
      val mid = 0.5 * (lo + hi)
      val half = 0.5 * (hi - lo)
      val f0 = evalIntegrandAt(expr, mid, ctx)
      var kSum = gkWeights[0] * f0
      var gSum = gWeights[0] * f0
      for (i in 1..7) {
        val dx = half * gkNodes[i]
        val fSum = evalIntegrandAt(expr, mid - dx, ctx) + evalIntegrandAt(expr, mid + dx, ctx)
        kSum += gkWeights[i] * fSum
        if (gWeights[i] != 0.0) gSum += gWeights[i] * fSum
      }
      return (kSum * half) to (abs(kSum - gSum) * half)
    }

    fun recurse(lo: Double, hi: Double, tol: Double, depth: Int): Double {
      val (est, err) = gk15(lo, hi)
      if (err <= tol || depth >= 8) return est
      val mid = 0.5 * (lo + hi)
      return recurse(lo, mid, tol * 0.5, depth + 1) + recurse(mid, hi, tol * 0.5, depth + 1)
    }

    return recurse(a, b, 1e-9, 0)
  }

  private fun numericalDerivative(expr: MathRow, x0: Double, ctx: EvalContext): Double {
    val h = max(1e-5, abs(x0) * 1e-5)
    val fP1 = evalIntegrandAt(expr, x0 + h, ctx)
    val fM1 = evalIntegrandAt(expr, x0 - h, ctx)
    val fP2 = evalIntegrandAt(expr, x0 + 2 * h, ctx)
    val fM2 = evalIntegrandAt(expr, x0 - 2 * h, ctx)
    return (-fP2 + 8.0 * fP1 - 8.0 * fM1 + fM2) / (12.0 * h)
  }

  private fun findTopLevelVerifyOp(row: MathRow): Pair<Int, String>? {
    var depth = 0
    var found: Pair<Int, String>? = null
    row.items.forEachIndexed { idx, el ->
      if (el is MathElement.Token) {
        when (el.kind) {
          TokenKind.PAREN_OPEN, TokenKind.FUNC_OPEN -> depth++
          TokenKind.PAREN_CLOSE -> if (depth > 0) depth--
          TokenKind.OPERATOR -> {
            if (depth == 0 && (el.evalCode == "=" || el.evalCode == "≠")) {
              if (found != null) throw CalcSyntaxException()
              found = idx to el.evalCode
            }
          }
          else -> {}
        }
      }
    }
    return found
  }

  private fun approxEqual(a: Double, b: Double): Boolean {
    val scale = max(1.0, max(abs(a), abs(b)))
    return abs(a - b) <= 1e-10 * scale
  }

  private fun areResultsEqual(a: EvalResult, b: EvalResult): Boolean {
    return when {
      a is EvalResult.RealResult && b is EvalResult.RealResult ->
        approxEqual(a.value.toDouble(), b.value.toDouble())
      (a is EvalResult.ComplexResult || a is EvalResult.RealResult) &&
        (b is EvalResult.ComplexResult || b is EvalResult.RealResult) -> {
        val ca = toComplexDouble(a)
        val cb = toComplexDouble(b)
        approxEqual(ca.first, cb.first) && approxEqual(ca.second, cb.second)
      }
      a is EvalResult.MatrixResult && b is EvalResult.MatrixResult -> {
        if (a.rows != b.rows || a.cols != b.cols) false
        else a.data.indices.all { r -> a.data[r].indices.all { c -> approxEqual(a.data[r][c], b.data[r][c]) } }
      }
      a is EvalResult.VectorResult && b is EvalResult.VectorResult -> {
        if (a.data.size != b.data.size) false
        else a.data.indices.all { i -> approxEqual(a.data[i], b.data[i]) }
      }
      else -> throw CalcMathException()
    }
  }

  private fun summarizeEvalResult(res: EvalResult, settings: CalcSettings): String = when (res) {
    is EvalResult.RealResult -> formatDoubleClassWiz(res.value.toDouble(), settings)
    is EvalResult.ComplexResult -> "${formatDoubleClassWiz(res.re.toDouble(), settings)}+${formatDoubleClassWiz(res.im.toDouble(), settings)}i"
    is EvalResult.MatrixResult -> "Mat(${res.rows}×${res.cols})"
    is EvalResult.VectorResult -> "Vct(${res.data.size})"
    else -> ""
  }
}
