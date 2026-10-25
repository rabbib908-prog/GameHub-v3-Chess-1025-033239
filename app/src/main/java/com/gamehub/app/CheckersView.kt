package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class CheckersView(context: Context, private val mode: Int, private val diff: Int) : View(context) {
    private var st = CheckersState(CheckersState.initial(), 1, -1, 0)
    private var selected = -1
    private var thinking = false
    private var winner = 0
    private var endAt = 0L
    private var w1 = 0
    private var w2 = 0
    private var started = 1
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private var left0 = 0f
    private var top0 = 0f
    private var cell = 0f

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        tp.textSize = size
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF10152B.toInt())
        val u = min(width, height) / 400f
        val side = min(width * 0.96f, height * 0.62f)
        cell = side / 8f
        left0 = (width - side) / 2f
        top0 = height * 0.2f
        val b = st.board
        var red = 0
        var blue = 0
        for (v in b) {
            if (CheckersState.owner(v) == 1) red++
            if (CheckersState.owner(v) == 2) blue++
        }
        val n1 = if (mode == MODE_BOT) "আপনি" else "খেলোয়াড় ১"
        val n2 = if (mode == MODE_BOT) "বট" else "খেলোয়াড় ২"
        text(c, "🔴 $red   •   🔵 $blue", width / 2f, 48f * u, 26f * u, 0xFFFFFFFF.toInt())
        text(c, "জয়: $n1 $w1  –  $w2 $n2", width / 2f, 76f * u, 16f * u, 0xFFB0BEC5.toInt())
        val moves = st.legalMoves()
        val mustJump = moves.isNotEmpty() && abs(moves[0] / 64 / 8 - (moves[0] % 64) / 8) == 2
        val status = when {
            winner == 1 -> if (mode == MODE_BOT) "আপনি জিতেছেন! 🎉" else "খেলোয়াড় ১ (লাল) জিতেছেন! 🎉"
            winner == 2 -> if (mode == MODE_BOT) "বট জিতেছে! 🤖" else "খেলোয়াড় ২ (নীল) জিতেছেন! 🎉"
            winner == 3 -> "খেলা ড্র হয়েছে!"
            st.currentPlayer == 1 -> (if (mode == MODE_BOT) "আপনার পালা 🔴" else "খেলোয়াড় ১ এর পালা 🔴") + (if (mustJump) " – কাটতে হবে" else "")
            else -> (if (mode == MODE_BOT) "বট ভাবছে... 🔵" else "খেলোয়াড় ২ এর পালা 🔵") + (if (mustJump && mode != MODE_BOT) " – কাটতে হবে" else "")
        }
        text(c, status, width / 2f, top0 - 22f * u, 22f * u, 0xFFFFD740.toInt())

        p.style = Paint.Style.FILL
        for (r in 0..7) for (k in 0..7) {
            p.color = if ((r + k) % 2 == 1) 0xFF8D6E63.toInt() else 0xFFF5E6D3.toInt()
            c.drawRect(left0 + k * cell, top0 + r * cell, left0 + (k + 1) * cell, top0 + (r + 1) * cell, p)
        }
        if (selected >= 0) {
            p.color = 0x99FFD740.toInt()
            val sr = selected / 8
            val sc = selected % 8
            c.drawRect(left0 + sc * cell, top0 + sr * cell, left0 + (sc + 1) * cell, top0 + (sr + 1) * cell, p)
            p.color = 0xAA43A047.toInt()
            for (m in moves) {
                if (m / 64 == selected) {
                    val t = m % 64
                    c.drawCircle(left0 + (t % 8 + 0.5f) * cell, top0 + (t / 8 + 0.5f) * cell, cell * 0.2f, p)
                }
            }
        }
        for (i in 0 until 64) {
            val v = b[i]
            if (v == 0) continue
            val cx = left0 + (i % 8 + 0.5f) * cell
            val cy = top0 + (i / 8 + 0.5f) * cell
            p.style = Paint.Style.FILL
            p.color = 0x55000000
            c.drawCircle(cx + 3f * u, cy + 4f * u, cell * 0.38f, p)
            p.color = if (CheckersState.owner(v) == 1) 0xFFFF5252.toInt() else 0xFF29B6F6.toInt()
            c.drawCircle(cx, cy, cell * 0.38f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f * u
            p.color = 0xFF000000.toInt()
            c.drawCircle(cx, cy, cell * 0.38f, p)
            p.style = Paint.Style.FILL
            if (v > 2) text(c, "👑", cx, cy + cell * 0.14f, cell * 0.4f, 0xFFFFFFFF.toInt())
        }
        val foot = if (winner != 0) "আবার খেলতে ট্যাপ করুন" else "গুটিতে ট্যাপ করে ঘর বাছুন • ফিরতে ব্যাক বাটন"
        text(c, foot, width / 2f, top0 + cell * 8f + 44f * u, 18f * u, 0xFFB0BEC5.toInt())
    }

    private fun apply(m: Int) {
        st = st.play(m) as CheckersState
        selected = if (st.chainSquare >= 0) st.chainSquare else -1
        val w = st.winner()
        if (w != 0) {
            winner = w
            endAt = System.currentTimeMillis()
            if (w == 1) w1++ else if (w == 2) w2++
        } else if (mode == MODE_BOT && st.currentPlayer == 2) {
            thinking = true
            postDelayed({ botMove() }, 600)
        }
        invalidate()
    }

    private fun botMove() {
        thinking = false
        if (!isAttachedToWindow || winner != 0) return
        if (st.legalMoves().isEmpty()) return
        apply(UniversalBot.chooseMove(st, diff))
    }

    private fun restart() {
        started = 3 - started
        st = CheckersState(CheckersState.initial(), 1, -1, 0)
        selected = -1
        winner = 0
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        if (winner != 0) {
            if (System.currentTimeMillis() - endAt > 300) restart()
            return true
        }
        if (thinking || cell <= 0f) return true
        if (mode == MODE_BOT && st.currentPlayer != 1) return true
        if (e.x < left0 || e.y < top0 || e.x >= left0 + 8 * cell || e.y >= top0 + 8 * cell) return true
        val sq = ((e.y - top0) / cell).toInt() * 8 + ((e.x - left0) / cell).toInt()
        val moves = st.legalMoves()
        var dest = -1
        if (selected >= 0) {
            for (m in moves) if (m / 64 == selected && m % 64 == sq) dest = m
        }
        if (dest >= 0) {
            apply(dest)
        } else {
            var canSelect = false
            for (m in moves) if (m / 64 == sq) canSelect = true
            selected = if (canSelect) sq else -1
            invalidate()
        }
        return true
    }
}
