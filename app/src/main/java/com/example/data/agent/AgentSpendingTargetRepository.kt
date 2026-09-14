package com.example.data.agent

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AgentSpendingTarget
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import com.example.util.FinancialAgentNotificationManager
import com.example.util.formatBrNumber
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AgentSpendingTargetRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("finlink_agent_targets", Context.MODE_PRIVATE)

    fun getTargets(): List<AgentSpendingTarget> {
        val jsonStr = prefs.getString("targets_list", null) ?: return getDefaultInitialTargets()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AgentSpendingTarget>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AgentSpendingTarget(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        agentId = obj.optString("agentId", "guardiao_cartao"),
                        agentName = obj.optString("agentName", "Guardião do Cartão"),
                        targetType = obj.optString("targetType", "CARTAO_CREDITO"),
                        targetName = obj.optString("targetName", "Cartão de Crédito"),
                        limitAmount = obj.optDouble("limitAmount", 1500.0),
                        currentSpent = obj.optDouble("currentSpent", 0.0),
                        isNotifiedWarning = obj.optBoolean("isNotifiedWarning", false),
                        isNotifiedExceeded = obj.optBoolean("isNotifiedExceeded", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("AgentTargetRepo", "Erro ao carregar metas dos agentes", e)
            getDefaultInitialTargets()
        }
    }

    private fun getDefaultInitialTargets(): List<AgentSpendingTarget> {
        val initial = listOf(
            AgentSpendingTarget(
                id = "default_target_cartao",
                agentId = "guardiao_cartao",
                agentName = "Eduardo • Guardião do Cartão",
                targetType = "CARTAO_CREDITO",
                targetName = "Compras no Cartão de Crédito",
                limitAmount = 1500.0,
                currentSpent = 684.50,
                isNotifiedWarning = false,
                isNotifiedExceeded = false
            )
        )
        saveAllTargets(initial)
        return initial
    }

    fun saveTarget(target: AgentSpendingTarget) {
        val current = getTargets().toMutableList()
        val index = current.indexOfFirst { it.id == target.id }
        if (index >= 0) {
            current[index] = target
        } else {
            current.add(target)
        }
        saveAllTargets(current)
    }

    fun deleteTarget(targetId: String) {
        val current = getTargets().filter { it.id != targetId }
        saveAllTargets(current)
    }

    private fun saveAllTargets(list: List<AgentSpendingTarget>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("agentId", item.agentId)
                    put("agentName", item.agentName)
                    put("targetType", item.targetType)
                    put("targetName", item.targetName)
                    put("limitAmount", item.limitAmount)
                    put("currentSpent", item.currentSpent)
                    put("isNotifiedWarning", item.isNotifiedWarning)
                    put("isNotifiedExceeded", item.isNotifiedExceeded)
                    put("createdAt", item.createdAt)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("targets_list", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("AgentTargetRepo", "Erro ao salvar metas dos agentes", e)
        }
    }

    /**
     * Avalia o valor gasto atual de cada meta configurada pelo usuário e,
     * caso atinja >= 80% (preventivo) ou >= 100% (ultrapassado), emite a notificação educativa do agente responsável.
     */
    fun evaluateSpendingAndNotify(
        transactions: List<UnifiedTransaction>,
        creditCardOpenBill: Double = 0.0
    ): List<AgentSpendingTarget> {
        val currentTargets = getTargets()
        if (currentTargets.isEmpty()) return emptyList()

        val updatedTargets = currentTargets.map { target ->
            // Cálculo do gasto atual baseado no tipo de alvo configurado pelo usuário
            val calculatedSpent = when {
                target.targetType == "CARTAO_CREDITO" || target.targetName.contains("Cartão", ignoreCase = true) -> {
                    val txCardSpent = transactions.filter {
                        it.type != TransactionType.ENTRADA && (
                            it.paymentMethod.contains("Cartão", ignoreCase = true) ||
                            it.category.contains("Cartão", ignoreCase = true) ||
                            it.subtitle.contains("Fatura", ignoreCase = true) ||
                            it.subtitle.contains("Parcela", ignoreCase = true)
                        )
                    }.sumOf { it.amount }
                    // Considera o maior entre transações identificadas e o saldo em aberto do Open Finance
                    maxOf(txCardSpent, creditCardOpenBill)
                }
                target.targetType == "CATEGORIA" -> {
                    transactions.filter {
                        it.type != TransactionType.ENTRADA && (
                            it.category.contains(target.targetName, ignoreCase = true) ||
                            target.targetName.contains(it.category, ignoreCase = true)
                        )
                    }.sumOf { it.amount }
                }
                else -> {
                    // Alvo geral ou customizado
                    transactions.filter { it.type != TransactionType.ENTRADA }.sumOf { it.amount }
                }
            }

            var warningSent = target.isNotifiedWarning
            var exceededSent = target.isNotifiedExceeded

            // Se gasto for menor que 80%, reseta flags para novo ciclo
            if (calculatedSpent < target.limitAmount * 0.8) {
                warningSent = false
                exceededSent = false
            }

            // Alerta preventivo (≥ 80%)
            if (calculatedSpent >= target.limitAmount * 0.8 && calculatedSpent < target.limitAmount && !warningSent) {
                warningSent = true
                val pct = ((calculatedSpent / target.limitAmount) * 100).toInt()
                val alertText = "⚠️ Atenção! Você já atingiu $pct% (R$ ${formatBrNumber(calculatedSpent)}) do limite de R$ ${formatBrNumber(target.limitAmount)} definido para ${target.targetName}. Vamos priorizar despesas essenciais para fechar o mês com folga?"
                FinancialAgentNotificationManager.notifyAgentResponse(
                    context = context,
                    agentName = target.agentName,
                    responseText = alertText
                )
            }

            // Alerta de estouro de teto (≥ 100%)
            if (calculatedSpent >= target.limitAmount && !exceededSent) {
                exceededSent = true
                val alertText = "🚨 Limite Ultrapassado! Seus gastos em ${target.targetName} atingiram R$ ${formatBrNumber(calculatedSpent)}, ultrapassando sua meta de R$ ${formatBrNumber(target.limitAmount)}. Toque aqui para analisarmos no chat opções para reequilibrar o mês."
                FinancialAgentNotificationManager.notifyAgentResponse(
                    context = context,
                    agentName = target.agentName,
                    responseText = alertText
                )
            }

            target.copy(
                currentSpent = calculatedSpent,
                isNotifiedWarning = warningSent,
                isNotifiedExceeded = exceededSent
            )
        }

        saveAllTargets(updatedTargets)
        return updatedTargets
    }
}
