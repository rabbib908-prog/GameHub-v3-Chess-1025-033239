package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class PingPongView(context: Context, private val mode: Int, private val diff: Int) : View(context), Pausable {
    private var state = 0 // 0 ready, 1 play, 2 pause, 3 over
    private var s1 = 0
    private var s2 = 0
    private var bx = 0f
    private var by = 0f
    private var vx = 0f
    private var vy = 0f
    private var p1x = 0f
    private var p2x = 0f
    private var nextDir = 1
    private var waitT = 0f
    private var botErr = 0f
    private var lastT = 0L
    private var running = true
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val winScore = 5

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
    }

    override fun pauseGame() { running = false; lastT = 0L }

    override fun resumeGame() { running = true; lastT = 0L; invalidate() }

    private fun pw(): Float = width * 0.28f
    private fun ph(): Float = height * 0.022f
    private fun r(): Float = width * 0.03f
    private fun p1y(): Float = height * 0.9f
    private fun p2y(): Float = height * 0.1f
    private fun baseSpeed(): Float = height * 0.55f
    private fun maxSpeed(): Float = height * 1.1f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        p1x = w / 2f
        p2x = w / 2f
        bx = w / 2f
        by = h / 2f
    }

    private fun serve(dir: Int) {
        bx = width / 2f
        by = height / 2f
        vx = (Random.nextFloat() - 0.5f) * width * 0.5f
        vy = dir * baseSpeed()
    }

    private fun clampPad(x: Float): Float = max(pw() / 2f, min(width - pw() / 2f, x))

    private fun hit(px: Float, dir: Int, newY: Float) {
        val sp = min(hypot(vx, vy) * 1.05f, maxSpeed())
        val off = ((bx - px) / (pw() / 2f)).coerceIn(-1f, 1f)
        vx = off * sp * 0.75f
        vy = dir * sqrt(max(sp * sp - vx * vx, 1f))
        by = newY
        val errScale = if (diff == 1) 1.6f else if (diff == 2) 0.8f else 0.08f
        botErr = (Random.nextFloat() - 0.5f) * pw() * errScale
    }

    private fun scored(bottomMissed: Boolean) {
        if (bottomMissed) { s2++; nextDir = -1 } else { s1++; nextDir = 1 }
        if (s1 >= winScore || s2 >= winScore) {
            state = 3
        } else {
            state = 2
            waitT = 1f
        }
        bx = width / 2f
        by = height / 2f
        vx = 0f
        vy = 0f
    }

    private fun update(dt: Float) {
        if (state == 2) {
            waitT -= dt
            if (waitT <= 0f) { state = 1; serve(nextDir) }
            return
        }
        if (state != 1) return
        if (mode == MODE_BOT) {
            val speed = width * (if (diff == 1) 0.35f else if (diff == 2) 0.7f else 1.3f)
            val targetX = if (vy < 0f) bx + botErr else width / 2f
            val d = targetX - p2x
            val step = speed * dt
            p2x = clampPad(if (abs(d) <= step) targetX else p2x + (if (d > 0f) step else -step))
        }
        bx += vx * dt
        by += vy * dt
        if (bx - r() < 0f) { bx = r(); vx = abs(vx) }
        if (bx + r() > width) { bx = width - r(); vx = -abs(vx) }
        if (vy > 0f && by + r() >= p1y() - ph() / 2f && by - r() <= p1y() + ph() / 2f && abs(bx - p1x) <= pw() / 2f + r()) {
            hit(p1x, -1, p1y() - ph() / 2f - r())
        }
        if (vy < 0f && by - r() <= p2y() + ph() / 2f && by + r() >= p2y() - ph() / 2f && abs(bx - p2x) <= pw() / 2f + r()) {
            hit(p2x, 1, p2y() + ph() / 2f + r())
        }
        if (by - r() > height) scored(true)
        else if (by + r() < 0f) scored(false)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        tp.textSize = size
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (lastT == 0L) 0.016f else min((now - lastT) / 1e9f, 0.033f)
        lastT = now
        if (width > 0 && height > 0) {
            update(dt)
            render(c)
        }
        if (running) postInvalidateOnAnimation()
    }

    private fun render(c: Canvas) {
        val u = min(width, height) / 400f
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(0xFF1B5E20.toInt())
        p.style = Paint.Style.FILL
        p.color = 0x33FFFFFF
        var x = 0f
        while (x < w) {
            c.drawRect(x, h / 2f - 2f * u, x + 14f * u, h / 2f + 2f * u, p)
            x += 28f * u
        }
        text(c, s2.toString(), w * 0.88f, h * 0.44f, 70f * u, 0x55FFFFFF)
        text(c, s1.toString(), w * 0.88f, h * 0.58f, 70f * u, 0x55FFFFFF)

        p.color = 0xFF29B6F6.toInt()
        rect.set(p2x - pw() / 2f, p2y() - ph() / 2f, p2x + pw() / 2f, p2y() + ph() / 2f)
        c.drawRoundRect(rect, ph(), ph(), p)
        p.color = 0xFFFF5252.toInt()
        rect.set(p1x - pw() / 2f, p1y() - ph() / 2f, p1x + pw() / 2f, p1y() + ph() / 2f)
        c.drawRoundRect(rect, ph(), ph(), p)
        p.color = 0xFFFFFFFF.toInt()
        c.drawCircle(bx, by, r(), p)

        val n1 = if (mode == MODE_BOT) "আপনি" else "লাল"
        val n2 = if (mode == MODE_BOT) "বট" else "নীল"
        if (state == 0) {
            text(c, "পিং পং", w / 2f, h * 0.35f, 48f * u, 0xFFFFFFFF.toInt())
            text(c, "শুরু করতে ট্যাপ করুন", w / 2f, h * 0.45f, 26f * u, 0xFFFFFFFF.toInt())
            if (mode == MODE_FRIEND) text(c, "দুইজন নিজের অর্ধেকে আঙুল সরান", w / 2f, h * 0.65f, 20f * u, 0xFFB0BEC5.toInt())
            else text(c, "আঙুল সরিয়ে প্যাডেল চালান", w / 2f, h * 0.65f, 20f * u, 0xFFB0BEC5.toInt())
        }
        if (state == 3) {
            val win = if (s1 > s2) "$n1 জিতেছেন! 🎉" else "$n2 জিতেছে! 🎉"
            text(c, win, w / 2f, h * 0.4f, 38f * u, 0xFFFFD740.toInt())
            text(c, "$n1 $s1 – $s2 $n2", w / 2f, h * 0.48f, 28f * u, 0xFFFFFFFF.toInt())
            text(c, "আবার খেলতে ট্যাপ করুন", w / 2f, h * 0.56f, 24f * u, 0xFFFFFFFF.toInt())
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            if (state == 0) { state = 2; waitT = 0.4f; nextDir = 1 }
            else if (state == 3) { s1 = 0; s2 = 0; state = 0 }
        }
        for (i in 0 until e.pointerCount) {
            val x = e.getX(i)
            val y = e.getY(i)
            if (mode == MODE_BOT) p1x = clampPad(x)
            else if (y > height / 2f) p1x = clampPad(x)
            else p2x = clampPad(x)
        }
        return true
    }
}
