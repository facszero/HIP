package com.hip.data

import com.google.gson.Gson
import com.hip.database.HIPDatabase
import com.hip.model.*

/**
 * DataSeeder siembra la base de datos con:
 * - Ingredientes comunes de cocina uruguaya
 * - Recetas caseras realistas
 * - Tabla de sustituciones
 */
class DataSeeder(private val db: HIPDatabase) {

    private val gson = Gson()

    suspend fun seedIfEmpty() {
        if (db.ingredientDao().count() == 0) {
            seedIngredients()
        }
        if (db.recipeDao().count() == 0) {
            seedRecipes()
        }
        if (db.substitutionDao().count() == 0) {
            seedSubstitutions()
        }
    }

    // ─────────────────────────────────────────────────────
    //  INGREDIENTES BASE - Cocina uruguaya
    // ─────────────────────────────────────────────────────

    private suspend fun seedIngredients() {
        val ingredients = listOf(
            // CARNES
            Ingredient(
                name = "Pollo (entero o piezas)",
                normalizedName = "pollo",
                category = IngredientCategory.CARNES,
                aliases = gson.toJson(listOf("pollo entero", "pata muslo", "pechuga", "alitas")),
                defaultUnit = "kg",
                caloriesPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6,
                sodiumPer100g = 74.0
            ),
            Ingredient(
                name = "Carne picada",
                normalizedName = "carne picada",
                category = IngredientCategory.CARNES,
                aliases = gson.toJson(listOf("picada", "carne molida", "picada común")),
                defaultUnit = "kg",
                caloriesPer100g = 215.0,
                proteinPer100g = 26.0,
                carbsPer100g = 0.0,
                fatPer100g = 12.0,
                sodiumPer100g = 75.0
            ),
            Ingredient(
                name = "Asado de tira",
                normalizedName = "asado",
                category = IngredientCategory.CARNES,
                aliases = gson.toJson(listOf("tira de asado", "costilla", "costillar")),
                defaultUnit = "kg",
                caloriesPer100g = 290.0,
                proteinPer100g = 24.0,
                carbsPer100g = 0.0,
                fatPer100g = 21.0,
                sodiumPer100g = 60.0
            ),
            Ingredient(
                name = "Salchicha",
                normalizedName = "salchicha",
                category = IngredientCategory.CARNES,
                aliases = gson.toJson(listOf("salchichas", "salchicha alemana", "frankfurter")),
                defaultUnit = "unidad",
                caloriesPer100g = 290.0,
                proteinPer100g = 12.0,
                carbsPer100g = 2.0,
                fatPer100g = 26.0,
                sodiumPer100g = 800.0
            ),
            Ingredient(
                name = "Jamón cocido",
                normalizedName = "jamon",
                category = IngredientCategory.CARNES,
                aliases = gson.toJson(listOf("jamón", "jamon cocido", "paleta")),
                defaultUnit = "g",
                caloriesPer100g = 145.0,
                proteinPer100g = 18.0,
                carbsPer100g = 1.5,
                fatPer100g = 7.0,
                sodiumPer100g = 950.0
            ),

            // LÁCTEOS
            Ingredient(
                name = "Huevo",
                normalizedName = "huevo",
                category = IngredientCategory.HUEVOS,
                aliases = gson.toJson(listOf("huevos", "huevo de gallina")),
                defaultUnit = "unidad",
                caloriesPer100g = 155.0,
                proteinPer100g = 13.0,
                carbsPer100g = 1.1,
                fatPer100g = 11.0,
                sodiumPer100g = 124.0
            ),
            Ingredient(
                name = "Leche entera",
                normalizedName = "leche",
                category = IngredientCategory.LACTEOS,
                aliases = gson.toJson(listOf("leche entera", "leche descremada", "leche semidescremada")),
                defaultUnit = "litro",
                caloriesPer100g = 61.0,
                proteinPer100g = 3.2,
                carbsPer100g = 4.8,
                fatPer100g = 3.3,
                sodiumPer100g = 43.0
            ),
            Ingredient(
                name = "Queso mozzarella",
                normalizedName = "queso mozzarella",
                category = IngredientCategory.LACTEOS,
                aliases = gson.toJson(listOf("mozzarella", "muzarela", "queso rallado", "queso feta")),
                defaultUnit = "g",
                caloriesPer100g = 280.0,
                proteinPer100g = 22.0,
                carbsPer100g = 2.2,
                fatPer100g = 20.0,
                sodiumPer100g = 486.0
            ),
            Ingredient(
                name = "Queso Colonia",
                normalizedName = "queso colonia",
                category = IngredientCategory.LACTEOS,
                aliases = gson.toJson(listOf("colonia", "queso uruguayo", "queso bola")),
                defaultUnit = "g",
                caloriesPer100g = 365.0,
                proteinPer100g = 25.0,
                carbsPer100g = 1.3,
                fatPer100g = 29.0,
                sodiumPer100g = 680.0
            ),
            Ingredient(
                name = "Crema de leche",
                normalizedName = "crema",
                category = IngredientCategory.LACTEOS,
                aliases = gson.toJson(listOf("crema de leche", "nata", "cream")),
                defaultUnit = "ml",
                caloriesPer100g = 340.0,
                proteinPer100g = 2.4,
                carbsPer100g = 3.0,
                fatPer100g = 35.0,
                sodiumPer100g = 35.0
            ),
            Ingredient(
                name = "Mantequilla / Manteca",
                normalizedName = "manteca",
                category = IngredientCategory.LACTEOS,
                aliases = gson.toJson(listOf("manteca", "mantequilla", "butter")),
                defaultUnit = "g",
                caloriesPer100g = 717.0,
                proteinPer100g = 0.9,
                carbsPer100g = 0.1,
                fatPer100g = 81.0,
                sodiumPer100g = 11.0
            ),

            // VERDURAS
            Ingredient(
                name = "Cebolla",
                normalizedName = "cebolla",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("cebolla blanca", "cebolla morada", "cebolla de verdeo")),
                defaultUnit = "unidad",
                caloriesPer100g = 40.0,
                proteinPer100g = 1.1,
                carbsPer100g = 9.3,
                fatPer100g = 0.1,
                sodiumPer100g = 4.0,
                glycemicIndex = 10
            ),
            Ingredient(
                name = "Tomate",
                normalizedName = "tomate",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("tomates", "jitomate", "tomate perita", "tomate cherry")),
                defaultUnit = "unidad",
                caloriesPer100g = 18.0,
                proteinPer100g = 0.9,
                carbsPer100g = 3.9,
                fatPer100g = 0.2,
                sodiumPer100g = 5.0,
                glycemicIndex = 15
            ),
            Ingredient(
                name = "Papa",
                normalizedName = "papa",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("papas", "patata", "potato")),
                defaultUnit = "unidad",
                caloriesPer100g = 77.0,
                proteinPer100g = 2.0,
                carbsPer100g = 17.0,
                fatPer100g = 0.1,
                sodiumPer100g = 6.0,
                glycemicIndex = 65
            ),
            Ingredient(
                name = "Zanahoria",
                normalizedName = "zanahoria",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("zanahorias")),
                defaultUnit = "unidad",
                caloriesPer100g = 41.0,
                proteinPer100g = 0.9,
                carbsPer100g = 10.0,
                fatPer100g = 0.2,
                sodiumPer100g = 69.0,
                glycemicIndex = 35
            ),
            Ingredient(
                name = "Zapallito (zucchini)",
                normalizedName = "zapallito",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("zapallito verde", "zucchini", "zuchini")),
                defaultUnit = "unidad",
                caloriesPer100g = 17.0,
                proteinPer100g = 1.2,
                carbsPer100g = 3.1,
                fatPer100g = 0.3,
                sodiumPer100g = 8.0,
                glycemicIndex = 15
            ),
            Ingredient(
                name = "Morrón / Pimiento",
                normalizedName = "morron",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("morrón", "pimiento rojo", "pimiento verde", "morrón verde", "morrón rojo")),
                defaultUnit = "unidad",
                caloriesPer100g = 31.0,
                proteinPer100g = 1.0,
                carbsPer100g = 6.0,
                fatPer100g = 0.3,
                sodiumPer100g = 4.0,
                glycemicIndex = 15
            ),
            Ingredient(
                name = "Ajo",
                normalizedName = "ajo",
                category = IngredientCategory.CONDIMENTOS,
                aliases = gson.toJson(listOf("ajo", "diente de ajo", "ajo en polvo")),
                defaultUnit = "diente",
                caloriesPer100g = 149.0,
                proteinPer100g = 6.4,
                carbsPer100g = 33.0,
                fatPer100g = 0.5,
                sodiumPer100g = 17.0
            ),
            Ingredient(
                name = "Lechuga",
                normalizedName = "lechuga",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("lechuga criolla", "lechuga mantecosa", "lechuga repollada")),
                defaultUnit = "unidad",
                caloriesPer100g = 15.0,
                proteinPer100g = 1.4,
                carbsPer100g = 2.9,
                fatPer100g = 0.2,
                sodiumPer100g = 28.0,
                glycemicIndex = 10
            ),
            Ingredient(
                name = "Espinaca",
                normalizedName = "espinaca",
                category = IngredientCategory.VERDURAS,
                aliases = gson.toJson(listOf("espinacas", "espinaca fresca", "espinaca congelada")),
                defaultUnit = "g",
                caloriesPer100g = 23.0,
                proteinPer100g = 2.9,
                carbsPer100g = 3.6,
                fatPer100g = 0.4,
                sodiumPer100g = 79.0,
                glycemicIndex = 15
            ),

            // CEREALES Y PASTAS
            Ingredient(
                name = "Arroz",
                normalizedName = "arroz",
                category = IngredientCategory.CEREALES,
                aliases = gson.toJson(listOf("arroz largo fino", "arroz parboiled")),
                defaultUnit = "taza",
                caloriesPer100g = 130.0,
                proteinPer100g = 2.7,
                carbsPer100g = 28.0,
                fatPer100g = 0.3,
                sodiumPer100g = 1.0,
                glycemicIndex = 70
            ),
            Ingredient(
                name = "Fideos / Pasta",
                normalizedName = "fideos",
                category = IngredientCategory.CEREALES,
                aliases = gson.toJson(listOf("pasta", "espagueti", "tallarín", "macarrones", "penne")),
                defaultUnit = "g",
                caloriesPer100g = 158.0,
                proteinPer100g = 5.8,
                carbsPer100g = 31.0,
                fatPer100g = 0.9,
                sodiumPer100g = 6.0,
                glycemicIndex = 50
            ),
            Ingredient(
                name = "Harina",
                normalizedName = "harina",
                category = IngredientCategory.CEREALES,
                aliases = gson.toJson(listOf("harina de trigo", "harina 000", "harina 0000")),
                defaultUnit = "taza",
                caloriesPer100g = 364.0,
                proteinPer100g = 10.0,
                carbsPer100g = 76.0,
                fatPer100g = 1.0,
                sodiumPer100g = 2.0,
                glycemicIndex = 85
            ),
            Ingredient(
                name = "Pan rallado",
                normalizedName = "pan rallado",
                category = IngredientCategory.CEREALES,
                aliases = gson.toJson(listOf("pan rayado", "rebozador")),
                defaultUnit = "taza",
                caloriesPer100g = 395.0,
                proteinPer100g = 13.0,
                carbsPer100g = 73.0,
                fatPer100g = 4.5,
                sodiumPer100g = 612.0
            ),

            // CONDIMENTOS Y ACEITES
            Ingredient(
                name = "Aceite de girasol",
                normalizedName = "aceite",
                category = IngredientCategory.ACEITES,
                aliases = gson.toJson(listOf("aceite vegetal", "aceite de maíz", "aceite girasol")),
                defaultUnit = "cda",
                caloriesPer100g = 884.0,
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 100.0,
                sodiumPer100g = 0.0
            ),
            Ingredient(
                name = "Sal",
                normalizedName = "sal",
                category = IngredientCategory.CONDIMENTOS,
                aliases = gson.toJson(listOf("sal fina", "sal gruesa", "sal entrefina")),
                defaultUnit = "cda",
                caloriesPer100g = 0.0,
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 0.0,
                sodiumPer100g = 38758.0
            ),
            Ingredient(
                name = "Pimienta",
                normalizedName = "pimienta",
                category = IngredientCategory.CONDIMENTOS,
                aliases = gson.toJson(listOf("pimienta negra", "pimienta blanca")),
                defaultUnit = "cdita",
                caloriesPer100g = 251.0,
                proteinPer100g = 10.0,
                carbsPer100g = 64.0,
                fatPer100g = 3.3,
                sodiumPer100g = 20.0
            ),
            Ingredient(
                name = "Orégano",
                normalizedName = "oregano",
                category = IngredientCategory.CONDIMENTOS,
                aliases = gson.toJson(listOf("orégano seco")),
                defaultUnit = "cdita",
                caloriesPer100g = 265.0,
                proteinPer100g = 11.0,
                carbsPer100g = 69.0,
                fatPer100g = 4.3,
                sodiumPer100g = 15.0
            ),
            Ingredient(
                name = "Perejil",
                normalizedName = "perejil",
                category = IngredientCategory.CONDIMENTOS,
                aliases = gson.toJson(listOf("perejil fresco", "perejil seco")),
                defaultUnit = "cda",
                caloriesPer100g = 36.0,
                proteinPer100g = 3.0,
                carbsPer100g = 6.3,
                fatPer100g = 0.8,
                sodiumPer100g = 56.0
            ),

            // LEGUMBRES
            Ingredient(
                name = "Lentejas",
                normalizedName = "lentejas",
                category = IngredientCategory.LEGUMBRES,
                aliases = gson.toJson(listOf("lenteja", "lentejas rojas")),
                defaultUnit = "taza",
                caloriesPer100g = 116.0,
                proteinPer100g = 9.0,
                carbsPer100g = 20.0,
                fatPer100g = 0.4,
                fiberPer100g = 7.9,
                sodiumPer100g = 2.0,
                glycemicIndex = 29
            ),
            Ingredient(
                name = "Porotos / Frijoles",
                normalizedName = "porotos",
                category = IngredientCategory.LEGUMBRES,
                aliases = gson.toJson(listOf("frijoles", "alubias", "poroto negro", "poroto colorado")),
                defaultUnit = "taza",
                caloriesPer100g = 127.0,
                proteinPer100g = 8.7,
                carbsPer100g = 22.8,
                fatPer100g = 0.5,
                fiberPer100g = 6.4,
                sodiumPer100g = 2.0,
                glycemicIndex = 30
            ),

            // FRUTAS
            Ingredient(
                name = "Manzana",
                normalizedName = "manzana",
                category = IngredientCategory.FRUTAS,
                aliases = gson.toJson(listOf("manzanas", "manzana roja", "manzana verde")),
                defaultUnit = "unidad",
                caloriesPer100g = 52.0,
                proteinPer100g = 0.3,
                carbsPer100g = 14.0,
                fatPer100g = 0.2,
                fiberPer100g = 2.4,
                sodiumPer100g = 1.0,
                glycemicIndex = 36
            ),
            Ingredient(
                name = "Banana / Plátano",
                normalizedName = "banana",
                category = IngredientCategory.FRUTAS,
                aliases = gson.toJson(listOf("banana", "plátano", "banano")),
                defaultUnit = "unidad",
                caloriesPer100g = 89.0,
                proteinPer100g = 1.1,
                carbsPer100g = 23.0,
                fatPer100g = 0.3,
                fiberPer100g = 2.6,
                sodiumPer100g = 1.0,
                glycemicIndex = 52
            )
        )

        db.ingredientDao().insertAll(ingredients)
    }

    // ─────────────────────────────────────────────────────
    //  RECETAS CASERAS URUGUAYAS
    // ─────────────────────────────────────────────────────

    private suspend fun seedRecipes() {
        val recipes = buildList {

            // ── ENTRADAS ──────────────────────────────────────

            add(Recipe(
                name = "Sopa de verduras",
                description = "Sopa casera reconfortante con las verduras que tengas a mano",
                mealType = MealType.ENTRADA,
                category = RecipeCategory.SOPAS_CALDOS,
                difficulty = DifficultyLevel.MUY_FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "zanahoria", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "papa", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "cebolla", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "sal", "quantity" to "1", "unit" to "cdita"),
                    mapOf("name" to "aceite", "quantity" to "1", "unit" to "cda")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "zapallito", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "fideos", "quantity" to "50", "unit" to "g"),
                    mapOf("name" to "perejil", "quantity" to "1", "unit" to "cda")
                )),
                substitutions = gson.toJson(mapOf(
                    "papa" to listOf("batata"),
                    "zanahoria" to listOf("zapallito", "choclo")
                )),
                steps = gson.toJson(listOf(
                    "Pelar y cortar la cebolla en brunoise. Rehogar en aceite caliente 3 minutos.",
                    "Pelar y cortar las zanahorias y papas en cubos de 1.5 cm.",
                    "Agregar las verduras a la olla con 1.5 litros de agua fría.",
                    "Llevar a hervor, bajar el fuego y cocinar 20-25 minutos con tapa.",
                    "Opcional: agregar fideos en los últimos 8 minutos.",
                    "Condimentar con sal. Servir con perejil picado si tenés."
                )),
                prepTimeMinutes = 10,
                cookTimeMinutes = 25,
                requiredEquipment = gson.toJson(listOf("olla mediana", "cuchillo", "tabla")),
                estimatedCostUYU = 80,
                servings = 4,
                caloriesPerServing = 120,
                proteinPerServing = 3.0,
                carbsPerServing = 24.0,
                fatPerServing = 2.0,
                fiberPerServing = 3.5,
                sodiumPerServing = 350.0,
                nutritionalScore = 8,
                tags = gson.toJson(listOf("economica", "saludable", "vegana", "sin gluten")),
                isMicrowaveOk = false,
                isFreezerFriendly = true
            ))

            add(Recipe(
                name = "Ensalada mixta",
                description = "Ensalada fresca con lo básico de la heladera",
                mealType = MealType.ENTRADA,
                category = RecipeCategory.ENSALADAS,
                difficulty = DifficultyLevel.MUY_FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "lechuga", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "tomate", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "sal", "quantity" to "1", "unit" to "cdita"),
                    mapOf("name" to "aceite", "quantity" to "2", "unit" to "cdas")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "cebolla", "quantity" to "0.5", "unit" to "unidad"),
                    mapOf("name" to "zanahoria", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "huevo", "quantity" to "2", "unit" to "unidades")
                )),
                substitutions = gson.toJson(mapOf(
                    "lechuga" to listOf("espinaca", "rúcula")
                )),
                steps = gson.toJson(listOf(
                    "Lavar bien la lechuga, secar y trozar con las manos.",
                    "Cortar los tomates en gajos o rodajas.",
                    "Si usás cebolla, cortarla en pluma bien fina.",
                    "Mezclar todo, condimentar con sal y aceite al gusto.",
                    "Si agregás huevo: cocinar huevos duros 10 min, pelar y cortar en cuartos."
                )),
                prepTimeMinutes = 8,
                cookTimeMinutes = 0,
                requiredEquipment = gson.toJson(listOf("bowl", "cuchillo", "tabla")),
                estimatedCostUYU = 60,
                servings = 4,
                caloriesPerServing = 85,
                proteinPerServing = 3.0,
                carbsPerServing = 6.0,
                fatPerServing = 6.0,
                fiberPerServing = 2.0,
                sodiumPerServing = 290.0,
                nutritionalScore = 9,
                tags = gson.toJson(listOf("rapida", "saludable", "vegana", "sin gluten", "sin coccion")),
                isMicrowaveOk = false,
                isFreezerFriendly = false
            ))

            // ── PLATOS PRINCIPALES ────────────────────────────

            add(Recipe(
                name = "Tortilla de zapallitos",
                description = "Tortilla casera esponjosa con huevos y zapallito. Clásico uruguayo.",
                mealType = MealType.PLATO_PRINCIPAL,
                category = RecipeCategory.TARTAS_TORTILLAS,
                difficulty = DifficultyLevel.FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "huevo", "quantity" to "4", "unit" to "unidades"),
                    mapOf("name" to "zapallito", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "cebolla", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "sal", "quantity" to "1", "unit" to "cdita"),
                    mapOf("name" to "aceite", "quantity" to "2", "unit" to "cdas")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "queso mozzarella", "quantity" to "50", "unit" to "g"),
                    mapOf("name" to "perejil", "quantity" to "1", "unit" to "cda")
                )),
                substitutions = gson.toJson(mapOf(
                    "zapallito" to listOf("papa", "espinaca", "zanahoria rallada")
                )),
                steps = gson.toJson(listOf(
                    "Cortar el zapallito en rodajas finas o rallarlo groseramente.",
                    "Picar la cebolla en brunoise. Rehogar en sartén con aceite 5 min hasta transparente.",
                    "Agregar el zapallito y cocinar 5 min más, salar.",
                    "Batir los huevos con un tenedor. Mezclar con las verduras frías.",
                    "Calentar sartén con un chorrito de aceite a fuego medio.",
                    "Volcar la mezcla, mover los bordes con espátula. Cocinar 5-7 min hasta cuajar abajo.",
                    "Dar vuelta con un plato o tapa. Cocinar otros 4 min del otro lado.",
                    "Opcional: agregar queso rallado encima antes de dar vuelta."
                )),
                prepTimeMinutes = 10,
                cookTimeMinutes = 20,
                requiredEquipment = gson.toJson(listOf("sartén 24cm con tapa", "bowl", "espátula")),
                estimatedCostUYU = 120,
                servings = 3,
                caloriesPerServing = 220,
                proteinPerServing = 14.0,
                carbsPerServing = 8.0,
                fatPerServing = 15.0,
                fiberPerServing = 1.5,
                sodiumPerServing = 420.0,
                nutritionalScore = 7,
                tags = gson.toJson(listOf("vegetariana", "economica", "sin gluten")),
                isMicrowaveOk = false,
                isFreezerFriendly = false
            ))

            add(Recipe(
                name = "Arroz con pollo",
                description = "Guiso de arroz con pollo al estilo familiar. Rendidor y sabroso.",
                mealType = MealType.PLATO_PRINCIPAL,
                category = RecipeCategory.PASTA_ARROZ,
                difficulty = DifficultyLevel.MEDIO,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "pollo", "quantity" to "500", "unit" to "g"),
                    mapOf("name" to "arroz", "quantity" to "1.5", "unit" to "tazas"),
                    mapOf("name" to "cebolla", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "tomate", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "ajo", "quantity" to "2", "unit" to "dientes"),
                    mapOf("name" to "aceite", "quantity" to "3", "unit" to "cdas"),
                    mapOf("name" to "sal", "quantity" to "1.5", "unit" to "cditas")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "morron", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "zanahoria", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "perejil", "quantity" to "2", "unit" to "cdas"),
                    mapOf("name" to "pimienta", "quantity" to "1", "unit" to "cdita")
                )),
                substitutions = gson.toJson(mapOf(
                    "pollo" to listOf("pollo congelado descongelado"),
                    "tomate fresco" to listOf("puré de tomate enlatado", "salsa de tomate")
                )),
                steps = gson.toJson(listOf(
                    "Cortar el pollo en trozos y dorar en aceite caliente por todos lados. Retirar.",
                    "En la misma olla: rehogar cebolla picada y ajo 4 minutos.",
                    "Agregar tomate picado (o salsa). Cocinar 5 min revolviendo.",
                    "Incorporar el pollo, morrón y zanahoria si usás.",
                    "Agregar 3 tazas de agua caliente. Salar y pimentar.",
                    "Cuando rompa hervor, agregar el arroz. Revolver.",
                    "Cocinar a fuego bajo con tapa 18 minutos. No destapar.",
                    "Apagar fuego, reposar 5 minutos. Servir con perejil si tenés."
                )),
                prepTimeMinutes = 15,
                cookTimeMinutes = 35,
                requiredEquipment = gson.toJson(listOf("olla grande o cacerola", "cuchillo", "tabla")),
                estimatedCostUYU = 280,
                servings = 4,
                caloriesPerServing = 385,
                proteinPerServing = 28.0,
                carbsPerServing = 42.0,
                fatPerServing = 10.0,
                fiberPerServing = 2.0,
                sodiumPerServing = 580.0,
                nutritionalScore = 7,
                tags = gson.toJson(listOf("familiar", "rendidor", "sin gluten")),
                isMicrowaveOk = true,
                isFreezerFriendly = true
            ))

            add(Recipe(
                name = "Fideos con salsa de tomate y carne",
                description = "Pasta al estilo casero con salsa de carne picada. Clásico de cada semana.",
                mealType = MealType.PLATO_PRINCIPAL,
                category = RecipeCategory.PASTA_ARROZ,
                difficulty = DifficultyLevel.FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "fideos", "quantity" to "400", "unit" to "g"),
                    mapOf("name" to "carne picada", "quantity" to "300", "unit" to "g"),
                    mapOf("name" to "tomate", "quantity" to "3", "unit" to "unidades"),
                    mapOf("name" to "cebolla", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "ajo", "quantity" to "2", "unit" to "dientes"),
                    mapOf("name" to "aceite", "quantity" to "2", "unit" to "cdas"),
                    mapOf("name" to "sal", "quantity" to "1", "unit" to "cdita"),
                    mapOf("name" to "oregano", "quantity" to "1", "unit" to "cdita")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "queso mozzarella", "quantity" to "50", "unit" to "g"),
                    mapOf("name" to "morron", "quantity" to "1", "unit" to "unidad")
                )),
                substitutions = gson.toJson(mapOf(
                    "tomate fresco" to listOf("salsa de tomate enlatada", "puré de tomate")
                )),
                steps = gson.toJson(listOf(
                    "Poner a hervir agua con sal para los fideos (1 litro aprox).",
                    "Rehogar cebolla y ajo picados en aceite 5 minutos.",
                    "Agregar la carne picada, desgranar bien. Cocinar 8 min hasta dorar.",
                    "Incorporar el tomate picado (o salsa). Condimentar con orégano y sal.",
                    "Cocinar la salsa a fuego medio 15 minutos revolviendo.",
                    "Cuando el agua hierve, cocinar los fideos según el paquete (7-9 min).",
                    "Escurrir los fideos. Mezclar con la salsa o servir por encima.",
                    "Opcional: gratinar con queso rallado en horno o microondas 2 min."
                )),
                prepTimeMinutes = 10,
                cookTimeMinutes = 25,
                requiredEquipment = gson.toJson(listOf("olla para pasta", "sartén", "colador")),
                estimatedCostUYU = 260,
                servings = 4,
                caloriesPerServing = 450,
                proteinPerServing = 28.0,
                carbsPerServing = 52.0,
                fatPerServing = 13.0,
                fiberPerServing = 3.0,
                sodiumPerServing = 620.0,
                nutritionalScore = 6,
                tags = gson.toJson(listOf("familiar", "clasico", "rendidor")),
                isMicrowaveOk = true,
                isFreezerFriendly = true
            ))

            add(Recipe(
                name = "Milanesas de pollo al horno",
                description = "Milanesas tiernas de pechuga de pollo, empanadas y horneadas. Sin aceite extra.",
                mealType = MealType.PLATO_PRINCIPAL,
                category = RecipeCategory.MILANESAS,
                difficulty = DifficultyLevel.FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "pollo", "quantity" to "500", "unit" to "g pechuga"),
                    mapOf("name" to "huevo", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "pan rallado", "quantity" to "1", "unit" to "taza"),
                    mapOf("name" to "sal", "quantity" to "1", "unit" to "cdita"),
                    mapOf("name" to "aceite", "quantity" to "1", "unit" to "cda para placa")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "ajo", "quantity" to "1", "unit" to "diente"),
                    mapOf("name" to "oregano", "quantity" to "1", "unit" to "cdita"),
                    mapOf("name" to" perejil", "quantity" to "1", "unit" to "cda"),
                    mapOf("name" to "queso mozzarella", "quantity" to "50", "unit" to "g")
                )),
                substitutions = gson.toJson(mapOf(
                    "pan rallado" to listOf("avena molida + condimentos")
                )),
                steps = gson.toJson(listOf(
                    "Precalentar horno a 200°C (fuerte).",
                    "Filetear las pechugas bien finas (máx 1 cm) o golpear entre film.",
                    "Salar las pechugas. Mezclar el pan rallado con orégano y ajo rallado si usás.",
                    "Batir los huevos en un bowl con un poco de sal.",
                    "Pasar cada milanesa por huevo, luego por pan rallado. Presionar bien.",
                    "Engrasar levemente una placa para horno.",
                    "Disponer las milanesas sin superponerlas. Rociar con unas gotas de aceite encima.",
                    "Hornear 12 minutos, dar vuelta y hornear 10 minutos más hasta dorar.",
                    "Opcional: último minuto agregar queso y gratinar."
                )),
                prepTimeMinutes = 15,
                cookTimeMinutes = 25,
                requiredEquipment = gson.toJson(listOf("horno", "placa para horno", "bowl")),
                estimatedCostUYU = 300,
                servings = 4,
                caloriesPerServing = 280,
                proteinPerServing = 35.0,
                carbsPerServing = 18.0,
                fatPerServing = 7.0,
                fiberPerServing = 0.8,
                sodiumPerServing = 490.0,
                nutritionalScore = 8,
                tags = gson.toJson(listOf("saludable", "horno", "alto proteina")),
                isOvenRequired = true,
                isMicrowaveOk = false,
                isFreezerFriendly = true
            ))

            add(Recipe(
                name = "Guiso de lentejas",
                description = "Guiso nutritivo y económico de lentejas con verduras. Ideal para invierno.",
                mealType = MealType.PLATO_PRINCIPAL,
                category = RecipeCategory.GUISOS,
                difficulty = DifficultyLevel.FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "lentejas", "quantity" to "1.5", "unit" to "tazas"),
                    mapOf("name" to "cebolla", "quantity" to "1", "unit" to "unidad"),
                    mapOf("name" to "zanahoria", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "tomate", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "ajo", "quantity" to "2", "unit" to "dientes"),
                    mapOf("name" to "aceite", "quantity" to "2", "unit" to "cdas"),
                    mapOf("name" to "sal", "quantity" to "1", "unit" to "cdita")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "papa", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "salchicha", "quantity" to "2", "unit" to "unidades"),
                    mapOf("name" to "morron", "quantity" to "1", "unit" to "unidad")
                )),
                substitutions = gson.toJson(mapOf(
                    "lentejas" to listOf("porotos remojados")
                )),
                steps = gson.toJson(listOf(
                    "Remojar las lentejas 30 min si no son las de cocción rápida (opcional pero ideal).",
                    "Picar cebolla y ajo. Rehogar en aceite caliente 5 minutos.",
                    "Agregar tomate picado, morrón y zanahoria. Cocinar 5 min.",
                    "Incorporar las lentejas escurridas. Cubrir con 1 litro de agua.",
                    "Llevar a hervor, bajar el fuego. Cocinar 25-30 min con tapa.",
                    "Agregar papas en cubos a mitad de cocción si usás.",
                    "Agregar salchichas rodajeadas últimos 10 minutos.",
                    "Ajustar sal. Servir espeso con pan casero."
                )),
                prepTimeMinutes = 10,
                cookTimeMinutes = 35,
                requiredEquipment = gson.toJson(listOf("olla grande")),
                estimatedCostUYU = 120,
                servings = 5,
                caloriesPerServing = 290,
                proteinPerServing = 18.0,
                carbsPerServing = 42.0,
                fatPerServing = 5.0,
                fiberPerServing = 12.0,
                sodiumPerServing = 380.0,
                nutritionalScore = 9,
                tags = gson.toJson(listOf("economico", "alto proteina", "alta fibra", "vegano", "sin gluten")),
                isMicrowaveOk = true,
                isFreezerFriendly = true
            ))

            // ── POSTRES ───────────────────────────────────────

            add(Recipe(
                name = "Postre de banana",
                description = "Postre rápido sin cocción con banana y leche. Para cuando queda poco en la heladera.",
                mealType = MealType.POSTRE,
                category = RecipeCategory.POSTRES,
                difficulty = DifficultyLevel.MUY_FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "banana", "quantity" to "3", "unit" to "unidades"),
                    mapOf("name" to "leche", "quantity" to "0.5", "unit" to "taza")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "azucar", "quantity" to "2", "unit" to "cdas"),
                    mapOf("name" to "canela", "quantity" to "1", "unit" to "cdita")
                )),
                substitutions = gson.toJson(emptyMap<String, List<String>>()),
                steps = gson.toJson(listOf(
                    "Pelar las bananas y pisarlas con un tenedor en un bowl.",
                    "Agregar la leche poco a poco mientras se pisa hasta lograr crema.",
                    "Opcional: agregar azúcar y canela al gusto.",
                    "Llevar a la heladera 20 minutos antes de servir.",
                    "Servir en vasitos o copas."
                )),
                prepTimeMinutes = 5,
                cookTimeMinutes = 0,
                requiredEquipment = gson.toJson(listOf("bowl", "tenedor")),
                estimatedCostUYU = 40,
                servings = 3,
                caloriesPerServing = 110,
                proteinPerServing = 2.0,
                carbsPerServing = 26.0,
                fatPerServing = 1.0,
                fiberPerServing = 2.5,
                sodiumPerServing = 20.0,
                nutritionalScore = 7,
                tags = gson.toJson(listOf("rapido", "sin horno", "economico", "sin gluten")),
                isMicrowaveOk = false,
                isFreezerFriendly = false
            ))

            add(Recipe(
                name = "Manzanas asadas con canela",
                description = "Postre caliente y saludable. Manzanas en el horno con azúcar morena y canela.",
                mealType = MealType.POSTRE,
                category = RecipeCategory.POSTRES,
                difficulty = DifficultyLevel.MUY_FACIL,
                requiredIngredients = gson.toJson(listOf(
                    mapOf("name" to "manzana", "quantity" to "4", "unit" to "unidades"),
                    mapOf("name" to "manteca", "quantity" to "1", "unit" to "cda")
                )),
                optionalIngredients = gson.toJson(listOf(
                    mapOf("name" to "azucar", "quantity" to "2", "unit" to "cdas"),
                    mapOf("name" to "canela", "quantity" to "1", "unit" to "cdita")
                )),
                substitutions = gson.toJson(emptyMap<String, List<String>>()),
                steps = gson.toJson(listOf(
                    "Precalentar horno a 180°C.",
                    "Sacar el centro de las manzanas con cuchillo o descorazonador.",
                    "Mezclar manteca blanda con azúcar y canela. Rellenar el hueco de cada manzana.",
                    "Colocar en fuente para horno con un dedo de agua en el fondo.",
                    "Hornear 30-35 minutos hasta que estén tiernas.",
                    "Servir tibias solas o con un poco de crema o yogur."
                )),
                prepTimeMinutes = 8,
                cookTimeMinutes = 35,
                requiredEquipment = gson.toJson(listOf("horno", "fuente para horno")),
                estimatedCostUYU = 70,
                servings = 4,
                caloriesPerServing = 130,
                proteinPerServing = 0.5,
                carbsPerServing = 28.0,
                fatPerServing = 3.0,
                fiberPerServing = 3.5,
                sodiumPerServing = 25.0,
                nutritionalScore = 7,
                tags = gson.toJson(listOf("horno", "saludable", "vegana", "sin gluten")),
                isOvenRequired = true,
                isMicrowaveOk = true,
                isFreezerFriendly = false
            ))
        }

        db.recipeDao().insertAll(recipes)
    }

    // ─────────────────────────────────────────────────────
    //  TABLA DE SUSTITUCIONES
    // ─────────────────────────────────────────────────────

    private suspend fun seedSubstitutions() {
        val substitutions = listOf(
            Substitution("huevo", "huevo de codorniz (3 unidades)", 3.0, "Mismo valor nutricional"),
            Substitution("manteca", "aceite vegetal", 0.8, "Menos saturada"),
            Substitution("leche entera", "leche descremada", 1.0, "Menos calorías"),
            Substitution("crema", "yogur natural", 1.0, "Más proteína, menos grasa", affectsTexture = true),
            Substitution("arroz blanco", "arroz integral", 1.0, "Más fibra, mayor tiempo cocción"),
            Substitution("fideos blancos", "fideos integrales", 1.0, "Más fibra"),
            Substitution("harina blanca", "harina integral", 0.9, "Ajustar líquido"),
            Substitution("papa", "batata", 1.0, "Más fibra, menor índice glucémico"),
            Substitution("carne picada vacuna", "carne picada de pollo", 1.0, "Menos grasa"),
            Substitution("queso colonia", "queso mozzarella", 1.0, "Menos sodio"),
            Substitution("azúcar blanca", "azúcar mascabo", 0.9, "Más nutrientes, sabor similar"),
            Substitution("salsa de tomate enlatada", "tomate fresco procesado + sal", 1.0),
            Substitution("caldo de cubo", "caldo casero", 1.0, "Sin conservantes")
        )
        db.substitutionDao().insertAll(substitutions)
    }
}
