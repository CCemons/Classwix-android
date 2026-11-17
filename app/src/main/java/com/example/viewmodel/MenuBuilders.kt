package com.example.viewmodel

import com.example.engine.*

object MenuBuilders {

  fun buildCatalogRoot(state: CalculatorUiState): List<CatalogEntry> {
    val list = mutableListOf<CatalogEntry>()

    // Mode-specific top entries just like real fx-991CW!
    when (state.mode) {
      CalcMode.COMPLEX -> {
        list += CatalogEntry.SubCategory(
          "Complejos", "Complex", "Arg, Conjg, ReP, ImP, r∠θ",
          listOf(
            CatalogEntry.InsertAction("Arg( )", "Arg( )", "Argumento", "fn:Arg("),
            CatalogEntry.InsertAction("Conjg( )", "Conjg( )", "Conjugado", "fn:Conjg("),
            CatalogEntry.InsertAction("ReP( )", "ReP( )", "Parte real", "fn:ReP("),
            CatalogEntry.InsertAction("ImP( )", "ImP( )", "Parte imaginaria", "fn:ImP("),
            CatalogEntry.InsertAction("i (unidad imag.)", "i (imag. unit)", "", "const:i"),
            CatalogEntry.InsertAction("∠ (ángulo polar)", "∠ (polar angle)", "", "op:∠"),
            CatalogEntry.InsertAction("▶r∠θ", "▶r∠θ", "Forma polar", "fmt:POLAR"),
            CatalogEntry.InsertAction("▶a+bi", "▶a+bi", "Forma rectangular", "fmt:RECTANGULAR"),
          )
        )
      }
      CalcMode.MATRIX -> {
        list += CatalogEntry.SubCategory(
          "Matriz", "Matrix", "MatA..D, Det, Trn, Identidad",
          listOf(
            CatalogEntry.InsertAction("MatA", "MatA", "", "var:MatA"),
            CatalogEntry.InsertAction("MatB", "MatB", "", "var:MatB"),
            CatalogEntry.InsertAction("MatC", "MatC", "", "var:MatC"),
            CatalogEntry.InsertAction("MatD", "MatD", "", "var:MatD"),
            CatalogEntry.InsertAction("MatAns", "MatAns", "", "var:MatAns"),
            CatalogEntry.InsertAction("Det( )", "Det( )", "Determinante", "fn:Det("),
            CatalogEntry.InsertAction("Trn( )", "Trn( )", "Transpuesta", "fn:Trn("),
            CatalogEntry.InsertAction("Identity( )", "Identity( )", "Matriz identidad", "fn:Identity("),
          )
        )
      }
      CalcMode.VECTOR -> {
        list += CatalogEntry.SubCategory(
          "Vector", "Vector", "VctA..D, ·, ×, UnitVct, Angle",
          listOf(
            CatalogEntry.InsertAction("VctA", "VctA", "", "var:VctA"),
            CatalogEntry.InsertAction("VctB", "VctB", "", "var:VctB"),
            CatalogEntry.InsertAction("VctC", "VctC", "", "var:VctC"),
            CatalogEntry.InsertAction("VctD", "VctD", "", "var:VctD"),
            CatalogEntry.InsertAction("VctAns", "VctAns", "", "var:VctAns"),
            CatalogEntry.InsertAction("· (Prod. escalar)", "· (Dot Product)", "", "op:·"),
            CatalogEntry.InsertAction("Abs( ) (Módulo)", "Abs( ) (Magnitude)", "", "tpl:abs"),
            CatalogEntry.InsertAction("UnitVct( )", "UnitVct( )", "Vector unitario", "fn:UnitVct("),
            CatalogEntry.InsertAction("Angle( , )", "Angle( , )", "Ángulo entre vect.", "fn:Angle("),
          )
        )
      }
      CalcMode.BASE_N -> {
        list += CatalogEntry.SubCategory(
          "Base-N", "Base-N", "HEX, Lógica, Prefijos",
          listOf(
            CatalogEntry.InsertAction("and", "and", "Y lógico bit a bit", "logic:and"),
            CatalogEntry.InsertAction("or", "or", "O lógico bit a bit", "logic:or"),
            CatalogEntry.InsertAction("xor", "xor", "O exclusivo", "logic:xor"),
            CatalogEntry.InsertAction("xnor", "xnor", "NOR exclusivo", "logic:xnor"),
            CatalogEntry.InsertAction("Not( )", "Not( )", "Complemento bit", "fn:Not("),
            CatalogEntry.InsertAction("Neg( )", "Neg( )", "Negación a 2", "fn:Neg("),
            CatalogEntry.InsertAction("A (Hex 10)", "A (Hex 10)", "", "hex:A"),
            CatalogEntry.InsertAction("B (Hex 11)", "B (Hex 11)", "", "hex:B"),
            CatalogEntry.InsertAction("C (Hex 12)", "C (Hex 12)", "", "hex:C"),
            CatalogEntry.InsertAction("D (Hex 13)", "D (Hex 13)", "", "hex:D"),
            CatalogEntry.InsertAction("E (Hex 14)", "E (Hex 14)", "", "hex:E"),
            CatalogEntry.InsertAction("F (Hex 15)", "F (Hex 15)", "", "hex:F"),
            CatalogEntry.InsertAction("d (prefijo DEC)", "d (DEC prefix)", "", "hex:d"),
            CatalogEntry.InsertAction("h (prefijo HEX)", "h (HEX prefix)", "", "hex:h"),
            CatalogEntry.InsertAction("b (prefijo BIN)", "b (BIN prefix)", "", "hex:b"),
            CatalogEntry.InsertAction("o (prefijo OCT)", "o (OCT prefix)", "", "hex:o"),
          )
        )
        return list
      }
      else -> {}
    }

    list += CatalogEntry.SubCategory(
      "Análisis func.", "Func Analysis", "∫, d/dx, Σ, Π, |x|, log",
      listOf(
        CatalogEntry.InsertAction("∫(f(x))dx", "∫(f(x))dx", "Integral definida", "tpl:integral"),
        CatalogEntry.InsertAction("d/dx(f(x))|x=a", "d/dx(f(x))|x=a", "Derivada numérica", "tpl:deriv"),
        CatalogEntry.InsertAction("Σ(f(x))", "Σ(f(x))", "Sumatoria", "tpl:sum"),
        CatalogEntry.InsertAction("Π(f(x))", "Π(f(x))", "Productoria", "tpl:prod"),
        CatalogEntry.InsertAction("|x| (Valor abs.)", "|x| (Abs)", "Valor absoluto", "tpl:abs"),
        CatalogEntry.InsertAction("log_a(b)", "log_a(b)", "Logaritmo base a", "tpl:logbase"),
        CatalogEntry.InsertAction("f( )", "f( )", "Función f(x)", "fn:f("),
        CatalogEntry.InsertAction("g( )", "g( )", "Función g(x)", "fn:g("),
      )
    )

    list += CatalogEntry.SubCategory(
      "Probabilidad", "Probability", "x!, nPr, nCr, Ran#, Norm",
      listOf(
        CatalogEntry.InsertAction("x! (Factorial)", "x! (Factorial)", "", "post:!"),
        CatalogEntry.InsertAction("nPr (Permutación)", "nPr (Permutation)", "", "op:nPr"),
        CatalogEntry.InsertAction("nCr (Combinación)", "nCr (Combination)", "", "op:nCr"),
        CatalogEntry.InsertAction("Ran# (Aleatorio 0-1)", "Ran# (Random)", "", "const:Ran#"),
        CatalogEntry.InsertAction("RanInt#(a, b)", "RanInt#(a, b)", "Entero aleatorio", "fn:RanInt#("),
        CatalogEntry.InsertAction("NormPD(x,σ,μ)", "NormPD(x,σ,μ)", "Densidad normal", "fn:NormPD("),
        CatalogEntry.InsertAction("NormCD(a,b,σ,μ)", "NormCD(a,b,σ,μ)", "Acumulada normal", "fn:NormCD("),
        CatalogEntry.InsertAction("InvNorm(p,σ,μ)", "InvNorm(p,σ,μ)", "Normal inversa", "fn:InvNorm("),
      )
    )

    list += CatalogEntry.SubCategory(
      "Cálc. numérico", "Numeric Calc", "GCD, LCM, Mod, Int, Rnd",
      listOf(
        CatalogEntry.InsertAction("GCD(a, b)", "GCD(a, b)", "Máx. común divisor", "fn:GCD("),
        CatalogEntry.InsertAction("LCM(a, b)", "LCM(a, b)", "Mín. común múltiplo", "fn:LCM("),
        CatalogEntry.InsertAction("Mod (Residuo)", "Mod (Remainder)", "División módulo", "op:Mod"),
        CatalogEntry.InsertAction("Int(x)", "Int(x)", "Parte entera", "fn:Int("),
        CatalogEntry.InsertAction("Intg(x)", "Intg(x)", "Máximo entero ≤ x", "fn:Intg("),
        CatalogEntry.InsertAction("Rnd(x)", "Rnd(x)", "Redondeo", "fn:Rnd("),
      )
    )

    list += CatalogEntry.SubCategory(
      "Ángulo / Coord", "Angle / Coord", "°, ʳ, ᵍ, Pol, Rec",
      listOf(
        CatalogEntry.InsertAction("° (Grados)", "° (Degrees)", "", "post:°"),
        CatalogEntry.InsertAction("ʳ (Radianes)", "ʳ (Radians)", "", "post:ʳ"),
        CatalogEntry.InsertAction("ᵍ (Gradianes)", "ᵍ (Gradians)", "", "post:ᵍ"),
        CatalogEntry.InsertAction("Pol(x, y)", "Pol(x, y)", "Rect. a Polar", "fn:Pol("),
        CatalogEntry.InsertAction("Rec(r, θ)", "Rec(r, θ)", "Polar a Rect.", "fn:Rec("),
      )
    )

    list += CatalogEntry.SubCategory(
      "Hiperbólicas", "Hyperbolic", "sinh, cosh, tanh, inv.",
      listOf(
        CatalogEntry.InsertAction("sinh( )", "sinh( )", "", "fn:sinh("),
        CatalogEntry.InsertAction("cosh( )", "cosh( )", "", "fn:cosh("),
        CatalogEntry.InsertAction("tanh( )", "tanh( )", "", "fn:tanh("),
        CatalogEntry.InsertAction("sinh⁻¹( )", "sinh⁻¹( )", "", "fn:sinh⁻¹("),
        CatalogEntry.InsertAction("cosh⁻¹( )", "cosh⁻¹( )", "", "fn:cosh⁻¹("),
        CatalogEntry.InsertAction("tanh⁻¹( )", "tanh⁻¹( )", "", "fn:tanh⁻¹("),
      )
    )

    list += buildConstantsCategory(state.settings.isSpanish)
    list += buildConversionsCategory(state.settings.isSpanish)

    return list
  }

  fun buildConstantsCategory(isSpanish: Boolean): CatalogEntry.SubCategory {
    val grouped = CatalogData.constants.groupBy { if (isSpanish) it.categoryEs else it.categoryEn }
    val subCats = grouped.map { (catName, items) ->
      CatalogEntry.SubCategory(
        catName, catName, "${items.size} const.",
        items.map { c ->
          CatalogEntry.InsertAction(
            "${c.symbol}: ${c.nameEs}",
            "${c.symbol}: ${c.nameEn}",
            "${c.value} ${c.unit}",
            "const:${c.symbol}"
          )
        }
      )
    }
    return CatalogEntry.SubCategory("Const. científicas", "Sci Constants", "CODATA (35)", subCats)
  }

  fun buildConversionsCategory(isSpanish: Boolean): CatalogEntry.SubCategory {
    val grouped = CatalogData.conversions.groupBy { if (isSpanish) it.categoryEs else it.categoryEn }
    val subCats = grouped.map { (catName, items) ->
      CatalogEntry.SubCategory(
        catName, catName, "${items.size} conv.",
        items.map { u ->
          CatalogEntry.InsertAction(u.label, u.label, catName, "conv:${u.label}")
        }
      )
    }
    return CatalogEntry.SubCategory("Conv. unidades", "Unit Conversions", "35 unidades", subCats)
  }

  fun buildToolsMenu(state: CalculatorUiState): List<CatalogEntry> {
    return when (state.mode) {
      CalcMode.BASE_N -> listOf(
        CatalogEntry.InsertAction("Cambiar a DEC", "Switch to DEC", "Base 10", "base:DEC"),
        CatalogEntry.InsertAction("Cambiar a HEX", "Switch to HEX", "Base 16", "base:HEX"),
        CatalogEntry.InsertAction("Cambiar a BIN", "Switch to BIN", "Base 2", "base:BIN"),
        CatalogEntry.InsertAction("Cambiar a OCT", "Switch to OCT", "Base 8", "base:OCT"),
      )
      CalcMode.MATRIX -> listOf(
        CatalogEntry.InsertAction("Editar Matriz", "Edit Matrix", "MatA / MatB / MatC / MatD", "tool:edit_matrix"),
        CatalogEntry.InsertAction("Volver a cálculo Mat", "Matrix Calc", "Operar matrices", "tool:work_matrix"),
      )
      CalcMode.VECTOR -> listOf(
        CatalogEntry.InsertAction("Editar Vector", "Edit Vector", "VctA / VctB / VctC / VctD", "tool:edit_vector"),
        CatalogEntry.InsertAction("Volver a cálculo Vct", "Vector Calc", "Operar vectores", "tool:work_vector"),
      )
      CalcMode.TABLE -> listOf(
        CatalogEntry.InsertAction("Editar f(x)", "Edit f(x)", "", "tool:table_f"),
        CatalogEntry.InsertAction("Editar g(x)", "Edit g(x)", "", "tool:table_g"),
        CatalogEntry.InsertAction("Rango de tabla", "Table Range", "Inicio / Fin / Paso", "tool:table_range"),
        CatalogEntry.InsertAction("Generar tabla", "Generate Table", "", "tool:table_exec"),
      )
      CalcMode.STATISTICS -> listOf(
        CatalogEntry.InsertAction("Ver Resultados", "View Results", "x̄, σx, regresión", "tool:stat_results"),
        CatalogEntry.InsertAction("Editar Datos", "Edit Data", "Tabla x / y", "tool:stat_data"),
        CatalogEntry.InsertAction("Cambiar Tipo Est.", "Change Stat Type", "1-Var / Regresiones", "tool:stat_type"),
        CatalogEntry.InsertAction("Frecuencia Sí/No", "Frequency On/Off", "", "tool:stat_freq"),
      )
      CalcMode.EQUATION -> listOf(
        CatalogEntry.InsertAction("Tipo de ecuación", "Equation Type", "Simul / Polinómica / Solver", "tool:eq_menu"),
        CatalogEntry.InsertAction("Ir a Solver", "Open Solver", "Resolver ecuación f(x)=g(x)", "tool:eq_solver"),
        CatalogEntry.InsertAction("Guardar en Variable", "Store to Variable", "A–F, x, y, z", "tool:store"),
        CatalogEntry.InsertAction("Generar código QR", "Generate QR Code", "SHIFT + x", "tool:qr"),
      )
      CalcMode.SPREADSHEET -> listOf(
        CatalogEntry.InsertAction("Insertar =SUM(A1:A5)", "Insert =SUM(A1:A5)", "Suma de rango", "tool:sheet_sum"),
        CatalogEntry.InsertAction("Insertar =MEAN(A1:A5)", "Insert =MEAN(A1:A5)", "Promedio de rango", "tool:sheet_mean"),
        CatalogEntry.InsertAction("Borrar hoja", "Clear Sheet", "Vaciar celdas", "tool:sheet_clear"),
      )
      else -> listOf(
        CatalogEntry.InsertAction("Deshacer edición", "Undo Edit", "Ctrl+Z", "tool:undo"),
        CatalogEntry.InsertAction("Guardar en Variable", "Store to Variable", "A–F, x, y, z", "tool:store"),
        CatalogEntry.InsertAction("Definir f(x) / g(x)", "Define f(x) / g(x)", "Funciones", "tool:fx_menu"),
        CatalogEntry.InsertAction("Generar código QR", "Generate QR Code", "SHIFT + x", "tool:qr"),
        CatalogEntry.InsertAction("Limpiar historial", "Clear History", "", "tool:clear_hist"),
      )
    }
  }

  fun buildFxMenu(state: CalculatorUiState): List<CatalogEntry> {
    val fDefined = if (state.tableState.fDoc.root.isEmpty()) "∅" else state.settings.tr("Definida", "Defined")
    val gDefined = if (state.tableState.gDoc.root.isEmpty()) "∅" else state.settings.tr("Definida", "Defined")
    return listOf(
      CatalogEntry.InsertAction("Definir f(x)", "Define f(x)", fDefined, "fx:def_f"),
      CatalogEntry.InsertAction("Definir g(x)", "Define g(x)", gDefined, "fx:def_g"),
      CatalogEntry.InsertAction("f( )", "f( )", state.settings.tr("Evaluar f", "Evaluate f"), "fn:f("),
      CatalogEntry.InsertAction("g( )", "g( )", state.settings.tr("Evaluar g", "Evaluate g"), "fn:g("),
    )
  }

  fun buildFormatMenu(state: CalculatorUiState): List<CatalogEntry> {
    if (state.mode == CalcMode.COMPLEX) {
      return listOf(
        CatalogEntry.InsertAction("Rectangular (a+bi)", "Rectangular (a+bi)", "", "fmt:RECTANGULAR"),
        CatalogEntry.InsertAction("Polar (r∠θ)", "Polar (r∠θ)", "", "fmt:POLAR"),
        CatalogEntry.InsertAction("Decimal", "Decimal", "", "fmt:DECIMAL"),
        CatalogEntry.InsertAction("Estándar", "Standard", "", "fmt:STANDARD"),
      )
    }
    return listOf(
      CatalogEntry.InsertAction("Estándar", "Standard", "Fracción / Radical / π", "fmt:STANDARD"),
      CatalogEntry.InsertAction("Decimal", "Decimal", "Aproximación numérica", "fmt:DECIMAL"),
      CatalogEntry.InsertAction("Fracc. impropia (d/c)", "Improper Frac (d/c)", "", "fmt:IMPROPER_FRACTION"),
      CatalogEntry.InsertAction("Fracc. mixta (ab/c)", "Mixed Frac (ab/c)", "", "fmt:MIXED_FRACTION"),
      CatalogEntry.InsertAction("Factor primo", "Prime Factor", "Descomposición prima", "fmt:PRIME_FACTOR"),
      CatalogEntry.InsertAction("Sexagesimal (° ' \")", "Sexagesimal (° ' \")", "Grados/Min/Seg", "fmt:SEXAGESIMAL"),
      CatalogEntry.InsertAction("Notación científica", "Scientific", "a × 10ⁿ", "fmt:SCIENTIFIC"),
      CatalogEntry.InsertAction("Notación ingeniería", "Engineering", "Potencias × 10³ᵏ", "fmt:ENGINEERING"),
    )
  }
}
