# ClassWiz 991LA — Simulador Casio fx-991LA CW para Android

Aplicación Android desarrollada en **Kotlin** y **Jetpack Compose** que simula de forma fiel la calculadora científica **Casio ClassWiz fx-991LA CW**, incluyendo su diseño físico, distribución exacta de teclas y funciones `SHIFT`, pantalla LCD de 4 tonos de gris con renderizado matemático natural (*Natural Textbook Display*) y los 13 modos operativos del modelo original.

---

## Características principales

- **Diseño físico y teclado fiel al modelo fx-991LA CW**
  - Teclado cóncavo redondeado con cruceta circular (`▲ ▼ ◀ ▶` + `OK`), barra superior (`ON`, `HOME`, `CONFIG`, `BACK`, `PgUp`, `PgDn`) y fila de acceso rápido (`SHIFT`, `VARIABLE / STO`, `f(x) / g(x)`, `CATALOG`, `TOOLS`).
  - Mapeo exacto de funciones secundarias con **`SHIFT`**:
    - `7 → π`, `8 → e`, `9 → i`
    - `AC → OFF`, `EXE → ≈` (aproximación decimal), `FORMAT → S⇔D`
    - `x → QR` (generación de código QR real en pantalla), `VARIABLE → STO`, `f(x) → g(x)`
    - `□/□ → a b/c`, `√□ → ³√□`, `x² → x⁻¹`, `x^□ → ˣ√□`, `logₐ → ∫`, `(-) → |□|`, `°'" → x!`, `( → %`, `) → ,`, `d/dx → Σ`, `= → ≠`, `Ans → PreAns`.
- **Pantalla LCD de alta legibilidad**
  - Temas **LCD Clásico** y **Papel Blanco**, contraste ajustable, barra de estado (`S`, `D/R/G`, `√`, `FIX/SCI`, `i`, `DEC/HEX/BIN/OCT`, `STO`, batería) y menús con barra de desplazamiento sincronizada.
  - Bilingüe (**Español / English**) desde `CONFIG → Config. sistema`.
- **Motor matemático simbólico y numérico**
  - Aritmética exacta con fracciones, radicales (`√`), múltiplos de `π`, conversión sexagesimal, factorización en primos y formatos `Estándar`, `Decimal`, `Científica` e `Ingeniería`.
  - Integración numérica (Simpson adaptativo / Gauss), derivación numérica, sumatorias (`Σ`), productorias (`Π`), verificación de igualdades/desigualdades (`=` / `≠`) y catálogo con 35 constantes científicas CODATA y 35 conversiones de unidades.

---

## Modos disponibles (`HOME`)

1. **Calcular (`CALC`)** — Cálculos aritméticos, trigonométricos, logarítmicos, cálculo y verificación.
2. **Estadística (`STAT`)** — 1-Variable y 7 modelos de regresión (lineal, cuadrática, logarítmica, exponenciales, potencia e inversa) con soporte opcional de frecuencia.
3. **Distribución (`DIST`)** — Normal PD/CD, Normal Inversa, Binomial PD/CD y Poisson PD/CD.
4. **Hoja de cálculo (`SHT`)** — Cuadrícula `A1:E15` con fórmulas (`=SUM`, `=MEAN`, `=MIN`, `=MAX`).
5. **Tabla (`TBL`)** — Evaluación tabulada de `f(x)` y `g(x)` con rango configurable.
6. **Ecuación (`EQN`)** — Menú de 3 opciones igual al modelo original:
   - **1. Ec. simultánea** (sistemas lineales de 2, 3 o 4 incógnitas).
   - **2. Polinómica** (ecuaciones de grado 2, 3 o 4 con raíces reales y complejas).
   - **3. Solver** (resolución numérica ingresando una ecuación natural con `=`, valor inicial `x₀` y visualización de `x` e `Izq−Der`).
7. **Desigualdad (`INEQ`)** — Inecuaciones polinómicas de grado 2 a 4 (`>`, `<`, `≥`, `≤`).
8. **Complejos (`CMPLX`)** — Operaciones en forma rectangular (`a+bi`) y polar (`r∠θ`), `Arg`, `Conjg`, `ReP`, `ImP`.
9. **Base-N (`BASE`)** — Conversiones y operaciones lógicas bit a bit en `DEC`, `HEX`, `BIN` y `OCT`.
10. **Matriz (`MAT`)** — Matrices `MatA..MatD` hasta `4×4`, determinante, inversa, transpuesta e identidad.
11. **Vector (`VCT`)** — Vectores `VctA..VctD` 2D/3D, producto escalar/vectorial, módulo, vector unitario y ángulo.
12. **Proporción (`RATIO`)** — Resolución exacta de `A:B = X:D` y `A:B = C:X`.
13. **Caja Matemática (`MBOX`)** — Simulación probabilística de lanzamiento de dados y monedas.

---

## Tecnologías utilizadas

- **Lenguaje:** Kotlin
- **Interfaz:** Jetpack Compose & Material Design 3
- **Arquitectura:** MVVM (`ViewModel` + `StateFlow`)
- **Persistencia de preferencias:** Jetpack DataStore
- **Pruebas:** JUnit 4 + Robolectric (`app/src/test`)

---

## Compilación local y pruebas

### Requisitos
- **JDK 17** o superior
- **Android SDK** (`compileSdk 36`, `minSdk 24`, `targetSdk 36`)

### Comandos

```bash
# 1. Preparar el keystore de depuración si no existe localmente
base64 -d debug.keystore.base64 > debug.keystore

# 2. Dar permisos de ejecución al wrapper (Linux/macOS)
chmod +x ./gradlew

# 3. Ejecutar pruebas unitarias y de comportamiento
./gradlew testDebugUnitTest

# 4. Compilar el APK de depuración
./gradlew assembleDebug
```

El APK generado se ubicará en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## CI/CD con GitHub Actions

El repositorio incluye el flujo de trabajo `.github/workflows/build-apk.yml`, que en cada `push` a `main` / `master`, etiquetas `v*` o ejecución manual (`workflow_dispatch`):

1. Configura **Java 17** (`temurin`) y caché de Gradle.
2. Reconstruye `debug.keystore` desde `debug.keystore.base64` (sin exponer claves ni requerir secretos manuales).
3. Concede permisos a `./gradlew` y ejecuta `./gradlew assembleDebug`.
4. Publica automáticamente `app/build/outputs/apk/debug/app-debug.apk` en **GitHub Releases** mediante `softprops/action-gh-release@v2`.
