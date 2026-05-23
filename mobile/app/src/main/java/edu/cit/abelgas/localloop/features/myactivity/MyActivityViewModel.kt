package edu.cit.abelgas.localloop.features.myactivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.myactivity.model.*
import edu.cit.abelgas.localloop.shared.api.ApiClient
import kotlinx.coroutines.launch

class MyActivityViewModel : ViewModel() {

    // ── Active tab ────────────────────────────────────────────────────────────
    private val _activeTab = MutableLiveData(ActivityTab.POSTED)
    val activeTab: LiveData<ActivityTab> = _activeTab

    // ── Lists ─────────────────────────────────────────────────────────────────
    private val _postedState = MutableLiveData<ActivityListState>(ActivityListState.Loading)
    val postedState: LiveData<ActivityListState> = _postedState

    private val _claimedState = MutableLiveData<ActivityListState>(ActivityListState.Loading)
    val claimedState: LiveData<ActivityListState> = _claimedState

    private val _completedState = MutableLiveData<ActivityListState>(ActivityListState.Loading)
    val completedState: LiveData<ActivityListState> = _completedState

    // ── Summary stats ─────────────────────────────────────────────────────────
    private val _summary = MutableLiveData(ActivitySummary())
    val summary: LiveData<ActivitySummary> = _summary

    // ── Action feedback ───────────────────────────────────────────────────────
    private val _actionLoading = MutableLiveData<Long?>(null)  // favorId in progress
    val actionLoading: LiveData<Long?> = _actionLoading

    private val _actionError = MutableLiveData<String?>(null)
    val actionError: LiveData<String?> = _actionError

    private val _actionSuccess = MutableLiveData<String?>(null)
    val actionSuccess: LiveData<String?> = _actionSuccess

    // Internal raw lists (kept for recalculating summary)
    private var postedList:    List<ActivityFavorDto> = emptyList()
    private var claimedList:   List<ActivityFavorDto> = emptyList()
    private var completedList: List<ActivityFavorDto> = emptyList()
    private var repScore: Int = 0

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        loadAll()
    }

    fun loadAll() {
        loadPosted()
        loadClaimed()
        loadReputation()
    }

    // ── Tab switching ─────────────────────────────────────────────────────────
    fun setTab(tab: ActivityTab) {
        _activeTab.value = tab
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load: my posted favors — GET /api/favors/my-posted
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadPosted() {
        _postedState.value = ActivityListState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.service.getMyPostedFavors(page = 0, size = 50)
                if (response.isSuccessful) {
                    val list = response.body()?.data?.content ?: emptyList()
                    postedList = list
                    _postedState.value = if (list.isEmpty()) ActivityListState.Empty
                    else ActivityListState.Success(list)
                    recalculateSummary()
                } else {
                    _postedState.value = ActivityListState.Error(
                        "Could not load posted favors (${response.code()}).")
                }
            } catch (e: Exception) {
                _postedState.value = ActivityListState.Error("Could not load posted favors.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load: my claimed + completed favors — GET /api/favors/my-claimed
    // Backend returns both CLAIMED and COMPLETED — we split them here
    // mirrors web: list.filter(f => f.status === 'CLAIMED') etc.
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadClaimed() {
        _claimedState.value   = ActivityListState.Loading
        _completedState.value = ActivityListState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.service.getMyClaimedFavors(page = 0, size = 50)
                if (response.isSuccessful) {
                    val all = response.body()?.data?.content ?: emptyList()
                    claimedList   = all.filter { it.status == "CLAIMED" }
                    completedList = all.filter { it.status == "COMPLETED" }

                    _claimedState.value = if (claimedList.isEmpty()) ActivityListState.Empty
                    else ActivityListState.Success(claimedList)

                    _completedState.value = if (completedList.isEmpty()) ActivityListState.Empty
                    else ActivityListState.Success(completedList)

                    recalculateSummary()
                } else {
                    val msg = "Could not load claimed favors (${response.code()})."
                    _claimedState.value   = ActivityListState.Error(msg)
                    _completedState.value = ActivityListState.Error(msg)
                }
            } catch (e: Exception) {
                val msg = "Could not load claimed favors."
                _claimedState.value   = ActivityListState.Error(msg)
                _completedState.value = ActivityListState.Error(msg)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load: reputation — GET /api/users/me/reputation
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadReputation() {
        viewModelScope.launch {
            try {
                val response = ApiClient.service.getMyReputation()
                if (response.isSuccessful) {
                    repScore = response.body()?.data?.reputationScore ?: 0
                    recalculateSummary()
                }
            } catch (_: Exception) { /* use fallback 0 */ }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Summary recalculation — mirrors web derived stats exactly
    // ─────────────────────────────────────────────────────────────────────────
    private fun recalculateSummary() {
        val posted    = postedList.size
        val completed = postedList.count { it.status == "COMPLETED" }
        val rate      = if (posted > 0) (completed * 100) / posted else 0

        _summary.value = ActivitySummary(
            reputationScore      = repScore,
            postedCount          = posted,
            claimedCount         = claimedList.size,
            completedCount       = completedList.size,
            openCount            = postedList.count { it.status == "OPEN" },
            postedClaimedCount   = postedList.count { it.status == "CLAIMED" },
            postedCompletedCount = completed,
            completionRate       = rate
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION: Delete favor — DELETE /api/favors/{id}
    // Only OPEN favors can be deleted (enforced by backend too)
    // ─────────────────────────────────────────────────────────────────────────
    fun deleteFavor(favorId: Long) {
        _actionLoading.value = favorId
        viewModelScope.launch {
            try {
                val response = ApiClient.service.deleteFavor(favorId)
                if (response.isSuccessful) {
                    postedList = postedList.filter { it.id != favorId }
                    _postedState.value = if (postedList.isEmpty()) ActivityListState.Empty
                    else ActivityListState.Success(postedList)
                    recalculateSummary()
                    _actionSuccess.value = "Favor deleted."
                } else {
                    _actionError.value = "Could not delete this favor (${response.code()})."
                }
            } catch (e: Exception) {
                _actionError.value = "Could not delete this favor."
            } finally {
                _actionLoading.value = null
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION: Confirm completion — PUT /api/favors/{id}/complete
    // Only the requester can call this on a CLAIMED favor
    // Awards +1 rep to claimer (handled server-side)
    // ─────────────────────────────────────────────────────────────────────────
    fun confirmComplete(favorId: Long) {
        _actionLoading.value = favorId
        viewModelScope.launch {
            try {
                val response = ApiClient.service.completeFavor(favorId)
                if (response.isSuccessful) {
                    val updated = response.body()?.data
                    if (updated != null) {
                        postedList = postedList.map { if (it.id == favorId) updated else it }
                        _postedState.value = ActivityListState.Success(postedList)
                    }
                    loadReputation()
                    loadClaimed()   // refresh claimed/completed counts
                    _actionSuccess.value = "Favor marked as complete! +1 rep awarded to helper."
                } else {
                    _actionError.value = "Could not confirm completion (${response.code()})."
                }
            } catch (e: Exception) {
                _actionError.value = "Could not confirm completion."
            } finally {
                _actionLoading.value = null
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION: Reopen favor — PUT /api/favors/{id}/reopen
    // Only the requester can call this on a CLAIMED favor
    // Deducts -2 rep from claimer (handled server-side)
    // ─────────────────────────────────────────────────────────────────────────
    fun reopenFavor(favorId: Long) {
        _actionLoading.value = favorId
        viewModelScope.launch {
            try {
                val response = ApiClient.service.reopenFavor(favorId)
                if (response.isSuccessful) {
                    val updated = response.body()?.data
                    if (updated != null) {
                        postedList = postedList.map { if (it.id == favorId) updated else it }
                        _postedState.value = ActivityListState.Success(postedList)
                    }
                    _actionSuccess.value = "Favor re-opened successfully."
                } else {
                    _actionError.value = "Could not re-open this favor (${response.code()})."
                }
            } catch (e: Exception) {
                _actionError.value = "Could not re-open this favor."
            } finally {
                _actionLoading.value = null
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION: Cancel claim — PUT /api/favors/{id}/cancel-claim
    // Only the claimer can call this. Deducts -1 rep (server-side)
    // ─────────────────────────────────────────────────────────────────────────
    fun cancelClaim(favorId: Long) {
        _actionLoading.value = favorId
        viewModelScope.launch {
            try {
                val response = ApiClient.service.cancelClaim(favorId)
                if (response.isSuccessful) {
                    claimedList = claimedList.filter { it.id != favorId }
                    _claimedState.value = if (claimedList.isEmpty()) ActivityListState.Empty
                    else ActivityListState.Success(claimedList)
                    loadReputation()
                    _actionSuccess.value = "Claim cancelled."
                } else {
                    _actionError.value = "Could not cancel this claim (${response.code()})."
                }
            } catch (e: Exception) {
                _actionError.value = "Could not cancel this claim."
            } finally {
                _actionLoading.value = null
            }
        }
    }

    fun clearActionError()   { _actionError.value = null }
    fun clearActionSuccess() { _actionSuccess.value = null }
}