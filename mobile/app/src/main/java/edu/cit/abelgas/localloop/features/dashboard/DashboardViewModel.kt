package edu.cit.abelgas.localloop.features.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDto
import edu.cit.abelgas.localloop.features.dashboard.model.ReputationDto
import edu.cit.abelgas.localloop.features.dashboard.model.WeatherDto
import edu.cit.abelgas.localloop.shared.api.ApiClient
import kotlinx.coroutines.launch
import java.io.IOException

class DashboardViewModel : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 5
    }

    val categories = listOf("All", "Errand", "Pet Care", "Tool Borrowing", "Plant Watering", "Other")

    private val _activeCategory = MutableLiveData("All")
    val activeCategory: LiveData<String> = _activeCategory

    private val _weather = MutableLiveData<WeatherDto?>()
    val weather: LiveData<WeatherDto?> = _weather

    private val _weatherLoading = MutableLiveData(true)
    val weatherLoading: LiveData<Boolean> = _weatherLoading

    private val _favors = MutableLiveData<List<FavorDto>>(emptyList())
    val favors: LiveData<List<FavorDto>> = _favors

    private val _favorsLoading = MutableLiveData(true)
    val favorsLoading: LiveData<Boolean> = _favorsLoading

    private val _favorsError = MutableLiveData("")
    val favorsError: LiveData<String> = _favorsError

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    private val _claimError = MutableLiveData<String?>(null)
    val claimError: LiveData<String?> = _claimError

    private var currentPage = 0

    private val _announcements = MutableLiveData<List<AnnouncementDto>>(emptyList())
    val announcements: LiveData<List<AnnouncementDto>> = _announcements

    private val _announcementsLoading = MutableLiveData(true)
    val announcementsLoading: LiveData<Boolean> = _announcementsLoading

    private val _reputation = MutableLiveData<ReputationDto?>()
    val reputation: LiveData<ReputationDto?> = _reputation

    private val _reputationLoading = MutableLiveData(true)
    val reputationLoading: LiveData<Boolean> = _reputationLoading

    // ─────────────────────────────────────────────────────────────────────────
    // Init — weather, favors, announcements load here.
    // ✅ loadReputation() is NOT called here — it needs the fallback score
    //    from the user object, which only DashboardActivity has after reading
    //    SharedPrefs. DashboardActivity calls loadReputation(fallbackScore)
    //    explicitly in onCreate(). Calling it here caused a race where the
    //    ViewModel fired before ApiClient.init(prefs) was guaranteed complete.
    // ─────────────────────────────────────────────────────────────────────────
    init {
        loadWeather()
        loadFavors(reset = true)
        loadAnnouncements()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weather
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadWeather() {
        viewModelScope.launch {
            _weatherLoading.value = true
            try {
                val response = ApiClient.service.getWeather()
                if (response.isSuccessful) {
                    _weather.value = response.body()?.data
                } else {
                    _weather.value = null
                }
            } catch (e: IOException) {
                _weather.value = null
            } catch (e: Exception) {
                _weather.value = null
            } finally {
                _weatherLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Favors
    // ─────────────────────────────────────────────────────────────────────────
    fun loadFavors(reset: Boolean = false) {
        if (reset) {
            currentPage = 0
            _hasMore.value = true
        }
        val pageToLoad = currentPage
        viewModelScope.launch {
            if (pageToLoad == 0) _favorsLoading.value = true
            else _loadingMore.value = true

            try {
                val category = _activeCategory.value?.let { if (it == "All") null else it }
                val response = ApiClient.service.getFavors(
                    page     = pageToLoad,
                    size     = PAGE_SIZE,
                    status   = "OPEN",
                    category = category
                )

                if (response.isSuccessful) {
                    val body  = response.body()?.data
                    val list  = body?.content ?: emptyList()
                    val total = body?.totalPages ?: 0

                    if (reset) _favors.value = list
                    else _favors.value = (_favors.value ?: emptyList()) + list

                    _hasMore.value = if (total > 0) pageToLoad + 1 < total
                    else list.size == PAGE_SIZE
                    _favorsError.value = ""
                } else {
                    _favorsError.value = "Could not load favors (${response.code()})."
                }
            } catch (e: IOException) {
                _favorsError.value = "No internet connection."
            } catch (e: Exception) {
                _favorsError.value = "Could not load favors."
            } finally {
                _favorsLoading.value = false
                _loadingMore.value   = false
            }
        }
    }

    fun loadMoreFavors() {
        if (_loadingMore.value == true || _hasMore.value == false) return
        currentPage++
        loadFavors(reset = false)
    }

    fun setCategory(category: String) {
        if (_activeCategory.value == category) return
        _activeCategory.value = category
        loadFavors(reset = true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Claim
    // ─────────────────────────────────────────────────────────────────────────
    fun claimFavor(favorId: Long, currentUserId: Long?) {
        val favor = _favors.value?.find { it.id == favorId } ?: return
        if (currentUserId != null && favor.requesterId == currentUserId) return

        viewModelScope.launch {
            try {
                val response = ApiClient.service.claimFavor(favorId)
                if (response.isSuccessful) {
                    _favors.value = _favors.value?.filter { it.id != favorId }
                    _claimError.value = null
                } else {
                    _claimError.value = "Could not claim this favor (${response.code()})."
                }
            } catch (e: IOException) {
                _claimError.value = "No internet connection."
            } catch (e: Exception) {
                _claimError.value = "Could not claim this favor."
            }
        }
    }

    fun clearClaimError() { _claimError.value = null }

    // ─────────────────────────────────────────────────────────────────────────
    // Announcements
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadAnnouncements() {
        viewModelScope.launch {
            _announcementsLoading.value = true
            try {
                val response = ApiClient.service.getAnnouncements(page = 0, size = 3)
                if (response.isSuccessful) {
                    _announcements.value = response.body()?.data?.content ?: emptyList()
                } else {
                    _announcements.value = emptyList()
                }
            } catch (e: Exception) {
                _announcements.value = emptyList()
            } finally {
                _announcementsLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reputation
    // Called explicitly by DashboardActivity.onCreate() with fallback score
    // from SharedPrefs — NOT from init{} to avoid race with ApiClient.init()
    // ─────────────────────────────────────────────────────────────────────────
    fun loadReputation(fallbackScore: Int = 0) {
        viewModelScope.launch {
            _reputationLoading.value = true
            try {
                val response = ApiClient.service.getMyReputation()
                if (response.isSuccessful) {
                    _reputation.value = response.body()?.data
                } else {
                    _reputation.value = ReputationDto(reputationScore = fallbackScore)
                }
            } catch (e: Exception) {
                _reputation.value = ReputationDto(reputationScore = fallbackScore)
            } finally {
                _reputationLoading.value = false
            }
        }
    }
}