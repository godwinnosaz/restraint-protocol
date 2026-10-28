package com.brainiactechne.restraintprotocol

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Opt-in foreground-app guard. It observes package transitions only; it does not read
 * text, screenshots, messages, or media from other apps.
 */
class AppRestrictionAccessibilityService : AccessibilityService() {
    private var lastGateLaunchAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val targetPackage = event?.packageName?.toString() ?: return
        if (targetPackage == packageName) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val now = System.currentTimeMillis()
        val deadline = getSharedPreferences("rp_commitments", MODE_PRIVATE)
            .getLong("restriction_deadline", 0L)
        if (deadline <= now) return

        val protectedPackages = getSharedPreferences("rp_settings", MODE_PRIVATE)
            .getStringSet("blocked_packages", emptySet())
            .orEmpty()
        if (targetPackage !in protectedPackages || now - lastGateLaunchAt < 1_200L) return

        lastGateLaunchAt = now
        startActivity(
            Intent(this, RestrictionGateActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(RestrictionGateActivity.EXTRA_DEADLINE, deadline)
            }
        )
    }

    override fun onInterrupt() = Unit
}
