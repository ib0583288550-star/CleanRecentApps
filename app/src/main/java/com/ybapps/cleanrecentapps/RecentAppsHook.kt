package com.ybapps.cleanrecentapps

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_LoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers

/**
 * Experimental Quickstep integration.
 *
 * Recents is not owned by Smart Launcher on many Android builds. The hook therefore
 * looks for the AOSP Quickstep RecentsView in whichever process loads it, instead of
 * assuming that the third-party home launcher owns the overview screen.
 */
class RecentAppsHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook the base View callback so this also works when RecentsView
            // inherits onAttachedToWindow rather than overriding it.
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val recents = param.thisObject as? ViewGroup ?: return
                        if (!recents.javaClass.name.contains("RecentsView", ignoreCase = true)) return
                        if (recents.tag == TAG_MARKER) return
                        recents.tag = TAG_MARKER
                        injectClearButton(recents)
                    }
                }
            )
            XposedBridge.log("CleanRecentApps: RecentsView attachment watcher active in ${lpparam.packageName}")
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: attachment hook failed in ${lpparam.packageName}: $t")
        }
    }

    private fun injectClearButton(recents: ViewGroup) {
        val context = recents.context
        val settings = XSharedPreferences("com.ybapps.cleanrecentapps", "settings").apply { reload() }
        val offsetX = settings.getInt("x", 0)
        val offsetY = settings.getInt("y", 0)
        val scale = settings.getInt("size", 100).coerceIn(70, 150) / 100f
        val button = TextView(context).apply {
            text = "ניקוי הכל"
            textSize = 14f * scale
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(
                dp(context, (18 * scale).toInt()),
                dp(context, (12 * scale).toInt()),
                dp(context, (18 * scale).toInt()),
                dp(context, (12 * scale).toInt())
            )
            background = GradientDrawable().apply {
                setColor(Color.rgb(25, 118, 210))
                cornerRadius = dp(context, 28).toFloat()
            }
            elevation = dp(context, 8).toFloat()
            contentDescription = "ניקוי כל האפליקציות האחרונות"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (!dismissAll(recents)) {
                    XposedBridge.log("CleanRecentApps: dismissAllTasks method not found on ${recents.javaClass.name}")
                }
            }
        }

        try {
            recents.addView(
                button,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            button.bringToFront()
            recents.post {
                val centeredX = (recents.width - button.width) / 2f
                val centeredY = (recents.height - button.height) / 2f
                button.x = (centeredX + dp(context, offsetX)).coerceIn(
                    0f, (recents.width - button.width).coerceAtLeast(0).toFloat()
                )
                button.y = (centeredY + dp(context, offsetY)).coerceIn(
                    0f, (recents.height - button.height).coerceAtLeast(0).toFloat()
                )
                button.bringToFront()
            }
            XposedBridge.log("CleanRecentApps: clear-all button injected into ${recents.javaClass.name}")
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: button injection failed: $t")
        }
    }

    private fun dismissAll(recents: Any): Boolean {
        return try {
            XposedHelpers.callMethod(recents, "dismissAllTasks", true)
            true
        } catch (_: Throwable) {
            try {
                XposedHelpers.callMethod(recents, "dismissAllTasks")
                true
            } catch (t: Throwable) {
                XposedBridge.log("CleanRecentApps: dismissAllTasks failed: $t")
                false
            }
        }
    }

    private fun dp(viewContext: android.content.Context, value: Int): Int =
        (value * viewContext.resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        // Stable internal tag key used only to prevent duplicate injection.
        private const val TAG_MARKER = "yb.apps.cleanrecentapps.injected"
    }
}
