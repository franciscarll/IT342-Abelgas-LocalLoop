package edu.cit.abelgas.localloop.features.favorfeed.model

sealed class FeedUiState {
    object Loading : FeedUiState()
    object Success : FeedUiState()
    object Empty   : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}