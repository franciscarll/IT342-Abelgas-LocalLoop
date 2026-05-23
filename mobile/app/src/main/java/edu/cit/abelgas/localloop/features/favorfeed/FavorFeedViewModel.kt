package edu.cit.abelgas.localloop.features.favorfeed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDto
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.PAGE_SIZE
import edu.cit.abelgas.localloop.features.favorfeed.model.FeedUiState
import edu.cit.abelgas.localloop.shared.api.ApiClient
import kotlinx.coroutines.launch

class FavorFeedViewModel : ViewModel() {

    // ── Exposed LiveData ──────────────────────────────────────────────────────
    private val _favors      = MutableLiveData<List<FavorDto>>(emptyList())
    val favors: LiveData<List<FavorDto>> = _favors

    private val _uiState     = MutableLiveData<FeedUiState>(FeedUiState.Loading)
    val uiState: LiveData<FeedUiState> = _uiState

    private val _currentPage = MutableLiveData(0)
    val currentPage: LiveData<Int> = _currentPage

    private val _totalPages  = MutableLiveData(1)
    val totalPages: LiveData<Int> = _totalPages

    private val _claimError  = MutableLiveData<String?>(null)
    val claimError: LiveData<String?> = _claimError

    // ── Filter state ──────────────────────────────────────────────────────────
    var searchQuery    = ""
    var activeCategory = "All"
    var statusFilter   = ""
    var sortBy         = "newest"

    // ── NO MORE lateinit apiService field ─────────────────────────────────────
    // FavorApiService is removed. We use ApiClient.service (shared ApiService)
    // directly — same singleton, same lazy token interceptor, one source of truth.

    // ── Fetch ─────────────────────────────────────────────────────────────────
    fun fetchFavors(page: Int = 0) {
        _uiState.value = FeedUiState.Loading
        viewModelScope.launch {
            try {
                val category = if (activeCategory == "All") null else activeCategory
                val status   = statusFilter.ifEmpty { "OPEN" }

                Log.d("FavorFeed", ">>> fetchFavors() fired — page=$page " +
                        "category=$category status=$status search=$searchQuery sort=$sortBy")

                // Uses shared ApiService — Bearer token injected by ApiClient interceptor
                val response = ApiClient.service.getFavors(
                    page     = page,
                    size     = PAGE_SIZE,
                    status   = status,
                    category = category
                )

                Log.d("FavorFeed", ">>> HTTP ${response.code()} — " +
                        "success=${response.body()?.success}")

                if (response.isSuccessful) {
                    val pageData = response.body()?.data

                    if (pageData != null) {
                        Log.d("FavorFeed", ">>> items=${pageData.content.size} " +
                                "totalPages=${pageData.totalPages}")

                        // Client-side search filter (mirrors web filter logic)
                        val filtered = if (searchQuery.isNotEmpty()) {
                            pageData.content.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                        it.description.contains(searchQuery, ignoreCase = true)
                            }
                        } else {
                            pageData.content
                        }

                        _favors.value      = filtered
                        _totalPages.value  = pageData.totalPages.coerceAtLeast(1)
                        _currentPage.value = page
                        _uiState.value     = if (filtered.isEmpty())
                            FeedUiState.Empty else FeedUiState.Success
                    } else {
                        Log.d("FavorFeed", ">>> response.body().data is null")
                        _uiState.value = FeedUiState.Error("Could not load favors.")
                    }
                } else {
                    // 401, 403, 500, etc. — now handled gracefully instead of throwing
                    Log.e("FavorFeed", ">>> Error ${response.code()}: " +
                            "${response.errorBody()?.string()}")
                    _uiState.value = FeedUiState.Error(
                        if (response.code() == 401)
                            "Session expired. Please log in again."
                        else
                            "Could not load favors (${response.code()})."
                    )
                }

            } catch (e: Exception) {
                Log.e("FavorFeed", ">>> EXCEPTION in fetchFavors()", e)
                _uiState.value = FeedUiState.Error("Could not load favors. Please try again.")
            }
        }
    }

    // ── Claim ─────────────────────────────────────────────────────────────────
    fun claimFavor(favorId: Long, currentUserId: Long?) {
        val favor = _favors.value?.find { it.id == favorId }
        if (favor?.requesterId == currentUserId) return

        viewModelScope.launch {
            try {
                val response = ApiClient.service.claimFavor(favorId)
                if (response.isSuccessful) {
                    _favors.value = _favors.value?.filter { it.id != favorId }
                } else {
                    _claimError.value = "Could not claim this favor (${response.code()})."
                }
            } catch (e: Exception) {
                _claimError.value = e.message ?: "Could not claim this favor."
            }
        }
    }

    fun applyFilters() = fetchFavors(0)

    fun clearClaimError() { _claimError.value = null }
}