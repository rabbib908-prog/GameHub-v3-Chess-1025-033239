package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class MemoryView(context: Context) : View(context), Pausable {
    private val symbols = listOf("🐶", "🐱", "🦊", "🐼", "🐸", "🦁", "🐵", "🐯")
    private val prefs = context.getSharedPreferences("gamehub", Context.MODE_PRIVATE)
    private var best = prefs.getInt("memBest", 0)
    private val cards = IntArray(16)
    private val revealed = BooleanArray(16)
    private val matched = BooleanArray(16)
    private var first = -1
    private var lock = false
    private var moves = 0
    private var won = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var gap = 0f
    private var cw = 0f
    private var ch = 0f
    private var top0 = 0f

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
        shuffle()
    }

    override fun pauseGame() { }

    override fun resumeGame() { }

    private fun shuffle() {
        val l = (0 until 8).flatMap { listOf(it, it) }.shuffled()
        for (i in 0 until 16) {
            cards[i] = l[i]
            revealed[i] = false
            matched[i] = false
        }
        first = -1
        lock = false
        moves = 0
        won = false
    }

    private fun layout() {
        val u = min(width, height) / 400f
        gap = 10f * u
        cw = (width - 5f * gap) / 4f
        ch = min(cw * 1.15f, (height * 0.7f - 3f * gap) / 4f)
        top0 = (height - (4f * ch + 3f * gap)) / 2f + 30f * u
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF10152B.toInt())
        layout()
        val u = min(width, height) / 400f
        tp.color = 0xFFFFFFFF.toInt()
        tp.textSize = 24f * u
        c.drawText("চাল: $moves    সেরা: " + (if (best == 0) "-" else best.toString()), width / 2f, 60f * u, tp)
        for (i in 0 until 16) {
            val x = gap + (i % 4) * (cw + gap)
            val y = top0 + (i / 4) * (ch + gap)
            rect.set(x, y, x + cw, y + ch)
            p.style = Paint.Style.FILL
            p.color = if (matched[i]) 0xFF2E7D32.toInt() else if (revealed[i]) 0xFFFFFFFF.toInt() else 0xFF3D5AFE.toInt()
            c.drawRoundRect(rect, 16f * u, 16f * u, p)
            tp.textSize = ch * 0.5f
            if (revealed[i] || matched[i]) {
                tp.color = 0xFF000000.toInt()
                c.drawText(symbols[cards[i]], x + cw / 2f, y + ch / 2f + ch * 0.18f, tp)
            } else {
                tp.color = 0xFFFFFFFF.toInt()
                c.drawText("?", x + cw / 2f, y + ch / 2f + ch * 0.18f, tp)
            }
        }
        if (won) {
            tp.color = 0xFFFFFFFF.toInt()
            tp.textSize = 24f * u
            c.drawText("🎉 জিতেছেন! আবার খেলতে ট্যাপ করুন", width / 2f, height - 50f * u, tp)
        } else {
            tp.color = 0xFFB0BEC5.toInt()
            tp.textSize = 18f * u
            c.drawText("একই ছবির জোড়া খুঁজুন", width / 2f, height - 50f * u, tp)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        if (won) { shuffle(); invalidate(); return true }
        if (lock) return true
        var idx = -1
        for (i in 0 until 16) {
            val x = gap + (i % 4) * (cw + gap)
            val y = top0 + (i / 4) * (ch + gap)
            if (e.x >= x && e.x <= x + cw && e.y >= y && e.y <= y + ch) idx = i
        }
        if (idx < 0 || revealed[idx] || matched[idx]) return true
        revealed[idx] = true
        if (first == -1) {
            first = idx
        } else {
            moves++
            val a = first
            val b = idx
            first = -1
            if (cards[a] == cards[b]) {
                matched[a] = true
                matched[b] = true
                revealed[a] = false
                revealed[b] = false
                if (matched.all { it }) {
                    won = true
                    if (best == 0 || moves < best) {
                        best = moves
                        prefs.edit().putInt("memBest", best).apply()
                    }
                }
            } else {
                lock = true
                postDelayed({
                    if (isAttachedToWindow) {
                        revealed[a] = false
                        revealed[b] = false
                        lock = false
                        invalidate()
                    }
                }, 700)
            }
        }
        invalidate()
        return true
    }
}
