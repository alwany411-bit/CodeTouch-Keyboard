# CodeTouch Keyboard — تصميم النظام (المرحلة 1)

## 1. نظرة عامة
لوحة مفاتيح لمسية مخصصة للمبرمجين، بنسختين مستقلتين تشتركان في نفس منطق التخطيطات (Layouts) والاقتراحات:

- **Android**: تُبنى كـ `InputMethodService` حقيقي (IME) يعمل داخل أي تطبيق (VS Code عبر SSH، تطبيقات ترمينال، متصفح، إلخ).
- **Windows**: تطبيق مستقل (WPF/WinUI3) يعرض لوحة لمس عائمة، ويُرسل ضغطات المفاتيح كأحداث نظام حقيقية (Keyboard Input Injection) إلى أي نافذة نشطة (VS Code, Visual Studio, JetBrains).
- **Shared**: ملفات JSON تصف كل تخطيط (Python/Web/Game...) بشكل موحّد، تُقرأ من كِلا النسختين.

## 2. المبادئ التصميمية
1. الأزرار بحجم لمس حقيقي (لا حجم زر كمبيوتر) — نفس ثوابت لوحة مفاتيح الهاتف (ارتفاع صف ≈ 56dp، تباعد ≈ 4dp).
2. تخطيط QWERTY الأساسي لا يتغير مكانيًا — الطبقات الإضافية (رموز/اختصارات) تُستدعى بالتبديل، لا باستبدال الحروف.
3. كل طبقة = `KeyboardLayer` مستقلة قابلة للتحميل من JSON بلا إعادة بناء الكود.
4. محرك الاقتراحات منفصل تمامًا عن واجهة الرسم (Suggestion Engine ↔ Keyboard View) عبر واجهة (Interface) موحّدة، بحيث يمكن استبداله لاحقًا بـ AI محلي أو API خارجي دون تعديل الواجهة.

## 3. البنية الطبقية (Layers) العامة
```
Layer 0: Letters      -> QWERTY / عربي
Layer 1: Symbols       -> { } ( ) [ ] < > ; : " ' / \ | = + - * %
Layer 2: Shortcuts     -> Ctrl+C, Ctrl+V, Ctrl+Z, Ctrl+S, Ctrl+F, Ctrl+Shift+P, Tab, Esc
Layer 3 (لاحقًا): Language-specific quick-symbols (حسب اللغة المكتشفة)
```

## 4. هيكل مجلدات المشروع الكامل (محدَّث بعد المراجعة والتسليم النهائي)
```
CodeTouch-Keyboard/
├── ARCHITECTURE.md
├── README.md
├── gradle/wrapper/gradle-wrapper.properties
├── settings.gradle.kts
├── build.gradle.kts
├── shared/
│   └── layouts/
│       └── python_layout.json
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/codetouch/keyboard/
│       │   ├── ime/
│       │   │   └── CodeTouchIME.kt         # InputMethodService الرئيسي
│       │   ├── view/
│       │   │   └── CodeKeyboardView.kt     # رسم الأزرار + اللمس + السحب
│       │   ├── layout/
│       │   │   └── KeyboardLayouts.kt      # تعريف الطبقات
│       │   ├── suggestion/
│       │   │   └── SuggestionEngine.kt     # واجهة + قواميس 7 لغات
│       │   ├── settings/
│       │   │   └── PreferencesManager.kt   # SharedPreferences + ProgrammingLanguage
│       │   └── MainActivity.kt             # شاشة الإعدادات (تفعيل + تخصيص)
│       └── res/
│           ├── xml/method.xml              # وصف الـ IME لنظام Android
│           ├── mipmap*/ic_launcher*         # أيقونة التطبيق (Adaptive + Legacy)
│           └── values/{strings,colors,styles}.xml
└── windows/                                    # سيُبنى في مرحلة لاحقة (بعد استقرار المنطق المشترك)
    └── README.md
```

> ملاحظة: النسخة الأصلية وضعت مشروع Android داخل مجلد فرعي `android/`.
> بعد المراجعة تم نقله ليكون جذر المستودع مباشرة، لأن هذا هو الشكل الذي
> يتوقعه Android Studio/AndroidIDE و`./gradlew` افتراضيًا بدون إعداد إضافي.

## 5. خارطة المراحل (كما طلبت) وحالتها الآن
| المرحلة | المحتوى | الحالة |
|---|---|---|
| 1 | تصميم النظام | ✅ هذه الرسالة |
| 2 | الواجهة الأساسية (Android) | ✅ هذه الرسالة |
| 3 | برمجة لوحة المفاتيح (لمس/سحب/طبقات) | ✅ نسخة أولى تعمل |
| 4 | طبقات البرمجة الكاملة + الضغط الطويل | 🔜 الرسالة القادمة |
| 5 | الذكاء والاقتراحات (pri → print/console.log) | 🔜 |
| 6 | اختبار وتحسين السرعة + نسخة Windows | 🔜 |

كل مرحلة قادمة ستُبنى فوق هذا الأساس دون إعادة كتابته.
