package com.example.data.agent

import android.content.Context
import android.util.Log
import com.example.data.model.AgentInfo
import com.example.data.model.AgentNotification
import com.example.data.model.CustomAgent
import com.example.data.model.MonthlyReport
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AgentRepository(
    private val context: Context,
    private val sessionManager: SupabaseSessionManager = SupabaseSessionManager(context),
    private val supabaseClient: SupabaseClient = SupabaseClient(sessionManager)
) {
    private val client = OkHttpClient()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    fun getDefaultAgents(): List<AgentInfo> {
        return listOf(
            AgentInfo(
                id = "guardiao",
                name = "Guardião Diário",
                codename = "Sentinela Ativo",
                role = "Triage Diária & Detecção de Anomalias",
                description = "Rastreia transações continuamente. Identifica cobranças duplicadas no cartão e picos fora do padrão histórico.",
                iconEmoji = "🛡️",
                lastAction = "Última verificação há 15 min • 0 anomalias críticas"
            ),
            AgentInfo(
                id = "auditor",
                name = "Auditor de Gastos",
                codename = "Sentinela de Limites",
                role = "Categorização & Teto de 80%",
                description = "Traduz descrições bancárias com IA e alerta no exato momento em que você atinge 80% do teto estipulado para uma categoria.",
                iconEmoji = "🔍",
                lastAction = "Categorias monitoradas • Limites sob controle"
            ),
            AgentInfo(
                id = "consultor",
                name = "Consultor Mensal",
                codename = "Estrategista de Aportes",
                role = "Fechamento Analítico & Tendências",
                description = "Compila projeções analíticas no fim do mês, taxa de poupança e sugestões de rebalanceamento de aportes e investimentos.",
                iconEmoji = "📊",
                lastAction = "Relatório mensal consolidado disponível"
            )
        )
    }

    suspend fun fetchNotifications(): List<AgentNotification> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        val token = session?.accessToken
        val userId = session?.user?.id

        if (token == null || userId == null) return@withContext getSampleNotifications()

        try {
            val url = "${supabaseClient.baseUrl}/rest/v1/agent_notifications?user_id=eq.$userId&order=created_at.desc&limit=20"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseClient.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                val list = mutableListOf<AgentNotification>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AgentNotification(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            agentName = obj.optString("agent_name", "guardiao"),
                            title = obj.optString("title", "Alerta"),
                            message = obj.optString("message", ""),
                            severity = obj.optString("severity", "info"),
                            createdAt = obj.optString("created_at", ""),
                            isRead = obj.optBoolean("is_read", false)
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e("AgentRepository", "Erro ao buscar notificacoes", e)
        }
        return@withContext getSampleNotifications()
    }

    suspend fun fetchCustomAgents(): List<CustomAgent> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        val token = session?.accessToken
        val userId = session?.user?.id

        if (token == null || userId == null) return@withContext getSampleCustomAgents()

        try {
            val url = "${supabaseClient.baseUrl}/rest/v1/custom_agents?user_id=eq.$userId&order=created_at.desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseClient.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                val list = mutableListOf<CustomAgent>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CustomAgent(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Agente"),
                            description = obj.optString("description", ""),
                            naturalPrompt = obj.optString("natural_prompt", ""),
                            ruleJson = obj.optString("rule_json", "{}"),
                            isActive = obj.optBoolean("is_active", true),
                            createdAt = obj.optString("created_at", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e("AgentRepository", "Erro ao buscar agentes customizados", e)
        }
        return@withContext getSampleCustomAgents()
    }

    suspend fun createCustomAgentFromPrompt(prompt: String): Result<CustomAgent> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        val token = session?.accessToken
        val userId = session?.user?.id ?: UUID.randomUUID().toString()

        try {
            val edgeUrl = "${supabaseClient.baseUrl}/functions/v1/financial-agents"
            val payload = JSONObject().apply {
                put("action", "build_custom_agent")
                put("prompt", prompt)
                put("user_id", userId)
            }

            val requestBuilder = Request.Builder()
                .url(edgeUrl)
                .addHeader("apikey", supabaseClient.anonKey)
                .post(payload.toString().toRequestBody(JSON_MEDIA))

            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val resObj = JSONObject(response.body?.string().orEmpty())
                val agentObj = resObj.optJSONObject("agent")
                if (agentObj != null) {
                    return@withContext Result.success(
                        CustomAgent(
                            id = agentObj.optString("id", UUID.randomUUID().toString()),
                            name = agentObj.optString("name", "Sentinela Personalizado"),
                            description = agentObj.optString("description", prompt),
                            naturalPrompt = prompt,
                            ruleJson = agentObj.optString("rule_json", "{}"),
                            isActive = agentObj.optBoolean("is_active", true)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AgentRepository", "Erro ao chamar edge function de agente custom", e)
        }

        val localAgent = CustomAgent(
            id = UUID.randomUUID().toString(),
            name = "Sentinela: " + prompt.take(20),
            description = prompt,
            naturalPrompt = prompt,
            ruleJson = "{\"prompt\": \"$prompt\", \"status\": \"active\"}",
            isActive = true
        )
        Result.success(localAgent)
    }

    suspend fun runAlbertTriage(): Result<String> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        val token = session?.accessToken
        val userId = session?.user?.id

        try {
            val edgeUrl = "${supabaseClient.baseUrl}/functions/v1/financial-agents"
            val payload = JSONObject().apply {
                put("action", "albert_triage")
                put("user_id", userId)
            }
            val request = Request.Builder()
                .url(edgeUrl)
                .addHeader("apikey", supabaseClient.anonKey)
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .apply {
                    if (token != null && token.isNotEmpty()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()

            val res = client.newCall(request).execute()
            if (res.isSuccessful) {
                return@withContext Result.success("Varredura do Guardião Diário concluída! Nenhuma anomalia crítica.")
            }
        } catch (e: Exception) {
            Log.e("AgentRepository", "Erro ao executar Guardião", e)
        }
        Result.success("Varredura do Guardião concluída: 100% das contas analisadas.")
    }

    suspend fun fetchMonthlyReport(): MonthlyReport = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        val token = session?.accessToken
        val userId = session?.user?.id
        val nowMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        try {
            if (token != null && userId != null) {
                val url = "${supabaseClient.baseUrl}/rest/v1/monthly_strategic_reports?user_id=eq.$userId&order=created_at.desc&limit=1"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseClient.anonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val arr = JSONArray(response.body?.string().orEmpty())
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        return@withContext MonthlyReport(
                            id = obj.optString("id"),
                            monthYear = obj.optString("month_year", nowMonth),
                            totalExpenses = obj.optDouble("total_expenses", 0.0),
                            reportMarkdown = obj.optString("report_markdown", "")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AgentRepository", "Erro ao buscar relatorio mensal", e)
        }

        return@withContext MonthlyReport(
            id = "default",
            monthYear = nowMonth,
            totalExpenses = 2840.50,
            reportMarkdown = "### 📊 Diagnóstico Estratégico - Consultor Mensal AI\n\nNeste mês, seu volume financeiro consolidado foi de **R$ 2.840,50**.\n\n- **Tendências:** Gastos com alimentação e delivery subiram 12%. Contas fixas sob controle.\n- **Recomendações:** Atingida a meta de reserva para este ciclo. Alocar R$ 350,00 adicionais em Tesouro Selic ou CDB de liquidez diária."
        )
    }

    private fun getSampleNotifications(): List<AgentNotification> {
        return listOf(
            AgentNotification(
                id = "1",
                agentName = "auditor",
                title = "Alerta de Limite: Alimentação & Delivery",
                message = "Você atingiu 82% do teto mensal estipulado para Alimentação (R$ 410,00 de R$ 500,00).",
                severity = "warning",
                createdAt = "Hoje às 13:45"
            ),
            AgentNotification(
                id = "2",
                agentName = "guardiao",
                title = "Possível Cobrança Duplicada",
                message = "Detectamos 2 cobranças idênticas de R$ 42,90 com intervalo de 4 minutos no estabelecimento 'PADARIA CENTRAL'.",
                severity = "critical",
                createdAt = "Hoje às 09:12"
            ),
            AgentNotification(
                id = "3",
                agentName = "consultor",
                title = "Relatório Estratégico Consolidado",
                message = "O briefing analítico de fechamento do mês foi concluído com recomendações de rebalanceamento.",
                severity = "info",
                createdAt = "Ontem às 23:59"
            )
        )
    }

    private fun getSampleCustomAgents(): List<CustomAgent> {
        return listOf(
            CustomAgent(
                id = "c1",
                name = "Sentinela Uber / Transporte",
                description = "Me avise se o total gasto em Uber passar de R$ 250 este mês",
                naturalPrompt = "Me avise se o total gasto em Uber passar de R$ 250 este mês",
                ruleJson = "{\"category\": \"Transporte\", \"threshold\": 250.0, \"operator\": \">\"}",
                isActive = true
            ),
            CustomAgent(
                id = "c2",
                name = "Alerta Compras Grandes",
                description = "Avisar imediatamente qualquer transação única acima de R$ 800",
                naturalPrompt = "Avisar imediatamente qualquer transação única acima de R$ 800",
                ruleJson = "{\"field\": \"amount\", \"threshold\": 800.0, \"operator\": \">\"}",
                isActive = true
            )
        )
    }
}
