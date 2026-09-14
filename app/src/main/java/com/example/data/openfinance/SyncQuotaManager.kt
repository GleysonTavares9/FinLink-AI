package com.example.data.openfinance

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncQuotaManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("finlink_sync_quota", Context.MODE_PRIVATE)

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Verifica se o auto-sync diário já foi realizado hoje ao abrir o app.
     * Se ainda não foi realizado, executa a ação e grava a data atual.
     */
    fun checkAndPerformDailyAutoSync(onPerformSync: () -> Unit) {
        val today = getTodayDateString()
        val lastAutoSync = prefs.getString("last_auto_sync_date", "")
        if (lastAutoSync != today) {
            prefs.edit().putString("last_auto_sync_date", today).apply()
            onPerformSync()
        }
    }

    /**
     * Retorna a quantidade de sincronizações manuais restantes hoje (máximo 2).
     */
    fun getRemainingManualSyncs(): Int {
        val today = getTodayDateString()
        val savedDate = prefs.getString("manual_sync_date", "")
        if (savedDate != today) {
            // Novo dia: reseta o contador
            prefs.edit().putString("manual_sync_date", today).putInt("manual_sync_count", 0).apply()
            return 2
        }
        val count = prefs.getInt("manual_sync_count", 0)
        return (2 - count).coerceAtLeast(0)
    }

    /**
     * Tenta consumir uma das 2 atualizações manuais do dia.
     * Retorna true se permitido, ou false se o limite de 2 atualizações já tiver sido atingido.
     */
    fun consumeManualSync(): Boolean {
        val remaining = getRemainingManualSyncs()
        if (remaining <= 0) return false

        val currentCount = 2 - remaining
        prefs.edit().putInt("manual_sync_count", currentCount + 1).apply()
        return true
    }
}
