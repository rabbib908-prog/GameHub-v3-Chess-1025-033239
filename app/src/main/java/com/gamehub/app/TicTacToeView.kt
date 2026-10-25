package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class TicTacToeView(context: Context, private val mode: Int, private val diff: Int) : View(context) {
    private val lines = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
    )
    private val b = IntArray(9)
    private var turn = 1
    private var starter = 1
    private var winner = 0
    private var winLine: IntArray? = null
    private var s1 = 0
    private var s2 = 0
    private var sd = 0
    private var thinking = false
    private var endAt = 0L
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bx = 0f
    private var by = 0f
    private var cs = 0f

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
        p.strokeCap = Paint.Cap.ROUND
    }

    private fun cx(i: Int): Float = bx + (i % 3 + 0.5f) * cs
    private fun cy(i: Int): Float = by + (i / 3 + 0.5f) * cs

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        tp.textSize = size
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF10152B.toInt())
        val u = min(width, height) / 400f
        val side = min(width * 0.9f, height * 0.5f)
        cs = side / 3f
        bx = (width - side) / 2f
        by = height * 0.3f
        val name1 = if (mode == MODE_BOT) "আপনি" else "খেলোয়াড় ১"
        val name2 = if (mode == MODE_BOT) "বট" else "খেলোয়াড় ২"
        text(c, "X  $s1   •   ড্র  $sd   •   O  $s2", width / 2f, 50f * u, 24f * u, 0xFFFFFFFF.toInt())
        text(c, "X = $name1     O = $name2", width / 2f, 80f * u, 16f * u, 0xFFB0BEC5.toInt())
        val status = when {
            winner == 1 -> if (mode == MODE_BOT) "আপনি জিতেছেন! 🎉" else "খেলোয়াড় ১ (X) জিতেছেন! 🎉"
            winner == 2 -> if (mode == MODE_BOT) "বট জিতেছে! 🤖" else "খেলোয়াড় ২ (O) জিতেছেন! 🎉"
            winner == 3 -> "খেলা ড্র হয়েছে!"
            turn == 1 -> if (mode == MODE_BOT) "আপনার পালা (X)" else "খেলোয়াড় ১ এর পালা (X)"
            else -> if (mode == MODE_BOT) "বট ভাবছে..." else "খেলোয়াড় ২ এর পালা (O)"
        }
        text(c, status, width / 2f, by - 30f * u, 26f * u, 0xFFFFD740.toInt())
        p.style = Paint.Style.STROKE
        p.strokeWidth = 6f * u
        p.color = 0xFF5C6BC0.toInt()
        for (k in 1..2) {
            c.drawLine(bx + k * cs, by + 8f * u, bx + k * cs, by + side - 8f * u, p)
            c.drawLine(bx + 8f * u, by + k * cs, bx + side - 8f * u, by + k * cs, p)
        }
        p.strokeWidth = 10f * u
        for (i in 0 until 9) {
            if (b[i] == 1) {
                p.color = 0xFFFF5252.toInt()
                val d = cs * 0.28f
                c.drawLine(cx(i) - d, cy(i) - d, cx(i) + d, cy(i) + d, p)
                c.drawLine(cx(i) + d, cy(i) - d, cx(i) - d, cy(i) + d, p)
            } else if (b[i] == 2) {
                p.color = 0xFF40C4FF.toInt()
                c.drawCircle(cx(i), cy(i), cs * 0.28f, p)
            }
        }
        val wl = winLine
        if (wl != null) {
            p.color = 0xFFFFD740.toInt()
            p.strokeWidth = 12f * u
            c.drawLine(cx(wl[0]), cy(wl[0]), cx(wl[2]), cy(wl[2]), p)
        }
        p.style = Paint.Style.FILL
        val foot = if (winner != 0) "আবার খেলতে ট্যাপ করুন" else "ফিরতে ব্যাক বাটন চাপুন"
        text(c, foot, width / 2f, by + side + 50f * u, 20f * u, 0xFFB0BEC5.toInt())
    }

    private fun winnerOf(bd: IntArray): Int {
        for (l in lines) {
            if (bd[l[0]] != 0 && bd[l[0]] == bd[l[1]] && bd[l[1]] == bd[l[2]]) return bd[l[0]]
        }
        for (v in bd) if (v == 0) return 0
        return 3
    }

    private fun finish(w: Int) {
        winner = w
        endAt = System.currentTimeMillis()
        if (w == 1) s1++ else if (w == 2) s2++ else sd++
        if (w != 3) winLine = lines.firstOrNull { b[it[0]] == w && b[it[1]] == w && b[it[2]] == w }
    }

    private fun afterMove() {
        val w = winnerOf(b)
        if (w != 0) {
            finish(w)
        } else {
            turn = 3 - turn
            if (mode == MODE_BOT && turn == 2) {
                thinking = true
                postDelayed({ botMove() }, 500)
            }
        }
        invalidate()
    }

    private fun newRound() {
        b.fill(0)
        winner = 0
        winLine = null
        starter = 3 - starter
        turn = starter
        if (mode == MODE_BOT && turn == 2) {
            thinking = true
            postDelayed({ botMove() }, 500)
        }
        invalidate()
    }

    private fun emptyCells(): List<Int> = (0 until 9).filter { b[it] == 0 }

    private fun pick(): Int = UniversalBot.chooseMove(TttState(b.copyOf(), 2), diff)

    private fun botMove() {
        thinking = false
        if (!isAttachedToWindow || winner != 0) return
        if (emptyCells().isEmpty()) return
        b[pick()] = 2
        afterMove()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        if (winner != 0) {
            if (System.currentTimeMillis() - endAt > 300) newRound()
            return true
        }
        if (thinking || cs <= 0f) return true
        if (mode == MODE_BOT && turn != 1) return true
        if (e.x < bx || e.y < by || e.x >= bx + 3 * cs || e.y >= by + 3 * cs) return true
        val col = ((e.x - bx) / cs).toInt()
        val row = ((e.y - by) / cs).toInt()
        val idx = row * 3 + col
        if (idx < 0 || idx > 8 || b[idx] != 0) return true
        b[idx] = turn
        afterMove()
        return true
    }
}
