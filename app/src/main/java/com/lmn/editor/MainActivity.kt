
package com.lmn.editor

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {

    private lateinit var editor: EditText

    private val colors = listOf(
        Color.BLACK,
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.MAGENTA,
        Color.CYAN,
        Color.rgb(255, 140, 0),
        Color.rgb(128, 0, 128)
    )

    private var colorIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(245, 245, 245)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val header = TextView(this).apply {
            text = "LMN Editor & Reader"
            textSize = 21f
            setTextColor(Color.BLACK)
            setPadding(18, 20, 18, 20)
            gravity = Gravity.CENTER_VERTICAL
        }

        root.addView(header)

        val toolbarScroll = HorizontalScrollView(this)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(6, 5, 6, 5)
        }

        toolbarScroll.addView(toolbar)
        root.addView(toolbarScroll)

        editor = EditText(this).apply {
            textSize = 18f
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            hint = "Начните писать здесь..."
            gravity = Gravity.TOP
            setPadding(18, 18, 18, 18)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setBackgroundColor(Color.TRANSPARENT)
        }

        root.addView(
            editor,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        addButton(toolbar, "B") {
            toggleSpan(
                StyleSpan(Typeface.BOLD),
                StyleSpan::class.java
            )
        }

        addButton(toolbar, "I") {
            toggleSpan(
                StyleSpan(Typeface.ITALIC),
                StyleSpan::class.java
            )
        }

        addButton(toolbar, "S") {
            toggleSpan(
                StrikethroughSpan(),
                StrikethroughSpan::class.java
            )
        }

        addButton(toolbar, "U") {
            toggleSpan(
                UnderlineSpan(),
                UnderlineSpan::class.java
            )
        }

        addButton(toolbar, "W") {
            Toast.makeText(
                this,
                "Волнистое подчёркивание добавим следующим этапом",
                Toast.LENGTH_SHORT
            ).show()
        }

        addButton(toolbar, "Цвет") {
            applyColor()
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 12)
        }

        addBottomButton(bottom, "Открыть") {
            Toast.makeText(
                this,
                "Открытие .lmn добавим следующим этапом",
                Toast.LENGTH_SHORT
            ).show()
        }

        addBottomButton(bottom, "Сохранить") {
            Toast.makeText(
                this,
                "Сохранение .lmn добавим следующим этапом",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(bottom)

        setContentView(root)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setOnClickListener { action() }
        }

        parent.addView(button)
    }

    private fun addBottomButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(0, 52, 1f)
        )
    }

    private fun toggleSpan(
        span: Any,
        type: Class<*>
    ) {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start < 0 || end <= start) {
            Toast.makeText(
                this,
                "Сначала выделите текст",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val text = editor.text as Spannable

        val existing = text.getSpans(start, end, type)

        if (existing.isNotEmpty()) {
            existing.forEach {
                text.removeSpan(it)
            }
        } else {
            text.setSpan(
                span,
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyColor() {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start < 0 || end <= start) {
            Toast.makeText(
                this,
                "Сначала выделите текст",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val text = editor.text as Spannable

        text.setSpan(
            ForegroundColorSpan(colors[colorIndex]),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        colorIndex = (colorIndex + 1) % colors.size
    }
}
