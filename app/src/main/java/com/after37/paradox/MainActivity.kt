package com.after37.paradox

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var board: PuzzleBoard
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 17, 34)
        window.navigationBarColor = Color.rgb(11, 17, 34)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 12)
            setBackgroundColor(Color.rgb(11, 17, 34))
        }
        root.addView(TextView(this).apply {
            text = "ПАРАДОКС"
            textSize = 28f
            setTextColor(Color.rgb(103, 232, 249))
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }, LinearLayout.LayoutParams(-1, 48))

        status = TextView(this).apply {
            text = "Доберись до выхода. Эхо повторит твои ходы."
            textSize = 14f
            setTextColor(Color.rgb(190, 205, 226))
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 8)
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 48))

        board = PuzzleBoard(this) { status.text = it }
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        controls.addView(makeRow(listOf("▲", "↻")))
        controls.addView(makeRow(listOf("◀", "▼", "▶")))
        root.addView(controls, LinearLayout.LayoutParams(-1, 132))
        root.addView(TextView(this).apply {
            text = "Золотая ✦ клетка управляет дверью"
            textSize = 12f
            setTextColor(Color.rgb(112, 132, 164))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 28))
        setContentView(root)
    }

    private fun makeRow(labels: List<String>): LinearLayout {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
        labels.forEach { label ->
            val button = Button(this).apply {
                text = label
                textSize = 20f
                setTextColor(Color.WHITE)
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    if (label == "↻") Color.rgb(58, 75, 106) else Color.rgb(30, 48, 76)
                ))
                setOnClickListener {
                    if (label == "↻") board.reset() else board.move(label)
                }
            }
            row.addView(button, LinearLayout.LayoutParams(0, 56, 1f).apply {
                setMargins(5, 3, 5, 3)
            })
        }
        return row
    }
}

private class PuzzleBoard(
    context: android.content.Context,
    private val onStatus: (String) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = 7
    private val rows = 7
    private var px = 0
    private var py = 3
    private var moves = 0
    private var won = false
    private val history = mutableListOf<Pair<Int, Int>>()
    private var ghost: Pair<Int, Int>? = null
    private val walls = setOf(
        2 to 0, 2 to 1, 2 to 2, 4 to 4, 4 to 5, 4 to 6, 1 to 5, 2 to 5
    )
    private val switch = 3 to 1
    private val exit = 6 to 3
    private val gate = 4 to 3

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(11, 17, 34))
        val cell = minOf((width - 24f) / cols, (height - 24f) / rows)
        val left = (width - cell * cols) / 2f
        val top = (height - cell * rows) / 2f
        for (y in 0 until rows) for (x in 0 until cols) {
            val rect = RectF(left + x * cell + 3, top + y * cell + 3,
                left + (x + 1) * cell - 3, top + (y + 1) * cell - 3)
            paint.color = when {
                (x to y) in walls -> Color.rgb(40, 53, 78)
                (x to y) == gate && !gateOpen() -> Color.rgb(190, 65, 95)
                (x to y) == switch -> Color.rgb(145, 95, 31)
                (x to y) == exit -> Color.rgb(22, 120, 101)
                else -> Color.rgb(22, 32, 54)
            }
            canvas.drawRoundRect(rect, 9f, 9f, paint)
            if ((x to y) == switch) {
                paint.color = Color.rgb(255, 218, 120)
                paint.textSize = cell * 0.42f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("✦", rect.centerX(), rect.centerY() + cell * 0.14f, paint)
            }
            if ((x to y) == exit) {
                paint.color = Color.WHITE
                paint.textSize = cell * 0.28f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("ВЫХОД", rect.centerX(), rect.centerY() + cell * 0.1f, paint)
            }
        }
        ghost?.let { (gx, gy) ->
            val cx = left + (gx + 0.5f) * cell
            val cy = top + (gy + 0.5f) * cell
            paint.color = Color.rgb(167, 139, 250)
            canvas.drawCircle(cx, cy, cell * 0.27f, paint)
            paint.color = Color.WHITE
            paint.textSize = cell * 0.25f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("e", cx, cy + cell * 0.09f, paint)
        }
        val cx = left + (px + 0.5f) * cell
        val cy = top + (py + 0.5f) * cell
        paint.color = Color.rgb(103, 232, 249)
        canvas.drawCircle(cx, cy, cell * 0.31f, paint)
        paint.color = Color.rgb(11, 17, 34)
        canvas.drawCircle(cx, cy, cell * 0.11f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    fun move(direction: String) {
        if (won) return
        val (dx, dy) = when (direction) {
            "▲" -> 0 to -1
            "▼" -> 0 to 1
            "◀" -> -1 to 0
            "▶" -> 1 to 0
            else -> return
        }
        val nx = px + dx
        val ny = py + dy
        if (nx !in 0 until cols || ny !in 0 until rows || (nx to ny) in walls) return
        if ((nx to ny) == gate && !gateOpen()) {
            onStatus("Дверь закрыта! Активируй золотой переключатель.")
            return
        }
        history.add(px to py)
        px = nx
        py = ny
        moves++
        ghost = if (history.size >= 3) history[history.size - 3] else null
        if ((px to py) == exit && gateOpen()) {
            won = true
            onStatus("Уровень пройден! Нажми ↻, чтобы сыграть снова.")
        } else {
            onStatus("Ход $moves • " + if (gateOpen()) "Дверь открыта — к выходу!" else "Найди путь к переключателю")
        }
        invalidate()
    }

    private fun gateOpen(): Boolean = (px to py) == switch || ghost == switch

    fun reset() {
        px = 0
        py = 3
        moves = 0
        won = false
        history.clear()
        ghost = null
        onStatus("Новый старт. Используй эхо, чтобы открыть путь.")
        invalidate()
    }
}
