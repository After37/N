package com.after37.paradox

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.HapticFeedbackConstants
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 13, 30)
        window.navigationBarColor = Color.rgb(12, 13, 30)
        window.decorView.systemUiVisibility = 0
        setContentView(CrystalClickerView(this))
    }
}

private class CrystalClickerView(context: Context) : View(context) {
    private val prefs = context.getSharedPreferences("crystal_clicker", Context.MODE_PRIVATE)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val number = NumberFormat.getIntegerInstance(Locale("ru", "RU"))
    private var coins = prefs.getLong("coins", 0L)
    private var power = prefs.getInt("power", 1)
    private var upgradeLevel = prefs.getInt("upgrade", 0)
    private var gemScale = 1f
    private var tapCount = 0
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var floatText = ""
    private var floatUntil = 0L
    private var buttonRect = RectF()
    private var gemRect = RectF()
    private var upgradeRect = RectF()
    private var pulse = 0f

    private val bg = Color.rgb(12, 13, 30)
    private val white = Color.rgb(247, 248, 255)
    private val muted = Color.rgb(155, 163, 195)
    private val cyan = Color.rgb(73, 224, 239)
    private val purple = Color.rgb(143, 91, 255)
    private val gold = Color.rgb(255, 201, 91)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(bg)

        // Soft decorative glows
        p.shader = android.graphics.RadialGradient(w * .5f, h * .39f, w * .48f,
            intArrayOf(Color.rgb(42, 32, 91), bg), null, android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        text(c, "КРИСТАЛЛ", w / 2, h * .085f, 15f, cyan, true, Paint.Align.CENTER, 1.5f)
        text(c, "КЛИКЕР", w / 2, h * .125f, 29f, white, true, Paint.Align.CENTER)
        text(c, "ТАПАЙ. КОПИ. УСИЛИВАЙСЯ.", w / 2, h * .165f, 10f, muted, false, Paint.Align.CENTER, 1.1f)

        // Counter panel
        val panel = RectF(w * .09f, h * .205f, w * .91f, h * .30f)
        rounded(c, panel, 20f, Color.rgb(25, 27, 56))
        stroke(c, panel, 20f, Color.rgb(64, 64, 111), 1.2f)
        text(c, "ТВОИ КРИСТАЛЛЫ", w / 2, panel.top + panel.height() * .34f, 10f, muted, true, Paint.Align.CENTER, .8f)
        text(c, number.format(coins), w / 2, panel.top + panel.height() * .77f, 29f, gold, true, Paint.Align.CENTER)

        // Main crystal hit target
        val size = minOf(w * .58f, h * .34f)
        val cx = w / 2
        val cy = h * .485f
        gemRect = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        p.color = Color.argb(38, 100, 75, 255)
        c.drawCircle(cx, cy, size * .57f + pulse * 12f, p)
        p.color = Color.argb(25, 80, 220, 255)
        c.drawCircle(cx, cy, size * .43f, p)
        drawGem(c, cx, cy, size * .36f * gemScale)
        text(c, "+$power", cx, cy + size * .55f, 18f, cyan, true, Paint.Align.CENTER)
        text(c, "НАЖМИ НА КРИСТАЛЛ", cx, cy + size * .73f, 10f, muted, true, Paint.Align.CENTER, .8f)

        // Stats
        val statY = h * .70f
        val gap = w * .035f
        val boxW = (w * .82f - gap) / 2f
        val left = w * .09f
        val s1 = RectF(left, statY, left + boxW, statY + h * .075f)
        val s2 = RectF(left + boxW + gap, statY, left + boxW * 2 + gap, statY + h * .075f)
        rounded(c, s1, 14f, Color.rgb(24, 27, 51))
        rounded(c, s2, 14f, Color.rgb(24, 27, 51))
        text(c, "ЗА ТАП", s1.centerX(), s1.top + s1.height() * .36f, 9f, muted, true, Paint.Align.CENTER)
        text(c, "$power ✦", s1.centerX(), s1.top + s1.height() * .76f, 16f, cyan, true, Paint.Align.CENTER)
        text(c, "УЛУЧШЕНИЯ", s2.centerX(), s2.top + s2.height() * .36f, 9f, muted, true, Paint.Align.CENTER)
        text(c, "$upgradeLevel", s2.centerX(), s2.top + s2.height() * .76f, 16f, gold, true, Paint.Align.CENTER)

        // Upgrade button
        val cost = upgradeCost()
        upgradeRect = RectF(w * .08f, h * .80f, w * .92f, h * .91f)
        val canBuy = coins >= cost
        rounded(c, upgradeRect, 20f, if (canBuy) Color.rgb(105, 69, 218) else Color.rgb(48, 43, 79))
        if (canBuy) {
            stroke(c, upgradeRect, 20f, Color.rgb(185, 153, 255), 1.5f)
        }
        text(c, "УЛУЧШИТЬ СИЛУ", w / 2, upgradeRect.top + upgradeRect.height() * .40f, 15f, white, true, Paint.Align.CENTER, .5f)
        text(c, "Цена: ${number.format(cost)} кристаллов  •  +1 за тап", w / 2, upgradeRect.top + upgradeRect.height() * .73f, 10f, if (canBuy) Color.rgb(235, 225, 255) else muted, false, Paint.Align.CENTER)

        text(c, "СОВЕТ: КАЖДЫЙ ТАП ПРИБЛИЖАЕТ К СЛЕДУЮЩЕМУ УЛУЧШЕНИЮ", w / 2, h * .96f, 8f, muted, false, Paint.Align.CENTER, .2f)

        if (System.currentTimeMillis() < floatUntil) {
            val elapsed = (floatUntil - System.currentTimeMillis()).toFloat() / 650f
            text(c, floatText, lastTapX, lastTapY - (1f - elapsed) * 70f, 23f, gold, true, Paint.Align.CENTER)
            postInvalidateOnAnimation()
        }
    }

    private fun drawGem(c: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path().apply {
            moveTo(cx, cy - r * 1.12f)
            lineTo(cx + r * .88f, cy - r * .42f)
            lineTo(cx + r * .62f, cy + r * .78f)
            lineTo(cx, cy + r * 1.12f)
            lineTo(cx - r * .62f, cy + r * .78f)
            lineTo(cx - r * .88f, cy - r * .42f)
            close()
        }
        p.shader = android.graphics.LinearGradient(cx-r, cy-r, cx+r, cy+r,
            intArrayOf(Color.rgb(106, 250, 255), Color.rgb(73, 126, 255), Color.rgb(150, 75, 255)),
            null, android.graphics.Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        val shine = Path().apply {
            moveTo(cx, cy - r * 1.02f)
            lineTo(cx + r * .75f, cy - r * .38f)
            lineTo(cx, cy - r * .12f)
            lineTo(cx - r * .25f, cy - r * .45f)
            close()
        }
        p.color = Color.argb(115, 255, 255, 255)
        c.drawPath(shine, p)
        p.color = Color.argb(105, 255, 255, 255)
        p.strokeWidth = maxOf(1f, r * .025f)
        p.style = Paint.Style.STROKE
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                if (gemRect.contains(e.x, e.y)) {
                    coins += power.toLong()
                    tapCount++
                    lastTapX = e.x
                    lastTapY = e.y
                    floatText = "+$power ✦"
                    floatUntil = System.currentTimeMillis() + 650L
                    gemScale = .88f
                    pulse = 1f
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    ValueAnimator.ofFloat(.88f, 1f).apply {
                        duration = 180
                        interpolator = OvershootInterpolator(2f)
                        addUpdateListener {
                            gemScale = it.animatedValue as Float
                            pulse = 1f - it.animatedFraction
                            invalidate()
                        }
                        start()
                    }
                    save()
                    invalidate()
                } else if (upgradeRect.contains(e.x, e.y)) {
                    val cost = upgradeCost()
                    if (coins >= cost) {
                        coins -= cost
                        power++
                        upgradeLevel++
                        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        floatText = "СИЛА +1!"
                        lastTapX = width / 2f
                        lastTapY = upgradeRect.top
                        floatUntil = System.currentTimeMillis() + 650L
                        save()
                    } else {
                        floatText = "ЕЩЁ ${number.format(cost - coins)} ✦"
                        lastTapX = width / 2f
                        lastTapY = upgradeRect.top
                        floatUntil = System.currentTimeMillis() + 650L
                    }
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }

    private fun upgradeCost(): Long = (25L * (upgradeLevel + 1) * (upgradeLevel + 1)).coerceAtMost(2_000_000_000L)

    private fun save() {
        prefs.edit().putLong("coins", coins).putInt("power", power).putInt("upgrade", upgradeLevel).apply()
    }

    private fun text(c: Canvas, value: String, x: Float, y: Float, sizeSp: Float, color: Int,
                     bold: Boolean, align: Paint.Align, spacing: Float = 0f) {
        p.shader = null
        p.color = color
        p.textSize = sizeSp * resources.displayMetrics.scaledDensity
        p.textAlign = align
        p.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        p.letterSpacingCompat = spacing
        c.drawText(value, x, y, p)
    }

    private fun rounded(c: Canvas, rect: RectF, radius: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        c.drawRoundRect(rect, radius, radius, p)
    }

    private fun stroke(c: Canvas, rect: RectF, radius: Float, color: Int, width: Float) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = width
        p.color = color
        c.drawRoundRect(rect, radius, radius, p)
        p.style = Paint.Style.FILL
    }

    private var Paint.letterSpacingCompat: Float
        get() = 0f
        set(value) { /* Canvas Paint has no letter-spacing property. */ }
}
