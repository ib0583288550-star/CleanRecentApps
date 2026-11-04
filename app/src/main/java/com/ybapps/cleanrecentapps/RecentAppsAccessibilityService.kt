package com.ybapps.cleanrecentapps

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

class RecentAppsAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var button: TextView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return
        val pkg = event.packageName?.toString().orEmpty()
        val cls = event.className?.toString().orEmpty()
        val marker = (pkg + " " + cls).lowercase()
        val isApp = pkg == packageName
        val recents = !isApp && (marker.contains("recents") || marker.contains("overview"))
        if (recents) showButton() else if (!isApp) hideButton()
    }

    private fun showButton() {
        if (button != null) return
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val scale = prefs.getInt("size", 100).coerceIn(70, 150) / 100f
        val view = TextView(this).apply {
            text = "ניקוי הכל"
            textSize = 15f * scale
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp((20 * scale).toInt()), dp((13 * scale).toInt()), dp((20 * scale).toInt()), dp((13 * scale).toInt()))
            background = GradientDrawable().apply {
                setColor(Color.rgb(25, 118, 210))
                cornerRadius = dp(30).toFloat()
            }
            elevation = dp(10).toFloat()
            contentDescription = "ניקוי כל האפליקציות האחרונות"
            setOnClickListener { clearRecentApps() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(90 + prefs.getInt("y", 0))
            x = dp(prefs.getInt("x", 0))
        }
        try {
            windowManager?.addView(view, params)
            button = view
        } catch (_: Throwable) { button = null }
    }

    private fun hideButton() {
        val old = button ?: return
        try { windowManager?.removeView(old) } catch (_: Throwable) {}
        button = null
    }

    private fun clearRecentApps() {
        val clear = findClearAll(rootInActiveWindow)
        if (clear != null) {
            clear.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }
        android.widget.Toast.makeText(this, "לא נמצא כפתור ניקוי במסך הזה", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun findClearAll(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val labels = listOf("clear all", "close all", "remove all", "נקה הכל", "סגור הכל", "ניקוי הכל")
        val label = (node.text?.toString().orEmpty() + " " + node.contentDescription?.toString().orEmpty()).lowercase()
        if (node.isClickable && labels.any { label.contains(it) }) return node
        for (i in 0 until node.childCount) {
            val found = findClearAll(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    override fun onInterrupt() {}
    override fun onDestroy() { hideButton(); super.onDestroy() }
}