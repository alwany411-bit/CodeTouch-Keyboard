package com.codetouch.keyboard.suggestion

import com.codetouch.keyboard.settings.ProgrammingLanguage

/**
 * عقد عام لمحرك الاقتراحات. التطبيق الحالي (DictionarySuggestionEngine) يعتمد
 * على قواميس بادئة->كلمة (سريع، بلا شبكة، يعمل فورًا).
 *
 * لاحقًا يمكن إضافة AiSuggestionEngine (Local AI أو API خارجي) يطبّق نفس
 * الواجهة، ويُستبدل في CodeTouchIME بسطر واحد دون تعديل أي كود آخر.
 */
interface SuggestionEngine {
    /**
     * @param prefix الجزء المكتوب حتى الآن من الكلمة الحالية (مثلاً "pri")
     * @param language لغة البرمجة الحالية لتحديد القاموس المناسب
     * @return قائمة اقتراحات مرتبة بالأولوية (الأكثر ترجيحًا أولاً)
     */
    fun suggest(prefix: String, language: ProgrammingLanguage): List<Suggestion>
}

/**
 * اقتراح واحد.
 * displayText: ما يظهر للمستخدم في شريط الاقتراحات.
 * insertText: النص الفعلي الذي يُدرَج عند الاختيار (قد يحوي أقواسًا فارغة).
 * cursorOffsetFromEnd: كم خانة يجب إرجاع المؤشر للخلف بعد الإدراج
 *   (مثلاً "print()" → المؤشر يجب أن يستقر بين القوسين، أي إزاحة 1 من النهاية).
 */
data class Suggestion(
    val displayText: String,
    val insertText: String,
    val cursorOffsetFromEnd: Int = 0
)

class DictionarySuggestionEngine : SuggestionEngine {

    override fun suggest(prefix: String, language: ProgrammingLanguage): List<Suggestion> {
        if (prefix.isBlank()) return emptyList()
        val dict = ProgrammingDictionaries.forLanguage(language)
        val lowerPrefix = prefix.lowercase()

        return dict
            .filter { it.trigger.startsWith(lowerPrefix) }
            .sortedWith(compareBy({ it.trigger != lowerPrefix }, { it.trigger.length }))
            .take(3)
            .map { it.toSuggestion() }
    }
}

/** إدخال قاموس واحد: بادئة الزناد -> النص المُدرَج */
data class DictEntry(
    val trigger: String,
    val insertText: String,
    val cursorOffsetFromEnd: Int = 0
) {
    fun toSuggestion() = Suggestion(
        displayText = insertText.trim(),
        insertText = insertText,
        cursorOffsetFromEnd = cursorOffsetFromEnd
    )
}

/**
 * القواميس الفعلية لكل لغة، تغطي المتطلب المذكور صراحة:
 * pri -> print() | fun -> function | cls -> class
 * بالإضافة إلى مفردات شائعة أخرى لكل لغة لتكون تجربة استخدام حقيقية
 * وليست مجرد أمثلة معزولة.
 */
object ProgrammingDictionaries {

    private val python = listOf(
        DictEntry("pri", "print()", 1),
        DictEntry("cls", "class ", 0),
        DictEntry("def", "def ", 0),
        DictEntry("imp", "import ", 0),
        DictEntry("ret", "return ", 0),
        DictEntry("sel", "self", 0),
        DictEntry("len", "len()", 1),
        DictEntry("ran", "range()", 1),
        DictEntry("if_", "if :", 2),
        DictEntry("for", "for  in :", 0),
        DictEntry("whi", "while :", 1),
        DictEntry("eli", "elif :", 1),
        DictEntry("try", "try:", 0),
        DictEntry("exc", "except ", 0),
        DictEntry("lam", "lambda ", 0)
    )

    private val javascript = listOf(
        DictEntry("con", "console.log()", 1),
        DictEntry("fun", "function () {}", 5),
        DictEntry("cls", "class {}", 3),
        DictEntry("con2", "const ", 0),
        DictEntry("let", "let ", 0),
        DictEntry("ret", "return ", 0),
        DictEntry("imp", "import  from ''", 0),
        DictEntry("exp", "export default ", 0),
        DictEntry("if_", "if () {}", 5),
        DictEntry("for", "for (let i = 0; i < ; i++) {}", 3),
        DictEntry("arr", "() => {}", 4),
        DictEntry("asy", "async () => {}", 4),
        DictEntry("awa", "await ", 0),
        DictEntry("try", "try {} catch (e) {}", 0)
    )

    private val java = listOf(
        DictEntry("pri", "System.out.println()", 1),
        DictEntry("cls", "class  {}", 3),
        DictEntry("pub", "public ", 0),
        DictEntry("pri2", "private ", 0),
        DictEntry("sta", "static ", 0),
        DictEntry("voi", "void ", 0),
        DictEntry("ret", "return ", 0),
        DictEntry("if_", "if () {}", 5),
        DictEntry("for", "for (int i = 0; i < ; i++) {}", 3),
        DictEntry("new", "new ", 0),
        DictEntry("imp", "import ;", 1),
        DictEntry("try", "try {} catch (Exception e) {}", 0)
    )

    private val kotlin = listOf(
        DictEntry("pri", "println()", 1),
        DictEntry("fun", "fun ", 0),
        DictEntry("cls", "class  {}", 3),
        DictEntry("val", "val ", 0),
        DictEntry("var", "var ", 0),
        DictEntry("if_", "if () {}", 5),
        DictEntry("for", "for (i in ) {}", 4),
        DictEntry("whe", "when {}", 1),
        DictEntry("dat", "data class  ()", 2),
        DictEntry("nul", "null", 0),
        DictEntry("ove", "override fun ", 0)
    )

    private val cpp = listOf(
        DictEntry("cou", "cout << ", 0),
        DictEntry("cin", "cin >> ", 0),
        DictEntry("inc", "#include <>", 1),
        DictEntry("int", "int ", 0),
        DictEntry("voi", "void ", 0),
        DictEntry("cls", "class  {};", 4),
        DictEntry("if_", "if () {}", 5),
        DictEntry("for", "for (int i = 0; i < ; i++) {}", 3),
        DictEntry("ret", "return ", 0),
        DictEntry("std", "std::", 0),
        DictEntry("con", "const ", 0)
    )

    private val csharp = listOf(
        DictEntry("con", "Console.WriteLine()", 1),
        DictEntry("cls", "class  {}", 3),
        DictEntry("pub", "public ", 0),
        DictEntry("pri", "private ", 0),
        DictEntry("sta", "static ", 0),
        DictEntry("voi", "void ", 0),
        DictEntry("if_", "if () {}", 5),
        DictEntry("for", "for (int i = 0; i < ; i++) {}", 3),
        DictEntry("var", "var ", 0),
        DictEntry("usi", "using ;", 1),
        DictEntry("ret", "return ", 0)
    )

    private val htmlCss = listOf(
        DictEntry("div", "<div></div>", 6),
        DictEntry("spa", "<span></span>", 7),
        DictEntry("cla", "class=\"\"", 1),
        DictEntry("id_", "id=\"\"", 1),
        DictEntry("hre", "href=\"\"", 1),
        DictEntry("img", "<img src=\"\" />", 4),
        DictEntry("fle", "display: flex;", 0),
        DictEntry("gri", "display: grid;", 0),
        DictEntry("col", "color: ;", 1),
        DictEntry("bac", "background-color: ;", 1),
        DictEntry("mar", "margin: ;", 1),
        DictEntry("pad", "padding: ;", 1)
    )

    fun forLanguage(language: ProgrammingLanguage): List<DictEntry> = when (language) {
        ProgrammingLanguage.PYTHON -> python
        ProgrammingLanguage.JAVASCRIPT -> javascript
        ProgrammingLanguage.JAVA -> java
        ProgrammingLanguage.KOTLIN -> kotlin
        ProgrammingLanguage.CPP -> cpp
        ProgrammingLanguage.CSHARP -> csharp
        ProgrammingLanguage.HTML_CSS -> htmlCss
    }
}
