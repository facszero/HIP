package com.hip.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.hip.model.CookingLevel
import com.hip.model.NutritionalGoal
import com.hip.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension para DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hip_preferences")

class PreferencesManager(private val context: Context) {

    private val gson = Gson()

    // Keys
    private object Keys {
        val USER_PROFILE = stringPreferencesKey("user_profile")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_ENABLED = booleanPreferencesKey("llm_enabled")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LAST_SCAN_SESSION = stringPreferencesKey("last_scan_session")
        val PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
    }

    // ─────────────────────────────────────────────────────
    //  USER PROFILE
    // ─────────────────────────────────────────────────────

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.USER_PROFILE]
        if (json.isNullOrBlank()) {
            UserProfile()  // perfil por defecto
        } else {
            runCatching { gson.fromJson(json, UserProfile::class.java) }.getOrElse { UserProfile() }
        }
    }

    suspend fun getUserProfile(): UserProfile = userProfileFlow.first()

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_PROFILE] = gson.toJson(profile)
        }
    }

    // ─────────────────────────────────────────────────────
    //  AJUSTES LLM
    // ─────────────────────────────────────────────────────

    val llmApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LLM_API_KEY] ?: ""
    }

    val llmEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LLM_ENABLED] ?: false
    }

    suspend fun saveLLMApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LLM_API_KEY] = key
        }
    }

    suspend fun setLLMEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LLM_ENABLED] = enabled
        }
    }

    // ─────────────────────────────────────────────────────
    //  MODO OFFLINE / PRIVACIDAD
    // ─────────────────────────────────────────────────────

    val offlineModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OFFLINE_MODE] ?: false
    }

    suspend fun setOfflineMode(offline: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OFFLINE_MODE] = offline
        }
    }

    // ─────────────────────────────────────────────────────
    //  FIRST LAUNCH / ONBOARDING
    // ─────────────────────────────────────────────────────

    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIRST_LAUNCH] = false
        }
    }

    val privacyAcceptedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRIVACY_ACCEPTED] ?: false
    }

    suspend fun setPrivacyAccepted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIVACY_ACCEPTED] = true
        }
    }

    // ─────────────────────────────────────────────────────
    //  HELPERS PARA PERFIL RÁPIDO
    // ─────────────────────────────────────────────────────

    suspend fun updateGoal(goal: NutritionalGoal) {
        val profile = getUserProfile()
        saveUserProfile(profile.copy(goal = goal))
    }

    suspend fun updateCookingLevel(level: CookingLevel) {
        val profile = getUserProfile()
        saveUserProfile(profile.copy(cookingLevel = level))
    }

    suspend fun updateAllergies(allergies: List<String>) {
        val profile = getUserProfile()
        saveUserProfile(profile.copy(allergies = allergies))
    }

    suspend fun updateEquipment(hasOven: Boolean, hasMicrowave: Boolean, hasBlender: Boolean) {
        val profile = getUserProfile()
        saveUserProfile(profile.copy(
            hasOven = hasOven,
            hasMicrowave = hasMicrowave,
            hasBlender = hasBlender
        ))
    }
}
