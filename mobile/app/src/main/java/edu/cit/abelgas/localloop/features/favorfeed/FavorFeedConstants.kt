package edu.cit.abelgas.localloop.features.favorfeed

object FavorFeedConstants {

    const val PAGE_SIZE = 5

    val CATEGORIES = listOf(
        "All", "Errand", "Pet Care", "Plant Watering", "Tool Borrowing", "Other"
    )

    // Mirrors web CATEGORY_ICONS exactly
    val CATEGORY_ICONS = mapOf(
        "Errand"          to "🛒",
        "Pet Care"        to "🐾",
        "Tool Borrowing"  to "🔧",
        "Plant Watering"  to "🌿",
        "Other"           to "📦"
    )

    // Mirrors web CATEGORY_TAG_COLORS — (bgHex, textHex)
    val CATEGORY_TAG_COLORS = mapOf(
        "Errand"          to Pair("#FFF3E0", "#E65100"),
        "Pet Care"        to Pair("#F3E5F5", "#7B1FA2"),
        "Tool Borrowing"  to Pair("#E8F5E9", "#2E7D32"),
        "Plant Watering"  to Pair("#E0F7FA", "#00695C"),
        "Other"           to Pair("#F5F5F5", "#424242")
    )

    // Mirrors web STATUS_COLORS — (bgHex, textHex)
    val STATUS_COLORS = mapOf(
        "OPEN"      to Pair("#FFF3E0", "#E65100"),
        "CLAIMED"   to Pair("#FFF8E1", "#F57F17"),
        "COMPLETED" to Pair("#E8F5E9", "#2E7D32")
    )

    // Mirrors web AVATAR_COLORS array exactly
    val AVATAR_COLORS = listOf(
        0xFFC8601A.toInt(), 0xFF2E86AB.toInt(), 0xFFA23B72.toInt(), 0xFFF18F01.toInt(),
        0xFF44BBA4.toInt(), 0xFFE94F37.toInt(), 0xFF6B4226.toInt(), 0xFF3A86FF.toInt()
    )
}