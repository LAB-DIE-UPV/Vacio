package com.example.Vacio.ConfiguracionPrefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase para gestionar SharedPreferences de factores de calibración y configuraciones del usuario.
 * Utiliza un archivo 'configuracion_factores' en modo privado.
 */
class ConfiguracionPrefs(context: Context) {
    // Obtención de SharedPreferences
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "configuracion_factores",
        Context.MODE_PRIVATE
    )

    // ---------------------------------------------------------
    // FACTORES LOCALES (calculados o por defecto) para corriente y tensión
    // ---------------------------------------------------------

    /**
     * Factor de conversión para corriente fase A (local)
     */
    var factorCorrienteA: Float
        get() = prefs.getFloat("factorCorrienteA", 1f)
        set(value) = prefs.edit().putFloat("factorCorrienteA", value).apply()

    /**
     * Factor de conversión para corriente fase B (local)
     */
    var factorCorrienteB: Float
        get() = prefs.getFloat("factorCorrienteB", 1f)
        set(value) = prefs.edit().putFloat("factorCorrienteB", value).apply()

    /**
     * Factor de conversión para tensión fase A (local)
     */
    var factorTensionA: Float
        get() = prefs.getFloat("factorTensionA", 1f)
        set(value) = prefs.edit().putFloat("factorTensionA", value).apply()

    /**
     * Factor de conversión para tensión fase B (local)
     */
    var factorTensionB: Float
        get() = prefs.getFloat("factorTensionB", 1f)
        set(value) = prefs.edit().putFloat("factorTensionB", value).apply()

    /**
     * Factor de conversión para tensión fase C (local)
     */
    var factorTensionC: Float
        get() = prefs.getFloat("factorTensionC", 1f)
        set(value) = prefs.edit().putFloat("factorTensionC", value).apply()

    // ---------------------------------------------------------
    // FACTORES RECIBIDOS DEL ESP32 (para calibración remota)
    // ---------------------------------------------------------

    /**
     * Factor de corriente fase A recibido del ESP32
     */
    var esp32FactorCorrienteA: Float
        get() = prefs.getFloat("esp32FactorCorrienteA", 1f)
        set(value) = prefs.edit().putFloat("esp32FactorCorrienteA", value).apply()

    /**
     * Factor de corriente fase B recibido del ESP32
     */
    var esp32FactorCorrienteB: Float
        get() = prefs.getFloat("esp32FactorCorrienteB", 1f)
        set(value) = prefs.edit().putFloat("esp32FactorCorrienteB", value).apply()

    /**
     * Factor de tensión fase A recibido del ESP32
     */
    var esp32FactorTensionA: Float
        get() = prefs.getFloat("esp32FactorTensionA", 1f)
        set(value) = prefs.edit().putFloat("esp32FactorTensionA", value).apply()

    /**
     * Factor de tensión fase B recibido del ESP32
     */
    var esp32FactorTensionB: Float
        get() = prefs.getFloat("esp32FactorTensionB", 1f)
        set(value) = prefs.edit().putFloat("esp32FactorTensionB", value).apply()

    /**
     * Factor de tensión fase C recibido del ESP32
     */
    var esp32FactorTensionC: Float
        get() = prefs.getFloat("esp32FactorTensionC", 1f)
        set(value) = prefs.edit().putFloat("esp32FactorTensionC", value).apply()

    // ---------------------------------------------------------
    // PREFERENCIAS DE USUARIO POR DEFECTO (valores iniciales)
    // ---------------------------------------------------------

    /**
     * Valor de corriente A por defecto elegido por el usuario
     */
    var defaultUserCurrentA: Float
        get() = prefs.getFloat("defaultUserCurrentA", 1f)
        set(value) = prefs.edit().putFloat("defaultUserCurrentA", value).apply()

    /**
     * Valor de corriente B por defecto elegido por el usuario
     */
    var defaultUserCurrentB: Float
        get() = prefs.getFloat("defaultUserCurrentB", 1f)
        set(value) = prefs.edit().putFloat("defaultUserCurrentB", value).apply()

    /**
     * Valor de tensión fase A por defecto elegido por el usuario
     */
    var defaultUserTensionA: Float
        get() = prefs.getFloat("defaultUserTensionA", 1f)
        set(value) = prefs.edit().putFloat("defaultUserTensionA", value).apply()

    /**
     * Valor de tensión fase B por defecto elegido por el usuario
     */
    var defaultUserTensionB: Float
        get() = prefs.getFloat("defaultUserTensionB", 1f)
        set(value) = prefs.edit().putFloat("defaultUserTensionB", value).apply()

    /**
     * Valor de tensión fase C por defecto elegido por el usuario
     */
    var defaultUserTensionC: Float
        get() = prefs.getFloat("defaultUserTensionC", 1f)
        set(value) = prefs.edit().putFloat("defaultUserTensionC", value).apply()
}