package com.lmn.editor

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import android.text.Layout
import android.text.style.LineBackgroundSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var editor: EditText
    private lateinit var toolsPanel: LinearLayout
    private var encryptedMode = false
    private val prefs by lazy { getSharedPreferences("lmn_editor", MODE_PRIVATE) }
    private val openRequest = 41
    private val saveRequest = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 8, 4)
        }
        val title = TextView(this).apply {
            text = "LMN Editor"
            textSize = 20f
            setTextColor(Color.BLACK)
        }
        header.addView(title, LinearLayout.LayoutParams(0, 52, 1f))
        header.addView(makeButton("Загрузить") { openDocument() })
        header.addView(makeButton("Сохранить") { saveDocument() })
        root.addView(header)

        editor = EditText(this).apply {
            hint = "Начните писать здесь..."
            textSize = 18f
            gravity = Gravity.TOP
            setPadding(18, 18, 18, 18)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setText(prefs.getString("draft", "") ?: "")
        }
        root.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))

        toolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 4, 5, 4)
            setBackgroundColor(Color.rgb(245, 245, 248))
        }
        root.addView(toolsPanel)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(245, 245, 248))
        }
        bottom.addView(menuButton("Выделение") { showTools("selection") })
        bottom.addView(menuButton("Формат") { showTools("format") })
        bottom.addView(menuButton("⇄\nШифр") { showTools("cipher") })
        root.addView(bottom)
        setContentView(root)
        showTools("selection")

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!encryptedMode) prefs.edit().putString("draft", LmnCodec.htmlFromSpanned(editor.text)).apply()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun makeButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 11f
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun menuButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 12f
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(0, 58, 1f)
        setOnClickListener { action() }
    }

    private fun toolButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 11f
        isAllCaps = false
        minWidth = 0
        setPadding(5, 0, 5, 0)
        layoutParams = LinearLayout.LayoutParams(-2, 46)
        setOnClickListener { action() }
    }

    private fun showTools(section: String) {
        toolsPanel.removeAllViews()
        val label = TextView(this).apply {
            text = when (section) {
                "selection" -> "Выделение"
                "format" -> "Форматирование"
                else -> "Шифрование документа"
            }
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(4, 2, 4, 4)
        }
        toolsPanel.addView(label)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }

        when (section) {
            "selection" -> {
                row.addView(toolButton("Всё") { editor.selectAll() })
                row.addView(toolButton("Копировать") { copySelection(false) })
                row.addView(toolButton("Вырезать") { copySelection(true) })
                row.addView(toolButton("Вставить") { pasteClipboard() })
            }
            "format" -> {
                row.addView(toolButton("Жирный") { applySpan(StyleSpan(Typeface.BOLD)) })
                row.addView(toolButton("Курсив") { applySpan(StyleSpan(Typeface.ITALIC)) })
                row.addView(toolButton("Зачёрк.") { applySpan(StrikethroughSpan()) })
                row.addView(toolButton("Подчерк.") { applySpan(UnderlineSpan()) })
                row.addView(toolButton("Волна") { applySpan(WavyUnderlineSpan()) })
                row.addView(toolButton("Цвет") { chooseColor() })
            }
            "cipher" -> {
                row.addView(toolButton("Зашифровать") { encryptEditor() })
                row.addView(toolButton("Расшифровать") { decryptEditor() })
            }
        }
        toolsPanel.addView(scroll)
    }

    private fun copySelection(removeAfterCopy: Boolean) {
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        if (start == end) { toast("Сначала выделите текст"); return }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LMN text", editor.text.substring(minOf(start, end), maxOf(start, end))))
        if (removeAfterCopy) editor.text.delete(minOf(start, end), maxOf(start, end))
        toast(if (removeAfterCopy) "Текст вырезан" else "Текст скопирован")
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val value = item?.coerceToText(this)?.toString() ?: ""
        if (value.isEmpty()) { toast("Буфер обмена пуст"); return }
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        editor.text.replace(minOf(start, end), maxOf(start, end), value)
    }

    private fun applySpan(span: Any) {
        if (encryptedMode) {
            toast("Сначала расшифруйте документ")
            return
        }
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        if (start == end) {
            toast("Сначала выделите фрагмент текста")
            return
        }
        editor.text.setSpan(span, minOf(start, end), maxOf(start, end), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun chooseColor() {
        val colors = intArrayOf(
            Color.RED, 0xFFFF9800.toInt(), Color.YELLOW,
            Color.GREEN, Color.CYAN, Color.BLUE, 0xFF8E24AA.toInt(),
            Color.BLACK, 0xFF795548.toInt()
        )
        val names = arrayOf("Красный", "Оранжевый", "Жёлтый", "Зелёный", "Голубой", "Синий", "Фиолетовый", "Чёрный", "Коричневый")
        AlertDialog.Builder(this)
            .setTitle("Цвет текста")
            .setItems(names) { _, which -> applySpan(ForegroundColorSpan(colors[which])) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun encryptEditor() {
        if (encryptedMode) {
            toast("Документ уже зашифрован")
            return
        }
        val html = LmnCodec.htmlFromSpanned(editor.text)
        editor.setText(LmnCodec.encrypt(html))
        editor.setSelection(editor.text.length)
        encryptedMode = true
        toast("Документ зашифрован")
    }

    private fun decryptEditor() {
        if (!encryptedMode) {
            toast("Документ уже расшифрован")
            return
        }
        val html = LmnCodec.decrypt(editor.text.toString())
        editor.setText(LmnCodec.spannedFromHtml(html))
        editor.setSelection(editor.text.length)
        encryptedMode = false
        prefs.edit().putString("draft", html).apply()
        toast("Документ расшифрован")
    }

    private fun openDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, openRequest)
    }

    private fun saveDocument() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "document.lmn")
        }
        startActivityForResult(intent, saveRequest)
    }

    @Deprecated("Uses document picker for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            if (requestCode == openRequest) {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                if (raw.startsWith("LMN1C:")) {
                    editor.setText(raw.removePrefix("LMN1C:"))
                    encryptedMode = true
                    toast("Загружен зашифрованный документ")
                } else {
                    val html = LmnCodec.decodeDocument(raw)
                    editor.setText(LmnCodec.spannedFromHtml(html))
                    encryptedMode = false
                    prefs.edit().putString("draft", html).apply()
                    toast("Документ загружен")
                }
            } else if (requestCode == saveRequest) {
                val content = if (encryptedMode) "LMN1C:" + editor.text.toString()
                    else LmnCodec.encodeDocument(LmnCodec.htmlFromSpanned(editor.text), false)
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                toast("Документ сохранён")
            }
        } catch (e: Exception) {
            toast("Ошибка файла: ${e.localizedMessage ?: "не удалось открыть"}")
        }
    }

    override fun onPause() {
        super.onPause()
        if (!encryptedMode && ::editor.isInitialized) {
            prefs.edit().putString("draft", LmnCodec.htmlFromSpanned(editor.text)).apply()
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private class WavyUnderlineSpan : LineBackgroundSpan {
        override fun drawBackground(
            canvas: Canvas, paint: Paint, left: Int, right: Int, top: Int,
            baseline: Int, bottom: Int, text: CharSequence, start: Int,
            end: Int, lineNumber: Int
        ) {
            val oldColor = paint.color
            val oldStyle = paint.style
            val oldWidth = paint.strokeWidth
            paint.color = 0xFF5E35B1.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            val path = Path()
            val y = baseline + 4f
            path.moveTo(left.toFloat(), y)
            var x = left.toFloat()
            var up = true
            while (x < right) {
                x += 4f
                path.lineTo(x, y + if (up) -2f else 2f)
                up = !up
            }
            canvas.drawPath(path, paint)
            paint.color = oldColor
            paint.style = oldStyle
            paint.strokeWidth = oldWidth
        }
    }
}
