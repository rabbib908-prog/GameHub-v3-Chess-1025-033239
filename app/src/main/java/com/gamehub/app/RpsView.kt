package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

class RpsView(context: Context, private val mode: Int, private val diff: Int) : View(context) {
    private val emoji = arrayOf("✊", "✋", "✌️")
    private val names = arrayOf("পাথর", "কাগজ", "কাঁচি")
    private var s1 = 0
    private var s2 = 0
    private var pick1 = -1
    private var pick2 = -1
    private var phase = 0 // 0 choose, 1 reveal, 2 over
    private var revealAt = 0L
    private var result = 0
    private val trans = Array(3) { IntArray(3) }
    private var prevPick = -1
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val target = 3

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
    }

    private fun btnX(i: Int): Float = width * (0.17f + 0.33f * i)
    private fun btnR(): Float = width * 0.14f

    private fun beats(a: Int, b: Int): Int = if (a == b) 0 else if ((a - b + 3) % 3 == 1) 1 else 2

    private fun botPick(): Int {
        val counter = { x: Int -> (x + 1) % 3 }
        if (diff == 1) return Random.nextInt(3)
        if (diff == 2) {
            return if (prevPick >= 0 && Random.nextInt(100) < 55) counter(prevPick) else Random.nextInt(3)
        }
        if (prevPick >= 0) {
            val row = trans[prevPick]
            var best = 0
            var bestV = -1
            for (i in 0..2) if (row[i] > bestV) { bestV = row[i]; best = i }
            if (bestV > 0) return counter(best)
        }
        return Random.nextInt(3)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        tp.textSize = size
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    private fun flippedText(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        c.save()
        c.rotate(180f, x, y - size * 0.3f)
        text(c, s, x, y, size, color)
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        val u = min(width, height) / 400f
        c.drawColor(0xFF2E7D32.toInt())
        p.style = Paint.Style.FILL
        p.color = 0xFF388E3C.toInt()
        c.drawRect(0f, height * 0.5f, width.toFloat(), height.toFloat(), p)
        p.color = 0xFF29B6F6.toInt()
        c.drawRect(width * 0.46f, 0f, width * 0.54f, height * 0.12f, p)
        p.color = 0xFFFF5252.toInt()
        c.drawRect(width * 0.46f, height * 0.88f, width * 0.54f, height.toFloat(), p)

        val n1 = if (mode == MODE_BOT) "আপনি" else "লাল"
        val n2 = if (mode == MODE_BOT) "বট" else "নীল"
        text(c, "$n1 $s1  –  $s2 $n2", width / 2f, height * 0.5f + 12f * u, 26f * u, 0xFFFFFFFF.toInt())

        val topY = height * 0.27f
        val botY = height * 0.73f
        val big = width * 0.28f
        if (phase == 0) {
            val q1 = if (pick1 >= 0) "✔ তৈরি" else "বাছাই করুন"
            val q2 = if (pick2 >= 0 || (mode == MODE_BOT && pick1 < 0)) (if (pick2 >= 0) "✔ তৈরি" else "...") else "বাছাই করুন"
            text(c, q1, width / 2f, botY, 28f * u, 0xFFFFFFFF.toInt())
            flippedText(c, if (mode == MODE_BOT) (if (pick1 >= 0) "..." else "বট অপেক্ষা করছে") else q2, width / 2f, topY, 28f * u, 0xFFFFFFFF.toInt())
        } else {
            text(c, emoji[pick1], width / 2f, botY + big * 0.3f, big, 0xFF000000.toInt())
            c.save()
            c.rotate(180f, width / 2f, topY)
            text(c, emoji[pick2], width / 2f, topY + big * 0.3f, big, 0xFF000000.toInt())
            c.restore()
            val msg = when (result) {
                1 -> if (mode == MODE_BOT) "আপনি জিতেছেন! 🎉" else "লাল জিতেছে! 🎉"
                2 -> if (mode == MODE_BOT) "বট জিতেছে! 🤖" else "নীল জিতেছে! 🎉"
                else -> "ড্র!"
            }
            text(c, msg, width / 2f, height * 0.5f - 24f * u, 30f * u, 0xFFFFD740.toInt())
        }
        if (phase == 2) {
            text(c, "আবার খেলতে ট্যাপ করুন", width / 2f, height * 0.5f + 60f * u, 24f * u, 0xFFFFFFFF.toInt())
        }

        for (i in 0..2) {
            drawBtn(c, btnX(i), height * 0.9f, i, false, pick1 == i)
            if (mode == MODE_FRIEND) drawBtn(c, btnX(i), height * 0.1f, i, true, pick2 == i)
        }
        if (mode == MODE_BOT) text(c, "বট: " + (if (phase == 0) "ভাবছে..." else names[pick2]), width / 2f, height * 0.1f, 22f * u, 0xFFFFFFFF.toInt())
    }

    private fun drawBtn(c: Canvas, x: Float, y: Float, i: Int, flipped: Boolean, chosen: Boolean) {
        p.style = Paint.Style.FILL
        p.color = if (chosen) 0xFFFFD740.toInt() else 0xFF455A64.toInt()
        c.drawCircle(x, y, btnR(), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = 0xFF000000.toInt()
        c.drawCircle(x, y, btnR(), p)
        p.style = Paint.Style.FILL
        c.save()
        if (flipped) c.rotate(180f, x, y)
        text(c, emoji[i], x, y + btnR() * 0.35f, btnR() * 1.05f, 0xFFFFFFFF.toInt())
        c.restore()
    }

    private fun resolve() {
        result = beats(pick1, pick2)
        if (result == 1) s1++ else if (result == 2) s2++
        phase = 1
        revealAt = System.currentTimeMillis()
        if (mode == MODE_BOT) {
            if (prevPick >= 0) trans[prevPick][pick1]++
            prevPick = pick1
        }
        invalidate()
        postDelayed({
            if (!isAttachedToWindow) return@postDelayed
            if (s1 >= target || s2 >= target) {
                phase = 2
            } else {
                pick1 = -1
                pick2 = -1
                phase = 0
            }
            invalidate()
        }, 1500)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        if (phase == 2) {
            s1 = 0; s2 = 0; pick1 = -1; pick2 = -1; phase = 0
            invalidate()
            return true
        }
        if (phase != 0) return true
        for (i in 0..2) {
            if (pick1 < 0 && hypot(e.x - btnX(i), e.y - height * 0.9f) <= btnR()) pick1 = i
            if (mode == MODE_FRIEND && pick2 < 0 && hypot(e.x - btnX(i), e.y - height * 0.1f) <= btnR()) pick2 = i
        }
        if (mode == MODE_BOT && pick1 >= 0 && pick2 < 0) pick2 = botPick()
        if (pick1 >= 0 && pick2 >= 0) resolve() else invalidate()
        return true
    }
}
