package com.example.engine

data class ScientificConstant(
  val symbol: String,
  val nameEs: String,
  val nameEn: String,
  val categoryEs: String,
  val categoryEn: String,
  val value: Double,
  val unit: String,
)

data class UnitConversion(
  val id: String,
  val label: String,
  val categoryEs: String,
  val categoryEn: String,
  val factor: Double,
  val offsetBefore: Double = 0.0,
  val offsetAfter: Double = 0.0,
) {
  fun convert(x: Double): Double = (x + offsetBefore) * factor + offsetAfter
}

object CatalogData {
  val constants: List<ScientificConstant> = listOf(
    // Universales
    ScientificConstant("c₀", "Velocidad luz vacío", "Speed of light", "Universales", "Universal", 299792458.0, "m/s"),
    ScientificConstant("h", "Constante de Planck", "Planck constant", "Universales", "Universal", 6.62607015e-34, "J·s"),
    ScientificConstant("ℏ", "Planck reducida", "Reduced Planck", "Universales", "Universal", 1.054571817e-34, "J·s"),
    ScientificConstant("G", "Gravitación universal", "Gravitational const.", "Universales", "Universal", 6.67430e-11, "m³/(kg·s²)"),
    ScientificConstant("ε₀", "Permitividad vacío", "Vacuum permittivity", "Electromag.", "Electromag.", 8.8541878128e-12, "F/m"),
    ScientificConstant("μ₀", "Permeabilidad vacío", "Vacuum permeability", "Electromag.", "Electromag.", 1.25663706212e-6, "N/A²"),
    ScientificConstant("Z₀", "Impedancia del vacío", "Impedance of vacuum", "Electromag.", "Electromag.", 376.730313668, "Ω"),
    // Electromagnéticas
    ScientificConstant("e", "Carga elemental", "Elementary charge", "Electromag.", "Electromag.", 1.602176634e-19, "C"),
    ScientificConstant("Φ₀", "Cuanto de flujo mag.", "Magnetic flux quantum", "Electromag.", "Electromag.", 2.067833848e-15, "Wb"),
    ScientificConstant("G₀", "Cuanto conductancia", "Conductance quantum", "Electromag.", "Electromag.", 7.748091729e-5, "S"),
    ScientificConstant("K_J", "Constante Josephson", "Josephson constant", "Electromag.", "Electromag.", 483597.8484e9, "Hz/V"),
    ScientificConstant("R_K", "Constante von Klitzing", "von Klitzing const.", "Electromag.", "Electromag.", 25812.80745, "Ω"),
    ScientificConstant("μ_B", "Magnetón de Bohr", "Bohr magneton", "Electromag.", "Electromag.", 9.2740100783e-24, "J/T"),
    ScientificConstant("μ_N", "Magnetón nuclear", "Nuclear magneton", "Electromag.", "Electromag.", 5.0507837461e-27, "J/T"),
    // Atómicas y nucleares
    ScientificConstant("m_e", "Masa del electrón", "Electron mass", "Atómicas", "Atomic", 9.1093837015e-31, "kg"),
    ScientificConstant("m_p", "Masa del protón", "Proton mass", "Atómicas", "Atomic", 1.67262192369e-27, "kg"),
    ScientificConstant("m_n", "Masa del neutrón", "Neutron mass", "Atómicas", "Atomic", 1.67492749804e-27, "kg"),
    ScientificConstant("m_μ", "Masa del muon", "Muon mass", "Atómicas", "Atomic", 1.883531627e-28, "kg"),
    ScientificConstant("a₀", "Radio de Bohr", "Bohr radius", "Atómicas", "Atomic", 5.29177210903e-11, "m"),
    ScientificConstant("α", "Const. estructura fina", "Fine-structure const.", "Atómicas", "Atomic", 7.2973525693e-3, ""),
    ScientificConstant("r_e", "Radio clásico electrón", "Classical electron r", "Atómicas", "Atomic", 2.8179403262e-15, "m"),
    ScientificConstant("λ_C", "Long. onda Compton", "Compton wavelength", "Atómicas", "Atomic", 2.42631023867e-12, "m"),
    ScientificConstant("R_∞", "Constante de Rydberg", "Rydberg constant", "Atómicas", "Atomic", 10973731.56816, "1/m"),
    // Fisicoquímicas
    ScientificConstant("N_A", "Constante de Avogadro", "Avogadro constant", "Fisicoquím.", "Phys.-Chem.", 6.02214076e23, "1/mol"),
    ScientificConstant("k", "Constante de Boltzmann", "Boltzmann constant", "Fisicoquím.", "Phys.-Chem.", 1.380649e-23, "J/K"),
    ScientificConstant("R", "Constante gases ideales", "Molar gas constant", "Fisicoquím.", "Phys.-Chem.", 8.314462618, "J/(mol·K)"),
    ScientificConstant("F", "Constante de Faraday", "Faraday constant", "Fisicoquím.", "Phys.-Chem.", 96485.33212, "C/mol"),
    ScientificConstant("u", "Unidad masa atómica", "Atomic mass unit", "Fisicoquím.", "Phys.-Chem.", 1.66053906660e-27, "kg"),
    ScientificConstant("V_m", "Volumen molar gas ideal", "Molar volume (STP)", "Fisicoquím.", "Phys.-Chem.", 0.02241396954, "m³/mol"),
    ScientificConstant("σ", "Stefan-Boltzmann", "Stefan-Boltzmann", "Fisicoquím.", "Phys.-Chem.", 5.670374419e-8, "W/(m²·K⁴)"),
    ScientificConstant("c₁", "Primera const. radiación", "First radiation const.", "Fisicoquím.", "Phys.-Chem.", 3.741771852e-16, "W·m²"),
    ScientificConstant("c₂", "Segunda const. radiación", "Second radiation const.", "Fisicoquím.", "Phys.-Chem.", 0.01438776877, "m·K"),
    // Adoptadas
    ScientificConstant("g", "Aceleración gravedad", "Standard gravity", "Adoptadas", "Adopted", 9.80665, "m/s²"),
    ScientificConstant("atm", "Atmósfera estándar", "Standard atmosphere", "Adoptadas", "Adopted", 101325.0, "Pa"),
    ScientificConstant("t₀", "Cero Celsius en Kelvin", "Celsius temp. zero", "Adoptadas", "Adopted", 273.15, "K"),
  )

  val conversions: List<UnitConversion> = listOf(
    // Longitud
    UnitConversion("in_cm", "in▶cm", "Longitud", "Length", 2.54),
    UnitConversion("cm_in", "cm▶in", "Longitud", "Length", 1.0 / 2.54),
    UnitConversion("ft_m", "ft▶m", "Longitud", "Length", 0.3048),
    UnitConversion("m_ft", "m▶ft", "Longitud", "Length", 1.0 / 0.3048),
    UnitConversion("yd_m", "yd▶m", "Longitud", "Length", 0.9144),
    UnitConversion("m_yd", "m▶yd", "Longitud", "Length", 1.0 / 0.9144),
    UnitConversion("mi_km", "mile▶km", "Longitud", "Length", 1.609344),
    UnitConversion("km_mi", "km▶mile", "Longitud", "Length", 1.0 / 1.609344),
    UnitConversion("nmi_m", "n mile▶m", "Longitud", "Length", 1852.0),
    UnitConversion("m_nmi", "m▶n mile", "Longitud", "Length", 1.0 / 1852.0),
    // Área
    UnitConversion("acre_m2", "acre▶m²", "Área", "Area", 4046.8564224),
    UnitConversion("m2_acre", "m²▶acre", "Área", "Area", 1.0 / 4046.8564224),
    // Volumen
    UnitConversion("galUS_L", "gal(US)▶L", "Volumen", "Volume", 3.785411784),
    UnitConversion("L_galUS", "L▶gal(US)", "Volumen", "Volume", 1.0 / 3.785411784),
    UnitConversion("galUK_L", "gal(UK)▶L", "Volumen", "Volume", 4.54609),
    UnitConversion("L_galUK", "L▶gal(UK)", "Volumen", "Volume", 1.0 / 4.54609),
    // Masa
    UnitConversion("oz_g", "oz▶g", "Masa", "Mass", 28.349523125),
    UnitConversion("g_oz", "g▶oz", "Masa", "Mass", 1.0 / 28.349523125),
    UnitConversion("lb_kg", "lb▶kg", "Masa", "Mass", 0.45359237),
    UnitConversion("kg_lb", "kg▶lb", "Masa", "Mass", 1.0 / 0.45359237),
    // Velocidad
    UnitConversion("kmh_ms", "km/h▶m/s", "Velocidad", "Velocity", 1.0 / 3.6),
    UnitConversion("ms_kmh", "m/s▶km/h", "Velocidad", "Velocity", 3.6),
    // Presión
    UnitConversion("atm_Pa", "atm▶Pa", "Presión", "Pressure", 101325.0),
    UnitConversion("Pa_atm", "Pa▶atm", "Presión", "Pressure", 1.0 / 101325.0),
    UnitConversion("mmHg_Pa", "mmHg▶Pa", "Presión", "Pressure", 133.322387415),
    UnitConversion("Pa_mmHg", "Pa▶mmHg", "Presión", "Pressure", 1.0 / 133.322387415),
    UnitConversion("kgfcm2_Pa", "kgf/cm²▶Pa", "Presión", "Pressure", 98066.5),
    UnitConversion("psi_kPa", "lbf/in²▶kPa", "Presión", "Pressure", 6.894757293),
    // Energía & Potencia
    UnitConversion("cal_J", "cal▶J", "Energía", "Energy", 4.184),
    UnitConversion("J_cal", "J▶cal", "Energía", "Energy", 1.0 / 4.184),
    UnitConversion("eV_J", "eV▶J", "Energía", "Energy", 1.602176634e-19),
    UnitConversion("hp_W", "hp▶W", "Potencia", "Power", 745.69987158),
    UnitConversion("W_hp", "W▶hp", "Potencia", "Power", 1.0 / 745.69987158),
    // Temperatura
    UnitConversion("F_C", "°F▶°C", "Temperatura", "Temperature", 5.0 / 9.0, offsetBefore = -32.0),
    UnitConversion("C_F", "°C▶°F", "Temperatura", "Temperature", 9.0 / 5.0, offsetAfter = 32.0),
  )

  fun findConstant(symbol: String): ScientificConstant? =
    constants.find { it.symbol == symbol }

  fun findConversion(label: String): UnitConversion? =
    conversions.find { it.label == label }
}
