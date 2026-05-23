package edu.cit.abelgas.localloop.features.announcements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.announcements.model.AnnouncementUiState
import edu.cit.abelgas.localloop.shared.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnouncementsViewModel : ViewModel() {

    private val _uiState = MutableLiveData<AnnouncementUiState>()
    val uiState: LiveData<AnnouncementUiState> = _uiState

    private var currentPage = 0
    private val pageSize    = 5
    private var activeQuery = ""

    fun load(page: Int = 0, query: String = activeQuery, forceRefresh: Boolean = false) {
        currentPage = page
        activeQuery = query
        _uiState.value = AnnouncementUiState.Loading

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.service.getAnnouncements(page = page, size = pageSize)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!.data!!
                    var all  = body.content

                    // Client-side search filter
                    if (activeQuery.isNotBlank()) {
                        val q = activeQuery.lowercase()
                        all = all.filter {
                            it.title.lowercase().contains(q) ||
                                    (it.content?.lowercase() ?: "").contains(q)  // FIX 1: content is nullable
                        }
                    }

                    if (all.isEmpty()) {
                        _uiState.value = AnnouncementUiState.Empty(activeQuery)
                    } else {
                        val pinned = all.firstOrNull { it.isPinned }
                        val list   = all.filter { !it.isPinned }
                        _uiState.value = AnnouncementUiState.Success(
                            pinned      = pinned,
                            items       = list,
                            currentPage = body.number,        // FIX 2: PagedResponse uses .number not .pageable.pageNumber
                            totalPages  = body.totalPages
                        )
                    }
                } else {
                    _uiState.value = AnnouncementUiState.Error(
                        "Failed to load announcements (HTTP ${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AnnouncementUiState.Error(
                    e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    fun onSearch(query: String) {
        if (query == activeQuery) return
        load(page = 0, query = query)
    }

    fun onPageSelected(page: Int) {
        load(page = page, query = activeQuery)
    }
}