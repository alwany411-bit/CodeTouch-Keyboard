package com.codetouch.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.codetouch.keyboard.settings.PreferencesManager
import com.codetouch.keyboard.settings.ProgrammingLanguage

/**
 * شاشة الإعدادات (المرحلة 6 - تخصيص تجربة الاستخدام):
 *  - تفعيل الكيبورد من إعدادات النظام + منتقي طريقة الإدخال للاختبار السريع.
 *  - تبديل الوضع الليلي/النهاري.
 *  - تكبير/تصغير حجم الأزرار.
 *  - اختيار لغة البرمجة الحالية لمحرك الاقتراحات.
 *  - اختيار/حفظ اسم تخطيط (Profile) — أساس "حفظ أكثر من تخطيط" المطلوب.
 *  - تفعيل/تعطيل شريط الاقتراحات بالكامل.
 *
 * كل تغيير يُحفظ فورًا عبر PreferencesManager، وتقرأه CodeTouchIME
 * في كل مرة تُفتح فيها اللوحة (onStartInputView).
 */
class MainActivity : Activity() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(this)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 96)
        }
        scroll.addView(root)

        root.addView(sectionTitle("CodeTouch Keyboard — الإعدادات"))

        root.addView(Button(this).apply {
            text = "1) تفعيل الكيبورد من إعدادات النظام"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })

        root.addView(Button(this).apply {
            text = "2) اختيار CodeTouch كلوحة الإدخال الحالية"
            setOnClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        })

        root.addView(spacer())
        root.addView(sectionTitle("المظهر"))

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@MainActivity).apply {
                text = "الوضع الليلي"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Switch(this@MainActivity).apply {
                isChecked = prefs.isDarkMode
                setOnCheckedChangeListener { _, checked -> prefs.isDarkMode = checked }
            })
        })

        root.addView(TextView(this).apply { text = "حجم الأزرار" })
        root.addView(SeekBar(this).apply {
            max = 60 // يمثل 0.80 .. 1.40 بخطوات 0.01
            progress = (((prefs.keySizeScale - 0.8f) * 100).toInt()).coerceIn(0, 60)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    prefs.keySizeScale = 0.8f + (value / 100f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })

        root.addView(spacer())
        root.addView(sectionTitle("الاقتراحات الذكية"))

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@MainActivity).apply {
                text = "تفعيل شريط الاقتراحات"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Switch(this@MainActivity).apply {
                isChecked = prefs.suggestionsEnabled
                setOnCheckedChangeListener { _, checked -> prefs.suggestionsEnabled = checked }
            })
        })

        root.addView(TextView(this).apply { text = "لغة البرمجة الحالية" })
        val languages = ProgrammingLanguage.entries.toTypedArray()
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                languages.map { it.displayName }
            )
            setSelection(languages.indexOfFirst { it.id == prefs.selectedLanguage }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    prefs.selectedLanguage = languages[position].id
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        })

        root.addView(spacer())
        root.addView(sectionTitle("تخطيطات محفوظة"))

        val profileNames = listOf("Default Programming", "Python", "Web Development", "Game Development")
        root.addView(TextView(this).apply { text = "اختر تخطيطًا محفوظًا" })
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, profileNames)
            setSelection(profileNames.indexOf(prefs.selectedProfileName).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    prefs.selectedProfileName = profileNames[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        })

        setContentView(scroll)
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setPadding(0, 24, 0, 16)
    }

    private fun spacer() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 32)
    }
}
