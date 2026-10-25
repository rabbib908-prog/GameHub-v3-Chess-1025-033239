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

class Connect4View(context: Context, private val mode: Int, private val diff: Int) : View(context) {
    private val rows = 6
    private val cols = 7
    private val b = Array(rows) { IntArray(cols) }
    private val dirs = arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(-1, 1))
    private var turn = 1
    private var starter = 1
    private var winner = 0
    private var winRun: IntArray? = null
    private var s1 = 0
    private var s2 = 0
    private var sd = 0
    private var thinking = false
    private var endAt = 0L
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
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
        cell = min(width * 0.96f / cols, height * 0.55f / rows)
        left0 = (width - cell * cols) / 2f
        top0 = height * 0.26f
        val name1 = if (mode == MODE_BOT) "আপনি" else "খেলোয়াড় ১"
        val name2 = if (mode == MODE_BOT) "বট" else "খেলোয়াড় ২"
        text(c, "🔴 $s1   •   ড্র $sd   •   🟡 $s2", width / 2f, 50f * u, 24f * u, 0xFFFFFFFF.toInt())
        text(c, "🔴 = $name1     🟡 = $name2", width / 2f, 80f * u, 16f * u, 0xFFB0BEC5.toInt())
        val status = when {
            winner == 1 -> if (mode == MODE_BOT) "আপনি জিতেছেন! 🎉" else "খেলোয়াড় ১ (লাল) জিতেছেন! 🎉"
            winner == 2 -> if (mode == MODE_BOT) "বট জিতেছে! 🤖" else "খেলোয়াড় ২ (হলুদ) জিতেছেন! 🎉"
            winner == 3 -> "খেলা ড্র হয়েছে!"
            turn == 1 -> if (mode == MODE_BOT) "আপনার পালা 🔴" else "খেলোয়াড় ১ এর পালা 🔴"
            else -> if (mode == MODE_BOT) "বট ভাবছে... 🟡" else "খেলোয়াড় ২ এর পালা 🟡"
        }
        text(c, status, width / 2f, top0 - 30f * u, 24f * u, 0xFFFFD740.toInt())
        p.style = Paint.Style.FILL
        p.color = 0xFF1A237E.toInt()
        rect.set(left0 - 6f * u, top0 - 6f * u, left0 + cell * cols + 6f * u, top0 + cell * rows + 6f * u)
        c.drawRoundRect(rect, 20f * u, 20f * u, p)
        for (r in 0 until rows) for (k in 0 until cols) {
            p.style = Paint.Style.FILL
            p.color = when (b[r][k]) {
                1 -> 0xFFE53935.toInt()
                2 -> 0xFFFDD835.toInt()
                else -> 0xFF10152B.toInt()
            }
            c.drawCircle(left0 + (k + 0.5f) * cell, top0 + (r + 0.5f) * cell, cell * 0.4f, p)
        }
        val wr = winRun
        if (wr != null) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 5f * u
            p.color = 0xFFFFFFFF.toInt()
            for (i in 0..3) {
                val rr = wr[0] + i * wr[2]
                val cc = wr[1] + i * wr[3]
                c.drawCircle(left0 + (cc + 0.5f) * cell, top0 + (rr + 0.5f) * cell, cell * 0.42f, p)
            }
            p.style = Paint.Style.FILL
        }
        val foot = if (winner != 0) "আবার খেলতে ট্যাপ করুন" else "কলামে ট্যাপ করুন • ফিরতে ব্যাক বাটন চাপুন"
        text(c, foot, width / 2f, top0 + cell * rows + 50f * u, 18f * u, 0xFFB0BEC5.toInt())
    }

    private fun validCols(bd: Array<IntArray>): List<Int> =
        (0 until cols).sortedBy { abs(it - 3) }.filter { bd[0][it] == 0 }

    private fun drop(bd: Array<IntArray>, col: Int, pl: Int): Int {
        for (r in rows - 1 downTo 0) {
            if (bd[r][col] == 0) {
                bd[r][col] = pl
                return r
            }
        }
        return -1
    }

    private fun runStart(bd: Array<IntArray>, pl: Int): IntArray? {
        for (r in 0 until rows) for (k in 0 until cols) {
            if (bd[r][k] != pl) continue
            for (d in dirs) {
                val er = r + 3 * d[0]
                val ec = k + 3 * d[1]
                if (er < 0 || er >= rows || ec < 0 || ec >= cols) continue
                if (bd[r + d[0]][k + d[1]] == pl && bd[r + 2 * d[0]][k + 2 * d[1]] == pl && bd[er][ec] == pl) {
                    return intArrayOf(r, k, d[0], d[1])
                }
            }
        }
        return null
    }

    private fun pickCol(): Int = UniversalBot.chooseMove(C4State(Array(rows) { b[it].copyOf() }, 2, 0), diff)

    private fun afterMove() {
        val run = runStart(b, turn)
        if (run != null) {
            winner = turn
            winRun = run
            endAt = System.currentTimeMillis()
            if (turn == 1) s1++ else s2++
        } else if (validCols(b).isEmpty()) {
            winner = 3
            endAt = System.currentTimeMillis()
            sd++
        } else {
            turn = 3 - turn
            if (mode == MODE_BOT && turn == 2) {
                thinking = true
                postDelayed({ botMove() }, 450)
            }
        }
        invalidate()
    }

    private fun botMove() {
        thinking = false
        if (!isAttachedToWindow || winner != 0) return
        if (validCols(b).isEmpty()) return
        drop(b, pickCol(), 2)
        afterMove()
    }

    private fun newRound() {
        for (r in 0 until rows) for (k in 0 until cols) b[r][k] = 0
        winner = 0
        winRun = null
        starter = 3 - starter
        turn = starter
        if (mode == MODE_BOT && turn == 2) {
            thinking = true
            postDelayed({ botMove() }, 450)
        }
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        if (winner != 0) {
            if (System.currentTimeMillis() - endAt > 300) newRound()
            return true
        }
        if (thinking || cell <= 0f) return true
        if (mode == MODE_BOT && turn != 1) return true
        if (e.x < left0 || e.x >= left0 + cell * cols) return true
        val col = ((e.x - left0) / cell).toInt()
        if (col < 0 || col >= cols || b[0][col] != 0) return true
        drop(b, col, turn)
        afterMove()
        return true
    }
}
