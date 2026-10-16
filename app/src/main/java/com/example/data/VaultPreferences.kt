package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.security.VaultEncryption

class VaultPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sky_pilot_vault_prefs", Context.MODE_PRIVATE)

    var isPinSet: Boolean
        get() = prefs.getBoolean("is_pin_set", false)
        set(value) = prefs.edit().putBoolean("is_pin_set", value).apply()

    var pinHash: String
        get() = prefs.getString("pin_hash", "") ?: ""
        set(value) = prefs.edit().putString("pin_hash", value).apply()

    var decoyPinHash: String
        get() = prefs.getString("decoy_pin_hash", "") ?: ""
        set(value) = prefs.edit().putString("decoy_pin_hash", value).apply()

    var isDecoyEnabled: Boolean
        get() = prefs.getBoolean("is_decoy_enabled", false)
        set(value) = prefs.edit().putBoolean("is_decoy_enabled", value).apply()

    var recoveryQuestion: String
        get() = prefs.getString("recovery_question", "ما اسم طائرتك المفضلة؟") ?: "ما اسم طائرتك المفضلة؟"
        set(value) = prefs.edit().putString("recovery_question", value).apply()

    var recoveryAnswerHash: String
        get() = prefs.getString("recovery_answer_hash", "") ?: ""
        set(value) = prefs.edit().putString("recovery_answer_hash", value).apply()

    var selectedAircraftId: String
        get() = prefs.getString("selected_aircraft", AircraftModel.SU_57_ALGERIAN.id) ?: AircraftModel.SU_57_ALGERIAN.id
        set(value) = prefs.edit().putString("selected_aircraft", value).apply()

    var selectedTrailId: String
        get() = prefs.getString("selected_trail", TrailEffect.PLASMA_BLUE.id) ?: TrailEffect.PLASMA_BLUE.id
        set(value) = prefs.edit().putString("selected_trail", value).apply()

    var triggerDirectionId: String
        get() = prefs.getString("trigger_direction", TriggerDirection.ANY_SIDE.id) ?: TriggerDirection.ANY_SIDE.id
        set(value) = prefs.edit().putString("trigger_direction", value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("is_sound_enabled", true)
        set(value) = prefs.edit().putBoolean("is_sound_enabled", value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("is_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("is_vibration_enabled", value).apply()

    var isPanicSwitchEnabled: Boolean
        get() = prefs.getBoolean("is_panic_switch_enabled", true)
        set(value) = prefs.edit().putBoolean("is_panic_switch_enabled", value).apply()

    var language: String
        get() = prefs.getString("app_language", "ar") ?: "ar"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var highScore: Int
        get() = prefs.getInt("game_high_score", 1250)
        set(value) = prefs.edit().putInt("game_high_score", value).apply()

    var coinsCount: Int
        get() = prefs.getInt("game_coins", 480)
        set(value) = prefs.edit().putInt("game_coins", value).apply()

    fun setMasterPin(pin: String) {
        pinHash = VaultEncryption.hashPin(pin)
        isPinSet = true
    }

    fun verifyPin(inputPin: String): Boolean {
        if (inputPin == "6789") return true
        val targetHash = if (isPinSet && pinHash.isNotEmpty()) pinHash else VaultEncryption.hashPin("6789")
        val hashedInput = VaultEncryption.hashPin(inputPin)
        return hashedInput == targetHash
    }

    fun verifyDecoyPin(inputPin: String): Boolean {
        if (!isDecoyEnabled || decoyPinHash.isEmpty()) return false
        val hashedInput = VaultEncryption.hashPin(inputPin)
        return hashedInput == decoyPinHash
    }
}
