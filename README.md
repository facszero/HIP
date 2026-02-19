# 🧠 Heladera Inteligente Pro (HIP)

**IA nutricional + visión computacional + recetas caseras uruguayas**

---

## 📋 Descripción

HIP es una aplicación Android que detecta los ingredientes de tu heladera mediante visión computacional guiada y genera exactamente 3 recetas realistas (entrada, plato principal, postre) adaptadas a tu perfil nutricional, sin inventar ingredientes ni usar cocina de restaurante.

---

## 🏗 Arquitectura

```
app/
├── HIPApplication.kt                 # Application class, inyección de dependencias manual
│
├── model/
│   └── Models.kt                     # Ingredient, Recipe, UserProfile, RecipeMatch, etc.
│
├── database/
│   └── HIPDatabase.kt                # Room DB + DAOs + TypeConverters
│
├── data/
│   └── DataSeeder.kt                 # Datos semilla: 30+ ingredientes, 8+ recetas caseras
│
├── engine/
│   └── RecipeEngine.kt               # Motor de matching estructurado (no LLM)
│
├── vision/
│   └── IngredientDetector.kt         # ML Kit object detection + labeling
│
├── llm/
│   └── LLMAdapter.kt                 # Adaptación opcional con prompts cerrados
│
├── utils/
│   └── PreferencesManager.kt         # DataStore para perfil y configuración
│
└── ui/
    ├── main/
    │   └── MainActivity.kt           # Home screen
    ├── scan/
    │   ├── ScanActivity.kt           # Escaneo guiado 3 pasos + CameraX
    │   └── ScanOverlayView.kt        # Custom view con guías visuales
    ├── review/
    │   └── IngredientReviewActivity.kt # Validación humana de ingredientes
    ├── recipes/
    │   ├── RecipeListActivity.kt     # Los 3 matches del menú
    │   └── RecipeDetailActivity.kt   # Detalle + pasos + nutrición
    └── profile/
        └── ProfileSetupActivity.kt   # Configuración de perfil nutricional
```

---

## 🔬 Decisiones técnicas clave

### 1. Motor de matching estructurado (no generativo)

El `RecipeEngine` **no usa LLM para crear recetas**. Usa un algoritmo de matching que:

1. Normaliza los ingredientes detectados (lowercase, sin acentos)
2. Para cada receta calcula un `MatchScore`:
   - Base: `ingredientes_disponibles / ingredientes_requeridos`
   - Bonus por ingredientes opcionales disponibles
   - Bonus por alineación nutricional con objetivo del usuario
   - Penalty por ingredientes faltantes
3. Filtra por perfil (tiempo, equipamiento, nivel, alergias)
4. Selecciona la mejor entrada + principal + postre

### 2. Escaneo guiado en 3 pasos

En lugar de confiar en una sola foto:
- **Paso 1**: Foto general (toda la heladera)
- **Paso 2**: Sector superior (estantes altos)  
- **Paso 3**: Sector inferior y cajones

Cada foto se analiza con ML Kit. Los resultados se fusionan deduplicando por `normalizedName`, conservando el de mayor confianza.

### 3. Validación humana obligatoria

Después del escaneo, el usuario:
- Ve todos los ingredientes detectados con % de confianza
- Confirma o elimina cada uno
- Ajusta el estado (crudo/cocido/congelado)
- Agrega manualmente lo que no fue detectado

**Las recetas solo se generan con ingredientes confirmados.**

### 4. LLM como capa de presentación, no como motor

El `LLMAdapter` solo recibe:
- Lista de ingredientes disponibles (texto)
- Pasos originales de la receta
- Perfil del usuario

Y devuelve pasos reformulados en lenguaje cotidiano rioplatense. **No puede inventar ingredientes fuera del contexto.** Si la API no está disponible, se usan los pasos originales.

### 5. Privacy-first

- Las fotos nunca salen del dispositivo
- El LLM cloud solo recibe texto (nombres de ingredientes)
- No hay almacenamiento de fotos
- Modo offline 100% funcional sin LLM

---

## 🚀 Setup

### Pre-requisitos
- Android Studio Hedgehog o superior
- JDK 17
- Android SDK 34
- Dispositivo/emulador con Android 8.0+ (API 26+)

### Pasos

```bash
# 1. Clonar
git clone https://github.com/tu-usuario/HIP.git
cd HIP

# 2. Crear local.properties
echo "sdk.dir=/ruta/a/tu/sdk" > local.properties

# 3. Abrir en Android Studio y sincronizar Gradle

# 4. Ejecutar en dispositivo con cámara
```

### Configuración LLM (opcional)

Si querés usar la adaptación IA:
1. Obtené una API key de OpenAI
2. En la app: Configuración → Configurar IA → Pegá tu API key

---

## 📊 Base de datos de recetas

Las recetas están modeladas como:

```kotlin
Recipe(
    name = "Tortilla de zapallitos",
    requiredIngredients = [...],  // debe tener para hacer la receta
    optionalIngredients = [...],  // mejoran el resultado
    substitutions = {...},        // si falta X, podés usar Y
    steps = [...],                // instrucciones sin tecnicismos
    estimatedCostUYU = 120,       // costo estimado en pesos uruguayos
    nutritionalScore = 7          // 1-10 de calidad nutricional
)
```

Para agregar recetas al seed: editá `DataSeeder.kt` → función `seedRecipes()`.

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests (requiere dispositivo/emulador)
./gradlew connectedAndroidTest
```

---

## 🔮 Roadmap

| Feature | Estado |
|---|---|
| Escaneo guiado 3 pasos | ✅ v1.0 |
| Motor de matching estructurado | ✅ v1.0 |
| Perfil nutricional con objetivos | ✅ v1.0 |
| Tabla de sustituciones | ✅ v1.0 |
| Validación humana de ingredientes | ✅ v1.0 |
| Detección de fechas de vencimiento | 📋 v2.0 |
| Alertas de desperdicio | 📋 v2.0 |
| Plan semanal automático | 📋 v2.0 |
| Lista de compras inteligente | 📋 v2.0 |
| Integración con balanza digital | 📋 v3.0 |
| Modelo TFLite local para alimentos | 📋 v2.0 |

---

## 📄 Licencia

MIT — Fernando Cañete, Montevideo, Uruguay, 2025
