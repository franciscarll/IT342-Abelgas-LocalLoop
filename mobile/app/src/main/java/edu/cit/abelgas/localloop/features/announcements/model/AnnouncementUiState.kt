package edu.cit.abelgas.localloop.features.announcements.model

import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto

sealed class AnnouncementUiState {
    object Loading : AnnouncementUiState()

    data class Success(
        val pinned: AnnouncementDto?,
        val items: List<AnnouncementDto>,
        val currentPage: Int,
        val totalPages: Int
    ) : AnnouncementUiState()

    data class Empty(val query: String = "") : AnnouncementUiState()

    data class Error(val message: String) : AnnouncementUiState()
}