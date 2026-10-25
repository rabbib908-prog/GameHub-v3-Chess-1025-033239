package com.gamehub.app

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Any turn-based 2-player game can use the bot by implementing this interface.
 * Moves are plain integers (cell index, column number, etc.).
 * winner(): 0 = game continues, 1 or 2 = that player won, 3 = draw.
 */
interface GameState {
    val currentPlayer: Int
    fun legalMoves(): IntArray
    fun play(move: Int): GameState
    fun winner(): Int
    /** Optional: chance (0..1) that player 1 wins when the game is not finished. Default = unknown. */
    fun estimate(): Double = 0.5
}

object UniversalBot {
    private class Node(val state: GameState, val parent: Node?, val move: Int, val untried: ArrayList<Int>) {
        val children = ArrayList<Node>()
        var visits = 0
        var wins = 0.0
    }

    /** level 1 = easy (random), 2 = medium (tactics + quick search), 3 = hard (tactics + long search). */
    fun chooseMove(state: GameState, level: Int): Int {
        val moves = state.legalMoves()
        if (moves.size == 1) return moves[0]
        if (level <= 1) return moves[Random.nextInt(moves.size)]
        val me = state.currentPlayer
        for (m in moves) {
            if (state.play(m).winner() == me) return m
        }
        val safe = ArrayList<Int>()
        for (m in moves) {
            val s = state.play(m)
            var ok = true
            if (s.winner() == 0 && s.currentPlayer != me) {
                val o = s.currentPlayer
                for (r in s.legalMoves()) {
                    if (s.play(r).winner() == o) { ok = false; break }
                }
            }
            if (ok) safe.add(m)
        }
        val cands: List<Int> = if (safe.isEmpty()) moves.toList() else safe
        if (cands.size == 1) return cands[0]
        val budgetMs = if (level >= 3) 700L else 0L
        val minIter = if (level >= 3) 1500 else 250
        return mcts(state, cands, budgetMs, minIter)
    }

    private fun mcts(state: GameState, cands: List<Int>, budgetMs: Long, minIter: Int): Int {
        val root = Node(state, null, -1, ArrayList(cands))
        val end = System.currentTimeMillis() + budgetMs
        var iter = 0
        while (iter < minIter || System.currentTimeMillis() < end) {
            iter++
            var node = root
            while (node.untried.isEmpty() && node.children.isNotEmpty()) node = select(node)
            if (node.untried.isNotEmpty()) {
                val idx = Random.nextInt(node.untried.size)
                val mv = node.untried.removeAt(idx)
                val ns = node.state.play(mv)
                val untried = ArrayList<Int>()
                if (ns.winner() == 0) {
                    for (x in ns.legalMoves()) untried.add(x)
                }
                val child = Node(ns, node, mv, untried)
                node.children.add(child)
                node = child
            }
            var s = node.state
            var w = s.winner()
            var plies = 0
            while (w == 0 && plies < 90) {
                val ms = s.legalMoves()
                s = s.play(ms[Random.nextInt(ms.size)])
                w = s.winner()
                plies++
            }
            val p1 = when (w) {
                1 -> 1.0
                2 -> 0.0
                3 -> 0.5
                else -> s.estimate()
            }
            var n: Node? = node
            while (n != null) {
                n.visits++
                val par = n.parent
                if (par != null) {
                    val mover = par.state.currentPlayer
                    n.wins += if (mover == 1) p1 else 1.0 - p1
                }
                n = par
            }
        }
        var best = root.children[0]
        for (c in root.children) {
            if (c.visits > best.visits) best = c
        }
        return best.move
    }

    /** For games that provide estimate(): alpha-beta search to a fixed depth (used by chess). */
    fun chooseMoveSearch(state: GameState, level: Int): Int {
        val moves = state.legalMoves()
        if (moves.size == 1) return moves[0]
        if (level <= 1 && Random.nextInt(100) < 50) return moves[Random.nextInt(moves.size)]
        val me = state.currentPlayer
        val depth = when (level) {
            1 -> 1
            2 -> 2
            else -> 3
        }
        val scores = DoubleArray(moves.size)
        var best = -1e18
        for (i in moves.indices) {
            val child = state.play(moves[i])
            val v: Double = if (child.winner() == me) {
                1000.0
            } else {
                val pv = alphaBeta(child, depth - 1, -1e18, 1e18)
                if (me == 1) pv else -pv
            }
            scores[i] = v
            if (v > best) best = v
        }
        val eps = if (level <= 1) 0.15 else if (level == 2) 0.03 else 0.002
        val pool = ArrayList<Int>()
        for (i in moves.indices) {
            if (scores[i] >= best - eps) pool.add(moves[i])
        }
        return pool[Random.nextInt(pool.size)]
    }

    private fun alphaBeta(s: GameState, depth: Int, alpha0: Double, beta0: Double): Double {
        if (depth <= 0) return s.estimate()
        val w = s.winner()
        if (w == 1) return 1.0 + depth * 0.01
        if (w == 2) return -depth * 0.01
        if (w == 3) return 0.5
        val moves = s.legalMoves()
        var alpha = alpha0
        var beta = beta0
        if (s.currentPlayer == 1) {
            var v = -1e18
            for (m in moves) {
                val r = alphaBeta(s.play(m), depth - 1, alpha, beta)
                if (r > v) v = r
                if (v > alpha) alpha = v
                if (alpha >= beta) break
            }
            return v
        } else {
            var v = 1e18
            for (m in moves) {
                val r = alphaBeta(s.play(m), depth - 1, alpha, beta)
                if (r < v) v = r
                if (v < beta) beta = v
                if (alpha >= beta) break
            }
            return v
        }
    }

    private fun select(n: Node): Node {
        var best = n.children[0]
        var bestV = -1.0
        val lnN = ln(n.visits.toDouble())
        for (c in n.children) {
            val v = c.wins / c.visits + 1.41 * sqrt(lnN / c.visits)
            if (v > bestV) { bestV = v; best = c }
        }
        return best
    }
}

class TttState(private val b: IntArray, private val cur: Int) : GameState {
    private val w: Int = calc()

    override val currentPlayer: Int
        get() = cur

    private fun calc(): Int {
        for (l in LINES) {
            if (b[l[0]] != 0 && b[l[0]] == b[l[1]] && b[l[1]] == b[l[2]]) return b[l[0]]
        }
        for (v in b) if (v == 0) return 0
        return 3
    }

    override fun legalMoves(): IntArray {
        if (w != 0) return IntArray(0)
        return (0 until 9).filter { b[it] == 0 }.toIntArray()
    }

    override fun play(move: Int): GameState {
        val nb = b.copyOf()
        nb[move] = cur
        return TttState(nb, 3 - cur)
    }

    override fun winner(): Int = w

    companion object {
        private val LINES = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
    }
}

class C4State(private val b: Array<IntArray>, private val cur: Int, private val win: Int) : GameState {
    override val currentPlayer: Int
        get() = cur

    override fun winner(): Int = win

    override fun legalMoves(): IntArray {
        if (win != 0) return IntArray(0)
        return (0 until 7).filter { b[0][it] == 0 }.toIntArray()
    }

    override fun play(move: Int): GameState {
        val nb = Array(6) { b[it].copyOf() }
        var r = 5
        while (r >= 0 && nb[r][move] != 0) r--
        nb[r][move] = cur
        var w = 0
        if (four(nb, r, move, cur)) w = cur
        else if (nb[0].none { it == 0 }) w = 3
        return C4State(nb, 3 - cur, w)
    }

    private fun four(bd: Array<IntArray>, r: Int, c: Int, p: Int): Boolean {
        for (d in DIRS) {
            var n = 1
            for (sgn in intArrayOf(1, -1)) {
                var rr = r + sgn * d[0]
                var cc = c + sgn * d[1]
                while (rr in 0..5 && cc in 0..6 && bd[rr][cc] == p) {
                    n++
                    rr += sgn * d[0]
                    cc += sgn * d[1]
                }
            }
            if (n >= 4) return true
        }
        return false
    }

    companion object {
        private val DIRS = arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, -1))
    }
}

class CheckersState(private val b: IntArray, private val cur: Int, private val chain: Int, private val quiet: Int) : GameState {
    private var cache: IntArray? = null

    val board: IntArray
        get() = b

    val chainSquare: Int
        get() = chain

    override val currentPlayer: Int
        get() = cur

    override fun legalMoves(): IntArray {
        val c = cache
        if (c != null) return c
        val g = gen()
        cache = g
        return g
    }

    override fun winner(): Int {
        if (quiet >= 80) return 3
        return if (legalMoves().isEmpty()) 3 - cur else 0
    }

    private fun gen(): IntArray {
        val jumps = ArrayList<Int>()
        val steps = ArrayList<Int>()
        val fwd = if (cur == 1) -1 else 1
        for (i in 0 until 64) {
            if (chain >= 0 && i != chain) continue
            val p = b[i]
            if (p == 0 || owner(p) != cur) continue
            val king = p > 2
            for (d in DIRS) {
                if (!king && d[0] != fwd) continue
                val r = i / 8 + d[0]
                val c = i % 8 + d[1]
                if (r < 0 || r > 7 || c < 0 || c > 7) continue
                val t = r * 8 + c
                if (b[t] == 0) {
                    steps.add(i * 64 + t)
                } else if (owner(b[t]) == 3 - cur) {
                    val r2 = r + d[0]
                    val c2 = c + d[1]
                    if (r2 in 0..7 && c2 in 0..7 && b[r2 * 8 + c2] == 0) jumps.add(i * 64 + r2 * 8 + c2)
                }
            }
        }
        if (jumps.isNotEmpty()) return jumps.toIntArray()
        if (chain >= 0) return IntArray(0)
        return steps.toIntArray()
    }

    override fun play(move: Int): GameState {
        val from = move / 64
        val to = move % 64
        val nb = b.copyOf()
        var piece = nb[from]
        val wasMan = piece <= 2
        val r1 = from / 8
        val c1 = from % 8
        val r2 = to / 8
        val c2 = to % 8
        val jumped = abs(r2 - r1) == 2
        nb[from] = 0
        if (jumped) nb[((r1 + r2) / 2) * 8 + (c1 + c2) / 2] = 0
        var promoted = false
        if (piece == 1 && r2 == 0) { piece = 3; promoted = true }
        if (piece == 2 && r2 == 7) { piece = 4; promoted = true }
        nb[to] = piece
        val q = if (jumped || wasMan) 0 else quiet + 1
        if (jumped && !promoted) {
            val cont = CheckersState(nb, cur, to, q)
            if (cont.legalMoves().isNotEmpty()) return cont
        }
        return CheckersState(nb, 3 - cur, -1, q)
    }

    override fun estimate(): Double {
        var s = 0.0
        for (i in 0 until 64) {
            val p = b[i]
            if (p == 0) continue
            val v = if (p > 2) 1.7 else 1.0
            if (owner(p) == 1) s += v else s -= v
        }
        return 1.0 / (1.0 + exp(-s * 0.5))
    }

    companion object {
        private val DIRS = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))

        fun owner(p: Int): Int = if (p == 0) 0 else if (p == 1 || p == 3) 1 else 2

        fun initial(): IntArray {
            val b = IntArray(64)
            for (r in 0..7) for (c in 0..7) {
                if ((r + c) % 2 == 1) {
                    if (r <= 2) b[r * 8 + c] = 2
                    if (r >= 5) b[r * 8 + c] = 1
                }
            }
            return b
        }
    }
}
