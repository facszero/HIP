package com.hip

import android.app.Application
import com.hip.database.HIPDatabase
import com.hip.data.DataSeeder
import com.hip.engine.RecipeEngine
import com.hip.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HIPApplication : Application() {

    // Scope de la aplicación para operaciones de background
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Lazy init de la base de datos
    val database by lazy { HIPDatabase.getDatabase(this) }

    // Motor de recetas
    val recipeEngine by lazy { RecipeEngine(database) }

    // Gestor de preferencias
    val preferencesManager by lazy { PreferencesManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeDatabase()
    }

    private fun initializeDatabase() {
        applicationScope.launch {
            // Sembrar datos iniciales si es primera ejecución
            val seeder = DataSeeder(database)
            seeder.seedIfEmpty()
        }
    }

    companion object {
        lateinit var instance: HIPApplication
            private set
    }
}
