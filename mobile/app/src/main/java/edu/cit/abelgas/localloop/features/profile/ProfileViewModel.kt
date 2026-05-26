package edu.cit.abelgas.localloop.features.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.profile.model.ProfileResponseDto
import edu.cit.abelgas.localloop.features.profile.model.ProfileUpdateRequest
import edu.cit.abelgas.localloop.features.profile.model.RecentActivityItem
import edu.cit.abelgas.localloop.features.profile.model.UserDto
import edu.cit.abelgas.localloop.shared.api.ApiClient
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ProfileViewModel : ViewModel() {

    // Injected from Activity after init — avoids ViewModelFactory boilerplate
    var prefs: SharedPreferencesHelper? = null

    // ── Profile data ──────────────────────────────────────────────────────
    private val _profile = MutableLiveData<ProfileResponseDto?>()
    val profile: LiveData<ProfileResponseDto?> = _profile

    private val _profileLoading = MutableLiveData(false)
    val profileLoading: LiveData<Boolean> = _profileLoading

    // ── Recent activity ───────────────────────────────────────────────────
    private val _recentActivity = MutableLiveData<List<RecentActivityItem>>(emptyList())
    val recentActivity: LiveData<List<RecentActivityItem>> = _recentActivity

    // ── Save profile ──────────────────────────────────────────────────────
    private val _saveLoading = MutableLiveData(false)
    val saveLoading: LiveData<Boolean> = _saveLoading

    private val _saveSuccess = MutableLiveData<Boolean?>(null)
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    private val _saveError = MutableLiveData<String?>(null)
    val saveError: LiveData<String?> = _saveError

    // ── Photo upload ──────────────────────────────────────────────────────
    private val _photoUploading = MutableLiveData(false)
    val photoUploading: LiveData<Boolean> = _photoUploading

    // FIX 1: Added photoUploadSuccess so Activity knows when to update top bar
    private val _photoUploadSuccess = MutableLiveData<String?>(null)
    val photoUploadSuccess: LiveData<String?> = _photoUploadSuccess

    private val _photoError = MutableLiveData<String?>(null)
    val photoError: LiveData<String?> = _photoError

    // ─────────────────────────────────────────────────────────────────────
    // Load profile — GET /api/profile
    // ─────────────────────────────────────────────────────────────────────
    fun loadProfile() {
        viewModelScope.launch {
            _profileLoading.value = true
            try {
                val response = ApiClient.service.getProfile()
                if (response.isSuccessful) {
                    _profile.value = response.body()?.data
                    loadRecentActivity()
                }
            } catch (_: Exception) {
            } finally {
                _profileLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Recent Activity
    // ─────────────────────────────────────────────────────────────────────
    private fun loadRecentActivity() {
        viewModelScope.launch {
            try {
                val postedResp = ApiClient.service.getMyPostedFavors(page = 0, size = 5)
                val postedItems = if (postedResp.isSuccessful) {
                    postedResp.body()?.data?.content?.map { favor ->
                        RecentActivityItem(
                            id          = favor.id,
                            title       = favor.title,
                            status      = favor.status,
                            role        = "Posted",
                            dateDisplay = favor.createdAt
                        )
                    } ?: emptyList()
                } else emptyList()

                val claimedResp = ApiClient.service.getMyClaimedFavors(page = 0, size = 5)
                val claimedItems = if (claimedResp.isSuccessful) {
                    claimedResp.body()?.data?.content?.map { favor ->
                        RecentActivityItem(
                            id          = favor.id,
                            title       = favor.title,
                            status      = favor.status,
                            role        = "Claimed",
                            dateDisplay = favor.createdAt
                        )
                    } ?: emptyList()
                } else emptyList()

                _recentActivity.value = (postedItems + claimedItems)
                    .sortedByDescending { it.dateDisplay ?: "" }
                    .take(5)
            } catch (_: Exception) {
                _recentActivity.value = emptyList()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Update profile — PUT /api/profile
    // FIX 2: saves updated name back to SharedPreferences on success
    // ─────────────────────────────────────────────────────────────────────
    fun updateProfile(
        name: String,
        currentPassword: String?,
        newPassword: String?,
        confirmPassword: String?
    ) {
        viewModelScope.launch {
            _saveLoading.value = true
            try {
                val request = ProfileUpdateRequest(
                    name            = name,
                    currentPassword = if (!currentPassword.isNullOrEmpty()) currentPassword else null,
                    newPassword     = if (!newPassword.isNullOrEmpty()) newPassword else null,
                    confirmPassword = if (!confirmPassword.isNullOrEmpty()) confirmPassword else null
                )
                val response = ApiClient.service.updateProfile(request)
                if (response.isSuccessful) {
                    // FIX 2: persist updated name to SharedPreferences
                    prefs?.getUser()?.let { existing ->
                        prefs?.saveUser(existing.copy(name = name))
                    }
                    loadProfile()
                    _saveSuccess.value = true
                    _saveError.value = null
                } else {
                    val errorMsg = response.errorBody()?.string()
                        ?.let { parseErrorMessage(it) }
                        ?: "Could not save changes (${response.code()})."
                    _saveError.value = errorMsg
                }
            } catch (e: IOException) {
                _saveError.value = "No internet connection."
            } catch (e: Exception) {
                _saveError.value = "Could not save changes."
            } finally {
                _saveLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Upload photo — POST /api/profile/upload
    // FIX 3: saves updated profileImageUrl to SharedPreferences on success
    //         and emits photoUploadSuccess so Activity updates top bar
    // ─────────────────────────────────────────────────────────────────────
    fun uploadPhoto(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _photoUploading.value = true
            try {
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val filePart = MultipartBody.Part.createFormData(
                    name     = "file",
                    filename = "profile.jpg",
                    body     = requestBody
                )
                val response = ApiClient.service.uploadProfilePhoto(filePart)
                if (response.isSuccessful) {
                    // FIX 3a: get the returned profileImageUrl from the response
                    val updatedUser = response.body()?.data
                    val newImageUrl = updatedUser?.profileImageUrl

                    // FIX 3b: persist to SharedPreferences so next session loads it
                    prefs?.getUser()?.let { existing ->
                        prefs?.saveUser(
                            existing.copy(profileImageUrl = newImageUrl)
                        )
                    }

                    // FIX 3c: reload full profile data
                    loadProfile()

                    // FIX 3d: signal Activity with the new URL to update top bar
                    _photoUploadSuccess.value = newImageUrl
                    _photoError.value = null
                } else {
                    _photoError.value = "Could not upload photo (${response.code()})."
                }
            } catch (e: IOException) {
                _photoError.value = "No internet connection."
            } catch (e: Exception) {
                _photoError.value = "Could not upload photo."
            } finally {
                _photoUploading.value = false
            }
        }
    }

    private fun parseErrorMessage(raw: String): String? {
        return try {
            val json = org.json.JSONObject(raw)
            json.optJSONObject("error")?.optString("message")
                ?: json.optString("message").takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    fun clearSaveSuccess()        { _saveSuccess.value = null }
    fun clearSaveError()          { _saveError.value = null }
    fun clearPhotoError()         { _photoError.value = null }
    fun clearPhotoUploadSuccess() { _photoUploadSuccess.value = null }
}