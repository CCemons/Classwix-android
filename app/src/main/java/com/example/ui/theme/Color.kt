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

// Keycap Colors (Authentic CW Dark Slate & Accent Keys)
val KeyRoundDark = Color(0xFF2D323A)
val KeyRoundDarkTop = Color(0xFF383E48)
val KeyRoundDarkBottom = Color(0xFF23272E)

val KeyNumDark = Color(0xFF363C46)
val KeyNumDarkTop = Color(0xFF424955)
val KeyNumDarkBottom = Color(0xFF2B3038)

val KeyActionBlue = Color(0xFF1F4B7A)
val KeyActionBlueTop = Color(0xFF2A5E96)
val KeyActionBlueBottom = Color(0xFF16385C)

val KeyExeAccent = Color(0xFF495260)
val KeyExeAccentTop = Color(0xFF5A6576)
val KeyExeAccentBottom = Color(0xFF383F4B)

val KeyShiftActive = Color(0xFFD69A2D)
val KeyDPadOuter = Color(0xFF262A31)
val KeyDPadInner = Color(0xFF333842)
val KeyOkTop = Color(0xFF48505E)
val KeyOkBottom = Color(0xFF323842)

// Legend Colors
val LegendPrimary = Color(0xFFF3F5F8)
val LegendSecondaryShift = Color(0xFFE7B657)
val LegendMuted = Color(0xFFAEB6C2)
val BrandWhite = Color(0xFFECEFF4)
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
