package com.ybapps.cleanrecentapps

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = TextView(this).apply {
            text = "ניקוי מהיר\n\nמודול LSPosed להתאמה למסך האפליקציות האחרונות.\n\nיש להפעיל את המודול ב-LSPosed עבור SystemUI או Launcher ולאתחל."
            textSize = 18f
            setPadding(32, 48, 32, 32)
        }
        setContentView(message)
    }
}
