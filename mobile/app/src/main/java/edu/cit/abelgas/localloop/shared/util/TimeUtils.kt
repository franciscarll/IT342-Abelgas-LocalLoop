package edu.cit.abelgas.localloop.shared.util

/**
 * TimeUtils
 *
 * Mirrors web timeAgo() function exactly:
 *   diff < 60      → "${diff}s ago"
 *   diff < 3600    → "${floor(diff/60)} min ago"
 *   diff < 86400   → "${floor(diff/3600)} hr ago"
 *   else           → "${floor(diff/86400)} d ago"
 */
object TimeUtils {

    fun timeAgo(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateStr) ?: return ""
            val diff = ((System.currentTimeMillis() - date.time) / 1000).toInt()
            when {
                diff < 60    -> "${diff}s ago"
                diff < 3600  -> "${diff / 60} min ago"
                diff < 86400 -> "${diff / 3600} hr ago"
                else         -> "${diff / 86400} d ago"
            }
        } catch (e: Exception) {
            ""
        }
    }
}