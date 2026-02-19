package com.hip.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hip.model.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────
//  TYPE CONVERTERS
// ─────────────────────────────────────────────────────

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientCategory(value: IngredientCategory): String = value.name

    @TypeConverter
    fun toIngredientCategory(value: String): IngredientCategory =
        IngredientCategory.valueOf(value)

    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromRecipeCategory(value: RecipeCategory): String = value.name

    @TypeConverter
    fun toRecipeCategory(value: String): RecipeCategory = RecipeCategory.valueOf(value)

    @TypeConverter
    fun fromDifficultyLevel(value: DifficultyLevel): String = value.name

    @TypeConverter
    fun toDifficultyLevel(value: String): DifficultyLevel = DifficultyLevel.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}

// ─────────────────────────────────────────────────────
//  DAO - INGREDIENTES
// ─────────────────────────────────────────────────────

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients WHERE isActive = 1")
    fun getAllIngredients(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE normalizedName = :name LIMIT 1")
    suspend fun findByNormalizedName(name: String): Ingredient?

    @Query("""
        SELECT * FROM ingredients 
        WHERE normalizedName LIKE '%' || :query || '%' 
        OR aliases LIKE '%' || :query || '%'
        LIMIT 20
    """)
    suspend fun searchIngredients(query: String): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE category = :category")
    suspend fun getByCategory(category: IngredientCategory): List<Ingredient>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(ingredients: List<Ingredient>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ingredient: Ingredient): Long

    @Update
    suspend fun update(ingredient: Ingredient)

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────
//  DAO - RECETAS
// ─────────────────────────────────────────────────────

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE isActive = 1")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): Recipe?

    @Query("SELECT * FROM recipes WHERE mealType = :mealType AND isActive = 1")
    suspend fun getByMealType(mealType: MealType): List<Recipe>

    @Query("SELECT * FROM recipes WHERE category = :category AND isActive = 1")
    suspend fun getByCategory(category: RecipeCategory): List<Recipe>

    @Query("""
        SELECT * FROM recipes 
        WHERE difficulty IN (:levels) 
        AND isActive = 1
        AND (prepTimeMinutes + cookTimeMinutes) <= :maxTime
    """)
    suspend fun getByDifficultyAndTime(
        levels: List<DifficultyLevel>,
        maxTime: Int
    ): List<Recipe>

    @Query("""
        SELECT * FROM recipes 
        WHERE isActive = 1
        AND nutritionalScore >= :minScore
        ORDER BY nutritionalScore DESC
    """)
    suspend fun getByNutritionalScore(minScore: Int): List<Recipe>

    @Query("SELECT * FROM recipes WHERE isActive = 1")
    suspend fun getAllForMatching(): List<Recipe>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(recipe: Recipe): Long

    @Update
    suspend fun update(recipe: Recipe)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────
//  DAO - SUSTITUCIONES
// ─────────────────────────────────────────────────────

@Dao
interface SubstitutionDao {

    @Query("SELECT * FROM substitutions WHERE originalIngredient = :ingredient")
    suspend fun getSubstitutesFor(ingredient: String): List<Substitution>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(substitutions: List<Substitution>)

    @Query("SELECT COUNT(*) FROM substitutions")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────
//  BASE DE DATOS PRINCIPAL
// ─────────────────────────────────────────────────────

@Database(
    entities = [
        Ingredient::class,
        Recipe::class,
        Substitution::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HIPDatabase : RoomDatabase() {

    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun substitutionDao(): SubstitutionDao

    companion object {
        @Volatile
        private var INSTANCE: HIPDatabase? = null

        fun getDatabase(context: Context): HIPDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HIPDatabase::class.java,
                    "hip_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
