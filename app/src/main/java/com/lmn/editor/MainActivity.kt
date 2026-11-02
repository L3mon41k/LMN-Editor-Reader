package com.lmn.editor

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var editor: EditText
    private lateinit var toolsPanel: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        // Верхняя панель
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 10, 8)
        }

        val title = TextView(this).apply {
            text = "LMN Editor"
            textSize = 20f
            setTextColor(Color.BLACK)
        }

        header.addView(title, LinearLayout.LayoutParams(0, 55, 1f))
        header.addView(makeButton("Загрузить") {})
        header.addView(makeButton("Сохранить") {})

        root.addView(header)

        // Область редактора
        editor = EditText(this).apply {
            hint = "Начните писать здесь..."
            textSize = 18f
            gravity = Gravity.TOP
            setPadding(18, 18, 18, 18)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
        }

        root.addView(
            editor,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // Панель инструментов
        toolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 5, 5, 5)
            setBackgroundColor(Color.rgb(245, 245, 248))
        }

        root.addView(toolsPanel)

        // Нижнее меню
        val bottomMenu = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(245, 245, 248))
        }

        bottomMenu.addView(
            menuButton("Выделение") {
                showTools("selection")
            }
        )

        bottomMenu.addView(
            menuButton("Формат") {
                showTools("format")
            }
        )

        bottomMenu.addView(
            menuButton("⇄\nШифр") {
                showTools("cipher")
            }
        )

        root.addView(bottomMenu)

        setContentView(root)

        showTools("selection")
    }

private fun makeButton(
    text: String,
    action: () -> Unit
): Button {
    return Button(this).apply {
        this.text = text
        textSize = 11f
        isAllCaps = false
        setOnClickListener { action() }
    }
}

private fun menuButton(
    text: String,
    action: () -> Unit
): Button {
    return Button(this).apply {
        this.text = text
        textSize = 12f
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(0, 58, 1f)
        setOnClickListener { action() }
    }
}

private fun showTools(section: String) {
    toolsPanel.removeAllViews()

    val label = TextView(this).apply {
        text = when (section) {
            "selection" -> "Выделение"
            "format" -> "Форматирование"
            else -> "Шифр"
        }
        textSize = 14f
        setTextColor(Color.DKGRAY)
        gravity = Gravity.CENTER
        setPadding(5, 5, 5, 8)
    }

    toolsPanel.addView(label)

    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    if (section == "selection") {
        row.addView(toolButton("Выделить всё") {
            editor.selectAll()
        })
        row.addView(toolButton("Копировать") {
            editor.copy()
        })
        row.addView(toolButton("Вырезать") {
            editor.cut()
        })
        row.addView(toolButton("Вставить") {
            editor.paste()
        })
    }

    toolsPanel.addView(row)
}

private fun toolButton(
    text: String,
    action: () -> Unit
): Button {
    return Button(this).apply {
        this.text = text
        textSize = 11f
        isAllCaps = false
        minWidth = 0
        setPadding(3, 0, 3, 0)
        layoutParams = LinearLayout.LayoutParams(0, 48, 1f)
        setOnClickListener { action() }
    }
}
}
