package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Casio ClassWiz fx-991LA CW Hardware Chassis & Bezel Colors
val ChassisDarkTop = Color(0xFF282C33)
val ChassisDarkMid = Color(0xFF1E2127)
val ChassisDarkBottom = Color(0xFF17191E)
val ChassisBorderHighlight = Color(0xFF3A404A)
val BezelBlack = Color(0xFF0E1013)
val BezelInnerRim = Color(0xFF2B3038)
val DeskMatBackground = Color(0xFF111317)

// Solar Cell Colors
val SolarFrame = Color(0xFF141619)
val SolarCellBase = Color(0xFF2D211B)
val SolarCellHighlight = Color(0xFF3E2E26)
val SolarGridLine = Color(0xFF1B1310)

// Keycap Colors (Authentic Casio ClassWiz fx-991LA CW Matte Black + Gold SHIFT Palette)
val KeySocketWell = Color(0xFF101216)

val KeyRoundDark = Color(0xFF23262C)
val KeyRoundDarkTop = Color(0xFF2D3138)
val KeyRoundDarkBottom = Color(0xFF1A1D22)
val KeySciBorder = Color(0xFF3E434D)

val KeyNumDark = Color(0xFF3B3F46)
val KeyNumDarkTop = Color(0xFF474C54)
val KeyNumDarkBottom = Color(0xFF30343B)
val KeyNumBorder = Color(0xFF5A606B)

val KeyShiftGoldTop = Color(0xFFD6C28F)
val KeyShiftGoldMid = Color(0xFFBBA36A)
val KeyShiftGoldBottom = Color(0xFF8C7747)
val KeyShiftGoldBorder = Color(0xFFE3D3A6)
val KeyShiftGoldActiveTop = Color(0xFFF0D589)
val KeyShiftGoldIcon = Color(0xFF2B2415)

val KeyShiftActive = Color(0xFFE7BD53)
val KeyDPadOuter = Color(0xFF252930)
val KeyDPadInner = Color(0xFF353B45)
val KeyDPadRingBorder = Color(0xFF4D5666)
val KeyDPadDivider = Color(0xFF1B1E24)
val KeyOkTop = Color(0xFF353941)
val KeyOkBottom = Color(0xFF22252B)
val KeyOkBorder = Color(0xFF4C525E)

// Legend Colors
val LegendPrimary = Color(0xFFF2F4F7)
val LegendSecondaryShift = Color(0xFFE7C259)
val LegendMuted = Color(0xFFC2C7D0)
val BrandWhite = Color(0xFFF0F2F5)
val BrandCyan = Color(0xFF67C4DC)

// 4-Level Grayscale LCD Palettes
data class LcdPalette(
  val background: Color,
  val backgroundShade: Color,
  val inkDarkest: Color,    // Level 4: primary text, symbols, active selection fill
  val inkDark: Color,       // Level 3: secondary labels, borders
  val inkMid: Color,        // Level 2: subtle icons, dividers
  val inkLight: Color,      // Level 1: grid cells, inactive track
  val bezelShadow: Color,
)

val ClassicClassWizLcd = LcdPalette(
  background = Color(0xFFDCE3D3),
  backgroundShade = Color(0xFFCFD8C5),
  inkDarkest = Color(0xFF141914),
  inkDark = Color(0xFF434C43),
  inkMid = Color(0xFF7D887C),
  inkLight = Color(0xFFB5C0B2),
  bezelShadow = Color(0xFF9EA99A),
)

val PaperWhiteLcd = LcdPalette(
  background = Color(0xFFF1F3EF),
  backgroundShade = Color(0xFFE4E7E1),
  inkDarkest = Color(0xFF111312),
  inkDark = Color(0xFF464A47),
  inkMid = Color(0xFF848985),
  inkLight = Color(0xFFC5CAC3),
  bezelShadow = Color(0xFFB2B8B0),
)
