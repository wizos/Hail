package com.aistra.hail.ui.auth

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class CalculatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    public val display: TextView
    private var currentInput = ""
    private var onPasswordEntered: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding(32, 32, 32, 32)

        // 显示屏
        display = TextView(context).apply {
            text = "0"
            textSize = 48f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        addView(display, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // 按键网格
        val grid = GridLayout(context).apply {
            columnCount = 4
            rowCount = 5
            setPadding(0, 16, 0, 0)
        }

        val buttons = arrayOf(
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "-",
            "0", ".", "=", "+",
            "C", "", "", ""
        )

        buttons.forEach { label ->
            if (label.isNotEmpty()) {
                val button = Button(context).apply {
                    text = label
                    textSize = 24f
                    setOnClickListener { onButtonClick(label) }
                }
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                grid.addView(button, params)
            } else {
                grid.addView(View(context), GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                })
            }
        }

        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 4f))
    }

    private fun onButtonClick(label: String) {
        when (label) {
            "C" -> {
                currentInput = ""
                display.text = "0"
            }
            "=" -> {
                // 触发密码验证
                onPasswordEntered?.invoke(currentInput)
            }
            else -> {
                currentInput += label
                display.text = currentInput.ifEmpty { "0" }
            }
        }
    }

    fun setOnPasswordEntered(listener: (String) -> Unit) {
        onPasswordEntered = listener
    }

    fun clearInput() {
        currentInput = ""
        display.text = "0"
    }
}