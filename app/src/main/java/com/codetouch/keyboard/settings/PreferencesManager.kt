package com.codetouch.keyboard.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * غلاف بسيط فوق SharedPreferences لتخزين تفضيلات المستخدم واسترجاعها
 * من كل من MainActivity (الإعدادات) و CodeTouchIME (وقت الاستخدام الفعلي).
 * هذا هو أساس "حفظ أكثر من تخطيط" و"تخصيص مكان/حجم الأزرار" المطلوبين.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true) // الوضع الليلي هو الافتراضي
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    /** نسبة تكبير/تصغير الأزرار: 0.85 (مضغوط) .. 1.0 (عادي) .. 1.3 (كبير) */
    var keySizeScale: Float
        get() = prefs.getFloat(KEY_SIZE_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SIZE_SCALE, value.coerceIn(0.8f, 1.4f)).apply()

    /** لغة البرمجة الحالية المستخدمة لمحرك الاقتراحات */
    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, ProgrammingLanguage.PYTHON.id) ?: ProgrammingLanguage.PYTHON.id
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    /** اسم تخطيط الأزرار المحفوظ الحالي (Python / Web / Game / مخصص) */
    var selectedProfileName: String
        get() = prefs.getString(KEY_PROFILE, "Default Programming") ?: "Default Programming"
        set(value) = prefs.edit().putString(KEY_PROFILE, value).apply()

    /** تفعيل/تعطيل الاقتراحات بالكامل */
    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SUGGESTIONS_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "codetouch_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SIZE_SCALE = "key_size_scale"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_PROFILE = "selected_profile"
        private const val KEY_SUGGESTIONS_ENABLED = "suggestions_enabled"
    }
}

/** لغات البرمجة المدعومة في محرك الاقتراحات (المرحلة 5) */
enum class ProgrammingLanguage(val id: String, val displayName: String) {
    PYTHON("python", "Python"),
    CPP("cpp", "C++"),
    JAVASCRIPT("javascript", "JavaScript"),
    JAVA("java", "Java"),
    KOTLIN("kotlin", "Kotlin"),
    CSHARP("csharp", "C#"),
    HTML_CSS("html_css", "HTML/CSS");

    companion object {
        fun fromId(id: String): ProgrammingLanguage =
            entries.firstOrNull { it.id == id } ?: PYTHON
    }
}
