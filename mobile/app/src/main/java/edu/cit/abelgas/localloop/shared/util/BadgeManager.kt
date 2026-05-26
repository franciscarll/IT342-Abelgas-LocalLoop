package edu.cit.abelgas.localloop.shared.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.cit.abelgas.localloop.shared.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App-wide singleton that computes the activity badge count.
 *
 * Badge = (favors you POSTED that are now CLAIMED by someone)
 *       + (favors you CLAIMED that are still in CLAIMED status)
 *
 * Mirrors the web NotificationContext logic exactly.
 *
 * Usage:
 *   BadgeManager.refresh()          — call in onResume of every Activity
 *   BadgeManager.badgeCount.observe — observe in every Activity to update the nav badge
 */
object BadgeManager {

    private val _badgeCount = MutableLiveData(0)
    val badgeCount: LiveData<Int> get() = _badgeCount

    fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postedRes  = ApiClient.service.getMyPostedFavors(page = 0, size = 50)
                val claimedRes = ApiClient.service.getMyClaimedFavors(page = 0, size = 50)

                val postedList  = postedRes.body()?.data?.content  ?: emptyList<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>()
                val claimedList = claimedRes.body()?.data?.content ?: emptyList<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>()

                val postedClaimedCount = postedList.count  { it.status == "CLAIMED" }
                val claimedActiveCount = claimedList.count { it.status == "CLAIMED" }

                _badgeCount.postValue(postedClaimedCount + claimedActiveCount)
            } catch (_: Exception) {
                // Silently fail — keep previous value
            }
        }
    }

    fun clear() {
        _badgeCount.postValue(0)
    }
}