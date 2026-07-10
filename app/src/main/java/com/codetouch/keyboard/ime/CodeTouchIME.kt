package com.codetouch.keyboard.ime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import com.codetouch.keyboard.layout.DefaultLayouts
import com.codetouch.keyboard.layout.KeyboardLayer
import com.codetouch.keyboard.settings.PreferencesManager
import com.codetouch.keyboard.settings.ProgrammingLanguage
import com.codetouch.keyboard.suggestion.DictionarySuggestionEngine
import com.codetouch.keyboard.suggestion.Suggestion
import com.codetouch.keyboard.suggestion.SuggestionEngine
import com.codetouch.keyboard.view.CodeKeyboardView

/**
 * الخدمة الفعلية التي يستدعيها نظام Android كلوحة مفاتيح (IME).
 *
 * تُجمّع 3 مكوّنات:
 *  1) شريط تبديل سريع بين الطبقات الثلاث (المرحلة 4).
 *  2) شريط اقتراحات (المرحلة 5) مبني فوق SuggestionEngine قابل للاستبدال.
 *  3) CodeKeyboardView نفسها لعرض/التقاط اللمس.
 *
 * تقرأ إعداداتها من PreferencesManager في كل مرة تظهر فيها (onStartInputView)
 * بحيث تعكس فورًا أي تغيير من شاشة الإعدادات (الوضع الليلي، حجم الأزرار، اللغة).
 */
class CodeTouchIME : InputMethodService() {

    private lateinit var keyboardView: CodeKeyboardView
    private lateinit var suggestionBar: LinearLayout
    private lateinit var languageChip: TextView
    private lateinit var prefs: PreferencesManager

    private val suggestionEngine: SuggestionEngine = DictionarySuggestionEngine()
    private var currentLanguage: ProgrammingLanguage = ProgrammingLanguage.PYTHON
    private var lastKnownWord: String = ""

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
    }

    override fun onCreateInputView(): View {
        currentLanguage = ProgrammingLanguage.fromId(prefs.selectedLanguage)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        root.addView(buildQuickSwitchRow())
        root.addView(buildSuggestionBar())
        root.addView(buildKeyboardView())

        applyPreferencesToViews()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // إعادة قراءة الإعدادات في كل ظهور: يعكس أي تغيير حصل من شاشة الإعدادات فورًا
        currentLanguage = ProgrammingLanguage.fromId(prefs.selectedLanguage)
        applyPreferencesToViews()
        keyboardView.currentLayer = DefaultLayouts.lettersLayer
        updateSuggestions()
    }

    private fun applyPreferencesToViews() {
        keyboardView.isDarkMode = prefs.isDarkMode
        keyboardView.keySizeScale = prefs.keySizeScale

        val baseHeightDp = 220f
        val heightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            baseHeightDp * prefs.keySizeScale,
            resources.displayMetrics
        ).toInt()
        keyboardView.layoutParams = keyboardView.layoutParams.apply { height = heightPx }

        languageChip.text = currentLanguage.displayName
        suggestionBar.visibility = if (prefs.suggestionsEnabled) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------------
    // بناء شريط التبديل السريع بين الطبقات (المرحلة 4)
    // ---------------------------------------------------------------------
    private fun buildQuickSwitchRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1A1D21"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        }

        fun quickButton(label: String, onTap: () -> Unit) = TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { onTap() }
        }

        row.addView(quickButton("ABC") { switchLayer(DefaultLayouts.lettersLayer) })
        row.addView(quickButton("{ }") { switchLayer(DefaultLayouts.symbolsLayer) })
        row.addView(quickButton("Ctrl") { switchLayer(DefaultLayouts.shortcutsLayer) })

        languageChip = TextView(this).apply {
            text = currentLanguage.displayName
            setTextColor(Color.parseColor("#8AB4FF"))
            gravity = Gravity.CENTER
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { cycleLanguage() }
        }
        row.addView(languageChip)

        return row
    }

    private fun switchLayer(layer: KeyboardLayer) {
        keyboardView.currentLayer = layer
    }

    /** تبديل سريع للغة البرمجة الحالية دون الحاجة للدخول لشاشة الإعدادات */
    private fun cycleLanguage() {
        val all = ProgrammingLanguage.entries
        val nextIndex = (all.indexOf(currentLanguage) + 1) % all.size
        currentLanguage = all[nextIndex]
        prefs.selectedLanguage = currentLanguage.id
        languageChip.text = currentLanguage.displayName
        updateSuggestions()
    }

    // ---------------------------------------------------------------------
    // بناء شريط الاقتراحات (المرحلة 5)
    // ---------------------------------------------------------------------
    private fun buildSuggestionBar(): LinearLayout {
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(38)
            )
        }
        return suggestionBar
    }

    private fun updateSuggestions() {
        if (!::suggestionBar.isInitialized) return
        suggestionBar.removeAllViews()
        if (!prefs.suggestionsEnabled) return

        val word = extractCurrentWord()
        lastKnownWord = word
        val suggestions = suggestionEngine.suggest(word, currentLanguage)

        suggestions.forEach { suggestion ->
            val chip = TextView(this).apply {
                text = suggestion.displayText
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { commitSuggestion(suggestion) }
            }
            suggestionBar.addView(chip)
        }
    }

    /** يستخرج الكلمة الجارية قبل المؤشر مباشرة (أحرف/أرقام/شرطة سفلية فقط) */
    private fun extractCurrentWord(): String {
        val ic = currentInputConnection ?: return ""
        val textBefore = ic.getTextBeforeCursor(40, 0)?.toString() ?: return ""
        val match = Regex("[A-Za-z0-9_]*$").find(textBefore)
        return match?.value ?: ""
    }

    private fun commitSuggestion(suggestion: Suggestion) {
        val ic = currentInputConnection ?: return
        if (lastKnownWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastKnownWord.length, 0)
        }
        ic.commitText(suggestion.insertText, 1)
        repeat(suggestion.cursorOffsetFromEnd) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
        }
        lastKnownWord = ""
        updateSuggestions()
    }

    // ---------------------------------------------------------------------
    // بناء لوحة المفاتيح نفسها (المرحلة 3)
    // ---------------------------------------------------------------------
    private fun buildKeyboardView(): CodeKeyboardView {
        keyboardView = CodeKeyboardView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220)
            )
            currentLayer = DefaultLayouts.lettersLayer

            onKeyOutput = { text ->
                currentInputConnection?.commitText(text, 1)
                updateSuggestions()
            }

            onFunctionalKey = { action -> handleFunctionalKey(action) }
        }
        return keyboardView
    }

    private fun handleFunctionalKey(action: String) {
        val ic = currentInputConnection ?: return
        when (action) {
            "SWITCH_SYMBOLS" -> keyboardView.currentLayer = DefaultLayouts.symbolsLayer
            "SWITCH_LETTERS" -> keyboardView.currentLayer = DefaultLayouts.lettersLayer
            "SWITCH_SHORTCUTS" -> keyboardView.currentLayer = DefaultLayouts.shortcutsLayer

            "BACKSPACE" -> {
                ic.deleteSurroundingText(1, 0)
                updateSuggestions()
            }
            "ENTER" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                updateSuggestions()
            }
            "TAB" -> sendSimpleKey(KeyEvent.KEYCODE_TAB)
            "ESCAPE" -> sendSimpleKey(KeyEvent.KEYCODE_ESCAPE)

            // اختصارات حقيقية عبر Meta State بحيث يفهمها المحرر المستهدف كاختصار فعلي
            "CTRL_C" -> sendCtrlCombo(KeyEvent.KEYCODE_C)
            "CTRL_V" -> sendCtrlCombo(KeyEvent.KEYCODE_V)
            "CTRL_X" -> sendCtrlCombo(KeyEvent.KEYCODE_X)
            "CTRL_Z" -> sendCtrlCombo(KeyEvent.KEYCODE_Z)
            "CTRL_S" -> sendCtrlCombo(KeyEvent.KEYCODE_S)
            "CTRL_F" -> sendCtrlCombo(KeyEvent.KEYCODE_F)
            "CTRL_SHIFT_P" -> sendCtrlCombo(KeyEvent.KEYCODE_P, shift = true)
        }
    }

    private fun sendSimpleKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun sendCtrlCombo(keyCode: Int, shift: Boolean = false) {
        val ic = currentInputConnection ?: return
        var metaState = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

        val downTime = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        ic.sendKeyEvent(
            KeyEvent(downTime, System.currentTimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, metaState)
        )
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()
}
