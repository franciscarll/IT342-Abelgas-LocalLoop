package edu.cit.abelgas.localloop.shared.util

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.badge.BadgeDrawable
import edu.cit.abelgas.localloop.R

/**
 * Extension that applies (or clears) a badge on the Activity nav_item.
 * Call this from every Activity that hosts a BottomNavigationView.
 */
fun BottomNavigationView.applyActivityBadge(count: Int) {
    val badge: BadgeDrawable = getOrCreateBadge(R.id.nav_activity)
    if (count > 0) {
        badge.isVisible = true
        badge.number    = count
        // Match LocalLoop brand color
        badge.backgroundColor =
            context.getColor(com.google.android.material.R.color.design_default_color_error)
    } else {
        badge.isVisible = false
        removeBadge(R.id.nav_activity)
    }
}