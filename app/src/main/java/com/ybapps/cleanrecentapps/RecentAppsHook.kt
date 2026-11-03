package com.ybapps.cleanrecentapps

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Experimental AOSP Quickstep integration. OEM recents implementations may differ.
 */
class RecentAppsHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val quickstepClass = try {
            XposedHelpers.findClassIfExists("com.android.quickstep.views.RecentsView", lpparam.classLoader)
        } catch (_: Throwable) { null } ?: return

        XposedBridge.log("CleanRecentApps: Quickstep RecentsView found in ${lpparam.packageName}")
        try {
            XposedBridge.hookAllMethods(
                View::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val recents = param.thisObject as? ViewGroup ?: return
                        if (!quickstepClass.isInstance(recents)) return
                        if (recents.findViewWithTag<View>(BUTTON_MARKER) != null) return
                        injectClearButton(recents)
                    }
                }
            )
            XposedBridge.log("CleanRecentApps: attachment watcher active in ${lpparam.packageName}")
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: attachment hook failed: $t")
        }
    }

    private fun injectClearButton(recents: ViewGroup) {
        val context = recents.context
        val settings = try {
            context.contentResolver.call(
                Uri.parse("content://com.ybapps.cleanrecentapps.settings"),
                "get_settings", null, null
            )
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: cannot read settings provider: $t")
            null
        }
        val offsetX = settings?.getInt("x", 0) ?: 0
        val offsetY = settings?.getInt("y", 0) ?: 0
        val scale = ((settings?.getInt("size", 100) ?: 100).coerceIn(70, 150)) / 100f

        val button = TextView(context).apply {
            tag = BUTTON_MARKER
            text = "ניקוי הכל"
            textSize = 14f * scale
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(context, (18 * scale).toInt()), dp(context, (12 * scale).toInt()),
                dp(context, (18 * scale).toInt()), dp(context, (12 * scale).toInt()))
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
                    XposedBridge.log("CleanRecentApps: no compatible dismissAllTasks method on ${recents.javaClass.name}")
                }
            }
        }

        try {
            recents.addView(button, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            button.bringToFront()
            recents.post {
                if (button.parent !== recents || recents.width <= 0 || recents.height <= 0) return@post
                val maxX = (recents.width - button.width).coerceAtLeast(0).toFloat()
                val maxY = (recents.height - button.height).coerceAtLeast(0).toFloat()
                button.x = ((recents.width - button.width) / 2f + dp(context, offsetX)).coerceIn(0f, maxX)
                button.y = ((recents.height - button.height) / 2f + dp(context, offsetY)).coerceIn(0f, maxY)
                button.bringToFront()
            }
            XposedBridge.log("CleanRecentApps: button injected into ${recents.javaClass.name}")
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: button injection failed: $t")
        }
    }

    private fun dismissAll(recents: Any): Boolean = try {
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

    private fun dp(context: android.content.Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val BUTTON_MARKER = "yb.apps.cleanrecentapps.button"
    }
}