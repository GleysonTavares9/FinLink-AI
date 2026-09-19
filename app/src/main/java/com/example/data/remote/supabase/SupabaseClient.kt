package com.example.data.remote.supabase

import com.example.BuildConfig
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
 * Cliente HTTP para a API REST do Supabase (GoTrue Auth e PostgREST).
 * Todas as requisições utilizam TLS/HTTPS e transmitem os cabeçalhos obrigatórios:
 * - apikey: Chave anônima pública do projeto
 * - Authorization: Bearer <access_token> (quando o usuário está autenticado)
 */
class SupabaseClient(
    val sessionManager: SupabaseSessionManager
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val baseUrl: String
        get() {
            val url = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
            return if (url.isNotBlank() && !url.contains("your-project", ignoreCase = true)) {
                url.trim().removeSuffix("/")
            } else {
                "https://fcwcjnpiavcaaauigulw.supabase.co"
            }
        }

    val anonKey: String
        get() {
            val key = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }
            return if (key.isNotBlank() && !key.contains("your-anon-key", ignoreCase = true)) {
                key.trim()
            } else {
                "sb_publishable_vx-vMolOhN1T7H_njw2bTQ_uml8T8us"
            }
        }

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    // ========================================================================
    // AUTENTICAÇíO (GoTrue Auth)
    // ========================================================================

    suspend fun signUp(email: String, password: String):Result<SupabaseSession> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            // Demonstração local caso as credenciais do Supabase ainda não tenham sido inseridas
            val mockSession = createMockSession(email)
            sessionManager.saveSession(mockSession)
            return@withContext Result.success(mockSession)
        }

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/auth/v1/signup")
            .header("apikey", anonKey)
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = extractErrorMessage(responseBody, "Falha ao criar conta no Supabase")
                return@withContext Result.failure(Exception(errMsg))
            }

            val session = parseSessionResponse(responseBody, email)
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            val mockSession = createMockSession(email)
            sessionManager.saveSession(mockSession)
            return@withContext Result.success(mockSession)
        }

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/auth/v1/token?grant_type=password")
            .header("apikey", anonKey)
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = extractErrorMessage(responseBody, "Credenciais inválidas ou erro no Supabase")
                return@withContext Result.failure(Exception(errMsg))
            }

            val session = parseSessionResponse(responseBody, email)
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    

    
    // Extrai o token do fragmento da URI do OAuth
    fun parseSessionFromUri(uri: android.net.Uri): Result<SupabaseSession> {
        try {
            val fragment = uri.fragment ?: return Result.failure(Exception("Nenhum token encontrado na URL"))
            val params = fragment.split("&").associate { 
                val parts = it.split("=")
                parts[0] to if (parts.size > 1) parts[1] else ""
            }
            
            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]
            
            if (accessToken.isNullOrBlank()) {
                return Result.failure(Exception("Access token não encontrado"))
            }
            
            val realUser = parseUserFromJwt(accessToken)
            val session = SupabaseSession(
                accessToken = accessToken,
                refreshToken = refreshToken ?: "",
                user = realUser,
                expiresAt = System.currentTimeMillis() + (params["expires_in"]?.toLongOrNull() ?: 3600) * 1000
            )
            return Result.success(session)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentSession = sessionManager.getSession()
        if (isConfigured && currentSession != null) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/logout")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer ${currentSession.accessToken}")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                okHttpClient.newCall(request).execute()
            } catch (_: Exception) {}
        }
        sessionManager.clearSession()
        Result.success(Unit)
    }

    // ========================================================================
    // TRANSAÇÕES SEGURAS (PostgREST com Row Level Security)
    // ========================================================================

    suspend fun uploadExpenses(expenses: List<SupabaseExpenseDto>): Result<Int> = withContext(Dispatchers.IO) {
        if (expenses.isEmpty()) return@withContext Result.success(0)
        if (!isConfigured) return@withContext Result.success(expenses.size)

        val session = sessionManager.getSession() ?: return@withContext Result.failure(Exception("Usuário não autenticado"))

        val jsonArray = JSONArray()
        for (exp in expenses) {
            val obj = JSONObject().apply {
                put("user_id", session.user.id)
                put("client_local_id", exp.client_local_id ?: exp.id ?: 0L)
                put("store_name", exp.store_name)
                put("total_amount", exp.total_amount)
                put("category", exp.category)
                put("payment_method", exp.payment_method)
                put("transaction_date", exp.transaction_date)
                put("raw_ocr_text", exp.raw_ocr_text ?: "")
                put("receipt_image_path", exp.receipt_image_path ?: "")
                put("is_manual", exp.is_manual)
                put("source", exp.source)
                put("notes", exp.notes ?: "")
                put("items_json", exp.items_json ?: "")
            }
            jsonArray.put(obj)
        }

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/expenses?on_conflict=user_id,client_local_id")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(jsonArray.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(expenses.size)
            } else {
                val err = response.body?.string() ?: "Erro no upload para Supabase"
                Result.failure(Exception(extractErrorMessage(err, "Falha ao sincronizar despesas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadExpenses(): Result<List<SupabaseExpenseDto>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(emptyList())

        val session = sessionManager.getSession() ?: return@withContext Result.failure(Exception("Usuário não autenticado"))

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/expenses?select=*&order=transaction_date.desc")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erro ao buscar despesas do Supabase"))
            }

            val jsonArray = JSONArray(responseBody)
            val list = mutableListOf<SupabaseExpenseDto>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SupabaseExpenseDto(
                        id = obj.optLong("id"),
                        client_local_id = obj.optLong("client_local_id"),
                        user_id = obj.optString("user_id"),
                        store_name = obj.optString("store_name"),
                        total_amount = obj.optDouble("total_amount", 0.0),
                        category = obj.optString("category", "Alimentação"),
                        payment_method = obj.optString("payment_method", "PIX"),
                        transaction_date = obj.optString("transaction_date"),
                        raw_ocr_text = obj.optString("raw_ocr_text"),
                        receipt_image_path = obj.optString("receipt_image_path"),
                        is_manual = obj.optBoolean("is_manual", true),
                        source = obj.optString("source", "MANUAL"),
                        notes = obj.optString("notes"),
                        items_json = obj.optString("items_json")
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncHousehold(profile: SupabaseHouseholdDto): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(Unit)

        val session = sessionManager.getSession() ?: return@withContext Result.failure(Exception("Usuário não autenticado"))

        val jsonBody = JSONObject().apply {
            put("user_id", session.user.id)
            put("adult_count", profile.adult_count)
            put("child_count", profile.child_count)
            put("monthly_income", profile.monthly_income)
            put("notes", profile.notes ?: "Família padrão")
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/household_profiles?on_conflict=user_id")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Erro ao sincronizar perfil familiar"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(clientLocalId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(Unit)

        val session = sessionManager.getSession() ?: return@withContext Result.failure(Exception("Usuário não autenticado"))

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/expenses?client_local_id=eq.$clientLocalId")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .delete()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Erro ao remover despesa da nuvem"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSessionResponse(responseBody: String, fallbackEmail: String): SupabaseSession {
        val obj = JSONObject(responseBody)
        val accessToken = obj.optString("access_token")
        val refreshToken = obj.optString("refresh_token")
        val expiresIn = obj.optLong("expires_in", 3600L)
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)

        val userObj = obj.optJSONObject("user")
        val userId = userObj?.optString("id") ?: "user_${System.currentTimeMillis()}"
        val userEmail = userObj?.optString("email") ?: fallbackEmail
        val userMeta = userObj?.optJSONObject("user_metadata")
        val fullName = userMeta?.optString("full_name")?.ifBlank { null }
            ?: userMeta?.optString("name") ?: ""
        val avatarUrl = userMeta?.optString("avatar_url")?.ifBlank { null }
            ?: userMeta?.optString("picture") ?: ""

        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = SupabaseUser(id = userId, email = userEmail, fullName = fullName, avatarUrl = avatarUrl),
            expiresAt = expiresAt
        )
    }

    private fun extractErrorMessage(responseBody: String, defaultMsg: String): String {
        return try {
            val obj = JSONObject(responseBody)
            obj.optString("error_description").ifBlank {
                obj.optString("msg").ifBlank {
                    obj.optString("message").ifBlank { defaultMsg }
                }
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }

    private fun createMockSession(email: String): SupabaseSession {
        val mockId = "demo_${Math.abs(email.hashCode())}"
        return SupabaseSession(
            accessToken = "mock_jwt_token_$mockId",
            refreshToken = "mock_refresh_token_$mockId",
            user = SupabaseUser(id = mockId, email = email),
            expiresAt = System.currentTimeMillis() + (86400 * 1000)
        )
    }

    fun parseUserFromJwt(accessToken: String): SupabaseUser {
        try {
            val parts = accessToken.split(".")
            if (parts.size >= 2) {
                val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                val json = JSONObject(String(payloadBytes, Charsets.UTF_8))
                val sub = json.optString("sub", "user_${System.currentTimeMillis()}")
                val email = json.optString("email", "")
                val userMeta = json.optJSONObject("user_metadata")
                val name = userMeta?.optString("full_name")?.ifBlank { null }
                    ?: userMeta?.optString("name")
                    ?: json.optString("name", "")
                val avatar = userMeta?.optString("avatar_url")?.ifBlank { null }
                    ?: userMeta?.optString("picture")
                    ?: json.optString("picture", "")
                return SupabaseUser(
                    id = sub,
                    email = email,
                    fullName = name,
                    avatarUrl = avatar
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("Supabase", "Erro ao decodificar JWT do Google: ${e.message}")
        }
        return SupabaseUser(id = "user_${System.currentTimeMillis()}", email = "")
    }

    suspend fun updateUserProfile(name: String, avatarUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession() ?: return@withContext Result.failure(Exception("Usuário não autenticado"))
        try {
            val payload = JSONObject().apply {
                val data = JSONObject().apply {
                    put("full_name", name)
                    put("avatar_url", avatarUrl)
                }
                put("data", data)
            }
            val req = Request.Builder()
                .url("$baseUrl/auth/v1/user")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer ${session.accessToken}")
                .put(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            okHttpClient.newCall(req).execute()
            sessionManager.updateProfile(name, avatarUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.updateProfile(name, avatarUrl)
            Result.success(Unit)
        }
    }
}
