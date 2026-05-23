package edu.cit.abelgas.localloop.features.favorddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDetailDto
import edu.cit.abelgas.localloop.features.dashboard.model.RequesterStatsDto
import edu.cit.abelgas.localloop.shared.api.ApiService
import kotlinx.coroutines.launch

class FavorDetailViewModel : ViewModel() {

    // ── Exposed state ─────────────────────────────────────────────────────────
    private val _favor         = MutableLiveData<FavorDetailDto?>()
    val favor: LiveData<FavorDetailDto?> = _favor

    private val _requesterStats = MutableLiveData<RequesterStatsDto?>()
    val requesterStats: LiveData<RequesterStatsDto?> = _requesterStats

    private val _uiState       = MutableLiveData<DetailUiState>(DetailUiState.Loading)
    val uiState: LiveData<DetailUiState> = _uiState

    private val _actionState   = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    // ── Injected from Activity ────────────────────────────────────────────────
    lateinit var apiService: ApiService

    // ── Load favor detail — mirrors web useEffect([id]) ──────────────────────
    fun loadFavor(favorId: Long) {
        _uiState.value = DetailUiState.Loading
        viewModelScope.launch {
            try {
                val res = apiService.getFavorById(favorId)
                if (res.isSuccessful && res.body()?.data != null) {
                    val detail = res.body()!!.data!!
                    _favor.value = detail
                    _uiState.value = DetailUiState.Success

                    // Load requester stats after favor loads
                    // Mirrors web useEffect([favor?.requesterId])
                    detail.requesterId?.let { loadRequesterStats(it) }
                } else {
                    _uiState.value = DetailUiState.Error(
                        "Could not load favor details. Please try again."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(
                    "Could not load favor details. Please try again."
                )
            }
        }
    }

    // ── Load requester reputation stats ───────────────────────────────────────
    private fun loadRequesterStats(userId: Long) {
        viewModelScope.launch {
            try {
                val res = apiService.getUserReputation(userId)
                _requesterStats.value = res.body()?.data
            } catch (e: Exception) {
                _requesterStats.value = null
            }
        }
    }

    // ── Claim favor — mirrors web handleClaim() ───────────────────────────────
    fun claimFavor(favorId: Long) {
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                val res = apiService.claimFavorDetail(favorId)
                if (res.isSuccessful && res.body()?.data != null) {
                    _favor.value = res.body()!!.data
                    _actionState.value = ActionState.ClaimSuccess
                } else {
                    val msg = res.body()?.error?.message
                        ?: "Could not claim this favor."
                    _actionState.value = ActionState.Error(msg)
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(
                    "Could not claim this favor."
                )
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}

// ── UI state sealed classes ───────────────────────────────────────────────────
sealed class DetailUiState {
    object Loading : DetailUiState()
    object Success : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

sealed class ActionState {
    object Idle         : ActionState()
    object Loading      : ActionState()
    object ClaimSuccess : ActionState()
    data class Error(val message: String) : ActionState()
}