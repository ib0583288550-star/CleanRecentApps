package com.ybapps.cleanrecentapps

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private lateinit var xLabel: TextView
    private lateinit var yLabel: TextView
    private lateinit var sizeLabel: TextView
    private var currentX = 0
    private var currentY = 0
    private var currentSize = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentX = prefs.getInt("x", 0)
        currentY = prefs.getInt("y", 0)
        currentSize = prefs.getInt("size", 100)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(0xFF101820.toInt())
        }
        root.addView(TextView(this).apply {
            text = "ניקוי מהיר"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "התאמת כפתור ניקוי הכל"
            textSize = 16f
            setTextColor(0xFFB9C7D3.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        })

        root.addView(TextView(this).apply {
            text = "הפעלת השירות ללא LSPosed"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF16865B.toInt())
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setOnClickListener {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })
        root.addView(TextView(this).apply {
            text = "הפעל את שירות הנגישות של «ניקוי מהיר». אין צורך ב־Root או ב־LSPosed."
            textSize = 14f
            setTextColor(0xFFB9C7D3.toInt())
            setPadding(0, dp(12), 0, dp(16))
        })

        xLabel = addSetting(root, "מיקום אופקי")
        addSlider(root, "x", xLabel, -100, 100, currentX)
        yLabel = addSetting(root, "מיקום אנכי")
        addSlider(root, "y", yLabel, -100, 100, currentY)
        sizeLabel = addSetting(root, "גודל הכפתור")
        addSlider(root, "size", sizeLabel, 70, 150, currentSize)

        root.addView(TextView(this).apply {
            text = "הכפתור יופיע במסך האפליקציות האחרונות רק אם המכשיר משתמש ב־AOSP Quickstep ותומך בפעולת ניקוי המשימות."
            textSize = 14f
            setTextColor(0xFFB9C7D3.toInt())
            setPadding(0, dp(22), 0, dp(16))
        })
        root.addView(TextView(this).apply {
            text = "שמור הגדרות"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF1677D2.toInt())
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setOnClickListener {
                prefs.edit().putInt("x", currentX).putInt("y", currentY)
                    .putInt("size", currentSize).apply()
                Toast.makeText(this@MainActivity, "ההגדרות נשמרו", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(root)
    }

    private fun addSetting(parent: LinearLayout, title: String): TextView {
        val label = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, dp(14), 0, dp(4))
        }
        parent.addView(label)
        return label
    }

    private fun addSlider(parent: LinearLayout, key: String, label: TextView, min: Int, max: Int, initial: Int) {
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = (initial - min).coerceIn(0, max - min)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + min
                    when (key) {
                        "x" -> currentX = value
                        "y" -> currentY = value
                        "size" -> currentSize = value
                    }
                    label.text = when (key) {
                        "x" -> "מיקום אופקי: $value"
                        "y" -> "מיקום אנכי: $value"
                        else -> "גודל הכפתור: $value%"
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        label.text = when (key) {
            "x" -> "מיקום אופקי: $initial"
            "y" -> "מיקום אנכי: $initial"
            else -> "גודל הכפתור: $initial%"
        }
        parent.addView(seek)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}