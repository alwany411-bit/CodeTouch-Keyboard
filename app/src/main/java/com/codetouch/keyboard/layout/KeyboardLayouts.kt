package com.codetouch.keyboard.layout

import kotlinx.serialization.Serializable

/**
 * تمثيل مفتاح واحد على اللوحة.
 * label: النص الظاهر على الزر.
 * output: النص الفعلي الذي يُرسَل عند الضغط العادي.
 * longPressOutput: النص الذي يُرسَل عند الضغط المطوّل (اختياري).
 * widthWeight: وزن العرض النسبي داخل الصف (1f = عرض عادي).
 */
@Serializable
data class KeyDef(
    val label: String,
    val output: String,
    val longPressOutput: String? = null,
    val widthWeight: Float = 1f,
    val isFunctional: Boolean = false // مثل Shift, Backspace, Enter, تبديل الطبقة
)

@Serializable
data class KeyRow(val keys: List<KeyDef>)

@Serializable
data class KeyboardLayer(
    val name: String,
    val rows: List<KeyRow>
)

/**
 * مجموعة الطبقات الكاملة للغة برمجة أو وضع معيّن (Python / Web / Game...).
 * هذا الكائن هو ما يُحمَّل من ملفات shared/layouts/*.json مستقبلاً.
 */
@Serializable
data class LayoutProfile(
    val profileName: String,
    val letters: KeyboardLayer,
    val symbols: KeyboardLayer,
    val shortcuts: KeyboardLayer
)

/**
 * تخطيطات افتراضية مدمجة بالكود (Fallback) حتى قبل ربط تحميل JSON فعليًا.
 * الصف الأول من الحروف يحافظ على ترتيب QWERTY القياسي كما طلبت،
 * حتى لا يفقد المستخدم ذاكرة العضلات الخاصة به.
 */
object DefaultLayouts {

    private fun letterKey(c: String) = KeyDef(label = c, output = c)

    val lettersLayer = KeyboardLayer(
        name = "Letters",
        rows = listOf(
            KeyRow("qwertyuiop".map { letterKey(it.toString()) }),
            KeyRow("asdfghjkl".map { letterKey(it.toString()) }),
            KeyRow(
                listOf(KeyDef("⇧", "SHIFT_TOGGLE", isFunctional = true, widthWeight = 1.5f)) +
                    "zxcvbnm".map { letterKey(it.toString()) } +
                    listOf(KeyDef("⌫", "BACKSPACE", isFunctional = true, widthWeight = 1.5f))
            ),
            KeyRow(
                listOf(
                    KeyDef("123", "SWITCH_SYMBOLS", isFunctional = true, widthWeight = 1.3f),
                    KeyDef(",", ","),
                    KeyDef("space", " ", widthWeight = 4f),
                    KeyDef(".", "."),
                    KeyDef("⏎", "ENTER", isFunctional = true, widthWeight = 1.3f)
                )
            )
        )
    )

    // طبقة رموز البرمجة المطلوبة صراحة في المواصفات
    val symbolsLayer = KeyboardLayer(
        name = "Symbols",
        rows = listOf(
            KeyRow(listOf("{", "}", "(", ")", "[", "]", "<", ">").map {
                KeyDef(
                    label = it,
                    output = it,
                    longPressOutput = when (it) {
                        "(" -> "()"
                        "{" -> "{}"
                        "[" -> "[]"
                        else -> null
                    }
                )
            }),
            KeyRow(listOf(";", ":", "\"", "'", "/", "\\", "|").map { KeyDef(it, it) }),
            KeyRow(listOf("=", "+", "-", "*", "%").map { KeyDef(it, it) }),
            KeyRow(
                listOf(
                    KeyDef("ABC", "SWITCH_LETTERS", isFunctional = true, widthWeight = 1.3f),
                    KeyDef("space", " ", widthWeight = 4f),
                    KeyDef("⏎", "ENTER", isFunctional = true, widthWeight = 1.3f)
                )
            )
        )
    )

    // طبقة اختصارات البرمجة المطلوبة صراحة في المواصفات
    val shortcutsLayer = KeyboardLayer(
        name = "Shortcuts",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("Ctrl+C", "CTRL_C", isFunctional = true),
                    KeyDef("Ctrl+V", "CTRL_V", isFunctional = true),
                    KeyDef("Ctrl+X", "CTRL_X", isFunctional = true),
                    KeyDef("Ctrl+Z", "CTRL_Z", isFunctional = true)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("Ctrl+S", "CTRL_S", isFunctional = true),
                    KeyDef("Ctrl+F", "CTRL_F", isFunctional = true),
                    KeyDef("Ctrl+Shift+P", "CTRL_SHIFT_P", isFunctional = true, widthWeight = 1.6f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("Tab", "TAB", isFunctional = true),
                    KeyDef("Esc", "ESCAPE", isFunctional = true),
                    KeyDef("ABC", "SWITCH_LETTERS", isFunctional = true, widthWeight = 1.3f),
                    KeyDef("⏎", "ENTER", isFunctional = true, widthWeight = 1.3f)
                )
            )
        )
    )

    val default = LayoutProfile(
        profileName = "Default Programming",
        letters = lettersLayer,
        symbols = symbolsLayer,
        shortcuts = shortcutsLayer
    )
}
