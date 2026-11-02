package com.ybapps.cleanrecentapps

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("settings", MODE_WORLD_READABLE) }
    private lateinit var xLabel: TextView
    private lateinit var yLabel: TextView
    private lateinit var sizeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(0xFF101820.toInt())
        }

        val title = TextView(this).apply {
            text = "ניקוי מהיר"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "התאמת כפתור ניקוי הכל"
            textSize = 16f
            setTextColor(0xFFB9C7D3.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }
        root.addView(subtitle)

        xLabel = addSetting(root, "מיקום אופקי", "x")
        addSlider(root, "x", xLabel, -100, 100, prefs.getInt("x", 0))
        yLabel = addSetting(root, "מיקום אנכי", "y")
        addSlider(root, "y", yLabel, -100, 100, prefs.getInt("y", 0))
        sizeLabel = addSetting(root, "גודל הכפתור", "size")
        addSlider(root, "size", sizeLabel, 70, 150, prefs.getInt("size", 100))

        val note = TextView(this).apply {
            text = "הכפתור יופיע רק בממשק Recent Apps המבוסס על AOSP Quickstep. מיקום וגודל נקראים מההגדרות לאחר טעינת מסך האפליקציות האחרונות מחדש."
            textSize = 14f
            setTextColor(0xFFB9C7D3.toInt())
            setPadding(0, 24, 0, 12)
        }
        root.addView(note)

        val save = TextView(this).apply {
            text = "שמור הגדרות"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1677D2.toInt())
            setPadding(20, 18, 20, 18)
            setOnClickListener {
                prefs.edit()
                    .putInt("x", currentX)
                    .putInt("y", currentY)
                    .putInt("size", currentSize)
                    .apply()
                Toast.makeText(this@MainActivity, "ההגדרות נשמרו", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(save)
        setContentView(root)
    }

    private var currentX = 0
    private var currentY = 0
    private var currentSize = 100

    private fun addSetting(parent: LinearLayout, title: String, key: String): TextView {
        val label = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 18, 0, 4)
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
        when (key) {
            "x" -> currentX = initial
            "y" -> currentY = initial
            "size" -> currentSize = initial
        }
        label.text = when (key) {
            "x" -> "מיקום אופקי: $initial"
            "y" -> "מיקום אנכי: $initial"
            else -> "גודל הכפתור: $initial%"
        }
        parent.addView(seek)
    }
}
