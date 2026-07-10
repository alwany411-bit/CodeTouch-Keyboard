package com.codetouch.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.codetouch.keyboard.layout.KeyDef
import com.codetouch.keyboard.layout.KeyboardLayer

/**
 * View مخصّص يرسم صفوف المفاتيح الحالية ويتعامل مع اللمس بنفس منطق
 * كيبورد الهاتف: نقطة لمس واحدة تتحرك، الزر الحالي تحتها هو المُفعَّل،
 * مع دعم الضغط المطوّل، السحب بين الحروف، والاهتزاز.
 *
 * الفصل المتعمّد: هذا الـ View لا "يعرف" شيئًا عن IME أو عن نظام الاقتراحات؛
 * فقط يستدعي onKeyOutput عند إدخال فعلي. هذا يسمح باستبدال المحرك لاحقًا
 * دون لمس كود الرسم/اللمس.
 */
class CodeKeyboardView(context: Context) : View(context) {

    var currentLayer: KeyboardLayer = com.codetouch.keyboard.layout.DefaultLayouts.lettersLayer
        set(value) {
            field = value
            if (width > 0 && height > 0) layoutKeyRects(width.toFloat(), height.toFloat())
            invalidate()
        }

    /** نسبة حجم الأزرار (تخصيص المستخدم - المرحلة 6): 0.8 مضغوط .. 1.4 كبير */
    var keySizeScale: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.8f, 1.4f)
            textPaint.textSize = BASE_TEXT_SIZE * field
            invalidate()
        }

    /** الوضع الليلي/النهاري (المرحلة 6) */
    var isDarkMode: Boolean = true
        set(value) {
            field = value
            applyTheme()
            invalidate()
        }

    /** يُستدعى عند إدخال نص فعلي (بعد رفع الإصبع أو ضغط مطوّل) */
    var onKeyOutput: ((String) -> Unit)? = null

    /** يُستدعى عند مفاتيح التبديل/الوظيفية (SWITCH_SYMBOLS, BACKSPACE, CTRL_C, ...) */
    var onFunctionalKey: ((String) -> Unit)? = null

    /** يُستدعى عند كل لمسة فعلية (down/move لزر جديد) — مفيد لتحديث شريط الاقتراحات لاحقًا إن لزم */
    var onKeyPreview: ((KeyDef?) -> Unit)? = null

    private val keyPaint = Paint().apply { isAntiAlias = true }
    private val keyPressedPaint = Paint().apply {
        color = Color.parseColor("#3D6EF5")
        isAntiAlias = true
    }
    private val functionalKeyPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = BASE_TEXT_SIZE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val keyRects = mutableListOf<Pair<RectF, KeyDef>>()
    private var activeKey: KeyDef? = null
    private var activeRect: RectF? = null

    /** حالة Shift أحادية (تُطبَّق على الحرف التالي فقط ثم ترجع تلقائيًا، مثل كيبورد الهاتف) */
    private var isShifted: Boolean = false

    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressTriggered = false
    private val longPressRunnable = Runnable {
        activeKey?.longPressOutput?.let { output ->
            longPressTriggered = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onKeyOutput?.invoke(output)
        }
    }
    private val longPressDelayMs = 320L

    // منطقة تسامح إضافية حول كل زر لتحسين الاستجابة على الشاشات الصغيرة
    // (كيبورد الهاتف الحقيقي "يسامح" اللمسة القريبة من حافة الزر بدل رفضها)
    private val touchSlopPx = 6f

    init {
        applyTheme()
    }

    private fun applyTheme() {
        if (isDarkMode) {
            keyPaint.color = Color.parseColor("#2B2F36")
            functionalKeyPaint.color = Color.parseColor("#20242A")
            textPaint.color = Color.WHITE
        } else {
            keyPaint.color = Color.parseColor("#FFFFFF")
            functionalKeyPaint.color = Color.parseColor("#E4E6EB")
            textPaint.color = Color.parseColor("#1A1A1A")
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeyRects(w.toFloat(), h.toFloat())
    }

    /** يحسب مستطيلات كل مفتاح بناءً على وزن العرض (widthWeight) وعدد الصفوف */
    private fun layoutKeyRects(width: Float, height: Float) {
        keyRects.clear()
        val rows = currentLayer.rows
        if (rows.isEmpty() || width <= 0 || height <= 0) return
        val rowHeight = height / rows.size
        rows.forEachIndexed { rowIndex, row ->
            val totalWeight = row.keys.sumOf { it.widthWeight.toDouble() }.toFloat()
            if (totalWeight <= 0f) return@forEachIndexed
            var x = 0f
            val y = rowIndex * rowHeight
            row.keys.forEach { key ->
                val keyWidth = (key.widthWeight / totalWeight) * width
                val rect = RectF(x + 4f, y + 4f, x + keyWidth - 4f, y + rowHeight - 4f)
                keyRects.add(rect to key)
                x += keyWidth
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        keyRects.forEach { (rect, key) ->
            val isShiftKey = key.output == "SHIFT_TOGGLE"
            val paint = when {
                rect == activeRect -> keyPressedPaint
                isShiftKey && isShifted -> keyPressedPaint
                key.isFunctional -> functionalKeyPaint
                else -> keyPaint
            }
            canvas.drawRoundRect(rect, 16f, 16f, paint)
            val label = if (isShifted && !key.isFunctional && key.label.length == 1 && key.label[0].isLetter()) {
                key.label.uppercase()
            } else {
                key.label
            }
            canvas.drawText(label, rect.centerX(), rect.centerY() + textPaint.textSize / 3f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleTouchAt(event.x, event.y)
            MotionEvent.ACTION_MOVE -> {
                // دعم السحب بين الحروف: نحدّث الزر النشط إذا انتقل الإصبع لزر آخر،
                // مع منطقة تسامح لتجنب "رفض" اللمسات القريبة من الحواف.
                val found = findKeyAt(event.x, event.y) ?: findNearestKeyWithinSlop(event.x, event.y)
                if (found != null) {
                    val (rect, key) = found
                    if (rect != activeRect) {
                        cancelPendingLongPress()
                        activeRect = rect
                        activeKey = key
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onKeyPreview?.invoke(key)
                        scheduleLongPressIfNeeded()
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!longPressTriggered) {
                    activeKey?.let { key -> commitKey(key) }
                }
                cancelPendingLongPressf()
                activeKey = null
                activeRect = null
                onKeyPreview?.invoke(null)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
cancelPendingLongPress()
                activeKey = null
                activeRect = null
                onKeyPreview?.invoke(null)
                invalidate()
            }
        }
        return true
    }

    private fun handleTouchAt(x: Float, y: Float) {
        val found = findKeyAt(x, y) ?: findNearestKeyWithinSlop(x, y) ?: return
        val (rect, key) = found
        activeRect = rect
        activeKey = key
        longPressTriggered = false
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onKeyPreview?.invoke(key)
        scheduleLongPressIfNeeded()
        invalidate()
    }

    private fun scheduleLongPressIfNeeded() {
        cancelPendingLongPress()
        if (activeKey?.longPressOutput != null) {
            longPressHandler.postDelayed(longPressRunnable, longPressDelayMs)
        }
    }

    private fun cancelPendingLongPress() {
        longPressHandler.removeCallbacks(longPressRunnable)
    }

    private fun findKeyAt(x: Float, y: Float): Pair<RectF, KeyDef>? =
        keyRects.firstOrNull { (rect, _) -> rect.contains(x, y) }

    /** إذا لم تقع اللمسة داخل أي زر، نبحث عن أقرب زر ضمن هامش تسامح صغير */
    private fun findNearestKeyWithinSlop(x: Float, y: Float): Pair<RectF, KeyDef>? {
        return keyRects.minByOrNull { (rect, _) -> distanceToRect(rect, x, y) }
            ?.takeIf { (rect, _) -> distanceToRect(rect, x, y) <= touchSlopPx }
    }

    private fun distanceToRect(rect: RectF, x: Float, y: Float): Float {
        val dx = when {
            x < rect.left -> rect.left - x
            x > rect.right -> x - rect.right
            else -> 0f
        }
        val dy = when {
            y < rect.top -> rect.top - y
            y > rect.bottom -> y - rect.bottom
            else -> 0f
        }
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun commitKey(key: KeyDef) {
        if (key.output == "SHIFT_TOGGLE") {
            isShifted = !isShifted
            invalidate()
            return
        }
        if (key.isFunctional) {
            onFunctionalKey?.invoke(key.output)
            return
        }
        val output = if (isShifted && key.output.length == 1 && key.output[0].isLetter()) {
            key.output.uppercase()
        } else {
            key.output
        }
        onKeyOutput?.invoke(output)
        if (isShifted) {
            isShifted = false
            invalidate()
        }
    }

    companion object {
        private const val BASE_TEXT_SIZE = 42f
    }
}
