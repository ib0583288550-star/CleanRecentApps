package com.ybapps.cleanrecentapps

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_LoadPackage
import de.robv.android.xposed.XposedBridge
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
        val recentsClass = try {
            XposedHelpers.findClassIfExists(
                "com.android.quickstep.views.RecentsView",
                lpparam.classLoader
            )
        } catch (t: Throwable) {
            null
        } ?: return

        XposedBridge.log("CleanRecentApps: found Quickstep RecentsView in ${lpparam.packageName}")

        try {
            XposedHelpers.findAndHookMethod(
                recentsClass,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val recents = param.thisObject as? ViewGroup ?: return
                        if (recents.getTag(TAG_ID) == true) return
                        recents.setTag(TAG_ID, true)

                        val context = recents.context
                        val button = TextView(context).apply {
                            text = "ניקוי הכל"
                            textSize = 14f
                            setTextColor(Color.WHITE)
                            gravity = Gravity.CENTER
                            setPadding(dp(context, 18), dp(context, 12), dp(context, 18), dp(context, 12))
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
                                val margin = dp(context, 20)
                                button.x = (recents.width - button.width - margin).coerceAtLeast(0).toFloat()
                                button.y = (recents.height - button.height - margin).coerceAtLeast(0).toFloat()
                                button.bringToFront()
                            }
                            XposedBridge.log("CleanRecentApps: clear-all button injected")
                        } catch (t: Throwable) {
                            XposedBridge.log("CleanRecentApps: button injection failed: $t")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: RecentsView hook failed: $t")
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
        private const val TAG_ID = 0x59424150
    }
}
