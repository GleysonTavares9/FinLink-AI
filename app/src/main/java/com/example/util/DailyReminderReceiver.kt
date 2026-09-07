package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receptor de alarmes diários para enviar lembretes de registro e sincronização de dados.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reagenda os alarmes ao reiniciar o celular se estiver ativado
            if (DailyReminderManager.isReminderEnabled(context)) {
                DailyReminderManager.scheduleDailyReminder(context)
            }
        } else {
            // Dispara o lembrete diário
            DailyReminderManager.sendImmediateReminder(
                context = context,
                title = "FinLink AI • Lembrete Diário 📊",
                message = "Lembrete: registre seus cupons de hoje e sincronize suas contas para manter o orçamento em dia!"
            )
            // Reagenda para o próximo dia
            DailyReminderManager.scheduleDailyReminder(context)
        }
    }
}
