package com.example.engine

import java.util.UUID

enum class CalcMode(
  val titleEs: String,
  val titleEn: String,
  val shortTag: String,
) {
  CALCULATE("Calcular", "Calculate", "CALC"),
  STATISTICS("Estadística", "Statistics", "STAT"),
  DISTRIBUTION("Distribución", "Distribution", "DIST"),
  SPREADSHEET("Hoja cálc.", "Spreadsheet", "SHT"),
  TABLE("Tabla", "Table", "TBL"),
  EQUATION("Ecuación", "Equation", "EQN"),
  INEQUALITY("Desigualdad", "Inequality", "INEQ"),
  COMPLEX("Complejos", "Complex", "CMPLX"),
  BASE_N("Base-N", "Base-N", "BASE"),
  MATRIX("Matriz", "Matrix", "MAT"),
  VECTOR("Vector", "Vector", "VCT"),
  RATIO("Proporción", "Ratio", "RATIO"),
  MATH_BOX("Caja Mat.", "Math Box", "MBOX"),
}

enum class AngleUnit(val badge: String, val labelEs: String, val labelEn: String) {
  DEG("D", "Grados", "Degree"),
  RAD("R", "Radianes", "Radian"),
  GRAD("G", "Gradianes", "Gradian"),
}

enum class InputOutputMode(val labelEs: String, val labelEn: String) {
  MATH_MATH("MatE/MatS", "MathI/MathO"),
  MATH_DEC("MatE/DecS", "MathI/DecO"),
  LINE_LINE("LinE/LinS", "LineI/LineO"),
  LINE_DEC("LinE/DecS", "LineI/DecO"),
}

enum class NumberFormatMode {
  NORM_1,
  NORM_2,
  FIX,
  SCI,
}

enum class FractionFormat {
  MIXED,     // ab/c
  IMPROPER,  // d/c
}

enum class ComplexFormat(val labelEs: String, val labelEn: String) {
  RECTANGULAR("a+bi", "a+bi"),
  POLAR("r∠θ", "r∠θ"),
}

enum class BaseNMode(val radix: Int, val label: String) {
  DEC(10, "DEC"),
  HEX(16, "HEX"),
  BIN(2, "BIN"),
  OCT(8, "OCT"),
}

enum class SimplifyMode(val labelEs: String, val labelEn: String) {
  AUTO("Automático", "Auto"),
  MANUAL("Manual", "Manual"),
}

enum class QrVersion(val labelEs: String, val labelEn: String, val minVersion: Int) {
  V3("Versión 3", "Version 3", 3),
  V11("Versión 11", "Version 11", 4),
}

enum class AutoPowerOff(val labelEs: String, val labelEn: String, val minutes: Int) {
  MIN_10("10 min", "10 min", 10),
  MIN_60("60 min", "60 min", 60),
}

enum class LcdThemeMode(val labelEs: String, val labelEn: String) {
  CLASSIC("LCD Clásico", "Classic LCD"),
  PAPER("Papel Blanco", "Paper White"),
}

enum class CalcLanguage(val label: String) {
  ES("Español"),
  EN("English"),
}

data class CalcSettings(
  val angleUnit: AngleUnit = AngleUnit.DEG,
  val inputOutput: InputOutputMode = InputOutputMode.MATH_MATH,
  val numberFormat: NumberFormatMode = NumberFormatMode.NORM_1,
  val fixDigits: Int = 2,
  val sciDigits: Int = 3,
  val fractionFormat: FractionFormat = FractionFormat.IMPROPER,
  val complexFormat: ComplexFormat = ComplexFormat.RECTANGULAR,
  val statFrequency: Boolean = false,
  val digitSeparator: Boolean = false,
  val simplifyMode: SimplifyMode = SimplifyMode.AUTO,
  val qrVersion: QrVersion = QrVersion.V11,
  val autoPowerOff: AutoPowerOff = AutoPowerOff.MIN_10,
  val lcdContrast: Float = 0.85f,
  val lcdTheme: LcdThemeMode = LcdThemeMode.CLASSIC,
  val language: CalcLanguage = CalcLanguage.ES,
  val hapticsEnabled: Boolean = true,
) {
  val isSpanish: Boolean get() = language == CalcLanguage.ES
  val isLinearInput: Boolean get() = inputOutput == InputOutputMode.LINE_LINE || inputOutput == InputOutputMode.LINE_DEC
  val isDecimalOutput: Boolean get() = inputOutput == InputOutputMode.MATH_DEC || inputOutput == InputOutputMode.LINE_DEC
  fun tr(es: String, en: String): String = if (isSpanish) es else en
}

enum class TokenKind {
  DIGIT,
  DECIMAL,
  OPERATOR,
  FUNC_OPEN,
  PAREN_OPEN,
  PAREN_CLOSE,
  CONSTANT,
  VARIABLE,
  POSTFIX,
  SEPARATOR,
  UNIT_CONVERSION,
  LOGIC_OP,
}

sealed class MathElement {
  abstract val id: String

  data class Token(
    val text: String,
    val kind: TokenKind,
    val evalCode: String = text,
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class Fraction(
    val numerator: MathRow = MathRow(),
    val denominator: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class MixedFraction(
    val whole: MathRow = MathRow(),
    val numerator: MathRow = MathRow(),
    val denominator: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class Superscript(
    val exponent: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class Sqrt(
    val radicand: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class NthRoot(
    val index: MathRow = MathRow(),
    val radicand: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class LogBase(
    val base: MathRow = MathRow(),
    val argument: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class AbsBox(
    val inner: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class Integral(
    val integrand: MathRow = MathRow(),
    val lower: MathRow = MathRow(),
    val upper: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class Derivative(
    val expr: MathRow = MathRow(),
    val atValue: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class Summation(
    val expr: MathRow = MathRow(),
    val lower: MathRow = MathRow(),
    val upper: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  data class ProductOp(
    val expr: MathRow = MathRow(),
    val lower: MathRow = MathRow(),
    val upper: MathRow = MathRow(),
    override val id: String = UUID.randomUUID().toString(),
  ) : MathElement()

  fun slots(): List<MathRow> = when (this) {
    is Token -> emptyList()
    is Fraction -> listOf(numerator, denominator)
    is MixedFraction -> listOf(whole, numerator, denominator)
    is Superscript -> listOf(exponent)
    is Sqrt -> listOf(radicand)
    is NthRoot -> listOf(index, radicand)
    is LogBase -> listOf(base, argument)
    is AbsBox -> listOf(inner)
    is Integral -> listOf(integrand, lower, upper)
    is Derivative -> listOf(expr, atValue)
    is Summation -> listOf(lower, upper, expr)
    is ProductOp -> listOf(lower, upper, expr)
  }

  fun replaceSlot(slotIndex: Int, newRow: MathRow): MathElement = when (this) {
    is Token -> this
    is Fraction -> if (slotIndex == 0) copy(numerator = newRow) else copy(denominator = newRow)
    is MixedFraction -> when (slotIndex) {
      0 -> copy(whole = newRow)
      1 -> copy(numerator = newRow)
      else -> copy(denominator = newRow)
    }
    is Superscript -> copy(exponent = newRow)
    is Sqrt -> copy(radicand = newRow)
    is NthRoot -> if (slotIndex == 0) copy(index = newRow) else copy(radicand = newRow)
    is LogBase -> if (slotIndex == 0) copy(base = newRow) else copy(argument = newRow)
    is AbsBox -> copy(inner = newRow)
    is Integral -> when (slotIndex) {
      0 -> copy(integrand = newRow)
      1 -> copy(lower = newRow)
      else -> copy(upper = newRow)
    }
    is Derivative -> if (slotIndex == 0) copy(expr = newRow) else copy(atValue = newRow)
    is Summation -> when (slotIndex) {
      0 -> copy(lower = newRow)
      1 -> copy(upper = newRow)
      else -> copy(expr = newRow)
    }
    is ProductOp -> when (slotIndex) {
      0 -> copy(lower = newRow)
      1 -> copy(upper = newRow)
      else -> copy(expr = newRow)
    }
  }
}

data class MathRow(
  val items: List<MathElement> = emptyList(),
) {
  fun isEmpty(): Boolean = items.isEmpty()

  fun toPlainSummary(): String = items.joinToString("") { el ->
    when (el) {
      is MathElement.Token -> el.text
      is MathElement.Fraction -> "(${el.numerator.toPlainSummary()})/(${el.denominator.toPlainSummary()})"
      is MathElement.MixedFraction -> "${el.whole.toPlainSummary()} ${el.numerator.toPlainSummary()}/${el.denominator.toPlainSummary()}"
      is MathElement.Superscript -> "^(${el.exponent.toPlainSummary()})"
      is MathElement.Sqrt -> "√(${el.radicand.toPlainSummary()})"
      is MathElement.NthRoot -> "${el.index.toPlainSummary()}√(${el.radicand.toPlainSummary()})"
      is MathElement.LogBase -> "log_${el.base.toPlainSummary()}(${el.argument.toPlainSummary()})"
      is MathElement.AbsBox -> "|${el.inner.toPlainSummary()}|"
      is MathElement.Integral -> "∫(${el.integrand.toPlainSummary()}, ${el.lower.toPlainSummary()}, ${el.upper.toPlainSummary()})"
      is MathElement.Derivative -> "d/dx(${el.expr.toPlainSummary()})|${el.atValue.toPlainSummary()}"
      is MathElement.Summation -> "Σ(${el.expr.toPlainSummary()}, ${el.lower.toPlainSummary()}, ${el.upper.toPlainSummary()})"
      is MathElement.ProductOp -> "Π(${el.expr.toPlainSummary()}, ${el.lower.toPlainSummary()}, ${el.upper.toPlainSummary()})"
    }
  }
}

data class PathStep(
  val elementIndex: Int,
  val slotIndex: Int,
)

data class CursorPath(
  val steps: List<PathStep> = emptyList(),
  val index: Int = 0,
)

data class EditorDocument(
  val root: MathRow = MathRow(),
  val cursor: CursorPath = CursorPath(),
)

object MathEditor {

  fun getRowAt(root: MathRow, steps: List<PathStep>): MathRow {
    var current = root
    for (step in steps) {
      val el = current.items.getOrNull(step.elementIndex) ?: return current
      current = el.slots().getOrNull(step.slotIndex) ?: return current
    }
    return current
  }

  private fun updateRowAt(
    root: MathRow,
    steps: List<PathStep>,
    transform: (MathRow) -> MathRow,
  ): MathRow {
    if (steps.isEmpty()) return transform(root)
    val head = steps.first()
    val el = root.items.getOrNull(head.elementIndex) ?: return root
    val targetSlot = el.slots().getOrNull(head.slotIndex) ?: return root
    val updatedSlot = updateRowAt(targetSlot, steps.drop(1), transform)
    val updatedEl = el.replaceSlot(head.slotIndex, updatedSlot)
    val newItems = root.items.toMutableList()
    newItems[head.elementIndex] = updatedEl
    return MathRow(newItems)
  }

  fun insertToken(doc: EditorDocument, token: MathElement.Token, overwrite: Boolean = false): EditorDocument {
    val targetRow = getRowAt(doc.root, doc.cursor.steps)
    val idx = doc.cursor.index.coerceIn(0, targetRow.items.size)
    val newItems = targetRow.items.toMutableList().apply {
      if (overwrite && idx < size) {
        this[idx] = token
      } else {
        add(idx, token)
      }
    }
    val newRoot = updateRowAt(doc.root, doc.cursor.steps) { MathRow(newItems) }
    return EditorDocument(newRoot, doc.cursor.copy(index = idx + 1))
  }

  fun insertTemplate(
    doc: EditorDocument,
    element: MathElement,
    initialSlot: Int = 0,
    pullPrecedingAtom: Boolean = false,
  ): EditorDocument {
    val targetRow = getRowAt(doc.root, doc.cursor.steps)
    val idx = doc.cursor.index.coerceIn(0, targetRow.items.size)

    var insertIdx = idx
    var actualElement = element
    var targetSlotAfter = initialSlot
    val mutableItems = targetRow.items.toMutableList()

    if (pullPrecedingAtom && idx > 0) {
      val pulled = extractPrecedingTerm(mutableItems, idx)
      if (pulled.isNotEmpty() && element is MathElement.Fraction) {
        mutableItems.subList(idx - pulled.size, idx).clear()
        insertIdx = idx - pulled.size
        actualElement = element.copy(numerator = MathRow(pulled))
        targetSlotAfter = 1 // Jump straight to denominator like real ClassWiz!
      }
    }

    mutableItems.add(insertIdx, actualElement)
    val newRoot = updateRowAt(doc.root, doc.cursor.steps) { MathRow(mutableItems) }
    val newSteps = doc.cursor.steps + PathStep(insertIdx, targetSlotAfter)
    val slotRow = actualElement.slots().getOrNull(targetSlotAfter)
    val slotCursorIdx = if (targetSlotAfter == initialSlot) slotRow?.items?.size ?: 0 else 0
    return EditorDocument(newRoot, CursorPath(newSteps, slotCursorIdx))
  }

  private fun extractPrecedingTerm(items: List<MathElement>, cursorIdx: Int): List<MathElement> {
    if (cursorIdx <= 0) return emptyList()
    val last = items[cursorIdx - 1]
    if (last is MathElement.Token &&
      (last.kind == TokenKind.OPERATOR || last.kind == TokenKind.PAREN_OPEN ||
        last.kind == TokenKind.FUNC_OPEN || last.kind == TokenKind.SEPARATOR ||
        last.kind == TokenKind.LOGIC_OP)
    ) {
      return emptyList()
    }
    var start = cursorIdx - 1
    var parenDepth = 0
    while (start >= 0) {
      val cur = items[start]
      if (cur is MathElement.Token) {
        when (cur.kind) {
          TokenKind.PAREN_CLOSE -> parenDepth++
          TokenKind.PAREN_OPEN, TokenKind.FUNC_OPEN -> {
            if (parenDepth > 0) {
              parenDepth--
              if (parenDepth == 0 && cur.kind == TokenKind.FUNC_OPEN) {
                start--
                break
              }
            } else {
              start++
              break
            }
          }
          TokenKind.OPERATOR, TokenKind.SEPARATOR, TokenKind.LOGIC_OP, TokenKind.UNIT_CONVERSION -> {
            if (parenDepth == 0) {
              start++
              break
            }
          }
          else -> {}
        }
      }
      start--
    }
    val safeStart = start.coerceAtLeast(0)
    return if (safeStart < cursorIdx) items.subList(safeStart, cursorIdx).toList() else emptyList()
  }

  fun backspace(doc: EditorDocument): EditorDocument {
    val row = getRowAt(doc.root, doc.cursor.steps)
    val idx = doc.cursor.index.coerceIn(0, row.items.size)
    if (idx > 0) {
      val removing = row.items[idx - 1]
      val newItems = row.items.toMutableList().apply { removeAt(idx - 1) }
      val newRoot = updateRowAt(doc.root, doc.cursor.steps) { MathRow(newItems) }
      // If removing a fraction with content in numerator, restore numerator nicely?
      // Real ClassWiz deletes the whole template or steps into it. Let's unwrap if only numerator has items on Fraction.
      if (removing is MathElement.Fraction && removing.denominator.isEmpty() && !removing.numerator.isEmpty()) {
        val restored = row.items.toMutableList()
        restored.removeAt(idx - 1)
        restored.addAll(idx - 1, removing.numerator.items)
        val restoredRoot = updateRowAt(doc.root, doc.cursor.steps) { MathRow(restored) }
        return EditorDocument(restoredRoot, doc.cursor.copy(index = idx - 1 + removing.numerator.items.size))
      }
      return EditorDocument(newRoot, doc.cursor.copy(index = idx - 1))
    }

    // Cursor is at index 0 inside a slot: move out to parent or unwrap empty slot
    if (doc.cursor.steps.isNotEmpty()) {
      val parentSteps = doc.cursor.steps.dropLast(1)
      val lastStep = doc.cursor.steps.last()
      val parentRow = getRowAt(doc.root, parentSteps)
      val parentEl = parentRow.items.getOrNull(lastStep.elementIndex)
      if (parentEl != null) {
        if (lastStep.slotIndex > 0) {
          val prevSlot = parentEl.slots()[lastStep.slotIndex - 1]
          return EditorDocument(
            doc.root,
            CursorPath(parentSteps + lastStep.copy(slotIndex = lastStep.slotIndex - 1), prevSlot.items.size)
          )
        } else {
          // Delete empty template if all slots empty
          val allEmpty = parentEl.slots().all { it.isEmpty() }
          if (allEmpty) {
            val newParentItems = parentRow.items.toMutableList().apply { removeAt(lastStep.elementIndex) }
            val newRoot = updateRowAt(doc.root, parentSteps) { MathRow(newParentItems) }
            return EditorDocument(newRoot, CursorPath(parentSteps, lastStep.elementIndex))
          }
          // Step just before the element
          return EditorDocument(doc.root, CursorPath(parentSteps, lastStep.elementIndex))
        }
      }
    }
    return doc
  }

  fun moveLeft(doc: EditorDocument): EditorDocument {
    val row = getRowAt(doc.root, doc.cursor.steps)
    val idx = doc.cursor.index.coerceIn(0, row.items.size)
    if (idx > 0) {
      val prevEl = row.items[idx - 1]
      val slots = prevEl.slots()
      if (slots.isNotEmpty()) {
        val lastSlotIdx = slots.lastIndex
        val lastSlot = slots[lastSlotIdx]
        return EditorDocument(
          doc.root,
          CursorPath(doc.cursor.steps + PathStep(idx - 1, lastSlotIdx), lastSlot.items.size)
        )
      }
      return EditorDocument(doc.root, doc.cursor.copy(index = idx - 1))
    }

    if (doc.cursor.steps.isNotEmpty()) {
      val parentSteps = doc.cursor.steps.dropLast(1)
      val lastStep = doc.cursor.steps.last()
      val parentRow = getRowAt(doc.root, parentSteps)
      val parentEl = parentRow.items.getOrNull(lastStep.elementIndex)
      if (parentEl != null && lastStep.slotIndex > 0) {
        val prevSlotIdx = lastStep.slotIndex - 1
        val prevSlot = parentEl.slots()[prevSlotIdx]
        return EditorDocument(
          doc.root,
          CursorPath(parentSteps + PathStep(lastStep.elementIndex, prevSlotIdx), prevSlot.items.size)
        )
      }
      return EditorDocument(doc.root, CursorPath(parentSteps, lastStep.elementIndex))
    }

    // Wrap around to end of root if at very start
    return EditorDocument(doc.root, CursorPath(emptyList(), doc.root.items.size))
  }

  fun moveRight(doc: EditorDocument): EditorDocument {
    val row = getRowAt(doc.root, doc.cursor.steps)
    val idx = doc.cursor.index.coerceIn(0, row.items.size)
    if (idx < row.items.size) {
      val nextEl = row.items[idx]
      val slots = nextEl.slots()
      if (slots.isNotEmpty()) {
        return EditorDocument(
          doc.root,
          CursorPath(doc.cursor.steps + PathStep(idx, 0), 0)
        )
      }
      return EditorDocument(doc.root, doc.cursor.copy(index = idx + 1))
    }

    if (doc.cursor.steps.isNotEmpty()) {
      val parentSteps = doc.cursor.steps.dropLast(1)
      val lastStep = doc.cursor.steps.last()
      val parentRow = getRowAt(doc.root, parentSteps)
      val parentEl = parentRow.items.getOrNull(lastStep.elementIndex)
      if (parentEl != null) {
        val slots = parentEl.slots()
        if (lastStep.slotIndex < slots.lastIndex) {
          return EditorDocument(
            doc.root,
            CursorPath(parentSteps + PathStep(lastStep.elementIndex, lastStep.slotIndex + 1), 0)
          )
        }
      }
      return EditorDocument(doc.root, CursorPath(parentSteps, lastStep.elementIndex + 1))
    }

    // Wrap around to start of root if at very end
    return EditorDocument(doc.root, CursorPath(emptyList(), 0))
  }

  fun moveUp(doc: EditorDocument): EditorDocument? {
    if (doc.cursor.steps.isEmpty()) return null
    val parentSteps = doc.cursor.steps.dropLast(1)
    val lastStep = doc.cursor.steps.last()
    val parentRow = getRowAt(doc.root, parentSteps)
    val el = parentRow.items.getOrNull(lastStep.elementIndex) ?: return null
    val nextSlotIdx: Int? = when (el) {
      is MathElement.Fraction -> if (lastStep.slotIndex == 1) 0 else null
      is MathElement.MixedFraction -> if (lastStep.slotIndex == 2) 1 else null
      is MathElement.Integral -> if (lastStep.slotIndex == 1) 2 else if (lastStep.slotIndex == 0) 2 else null
      is MathElement.Summation -> if (lastStep.slotIndex == 0) 1 else null
      is MathElement.ProductOp -> if (lastStep.slotIndex == 0) 1 else null
      else -> null
    }
    return if (nextSlotIdx != null) {
      val target = el.slots()[nextSlotIdx]
      EditorDocument(
        doc.root,
        CursorPath(parentSteps + PathStep(lastStep.elementIndex, nextSlotIdx), target.items.size)
      )
    } else null
  }

  fun moveDown(doc: EditorDocument): EditorDocument? {
    if (doc.cursor.steps.isEmpty()) return null
    val parentSteps = doc.cursor.steps.dropLast(1)
    val lastStep = doc.cursor.steps.last()
    val parentRow = getRowAt(doc.root, parentSteps)
    val el = parentRow.items.getOrNull(lastStep.elementIndex) ?: return null
    val nextSlotIdx: Int? = when (el) {
      is MathElement.Fraction -> if (lastStep.slotIndex == 0) 1 else null
      is MathElement.MixedFraction -> if (lastStep.slotIndex == 1) 2 else null
      is MathElement.Integral -> if (lastStep.slotIndex == 2) 1 else null
      is MathElement.Summation -> if (lastStep.slotIndex == 1) 0 else null
      is MathElement.ProductOp -> if (lastStep.slotIndex == 1) 0 else null
      else -> null
    }
    return if (nextSlotIdx != null) {
      val target = el.slots()[nextSlotIdx]
      EditorDocument(
        doc.root,
        CursorPath(parentSteps + PathStep(lastStep.elementIndex, nextSlotIdx), target.items.size)
      )
    } else null
  }
}
