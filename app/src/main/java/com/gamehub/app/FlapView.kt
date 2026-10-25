package com.gamehub.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FlapView(context: Context) : View(context), Pausable {
    private class Pipe(var x: Float, val gapTop: Float, var passed: Boolean)
    private enum class S { READY, PLAY, DEAD }

    private val prefs = context.getSharedPreferences("gamehub", Context.MODE_PRIVATE)
    private var best = prefs.getInt("flapBest", 0)
    private var tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (e: Exception) {
        null
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val pipes = ArrayList<Pipe>()
    private var sky: Shader? = null

    private var u = 1f
    private var state = S.READY
    private var birdY = 0f
    private var vy = 0f
    private var score = 0
    private var lastT = 0L
    private var t = 0f
    private var groundX = 0f
    private var deadAt = 0L
    private var running = true

    init {
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT_BOLD
    }

    override fun pauseGame() { running = false; lastT = 0L }

    override fun resumeGame() { running = true; lastT = 0L; invalidate() }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tone?.release()
        tone = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        u = h / 800f
        sky = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF4EC0E8.toInt(), 0xFFBFEAF5.toInt(), Shader.TileMode.CLAMP)
        reset()
    }

    private fun pipeW(): Float = 78f * u
    private fun gap(): Float = 220f * u
    private fun groundY(): Float = height - 110f * u
    private fun birdX(): Float = width * 0.28f
    private fun birdR(): Float = 18f * u

    private fun reset() {
        pipes.clear()
        score = 0
        vy = 0f
        birdY = height * 0.45f
        state = S.READY
    }

    private fun flap() {
        vy = -560f * u
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    private fun die() {
        if (state == S.DEAD) return
        state = S.DEAD
        deadAt = System.currentTimeMillis()
        vy = -220f * u
        if (score > best) {
            best = score
            prefs.edit().putInt("flapBest", best).apply()
        }
        tone?.startTone(ToneGenerator.TONE_PROP_NACK, 150)
    }

    private fun update(dt: Float) {
        t += dt
        val speed = (200f + min(score, 30) * 3f) * u
        if (state != S.DEAD) groundX = (groundX - speed * dt) % (40f * u)
        when (state) {
            S.READY -> birdY = height * 0.45f + sin(t * 4f) * 10f * u
            S.PLAY -> {
                vy += 1700f * u * dt
                birdY += vy * dt
                for (pp in pipes) pp.x -= speed * dt
                if (pipes.isEmpty() || pipes[pipes.size - 1].x < width - 300f * u) {
                    val minTop = 90f * u
                    val maxTop = groundY() - gap() - 90f * u
                    pipes.add(Pipe(width + 20f * u, minTop + Random.nextFloat() * (maxTop - minTop), false))
                }
                pipes.removeAll { it.x + pipeW() < 0f }
                val bx = birdX()
                val r = birdR()
                if (birdY - r < 0f) { birdY = r; vy = 0f }
                if (birdY + r >= groundY()) { birdY = groundY() - r; die() }
                for (pp in pipes) {
                    if (bx + r > pp.x && bx - r < pp.x + pipeW()) {
                        if (birdY - r < pp.gapTop || birdY + r > pp.gapTop + gap()) die()
                    }
                    if (!pp.passed && pp.x + pipeW() < bx - r) {
                        pp.passed = true
                        score++
                        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
                    }
                }
            }
            S.DEAD -> {
                if (birdY + birdR() < groundY()) {
                    vy += 1700f * u * dt
                    birdY += vy * dt
                    if (birdY + birdR() > groundY()) birdY = groundY() - birdR()
                }
            }
        }
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

    private fun outlined(c: Canvas, text: String, x: Float, y: Float, size: Float) {
        tp.textSize = size
        tp.style = Paint.Style.STROKE
        tp.strokeWidth = size * 0.14f
        tp.color = 0xFF000000.toInt()
        c.drawText(text, x, y, tp)
        tp.style = Paint.Style.FILL
        tp.color = 0xFFFFFFFF.toInt()
        c.drawText(text, x, y, tp)
    }

    private fun plainText(c: Canvas, text: String, x: Float, y: Float, size: Float) {
        tp.style = Paint.Style.FILL
        tp.textSize = size
        tp.color = 0xFF5C3B1E.toInt()
        c.drawText(text, x, y, tp)
    }

    private fun pipeRect(c: Canvas, l: Float, tp0: Float, r: Float, b: Float) {
        rect.set(l, tp0, r, b)
        p.style = Paint.Style.FILL
        p.color = 0xFF73BF2E.toInt()
        c.drawRect(rect, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * u
        p.color = 0xFF3F6B17.toInt()
        c.drawRect(rect, p)
        p.style = Paint.Style.FILL
    }

    private fun render(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.shader = sky
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        p.color = 0xCCFFFFFF.toInt()
        for (i in 0..3) {
            var cx = (i * 0.3f * w - t * 14f * u) % (w + 240f * u)
            if (cx < -120f * u) cx += w + 240f * u
            val cy = h * (0.12f + 0.09f * i)
            c.drawCircle(cx, cy, 26f * u, p)
            c.drawCircle(cx + 28f * u, cy + 6f * u, 22f * u, p)
            c.drawCircle(cx - 28f * u, cy + 8f * u, 20f * u, p)
        }

        val gy = groundY()
        for (pp in pipes) {
            pipeRect(c, pp.x, -10f, pp.x + pipeW(), pp.gapTop)
            pipeRect(c, pp.x - 4f * u, pp.gapTop - 26f * u, pp.x + pipeW() + 4f * u, pp.gapTop)
            pipeRect(c, pp.x, pp.gapTop + gap(), pp.x + pipeW(), gy)
            pipeRect(c, pp.x - 4f * u, pp.gapTop + gap(), pp.x + pipeW() + 4f * u, pp.gapTop + gap() + 26f * u)
        }

        p.style = Paint.Style.FILL
        p.color = 0xFFDED895.toInt()
        c.drawRect(0f, gy, w, h, p)
        p.color = 0xFFC9C27A.toInt()
        var sx = groundX - 40f * u
        while (sx < w) {
            c.drawRect(sx, gy + 20f * u, sx + 20f * u, h, p)
            sx += 40f * u
        }
        p.color = 0xFF73BF2E.toInt()
        c.drawRect(0f, gy, w, gy + 18f * u, p)
        p.color = 0xFF3F6B17.toInt()
        c.drawRect(0f, gy + 18f * u, w, gy + 22f * u, p)

        drawBird(c)

        if (state != S.READY) outlined(c, score.toString(), w / 2f, 140f * u, 72f * u)
        if (state == S.READY) {
            outlined(c, "উড়ন্ত পাখি", w / 2f, h * 0.25f, 48f * u)
            outlined(c, "শুরু করতে ট্যাপ করুন", w / 2f, h * 0.62f, 32f * u)
            outlined(c, "সেরা: $best", w / 2f, h * 0.68f, 24f * u)
        }
        if (state == S.DEAD) {
            rect.set(w * 0.12f, h * 0.26f, w * 0.88f, h * 0.54f)
            p.style = Paint.Style.FILL
            p.color = 0xFFFFF3C4.toInt()
            c.drawRoundRect(rect, 24f * u, 24f * u, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 5f * u
            p.color = 0xFF5C3B1E.toInt()
            c.drawRoundRect(rect, 24f * u, 24f * u, p)
            p.style = Paint.Style.FILL
            plainText(c, "খেলা শেষ", w / 2f, h * 0.33f, 44f * u)
            plainText(c, "স্কোর  $score", w / 2f, h * 0.40f, 32f * u)
            plainText(c, "সেরা  $best", w / 2f, h * 0.45f, 32f * u)
            if (System.currentTimeMillis() - deadAt > 600) plainText(c, "আবার খেলতে ট্যাপ করুন", w / 2f, h * 0.51f, 26f * u)
        }
    }

    private fun drawBird(c: Canvas) {
        val r = birdR()
        val angle = if (state == S.READY) 0f else (vy / (700f * u) * 60f).coerceIn(-25f, 80f)
        c.save()
        c.translate(birdX(), birdY)
        c.rotate(angle)
        p.style = Paint.Style.FILL
        p.color = 0xFFF8D030.toInt()
        c.drawCircle(0f, 0f, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * u
        p.color = 0xFF5C3B1E.toInt()
        c.drawCircle(0f, 0f, r, p)
        p.style = Paint.Style.FILL
        val flapOffset = if (state == S.PLAY) sin(t * 22f) * r * 0.25f else 0f
        rect.set(-r * 0.9f, -r * 0.05f + flapOffset, r * 0.15f, r * 0.6f + flapOffset)
        p.color = 0xFFFFF0A0.toInt()
        c.drawOval(rect, p)
        p.color = 0xFFFFFFFF.toInt()
        c.drawCircle(r * 0.35f, -r * 0.3f, r * 0.38f, p)
        p.color = 0xFF000000.toInt()
        c.drawCircle(r * 0.5f, -r * 0.3f, r * 0.17f, p)
        path.reset()
        path.moveTo(r * 0.8f, -r * 0.05f)
        path.lineTo(r * 1.5f, r * 0.15f)
        path.lineTo(r * 0.8f, r * 0.4f)
        path.close()
        p.color = 0xFFF08030.toInt()
        c.drawPath(path, p)
        c.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            when (state) {
                S.READY -> { state = S.PLAY; flap() }
                S.PLAY -> flap()
                S.DEAD -> if (System.currentTimeMillis() - deadAt > 600) reset()
            }
        }
        return true
    }
}
