package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
val InterFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val IbmPlexSansFamily = FontFamily(Font(R.font.ibm_plex_sans, FontWeight.Normal))

val ClassWizTypography = Typography(
  displayMedium = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 28.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = IbmPlexSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 12.sp,
  ),
)
