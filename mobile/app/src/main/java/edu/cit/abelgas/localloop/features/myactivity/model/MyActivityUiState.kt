package edu.cit.abelgas.localloop.features.myactivity.model

// ─────────────────────────────────────────────────────────────────────────────
// Tab enum — maps to the three tab labels
// ─────────────────────────────────────────────────────────────────────────────
enum class ActivityTab { POSTED, CLAIMED, COMPLETED }

// ─────────────────────────────────────────────────────────────────────────────
// Per-tab loading state
// ─────────────────────────────────────────────────────────────────────────────
sealed class ActivityListState {
    object Loading : ActivityListState()
    data class Success(val items: List<ActivityFavorDto>) : ActivityListState()
    object Empty : ActivityListState()
    data class Error(val message: String) : ActivityListState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Card action types — what button was tapped on a favor card
// ─────────────────────────────────────────────────────────────────────────────
sealed class FavorAction {
    data class Delete(val favorId: Long) : FavorAction()
    data class ConfirmComplete(val favorId: Long) : FavorAction()
    data class Reopen(val favorId: Long) : FavorAction()
    data class CancelClaim(val favorId: Long) : FavorAction()
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary stats — derived from the loaded lists
// ─────────────────────────────────────────────────────────────────────────────
data class ActivitySummary(
    val reputationScore: Int = 0,
    val postedCount: Int = 0,
    val claimedCount: Int = 0,
    val completedCount: Int = 0,
    val openCount: Int = 0,
    val postedClaimedCount: Int = 0,
    val postedCompletedCount: Int = 0,
    val completionRate: Int = 0        // 0–100 percent
)