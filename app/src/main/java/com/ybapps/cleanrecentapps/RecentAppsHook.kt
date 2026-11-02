package com.ybapps.cleanrecentapps

import android.app.Activity
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_LoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class RecentAppsHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        val isLauncher = pkg == "com.android.launcher3" ||
            pkg == "com.google.android.apps.nexuslauncher" ||
            pkg.contains("ginlemon", ignoreCase = true) ||
            pkg.contains("smartlauncher", ignoreCase = true)
        if (pkg != "com.android.systemui" && !isLauncher) return

        XposedBridge.log("CleanRecentApps: diagnostic hook loaded in $pkg (${lpparam.processName})")

        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        XposedBridge.log(
                            "CleanRecentApps: ACTIVITY_RESUMED pkg=$pkg class=${activity.javaClass.name}"
                        )
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: activity diagnostic hook failed: $t")
        }

        try {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        val name = view.javaClass.name
                        if (name.contains("recent", true) ||
                            name.contains("overview", true) ||
                            name.contains("task", true) ||
                            name.contains("quickstep", true)) {
                            XposedBridge.log("CleanRecentApps: RECENTS_VIEW pkg=$pkg class=$name")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: view diagnostic hook failed: $t")
        }

        try {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "onWindowFocusChanged",
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        val name = view.javaClass.name
                        if (name.contains("recent", true) ||
                            name.contains("overview", true) ||
                            name.contains("task", true) ||
                            name.contains("quickstep", true)) {
                            val focused = param.args[0] as? Boolean ?: false
                            XposedBridge.log(
                                "CleanRecentApps: RECENTS_FOCUS pkg=$pkg class=$name focused=$focused"
                            )
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("CleanRecentApps: focus diagnostic hook failed: $t")
        }
    }
}
