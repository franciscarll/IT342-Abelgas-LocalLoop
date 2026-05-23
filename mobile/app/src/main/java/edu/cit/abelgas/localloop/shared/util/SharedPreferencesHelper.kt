package edu.cit.abelgas.localloop.shared.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import edu.cit.abelgas.localloop.features.auth.model.AuthData
import edu.cit.abelgas.localloop.features.profile.model.UserDto

class SharedPreferencesHelper(context: Context) {

    companion object {
        private const val PREFS_NAME = "localloop_prefs"
        private const val KEY_TOKEN  = "token"
        private const val KEY_USER   = "user"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Token ─────────────────────────────────────────────────────────────────
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    // ── User (UserDto) ────────────────────────────────────────────────────────
    fun getUser(): UserDto? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try { gson.fromJson(json, UserDto::class.java) } catch (e: Exception) { null }
    }

    fun saveUser(user: UserDto) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    // ── Save from AuthData (called after login / register) ────────────────────
    // AuthData contains both accessToken and user — this is what LoginActivity
    // and RegisterActivity receive from the backend after successful auth.
    fun saveAuthData(authData: AuthData) {
        prefs.edit()
            .putString(KEY_TOKEN, authData.accessToken)
            .putString(KEY_USER, gson.toJson(authData.user))
            .apply()
    }

    // ── Save token + UserDto separately (overload) ────────────────────────────
    fun saveAuthData(token: String, user: UserDto) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    // ── Session check ─────────────────────────────────────────────────────────
    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    // ── Logout ────────────────────────────────────────────────────────────────
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}