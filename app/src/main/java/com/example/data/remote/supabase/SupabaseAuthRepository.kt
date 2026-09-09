package com.example.data.remote.supabase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositório de Autenticação Segura integrado ao Supabase GoTrue Auth.
 */
class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val sessionManager: SupabaseSessionManager
) {

    private val _authState = MutableStateFlow<AuthUiState>(
        sessionManager.getSession()?.let { AuthUiState.Authenticated(it) } ?: AuthUiState.Unauthenticated
    )
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    suspend fun signUp(email: String, password: String): Result<SupabaseSession> {
        _authState.value = AuthUiState.Loading
        val result = client.signUp(email, password)
        result.fold(
            onSuccess = { session ->
                _authState.value = AuthUiState.Authenticated(session)
            },
            onFailure = { error ->
                _authState.value = AuthUiState.Error(error.message ?: "Erro ao registrar usuário")
            }
        )
        return result
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseSession> {
        _authState.value = AuthUiState.Loading
        val result = client.signIn(email, password)
        result.fold(
            onSuccess = { session ->
                _authState.value = AuthUiState.Authenticated(session)
            },
            onFailure = { error ->
                _authState.value = AuthUiState.Error(error.message ?: "Erro ao realizar login")
            }
        )
        return result
    }

    
    suspend fun handleOAuthCallback(uri: android.net.Uri) {
        _authState.value = AuthUiState.Loading
        val result = client.parseSessionFromUri(uri)
        result.fold(
            onSuccess = { session ->
                sessionManager.saveSession(session)
                _authState.value = AuthUiState.Authenticated(session)
            },
            onFailure = { error ->
                _authState.value = AuthUiState.Error(error.message ?: "Erro no OAuth")
            }
        )
    }

    suspend fun signOut() {
        client.signOut()
        _authState.value = AuthUiState.Unauthenticated
    }

    fun getCurrentSession(): SupabaseSession? {
        return sessionManager.getSession()
    }

    suspend fun updateUserProfile(name: String, avatarUrl: String): Result<Unit> {
        val res = client.updateUserProfile(name, avatarUrl)
        sessionManager.getSession()?.let { updatedSession ->
            _authState.value = AuthUiState.Authenticated(updatedSession)
        }
        return res
    }
}
