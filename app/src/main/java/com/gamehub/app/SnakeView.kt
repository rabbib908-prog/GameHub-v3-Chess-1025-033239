package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SnakeView(context: Context) : View(context), Pausable {
    private val prefs = context.getSharedPreferences("gamehub", Context.MODE_PRIVATE)
    private var best = prefs.getInt("snakeBest", 0)
    private val cols = 14
    private var rows = 20
    private var cell = 0f
    private var offY = 0f
    private val bodyX = ArrayList<Int>()
    private val bodyY = ArrayList<Int>()
    private var dx = 1
    private var dy = 0
    private var nextDx = 1
    private var nextDy = 0
    private var foodX = 0
    private var foodY = 0
    private var score = 0
    private var alive = true
    private var started = false
    private var acc = 0f
    private var lastT = 0L
    private var running = true
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
    }

    override fun pauseGame() { running = false; lastT = 0L }

    override fun resumeGame() { running = true; lastT = 0L; invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cell = w / cols.toFloat()
        rows = (h / cell).toInt()
        offY = (h - rows * cell) / 2f
        reset()
    }

    private fun reset() {
        bodyX.clear()
        bodyY.clear()
        val sx = cols / 2
        val sy = rows / 2
        for (i in 0..2) { bodyX.add(sx - i); bodyY.add(sy) }
        dx = 1; dy = 0; nextDx = 1; nextDy = 0
        score = 0
        alive = true
        started = false
        acc = 0f
        placeFood()
    }

    private fun placeFood() {
        var tries = 0
        while (true) {
            val x = Random.nextInt(cols)
            val y = Random.nextInt(rows)
            var on = false
            for (i in bodyX.indices) if (bodyX[i] == x && bodyY[i] == y) on = true
            tries++
            if (!on || tries > 500) { foodX = x; foodY = y; return }
        }
    }

    private fun step() {
        dx = nextDx
        dy = nextDy
        val hx = bodyX[0] + dx
        val hy = bodyY[0] + dy
        var hit = hx < 0 || hy < 0 || hx >= cols || hy >= rows
        if (!hit) {
            for (i in 0 until bodyX.size - 1) if (bodyX[i] == hx && bodyY[i] == hy) hit = true
        }
        if (hit) {
            alive = false
            if (score > best) { best = score; prefs.edit().putInt("snakeBest", best).apply() }
            return
        }
        bodyX.add(0, hx)
        bodyY.add(0, hy)
        if (hx == foodX && hy == foodY) {
            score++
            placeFood()
        } else {
            bodyX.removeAt(bodyX.size - 1)
            bodyY.removeAt(bodyY.size - 1)
        }
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (lastT == 0L) 0.016f else min((now - lastT) / 1e9f, 0.05f)
        lastT = now
        if (cell > 0f) {
            if (alive && started) {
                acc += dt
                val interval = max(0.07f, 0.17f - score * 0.004f)
                while (acc >= interval) {
                    acc -= interval
                    step()
                    if (!alive) break
                }
            }
            render(c)
        }
        if (running) postInvalidateOnAnimation()
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        tp.textSize = size
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    private fun render(c: Canvas) {
        c.drawColor(0xFF0F2A1A.toInt())
        p.style = Paint.Style.FILL
        for (y in 0 until rows) for (x in 0 until cols) {
            if ((x + y) % 2 == 0) {
                p.color = 0xFF133A24.toInt()
                c.drawRect(x * cell, offY + y * cell, (x + 1) * cell, offY + (y + 1) * cell, p)
            }
        }
        p.color = 0xFFFF5252.toInt()
        c.drawCircle((foodX + 0.5f) * cell, offY + (foodY + 0.5f) * cell, cell * 0.38f, p)
        for (i in bodyX.indices.reversed()) {
            p.color = if (i == 0) 0xFFB9F6CA.toInt() else 0xFF4CAF50.toInt()
            rect.set(bodyX[i] * cell + cell * 0.06f, offY + bodyY[i] * cell + cell * 0.06f, (bodyX[i] + 1) * cell - cell * 0.06f, offY + (bodyY[i] + 1) * cell - cell * 0.06f)
            c.drawRoundRect(rect, cell * 0.3f, cell * 0.3f, p)
        }
        val u = min(width, height) / 400f
        text(c, "স্কোর: $score    সেরা: $best", width / 2f, 40f * u, 22f * u, 0xFFFFFFFF.toInt())
        if (!started && alive) text(c, "স্বাইপ করে শুরু করুন", width / 2f, height * 0.8f, 26f * u, 0xFFFFFFFF.toInt())
        if (!alive) {
            text(c, "খেলা শেষ!", width / 2f, height * 0.45f, 44f * u, 0xFFFFFFFF.toInt())
            text(c, "আবার খেলতে ট্যাপ করুন", width / 2f, height * 0.52f, 26f * u, 0xFFFFFFFF.toInt())
        }
    }

    private fun turnTo(ddx: Float, ddy: Float) {
        if (!alive) return
        val nx: Int
        val ny: Int
        if (abs(ddx) > abs(ddy)) {
            nx = if (ddx > 0f) 1 else -1
            ny = 0
        } else {
            nx = 0
            ny = if (ddy > 0f) 1 else -1
        }
        if (nx == -dx && ny == -dy) return
        nextDx = nx
        nextDy = ny
        started = true
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; moved = false }
            MotionEvent.ACTION_MOVE -> {
                val ddx = e.x - downX
                val ddy = e.y - downY
                if (max(abs(ddx), abs(ddy)) > cell * 1.2f) {
                    turnTo(ddx, ddy)
                    downX = e.x
                    downY = e.y
                    moved = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) {
                    if (!alive) reset() else if (!started) started = true
                }
            }
        }
        return true
    }
}
