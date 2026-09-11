package com.example.data.agent

import android.content.Context
import android.util.Log
import com.example.data.model.AgentChatMessage
import com.example.data.model.AgentChatSession
import com.example.data.model.DefaultFinancialAgents
import com.example.data.remote.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Repositório de Histórico de Conversas com IA:
 * - HÍBRIDO OFFLINE/ONLINE (Local-First com Nuvem Supabase):
 *   1. Grava no cache local (SharedPreferences em JSON) para abertura imediata sem delay.
 *   2. Sincroniza em background com o Supabase (tabela custom_agents com action_type = 'chat_session'),
 *      garantindo que o histórico nunca seja perdido mesmo ao trocar de aparelho ou reinstalar o app.
 */
class ChatHistoryRepository(
    private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val prefs = context.getSharedPreferences("finlink_chat_history", Context.MODE_PRIVATE)
    private val PREF_KEY_SESSIONS = "saved_sessions_json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Carrega todas as sessões salvas (primeiro do cache local, depois atualiza com a nuvem)
     */
    suspend fun loadSessions(): List<AgentChatSession> = withContext(Dispatchers.IO) {
        val localList = loadFromLocal()

        // Tenta sincronizar com o Supabase online em segundo plano
        if (supabaseClient.isConfigured) {
            try {
                val cloudList = fetchFromSupabase()
                if (cloudList.isNotEmpty()) {
                    // Mescla sessões da nuvem com locais, priorizando mais recentes
                    val mergedMap = mutableMapOf<String, AgentChatSession>()
                    cloudList.forEach { mergedMap[it.id] = it }
                    localList.forEach { mergedMap[it.id] = it } // Locais têm mensagens mais recentes
                    val mergedList = mergedMap.values.toList()
                    saveToLocal(mergedList)
                    return@withContext mergedList
                }
            } catch (e: Exception) {
                Log.w("ChatHistoryRepo", "Não foi possível sincronizar histórico com Supabase online: ${e.message}")
            }
        }

        return@withContext localList
    }

    /**
     * Salva ou atualiza uma sessão de conversa (tanto local quanto online no Supabase)
     */
    suspend fun saveSession(session: AgentChatSession) = withContext(Dispatchers.IO) {
        if (session.messages.isEmpty()) return@withContext

        // 1. Salva imediatamente no armazenamento local
        val currentList = loadFromLocal().filter { it.id != session.id }
        val updatedList = listOf(session) + currentList
        saveToLocal(updatedList)

        // 2. Sincroniza com o Supabase online
        if (supabaseClient.isConfigured) {
            try {
                syncToSupabase(session)
            } catch (e: Exception) {
                Log.e("ChatHistoryRepo", "Erro ao salvar sessão no Supabase online", e)
            }
        }
    }

    /**
     * Remove uma sessão de conversa do histórico
     */
    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        // 1. Remove do cache local
        val updatedList = loadFromLocal().filter { it.id != sessionId }
        saveToLocal(updatedList)

        // 2. Remove do Supabase online
        if (supabaseClient.isConfigured) {
            try {
                val url = "${supabaseClient.baseUrl}/rest/v1/custom_agents?id=eq.$sessionId"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseClient.anonKey)
                    .addHeader("Authorization", "Bearer ${supabaseClient.anonKey}")
                    .delete()
                    .build()
                httpClient.newCall(req).execute()
            } catch (e: Exception) {
                Log.e("ChatHistoryRepo", "Erro ao remover sessão da nuvem", e)
            }
        }
    }

    // ========================================================================
    // PERSISTÊNCIA LOCAL (SharedPreferences em JSON estruturado)
    // ========================================================================

    private fun loadFromLocal(): List<AgentChatSession> {
        val rawJson = prefs.getString(PREF_KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<AgentChatSession>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(deserializeSession(obj))
            }
            list
        } catch (e: Exception) {
            Log.e("ChatHistoryRepo", "Erro ao desserializar histórico local", e)
            emptyList()
        }
    }

    private fun saveToLocal(list: List<AgentChatSession>) {
        try {
            val arr = JSONArray()
            list.take(30).forEach { session -> // Mantém as 30 conversas mais recentes
                arr.put(serializeSession(session))
            }
            prefs.edit().putString(PREF_KEY_SESSIONS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("ChatHistoryRepo", "Erro ao serializar histórico local", e)
        }
    }

    // ========================================================================
    // SINCRONIZAÇÃO NUVEM COM SUPABASE (Online)
    // ========================================================================

    private fun syncToSupabase(session: AgentChatSession) {
        val messagesJson = JSONArray().apply {
            session.messages.forEach { msg ->
                put(JSONObject().apply {
                    put("id", msg.id)
                    put("isUser", msg.isUser)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("personaId", msg.agentPersona?.id ?: "orquestrador")
                })
            }
        }.toString()

        val payload = JSONObject().apply {
            put("id", session.id)
            put("name", session.title.take(120))
            put("description", session.dateFormatted)
            put("trigger_rule", messagesJson)
            put("action_type", "chat_session")
            put("is_active", true)
        }.toString()

        val url = "${supabaseClient.baseUrl}/rest/v1/custom_agents?on_conflict=id"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseClient.anonKey)
            .addHeader("Authorization", "Bearer ${supabaseClient.anonKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w("ChatHistoryRepo", "Supabase retornou código ${response.code} ao salvar sessão")
        }
    }

    private fun fetchFromSupabase(): List<AgentChatSession> {
        val url = "${supabaseClient.baseUrl}/rest/v1/custom_agents?action_type=eq.chat_session&order=created_at.desc&limit=25"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseClient.anonKey)
            .addHeader("Authorization", "Bearer ${supabaseClient.anonKey}")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val responseBody = response.body?.string().orEmpty()
        val arr = JSONArray(responseBody)
        val sessions = mutableListOf<AgentChatSession>()

        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val sessId = item.optString("id")
            val title = item.optString("name", "Conversa")
            val dateFormatted = item.optString("description", "Recente")
            val rawMessagesJson = item.optString("trigger_rule", "[]")

            val messages = mutableListOf<AgentChatMessage>()
            try {
                val msgArr = JSONArray(rawMessagesJson)
                for (j in 0 until msgArr.length()) {
                    val mObj = msgArr.getJSONObject(j)
                    val personaId = mObj.optString("personaId", "orquestrador")
                    val persona = DefaultFinancialAgents.getById(personaId)
                    messages.add(
                        AgentChatMessage(
                            id = mObj.optString("id", java.util.UUID.randomUUID().toString()),
                            isUser = mObj.optBoolean("isUser", false),
                            agentPersona = if (mObj.optBoolean("isUser")) null else persona,
                            text = mObj.optString("text", ""),
                            timestamp = mObj.optString("timestamp", "Agora")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("ChatHistoryRepo", "Erro ao desserializar mensagens da nuvem para sessão $sessId", e)
            }

            if (messages.isNotEmpty()) {
                sessions.add(
                    AgentChatSession(
                        id = sessId,
                        title = title,
                        dateFormatted = dateFormatted,
                        messages = messages
                    )
                )
            }
        }

        return sessions
    }

    // ========================================================================
    // SERIALIZADORES JSON
    // ========================================================================

    private fun serializeSession(session: AgentChatSession): JSONObject {
        return JSONObject().apply {
            put("id", session.id)
            put("title", session.title)
            put("dateFormatted", session.dateFormatted)

            val msgArray = JSONArray()
            session.messages.forEach { msg ->
                msgArray.put(JSONObject().apply {
                    put("id", msg.id)
                    put("isUser", msg.isUser)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("personaId", msg.agentPersona?.id ?: "orquestrador")
                })
            }
            put("messages", msgArray)
        }
    }

    private fun deserializeSession(obj: JSONObject): AgentChatSession {
        val id = obj.optString("id")
        val title = obj.optString("title", "Conversa")
        val dateFormatted = obj.optString("dateFormatted", "Recente")

        val msgArray = obj.optJSONArray("messages") ?: JSONArray()
        val messages = mutableListOf<AgentChatMessage>()
        for (i in 0 until msgArray.length()) {
            val mObj = msgArray.getJSONObject(i)
            val personaId = mObj.optString("personaId", "orquestrador")
            val persona = DefaultFinancialAgents.getById(personaId)
            messages.add(
                AgentChatMessage(
                    id = mObj.optString("id", java.util.UUID.randomUUID().toString()),
                    isUser = mObj.optBoolean("isUser", false),
                    agentPersona = if (mObj.optBoolean("isUser")) null else persona,
                    text = mObj.optString("text", ""),
                    timestamp = mObj.optString("timestamp", "Agora")
                )
            )
        }

        return AgentChatSession(
            id = id,
            title = title,
            dateFormatted = dateFormatted,
            messages = messages
        )
    }
}
