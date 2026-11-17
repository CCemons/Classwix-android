package com.example.engine

/**
 * Generates an authentic ISO/IEC 18004 Byte-mode ECC-L QR matrix (Versions 1..4: 21x21 to 33x33)
 * with finder patterns, alignment patterns (v2..4), timing strips, Reed-Solomon ECC, and data masking.
 */
object QrEncoder {

  private data class VersionSpec(
    val version: Int,
    val size: Int,
    val dataCodewords: Int,
    val eccCodewords: Int,
    val alignPos: Int?,
  )

  private val specs = listOf(
    VersionSpec(1, 21, 19, 7, null),
    VersionSpec(2, 25, 34, 10, 18),
    VersionSpec(3, 29, 55, 15, 22),
    VersionSpec(4, 33, 80, 20, 26),
  )

  fun encode(text: String): List<List<Boolean>> {
    val rawBytes = text.toByteArray(Charsets.UTF_8)
    val spec = specs.firstOrNull { rawBytes.size <= it.dataCodewords - 2 } ?: specs.last()
    val maxBytes = spec.dataCodewords - 2
    val bytes = if (rawBytes.size > maxBytes) rawBytes.copyOf(maxBytes) else rawBytes

    // Build bit stream: Mode 0100 (4b) + CharCount (8b) + payload + terminator (0000) + padding
    val bits = mutableListOf<Int>()
    fun pushBits(value: Int, count: Int) {
      for (i in (count - 1) downTo 0) bits.add((value ushr i) and 1)
    }

    pushBits(0b0100, 4) // Byte mode
    pushBits(bytes.size, 8)
    for (b in bytes) pushBits(b.toInt() and 0xFF, 8)

    val maxBits = spec.dataCodewords * 8
    val termLen = minOf(4, maxBits - bits.size)
    repeat(termLen) { bits.add(0) }
    while (bits.size % 8 != 0) bits.add(0)

    val dataCw = IntArray(spec.dataCodewords)
    var cwIdx = 0
    var bitIdx = 0
    while (bitIdx + 7 < bits.size && cwIdx < spec.dataCodewords) {
      var v = 0
      for (k in 0 until 8) v = (v shl 1) or bits[bitIdx + k]
      dataCw[cwIdx++] = v
      bitIdx += 8
    }
    var padToggle = true
    while (cwIdx < spec.dataCodewords) {
      dataCw[cwIdx++] = if (padToggle) 0xEC else 0x11
      padToggle = !padToggle
    }

    val eccCw = reedSolomonRemainder(dataCw, spec.eccCodewords)
    val allCodewords = IntArray(dataCw.size + eccCw.size)
    dataCw.copyInto(allCodewords, 0)
    eccCw.copyInto(allCodewords, dataCw.size)

    val n = spec.size
    val matrix = Array(n) { BooleanArray(n) }
    val isFunction = Array(n) { BooleanArray(n) }

    fun setFn(r: Int, c: Int, dark: Boolean) {
      if (r in 0 until n && c in 0 until n) {
        matrix[r][c] = dark
        isFunction[r][c] = true
      }
    }

    // Finder patterns + separators
    fun drawFinder(top: Int, left: Int) {
      for (dr in -1..7) {
        for (dc in -1..7) {
          val r = top + dr
          val c = left + dc
          if (r !in 0 until n || c !in 0 until n) continue
          val dark = if (dr in 0..6 && dc in 0..6) {
            val dist = maxOf(kotlin.math.abs(dr - 3), kotlin.math.abs(dc - 3))
            dist != 2
          } else false
          setFn(r, c, dark)
        }
      }
    }
    drawFinder(0, 0)
    drawFinder(0, n - 7)
    drawFinder(n - 7, 0)

    // Alignment pattern for v2..v4
    spec.alignPos?.let { ap ->
      for (dr in -2..2) {
        for (dc in -2..2) {
          val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
          setFn(ap + dr, ap + dc, dist != 1)
        }
      }
    }

    // Timing patterns
    for (i in 8 until n - 8) {
      val dark = i % 2 == 0
      if (!isFunction[6][i]) setFn(6, i, dark)
      if (!isFunction[i][6]) setFn(i, 6, dark)
    }

    // Dark module & reserve format info areas
    setFn(n - 8, 8, true)
    for (i in 0..8) {
      if (i != 6) {
        if (!isFunction[8][i]) setFn(8, i, false)
        if (!isFunction[i][8]) setFn(i, 8, false)
      }
    }
    for (i in 0..7) {
      if (!isFunction[8][n - 1 - i]) setFn(8, n - 1 - i, false)
      if (!isFunction[n - 1 - i][8]) setFn(n - 1 - i, 8, false)
    }

    // Place data + ECC bits in zig-zag order
    val allBits = IntArray(allCodewords.size * 8)
    for (i in allCodewords.indices) {
      val b = allCodewords[i]
      for (k in 0..7) allBits[i * 8 + k] = (b ushr (7 - k)) and 1
    }

    var idx = 0
    var up = true
    var col = n - 1
    while (col >= 1) {
      if (col == 6) col--
      val rows = if (up) (n - 1) downTo 0 else 0 until n
      for (row in rows) {
        for (dx in 0..1) {
          val c = col - dx
          if (!isFunction[row][c]) {
            val bit = if (idx < allBits.size) allBits[idx++] == 1 else false
            // Mask pattern 0: (row + col) % 2 == 0
            val masked = if ((row + c) % 2 == 0) !bit else bit
            matrix[row][c] = masked
          }
        }
      }
      up = !up
      col -= 2
    }

    // Write format info for ECC Level L (01) & Mask 0 (000) -> 0x77C4 (with BCH + 0x5412 XOR)
    val formatBits = 0b111011111000100
    for (i in 0..5) setFn(8, i, ((formatBits ushr i) and 1) == 1)
    setFn(8, 7, ((formatBits ushr 6) and 1) == 1)
    setFn(8, 8, ((formatBits ushr 7) and 1) == 1)
    setFn(7, 8, ((formatBits ushr 8) and 1) == 1)
    for (i in 9..14) setFn(14 - i, 8, ((formatBits ushr i) and 1) == 1)

    for (i in 0..7) setFn(n - 1 - i, 8, ((formatBits ushr i) and 1) == 1)
    for (i in 8..14) setFn(8, n - 15 + i, ((formatBits ushr i) and 1) == 1)

    return matrix.map { it.toList() }
  }

  private fun reedSolomonRemainder(data: IntArray, eccCount: Int): IntArray {
    // Compute generator polynomial in GF(256) with 0x11D
    var gen = intArrayOf(1)
    for (i in 0 until eccCount) {
      val root = gfPow(i)
      val next = IntArray(gen.size + 1)
      for (j in gen.indices) {
        next[j] = next[j] xor gen[j]
        next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
      }
      gen = next
    }
    val res = IntArray(eccCount)
    for (b in data) {
      val factor = b xor res[0]
      System.arraycopy(res, 1, res, 0, eccCount - 1)
      res[eccCount - 1] = 0
      for (j in 0 until eccCount) {
        res[j] = res[j] xor gfMul(gen[j + 1], factor)
      }
    }
    return res
  }

  private fun gfMul(x: Int, y: Int): Int {
    var a = x
    var b = y
    var z = 0
    repeat(8) {
      if ((b and 1) != 0) z = z xor a
      val hi = a and 0x80
      a = (a shl 1) and 0xFF
      if (hi != 0) a = a xor 0x1D
      b = b ushr 1
    }
    return z
  }

  private fun gfPow(exp: Int): Int {
    var r = 1
    repeat(exp) { r = gfMul(r, 2) }
    return r
  }
}
