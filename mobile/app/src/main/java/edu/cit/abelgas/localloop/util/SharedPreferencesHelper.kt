package edu.cit.abelgas.localloop.util

import android.content.Context
import android.content.SharedPreferences
import edu.cit.abelgas.localloop.model.UserDto

class SharedPreferencesHelper(context: Context) {

    companion object {
        private const val PREF_NAME = "localloop_prefs"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_BARANGAY = "user_barangay"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_REPUTATION = "user_reputation"
        private const val KEY_USER_PROFILE_IMAGE = "user_profile_image"
    }

    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveAuthData(token: String, user: UserDto) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_BARANGAY, user.barangay)
            putString(KEY_USER_ROLE, user.role)
            putInt(KEY_USER_REPUTATION, user.reputationScore)
            putString(KEY_USER_PROFILE_IMAGE, user.profileImageUrl)
            apply()
        }
    }

    fun getBearerToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return "Bearer $token"
    }

    fun getRawToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserBarangay(): String = prefs.getString(KEY_USER_BARANGAY, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "ROLE_USER") ?: "ROLE_USER"
    fun getUserReputation(): Int = prefs.getInt(KEY_USER_REPUTATION, 0)
    fun getProfileImageUrl(): String? = prefs.getString(KEY_USER_PROFILE_IMAGE, null)
    fun isLoggedIn(): Boolean = prefs.getString(KEY_TOKEN, null) != null

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}