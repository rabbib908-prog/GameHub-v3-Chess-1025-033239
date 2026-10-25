package com.gamehub.app

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.exp

/**
 * Full chess rules. Pieces: 1 pawn, 2 knight, 3 bishop, 4 rook, 5 queen, 6 king.
 * White = positive, black = negative. Square index = row * 8 + col, row 0 is the black side (top).
 * Move = from | (to shl 6) | (promotion piece shl 12).
 * Player 1 = white, player 2 = black.
 */
class ChessState(
    private val b: IntArray,
    private val cur: Int,
    private val castle: Int,
    private val ep: Int,
    private val half: Int
) : GameState {
    private var cache: IntArray? = null

    val board: IntArray
        get() = b

    override val currentPlayer: Int
        get() = cur

    fun kingSquare(color: Int): Int {
        val k = if (color == 1) 6 else -6
        for (i in 0 until 64) if (b[i] == k) return i
        return -1
    }

    fun inCheck(): Boolean {
        val k = kingSquare(cur)
        return k >= 0 && attacked(b, k, 3 - cur)
    }

    override fun legalMoves(): IntArray {
        val c = cache
        if (c != null) return c
        val g = genLegal()
        cache = g
        return g
    }

    override fun winner(): Int {
        if (legalMoves().isEmpty()) return if (inCheck()) 3 - cur else 3
        if (half >= 100) return 3
        if (insufficient()) return 3
        return 0
    }

    private fun insufficient(): Boolean {
        var count = 0
        var minor = false
        for (v in b) {
            val t = abs(v)
            if (t == 0 || t == 6) continue
            count++
            if (t == 2 || t == 3) minor = true
        }
        return count == 0 || (count == 1 && minor)
    }

    private fun isEnemy(v: Int): Boolean = v != 0 && (v > 0) != (cur == 1)

    private fun addPawn(out: ArrayList<Int>, from: Int, to: Int, promo: Boolean) {
        if (promo) {
            for (p in 2..5) out.add(from or (to shl 6) or (p shl 12))
        } else {
            out.add(from or (to shl 6))
        }
    }

    private fun slide(out: ArrayList<Int>, i: Int, dirs: Array<IntArray>) {
        val r = i / 8
        val c = i % 8
        for (d in dirs) {
            var rr = r + d[0]
            var cc = c + d[1]
            while (rr in 0..7 && cc in 0..7) {
                val tv = b[rr * 8 + cc]
                if (tv == 0) {
                    out.add(i or ((rr * 8 + cc) shl 6))
                } else {
                    if (isEnemy(tv)) out.add(i or ((rr * 8 + cc) shl 6))
                    break
                }
                rr += d[0]
                cc += d[1]
            }
        }
    }

    private fun genPseudo(): ArrayList<Int> {
        val out = ArrayList<Int>()
        val sgn = if (cur == 1) 1 else -1
        for (i in 0 until 64) {
            val v = b[i]
            if (v == 0 || (v > 0) != (cur == 1)) continue
            val t = abs(v)
            val r = i / 8
            val c = i % 8
            when (t) {
                1 -> {
                    val dir = if (cur == 1) -1 else 1
                    val start = if (cur == 1) 6 else 1
                    val promoRow = if (cur == 1) 0 else 7
                    val r1 = r + dir
                    if (r1 in 0..7) {
                        if (b[r1 * 8 + c] == 0) {
                            addPawn(out, i, r1 * 8 + c, r1 == promoRow)
                            if (r == start && b[(r + 2 * dir) * 8 + c] == 0) out.add(i or (((r + 2 * dir) * 8 + c) shl 6))
                        }
                        for (dc in intArrayOf(-1, 1)) {
                            val cc = c + dc
                            if (cc in 0..7) {
                                val t2 = r1 * 8 + cc
                                if (isEnemy(b[t2]) || (t2 == ep && b[t2] == 0)) addPawn(out, i, t2, r1 == promoRow)
                            }
                        }
                    }
                }
                2 -> {
                    for (o in KN) {
                        val rr = r + o[0]
                        val cc = c + o[1]
                        if (rr in 0..7 && cc in 0..7) {
                            val tv = b[rr * 8 + cc]
                            if (tv == 0 || isEnemy(tv)) out.add(i or ((rr * 8 + cc) shl 6))
                        }
                    }
                }
                3 -> slide(out, i, DIAG)
                4 -> slide(out, i, ORTH)
                5 -> { slide(out, i, DIAG); slide(out, i, ORTH) }
                6 -> {
                    for (dr in -1..1) for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val rr = r + dr
                        val cc = c + dc
                        if (rr in 0..7 && cc in 0..7) {
                            val tv = b[rr * 8 + cc]
                            if (tv == 0 || isEnemy(tv)) out.add(i or ((rr * 8 + cc) shl 6))
                        }
                    }
                    val base = if (cur == 1) 56 else 0
                    if (i == base + 4) {
                        val enemy = 3 - cur
                        val kBit = if (cur == 1) 1 else 4
                        val qBit = if (cur == 1) 2 else 8
                        if ((castle and kBit) != 0 && b[base + 5] == 0 && b[base + 6] == 0 && b[base + 7] == sgn * 4 &&
                            !attacked(b, base + 4, enemy) && !attacked(b, base + 5, enemy) && !attacked(b, base + 6, enemy)
                        ) out.add(i or ((base + 6) shl 6))
                        if ((castle and qBit) != 0 && b[base + 1] == 0 && b[base + 2] == 0 && b[base + 3] == 0 && b[base] == sgn * 4 &&
                            !attacked(b, base + 4, enemy) && !attacked(b, base + 3, enemy) && !attacked(b, base + 2, enemy)
                        ) out.add(i or ((base + 2) shl 6))
                    }
                }
            }
        }
        return out
    }

    private fun applyBoard(m: Int): IntArray {
        val from = m and 63
        val to = (m shr 6) and 63
        val promo = (m shr 12) and 7
        val nb = b.copyOf()
        val v = nb[from]
        val t = abs(v)
        val sg = if (v > 0) 1 else -1
        nb[from] = 0
        if (t == 1 && to == ep && nb[to] == 0 && (from % 8) != (to % 8)) nb[(from / 8) * 8 + to % 8] = 0
        nb[to] = if (promo != 0) sg * promo else v
        if (t == 6 && abs((to % 8) - (from % 8)) == 2) {
            if (to % 8 == 6) { nb[from + 1] = nb[from + 3]; nb[from + 3] = 0 }
            else { nb[from - 1] = nb[from - 4]; nb[from - 4] = 0 }
        }
        return nb
    }

    private fun genLegal(): IntArray {
        val ks = kingSquare(cur)
        val caps = ArrayList<Int>()
        val quiet = ArrayList<Int>()
        for (m in genPseudo()) {
            val from = m and 63
            val to = (m shr 6) and 63
            val nb = applyBoard(m)
            val kingNow = if (abs(b[from]) == 6) to else ks
            if (kingNow >= 0 && attacked(nb, kingNow, 3 - cur)) continue
            val isCap = b[to] != 0 || (abs(b[from]) == 1 && to == ep && (from % 8) != (to % 8))
            if (isCap) caps.add(m) else quiet.add(m)
        }
        caps.addAll(quiet)
        return caps.toIntArray()
    }

    override fun play(move: Int): GameState {
        val from = move and 63
        val to = (move shr 6) and 63
        val nb = applyBoard(move)
        val v = b[from]
        val t = abs(v)
        var nc = castle
        if (t == 6) nc = if (cur == 1) (nc and 12) else (nc and 3)
        if (from == 63 || to == 63) nc = nc and 14
        if (from == 56 || to == 56) nc = nc and 13
        if (from == 7 || to == 7) nc = nc and 11
        if (from == 0 || to == 0) nc = nc and 7
        val nep = if (t == 1 && abs(to / 8 - from / 8) == 2) (from + to) / 2 else -1
        val cap = b[to] != 0 || (t == 1 && to == ep && (from % 8) != (to % 8))
        val nh = if (t == 1 || cap) 0 else half + 1
        return ChessState(nb, 3 - cur, nc, nep, nh)
    }

    override fun estimate(): Double {
        var cp = 0
        for (i in 0 until 64) {
            val v = b[i]
            if (v == 0) continue
            val t = abs(v)
            val r = i / 8
            val c = i % 8
            val cen = 7 - (abs(2 * r - 7) + abs(2 * c - 7)) / 2
            var s = when (t) {
                1 -> 100 + cen * 2 + (if (v > 0) 6 - r else r - 1) * 4
                2 -> 320 + cen * 6
                3 -> 330 + cen * 4
                4 -> 500
                5 -> 900
                else -> 0
            }
            if (v < 0) s = -s
            cp += s
        }
        return 1.0 / (1.0 + exp(-cp / 400.0))
    }

    companion object {
        private val KN = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1)
        )
        private val DIAG = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        private val ORTH = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))

        fun from(m: Int): Int = m and 63
        fun to(m: Int): Int = (m shr 6) and 63
        fun promo(m: Int): Int = (m shr 12) and 7

        private fun attacked(bd: IntArray, sq: Int, by: Int): Boolean {
            val sgn = if (by == 1) 1 else -1
            val r = sq / 8
            val c = sq % 8
            val pr = if (by == 1) r + 1 else r - 1
            if (pr in 0..7) {
                for (dc in intArrayOf(-1, 1)) {
                    val pc = c + dc
                    if (pc in 0..7 && bd[pr * 8 + pc] == sgn) return true
                }
            }
            for (o in KN) {
                val rr = r + o[0]
                val cc = c + o[1]
                if (rr in 0..7 && cc in 0..7 && bd[rr * 8 + cc] == sgn * 2) return true
            }
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val rr = r + dr
                val cc = c + dc
                if (rr in 0..7 && cc in 0..7 && bd[rr * 8 + cc] == sgn * 6) return true
            }
            for (d in DIAG) {
                var rr = r + d[0]
                var cc = c + d[1]
                while (rr in 0..7 && cc in 0..7) {
                    val v = bd[rr * 8 + cc]
                    if (v != 0) {
                        if (v == sgn * 3 || v == sgn * 5) return true
                        break
                    }
                    rr += d[0]
                    cc += d[1]
                }
            }
            for (d in ORTH) {
                var rr = r + d[0]
                var cc = c + d[1]
                while (rr in 0..7 && cc in 0..7) {
                    val v = bd[rr * 8 + cc]
                    if (v != 0) {
                        if (v == sgn * 4 || v == sgn * 5) return true
                        break
                    }
                    rr += d[0]
                    cc += d[1]
                }
            }
            return false
        }

        fun initial(): ChessState {
            val b = IntArray(64)
            val back = intArrayOf(4, 2, 3, 5, 6, 3, 2, 4)
            for (c in 0..7) {
                b[c] = -back[c]
                b[8 + c] = -1
                b[48 + c] = 1
                b[56 + c] = back[c]
            }
            return ChessState(b, 1, 15, -1, 0)
        }
    }
}

/** Vector chess piece models drawn with paths (no images needed). */
object PieceArt {
    private val paths = HashMap<Int, Path>()

    private fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float): Path {
        val p = Path()
        p.addRoundRect(RectF(l, t, r, b), rad, rad, Path.Direction.CW)
        return p
    }

    private fun circ(x: Float, y: Float, r: Float): Path {
        val p = Path()
        p.addCircle(x, y, r, Path.Direction.CW)
        return p
    }

    private fun oval(l: Float, t: Float, r: Float, b: Float): Path {
        val p = Path()
        p.addOval(RectF(l, t, r, b), Path.Direction.CW)
        return p
    }

    private fun poly(vararg pts: Float): Path {
        val p = Path()
        p.moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            p.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        p.close()
        return p
    }

    private fun merge(parts: List<Path>): Path {
        val out = Path(parts[0])
        for (i in 1 until parts.size) {
            val t = Path(out)
            if (t.op(parts[i], Path.Op.UNION)) out.set(t) else out.addPath(parts[i])
        }
        return out
    }

    private fun build(type: Int): Path {
        val base = rr(22f, 80f, 78f, 91f, 4f)
        return when (type) {
            1 -> merge(listOf(base, circ(50f, 31f, 13f), poly(35f, 81f, 41f, 52f, 50f, 46f, 59f, 52f, 65f, 81f)))
            2 -> {
                val h = Path()
                h.moveTo(32f, 81f)
                h.cubicTo(32f, 66f, 40f, 60f, 40f, 50f)
                h.cubicTo(36f, 48f, 30f, 46f, 26f, 40f)
                h.lineTo(26f, 34f)
                h.lineTo(34f, 26f)
                h.lineTo(38f, 14f)
                h.lineTo(46f, 22f)
                h.cubicTo(60f, 22f, 72f, 34f, 72f, 52f)
                h.lineTo(68f, 81f)
                h.close()
                merge(listOf(base, h))
            }
            3 -> merge(listOf(base, poly(36f, 81f, 43f, 52f, 50f, 46f, 57f, 52f, 64f, 81f), oval(40f, 22f, 60f, 52f), circ(50f, 18f, 5f)))
            4 -> merge(listOf(
                base,
                poly(32f, 81f, 35f, 42f, 65f, 42f, 68f, 81f),
                poly(30f, 24f, 40f, 24f, 40f, 30f, 45f, 30f, 45f, 24f, 55f, 24f, 55f, 30f, 60f, 30f, 60f, 24f, 70f, 24f, 70f, 42f, 30f, 42f)
            ))
            5 -> merge(listOf(
                base,
                poly(30f, 81f, 36f, 50f, 64f, 50f, 70f, 81f),
                poly(33f, 50f, 26f, 24f, 40f, 38f, 50f, 18f, 60f, 38f, 74f, 24f, 67f, 50f),
                circ(26f, 22f, 4f), circ(50f, 15f, 4f), circ(74f, 22f, 4f)
            ))
            else -> {
                val cr = Path()
                cr.moveTo(34f, 50f)
                cr.quadTo(30f, 32f, 50f, 32f)
                cr.quadTo(70f, 32f, 66f, 50f)
                cr.close()
                merge(listOf(
                    base,
                    poly(30f, 81f, 36f, 50f, 64f, 50f, 70f, 81f),
                    cr, rr(46f, 10f, 54f, 33f, 2f), rr(40f, 16f, 60f, 24f, 2f)
                ))
            }
        }
    }

    fun draw(c: Canvas, type: Int, white: Boolean, cx: Float, cy: Float, size: Float, p: Paint) {
        val path = paths.getOrPut(type) { build(type) }
        val s = size / 100f
        val line = if (white) 0xFF263238.toInt() else 0xFFCFD8DC.toInt()
        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        c.scale(s, s)
        p.style = Paint.Style.FILL
        p.color = if (white) 0xFFFFFFFF.toInt() else 0xFF2B2F36.toInt()
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.strokeJoin = Paint.Join.ROUND
        p.color = line
        c.drawPath(path, p)
        if (type == 2) {
            p.style = Paint.Style.FILL
            c.drawCircle(44f, 32f, 2.8f, p)
        }
        if (type == 3) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.5f
            c.drawLine(46f, 30f, 55f, 40f, p)
        }
        c.restore()
        p.style = Paint.Style.FILL
    }
}
