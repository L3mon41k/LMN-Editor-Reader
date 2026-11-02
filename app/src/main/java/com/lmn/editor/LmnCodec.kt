package com.lmn.editor

import android.text.Html
import android.text.Spanned
import android.util.Base64

/**
 * LMN text container.
 *
 * The document stores Android's HTML representation as UTF-8 Base64 so all
 * supported formatting survives a save/load cycle. Cipher mode transforms the
 * complete serialized payload, therefore formatting is restored after decrypt.
 */
object LmnCodec {
    private const val HEADER = "LMN1:"
    private const val CIPHER_HEADER = "LMN1C:"

    // User-defined starter tokens. Other Unicode characters use an unambiguous
    // hexadecimal escape, so the mapping is reversible for Russian/English,
    // punctuation, digits and emoji as well.
    private val encodeMap = linkedMapOf(
        'А' to "1₽&",
        'Б' to "5#4",
        'В' to "8@!",
        'Г' to "2%7"
    )
    private val decodeMap = encodeMap.entries.associate { it.value to it.key }
    private val tokenOrder = decodeMap.keys.sortedByDescending { it.length }

    fun encodeDocument(html: String, encrypted: Boolean): String {
        val payload = Base64.encodeToString(html.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return if (encrypted) CIPHER_HEADER + encrypt(payload) else HEADER + payload
    }

    fun decodeDocument(document: String): String {
        val raw = when {
            document.startsWith(CIPHER_HEADER) -> decrypt(document.removePrefix(CIPHER_HEADER))
            document.startsWith(HEADER) -> document.removePrefix(HEADER)
            else -> document
        }
        val decoded = try {
            String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: Exception) {
            document
        }
        return decoded
    }

    fun encrypt(text: String): String {
        val out = StringBuilder()
        text.forEach { ch ->
            val token = encodeMap[ch]
            if (token != null) out.append(token)
            else out.append("~").append(ch.code.toString(16)).append(";")
        }
        return out.toString()
    }

    fun decrypt(text: String): String {
        val out = StringBuilder()
        var index = 0
        while (index < text.length) {
            if (text[index] == '~') {
                val end = text.indexOf(';', index + 1)
                if (end > index + 1) {
                    val hex = text.substring(index + 1, end)
                    val code = hex.toIntOrNull(16)
                    if (code != null && Character.isValidCodePoint(code)) {
                        out.append(String(Character.toChars(code)))
                        index = end + 1
                        continue
                    }
                }
            }
            val token = tokenOrder.firstOrNull { text.startsWith(it, index) }
            if (token != null) {
                out.append(decodeMap[token])
                index += token.length
            } else {
                out.append(text[index])
                index++
            }
        }
        return out.toString()
    }

    fun htmlFromSpanned(text: Spanned): String =
        Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

    @Suppress("DEPRECATION")
    fun spannedFromHtml(html: String): Spanned =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
}
