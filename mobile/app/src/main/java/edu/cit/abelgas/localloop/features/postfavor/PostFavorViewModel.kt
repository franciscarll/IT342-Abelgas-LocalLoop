package edu.cit.abelgas.localloop.features.postfavor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.postfavor.model.CreateFavorRequest
import edu.cit.abelgas.localloop.shared.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class TitleError {
    object Empty : TitleError()
    object ExceedsLimit : TitleError()  // triggers counter warning + locks button
}

data class PostFavorUiState(
    val title: String = "",
    val titleError: TitleError? = TitleError.Empty,   // starts locked (empty)
    val selectedCategory: String? = null,
    val description: String = "",
    val dateNeeded: String? = null,        // ISO-8601 "yyyy-MM-dd" or null
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null
) {
    /** Submit button is enabled only when both required fields are valid */
    val canSubmit: Boolean
        get() = titleError == null
                && title.isNotEmpty()
                && selectedCategory != null
                && !isSubmitting
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class PostFavorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PostFavorUiState())
    val uiState: StateFlow<PostFavorUiState> = _uiState.asStateFlow()

    // ── Categories — must match backend enum values exactly ──────────────────
    // Confirmed from FavorFeedConstants.CATEGORIES pattern in your project
    val categories = listOf(
        "Errand",
        "Pet Care",
        "Plant Watering",
        "Tool Borrowing",
        "Grocery Help"
    )

    // ── Title input ──────────────────────────────────────────────────────────

    fun onTitleChanged(input: String) {
        val error: TitleError? = when {
            input.isEmpty()   -> TitleError.Empty
            input.length > 200 -> TitleError.ExceedsLimit
            else               -> null
        }
        _uiState.update { it.copy(title = input, titleError = error) }
    }

    // ── Category — exclusive single-selection ────────────────────────────────

    fun onCategorySelected(category: String) {
        _uiState.update { current ->
            current.copy(
                // Tapping the already-selected chip deselects it
                selectedCategory = if (current.selectedCategory == category) null else category
            )
        }
    }

    // ── Description ──────────────────────────────────────────────────────────

    fun onDescriptionChanged(input: String) {
        _uiState.update { it.copy(description = input) }
    }

    // ── Date ─────────────────────────────────────────────────────────────────

    fun onDateSelected(isoDate: String) {
        _uiState.update { it.copy(dateNeeded = isoDate) }
    }

    fun onDateCleared() {
        _uiState.update { it.copy(dateNeeded = null) }
    }

    // ── Clear error after Snackbar shown ────────────────────────────────────

    fun clearSubmitError() {
        _uiState.update { it.copy(submitError = null) }
    }

    // ── Submit ───────────────────────────────────────────────────────────────
    /**
     * Validates state, builds the exact JSON payload the backend expects,
     * dispatches on Dispatchers.IO, and updates state on success/failure.
     *
     * Uses ApiClient.service (shared singleton) to guarantee the same
     * Bearer token interceptor used by all other screens.
     */
    fun onPostFavorClicked() {
        val state = _uiState.value

        // Guard: should never reach here with button disabled, but safety-first
        if (!state.canSubmit) return

        val payload = CreateFavorRequest(
            title       = state.title.trim(),
            description = state.description.trim(),
            category    = state.selectedCategory!!,
            dateNeeded  = state.dateNeeded          // null → field omitted by Gson
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.service.postFavor(payload)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                } else {
                    val msg = response.body()?.error?.message
                        ?: "Failed to post favor (${response.code()})"
                    _uiState.update { it.copy(isSubmitting = false, submitError = msg) }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError  = e.localizedMessage ?: "Network error. Please try again."
                    )
                }
            }
        }
    }
}