package com.ybapps.cleanrecentapps

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_LoadPackage
import de.robv.android.xposed.XposedBridge

class RecentAppsHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        if (pkg != "com.android.systemui" &&
            pkg != "com.android.launcher3" &&
            pkg != "com.google.android.apps.nexuslauncher") return

        XposedBridge.log("CleanRecentApps: loaded in " + pkg + " (" + lpparam.processName + ")")
        // The RecentsView hierarchy is OEM-specific. Add the button only after
        // detecting the exact SystemUI/Launcher implementation on each device.
    }
}
