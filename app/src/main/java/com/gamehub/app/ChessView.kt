package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class ChessView(context: Context, private val mode: Int, private val diff: Int) : View(context) {
    private var st = ChessState.initial()
    private var selected = -1
    private var lastFrom = -1
    private var lastTo = -1
    private var thinking = false
    private var winner = 0
    private var endAt = 0L
    private var promoOpen = false
    private var promoFrom = -1
    private var promoTo = -1
    private var w1 = 0
    private var w2 = 0
    private var wd = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var left0 = 0f
    private var top0 = 0f
    private var cell = 0f
    private val promoTypes = intArrayOf(5, 4, 3, 2)

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) {
        tp.textSize = size
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    private fun sqX(i: Int): Float = left0 + (i % 8) * cell
    private fun sqY(i: Int): Float = top0 + (i / 8) * cell

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF10152B.toInt())
        val u = min(width, height) / 400f
        val side = min(width * 0.96f, height * 0.62f)
        cell = side / 8f
        left0 = (width - side) / 2f
        top0 = height * 0.2f
        val b = st.board
        val moves = st.legalMoves()
        val n1 = if (mode == MODE_BOT) "আপনি (সাদা)" else "সাদা"
        val n2 = if (mode == MODE_BOT) "বট (কালো)" else "কালো"
        text(c, "$n1 $w1  •  ড্র $wd  •  $w2 $n2", width / 2f, 50f * u, 19f * u, 0xFFFFFFFF.toInt())
        val check = winner == 0 && st.inCheck()
        val status = when {
            winner == 1 -> if (mode == MODE_BOT) "চেকমেট! আপনি জিতেছেন 🎉" else "চেকমেট! সাদা জিতেছে 🎉"
            winner == 2 -> if (mode == MODE_BOT) "চেকমেট! বট জিতেছে 🤖" else "চেকমেট! কালো জিতেছে 🎉"
            winner == 3 -> "খেলা ড্র হয়েছে!"
            st.currentPlayer == 1 -> (if (mode == MODE_BOT) "আপনার পালা ⚪" else "সাদার পালা ⚪") + (if (check) " – চেক!" else "")
            else -> (if (mode == MODE_BOT) "বট ভাবছে... ⚫" else "কালোর পালা ⚫") + (if (check) " – চেক!" else "")
        }
        text(c, status, width / 2f, top0 - 22f * u, 22f * u, 0xFFFFD740.toInt())

        p.style = Paint.Style.FILL
        for (i in 0 until 64) {
            p.color = if ((i / 8 + i % 8) % 2 == 0) 0xFFEEEED2.toInt() else 0xFF769656.toInt()
            c.drawRect(sqX(i), sqY(i), sqX(i) + cell, sqY(i) + cell, p)
        }
        if (lastFrom >= 0) {
            p.color = 0x66FFEB3B
            c.drawRect(sqX(lastFrom), sqY(lastFrom), sqX(lastFrom) + cell, sqY(lastFrom) + cell, p)
            c.drawRect(sqX(lastTo), sqY(lastTo), sqX(lastTo) + cell, sqY(lastTo) + cell, p)
        }
        if (check) {
            val k = st.kingSquare(st.currentPlayer)
            if (k >= 0) {
                p.color = 0xAAFF1744.toInt()
                c.drawRect(sqX(k), sqY(k), sqX(k) + cell, sqY(k) + cell, p)
            }
        }
        if (selected >= 0) {
            p.color = 0x99FFEB3B.toInt()
            c.drawRect(sqX(selected), sqY(selected), sqX(selected) + cell, sqY(selected) + cell, p)
        }
        for (i in 0 until 8) {
            text(c, "abcdefgh"[i].toString(), left0 + (i + 0.5f) * cell, top0 + cell * 8f + 16f * u, 13f * u, 0xFFB0BEC5.toInt())
            text(c, (8 - i).toString(), left0 - 8f * u, top0 + (i + 0.6f) * cell, 12f * u, 0xFFB0BEC5.toInt())
        }
        for (i in 0 until 64) {
            val v = b[i]
            if (v == 0) continue
            PieceArt.draw(c, abs(v), v > 0, sqX(i) + cell / 2f, sqY(i) + cell / 2f, cell * 0.94f, p)
        }
        if (selected >= 0) {
            for (m in moves) {
                if (ChessState.from(m) != selected) continue
                val t = ChessState.to(m)
                val cx = sqX(t) + cell / 2f
                val cy = sqY(t) + cell / 2f
                if (b[t] == 0) {
                    p.style = Paint.Style.FILL
                    p.color = 0x8822262B.toInt()
                    c.drawCircle(cx, cy, cell * 0.16f, p)
                } else {
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = cell * 0.09f
                    p.color = 0xCCE53935.toInt()
                    c.drawCircle(cx, cy, cell * 0.44f, p)
                    p.style = Paint.Style.FILL
                }
            }
        }
        val foot = if (winner != 0) "আবার খেলতে ট্যাপ করুন" else "গুটিতে ট্যাপ করে ঘর বাছুন • ফিরতে ব্যাক বাটন"
        text(c, foot, width / 2f, top0 + cell * 8f + 46f * u, 17f * u, 0xFFB0BEC5.toInt())

        if (promoOpen) {
            p.style = Paint.Style.FILL
            p.color = 0xAA000000.toInt()
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
            rect.set(left0 + cell * 1.5f, top0 + cell * 3.2f, left0 + cell * 6.5f, top0 + cell * 4.9f)
            p.color = 0xFF263238.toInt()
            c.drawRoundRect(rect, 20f * u, 20f * u, p)
            text(c, "প্রমোশন: কী হবে?", width / 2f, top0 + cell * 3.7f, 20f * u, 0xFFFFFFFF.toInt())
            val white = st.currentPlayer == 1
            for (i in 0..3) {
                PieceArt.draw(c, promoTypes[i], white, left0 + (2 + i + 0.5f) * cell, top0 + cell * 4.3f, cell * 0.9f, p)
            }
        }
    }

    private fun apply(m: Int) {
        lastFrom = ChessState.from(m)
        lastTo = ChessState.to(m)
        st = st.play(m) as ChessState
        selected = -1
        promoOpen = false
        val w = st.winner()
        if (w != 0) {
            winner = w
            endAt = System.currentTimeMillis()
            if (w == 1) w1++ else if (w == 2) w2++ else wd++
        } else if (mode == MODE_BOT && st.currentPlayer == 2) {
            thinking = true
            postDelayed({ botMove() }, 500)
        }
        invalidate()
    }

    private fun botMove() {
        thinking = false
        if (!isAttachedToWindow || winner != 0) return
        if (st.legalMoves().isEmpty()) return
        apply(UniversalBot.chooseMoveSearch(st, diff))
    }

    private fun restart() {
        st = ChessState.initial()
        selected = -1
        lastFrom = -1
        lastTo = -1
        winner = 0
        promoOpen = false
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
        val moves = st.legalMoves()
        if (promoOpen) {
            val i = ((e.x - (left0 + 2f * cell)) / cell).toInt()
            val inRow = e.y >= top0 + cell * 3.8f && e.y <= top0 + cell * 4.9f && e.x >= left0 + 2f * cell
            if (inRow && i in 0..3) {
                for (m in moves) {
                    if (ChessState.from(m) == promoFrom && ChessState.to(m) == promoTo && ChessState.promo(m) == promoTypes[i]) {
                        apply(m)
                        return true
                    }
                }
            }
            promoOpen = false
            invalidate()
            return true
        }
        if (e.x < left0 || e.y < top0 || e.x >= left0 + 8 * cell || e.y >= top0 + 8 * cell) return true
        val sq = ((e.y - top0) / cell).toInt() * 8 + ((e.x - left0) / cell).toInt()
        if (selected >= 0) {
            var count = 0
            var first = -1
            for (m in moves) {
                if (ChessState.from(m) == selected && ChessState.to(m) == sq) {
                    count++
                    if (first < 0) first = m
                }
            }
            if (count == 1) {
                apply(first)
                return true
            }
            if (count > 1) {
                promoFrom = selected
                promoTo = sq
                promoOpen = true
                invalidate()
                return true
            }
        }
        var canSelect = false
        for (m in moves) if (ChessState.from(m) == sq) canSelect = true
        selected = if (canSelect) sq else -1
        invalidate()
        return true
    }
}
