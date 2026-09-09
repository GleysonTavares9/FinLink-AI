package com.example.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerenciador de persistência da sessão do Supabase no Android.
 * Armazena com segurança os tokens JWT emitidos pelo GoTrue e os dados do usuário.
 */
class SupabaseSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("supabase_secure_session", Context.MODE_PRIVATE)

    fun saveSession(session: SupabaseSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_USER_EMAIL, session.user.email)
            .putString(KEY_USER_NAME, session.user.fullName)
            .putString(KEY_USER_AVATAR, session.user.avatarUrl)
            .putLong(KEY_EXPIRES_AT, session.expiresAt)
            .apply()
    }

    fun getSession(): SupabaseSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: ""
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val userEmail = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val userName = prefs.getString(KEY_USER_NAME, "") ?: ""
        val userAvatar = prefs.getString(KEY_USER_AVATAR, "") ?: ""
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = SupabaseUser(
                id = userId,
                email = userEmail,
                fullName = userName,
                avatarUrl = userAvatar
            ),
            expiresAt = expiresAt
        )
    }

    fun updateProfile(fullName: String, avatarUrl: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, fullName)
            .putString(KEY_USER_AVATAR, avatarUrl)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        val session = getSession()
        return session != null && session.accessToken.isNotBlank()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_full_name"
        private const val KEY_USER_AVATAR = "user_avatar_url"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
