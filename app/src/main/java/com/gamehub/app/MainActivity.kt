package com.gamehub.app

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class GameInfo(
    val name: String, val emoji: String, val c1: Int, val c2: Int,
    val desc: String, val two: Boolean, val make: (Int, Int) -> View
)

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private var current: View? = null
    private var onBack: (() -> Unit)? = null
    private var lastFilter = 0

    private val games: List<GameInfo> by lazy {
        listOf(
            GameInfo("উড়ন্ত পাখি", "🐦", 0xFF29B6F6.toInt(), 0xFF0277BD.toInt(),
                "স্ক্রিনে ট্যাপ করে পাখিকে উড়ান। পাইপের ফাঁক দিয়ে যান। ধাক্কা লাগলে খেলা শেষ। যত বেশি পাইপ পার, তত বেশি স্কোর!",
                false) { _, _ -> FlapView(this) },
            GameInfo("সাপ", "🐍", 0xFF66BB6A.toInt(), 0xFF1B5E20.toInt(),
                "স্বাইপ করে সাপকে ঘোরান। লাল বল খেয়ে বড় হন। দেওয়াল বা নিজের শরীরে ধাক্কা লাগলে শেষ।",
                false) { _, _ -> SnakeView(this) },
            GameInfo("স্মৃতি মিল", "🧠", 0xFFAB47BC.toInt(), 0xFF4A148C.toInt(),
                "কার্ড উল্টে একই ছবির জোড়া খুঁজুন। কম চালে সব জোড়া মেলালে জয়।",
                false) { _, _ -> MemoryView(this) },
            GameInfo("টিক ট্যাক টো", "❌", 0xFFFF7043.toInt(), 0xFFBF360C.toInt(),
                "৩×৩ ঘরে পালা করে X ও O বসান। পরপর তিনটা এক লাইনে মেলাতে পারলে জয়।",
                true) { m, d -> TicTacToeView(this, m, d) },
            GameInfo("চার মিলাও", "🔴", 0xFF5C6BC0.toInt(), 0xFF1A237E.toInt(),
                "কলামে ট্যাপ করে গুটি ফেলুন। সোজা, আড়াআড়ি বা কোনাকুনি পরপর ৪টা মেলালে জয়।",
                true) { m, d -> Connect4View(this, m, d) },
            GameInfo("পাথর কাগজ কাঁচি", "✊", 0xFF4CAF50.toInt(), 0xFF1B5E20.toInt(),
                "একসাথে বাছাই করুন: পাথর, কাগজ বা কাঁচি। পাথর কাঁচিকে, কাঁচি কাগজকে, কাগজ পাথরকে হারায়। আগে ৩ জিতলে জয়।",
                true) { m, d -> RpsView(this, m, d) },
            GameInfo("চেকার্স", "♟️", 0xFFFF8A65.toInt(), 0xFF8D3B1E.toInt(),
                "গুটি কোনাকুনি এগিয়ে নিন। প্রতিপক্ষের গুটির উপর দিয়ে লাফিয়ে কাটুন (কাটা বাধ্যতামূলক)। শেষ সারিতে পৌঁছালে রাজা হবেন। প্রতিপক্ষের সব গুটি শেষ বা আটকে দিলে জয়।",
                true) { m, d -> CheckersView(this, m, d) },
            GameInfo("দাবা", "♞", 0xFF546E7A.toInt(), 0xFF1C262B.toInt(),
                "পূর্ণ দাবার নিয়ম: ক্যাসলিং, এন-পাসাঁ, প্রমোশন সব আছে। গুটিতে ট্যাপ করে ঘর বাছুন। প্রতিপক্ষের রাজাকে চেকমেট করলে জয়।",
                true) { m, d -> ChessView(this, m, d) },
            GameInfo("পিং পং", "🏓", 0xFFFFCA28.toInt(), 0xFFEF6C00.toInt(),
                "আঙুল দিয়ে প্যাডেল সরিয়ে বল ফেরত পাঠান। বল মিস করলে প্রতিপক্ষ পয়েন্ট পায়। আগে ৫ পয়েন্ট পেলে জয়।",
                true) { m, d -> PingPongView(this, m, d) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        root.setBackgroundColor(0xFF10152B.toInt())
        setContentView(root)
        home(0)
    }

    override fun onPause() {
        super.onPause()
        (current as? Pausable)?.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        (current as? Pausable)?.resumeGame()
    }

    override fun onBackPressed() {
        val b = onBack
        if (b != null) b() else super.onBackPressed()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 16): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radius).toFloat()
        return g
    }

    private fun show(v: View, back: (() -> Unit)?) {
        (current as? Pausable)?.pauseGame()
        root.removeAllViews()
        current = v
        onBack = back
        root.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        t.gravity = Gravity.CENTER
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun button(text: String, color: Int, onClick: () -> Unit): Button {
        val btn = Button(this)
        btn.text = text
        btn.textSize = 18f
        btn.setAllCaps(false)
        btn.setTextColor(0xFFFFFFFF.toInt())
        btn.background = rounded(color)
        btn.setPadding(0, dp(14), 0, dp(14))
        btn.setOnClickListener { onClick() }
        return btn
    }

    private fun home(filter: Int) {
        lastFilter = filter
        val scroll = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), dp(28), dp(12), dp(24))
        col.addView(label("🎮 গেম হাব", 32f, 0xFFFFFFFF.toInt(), true))
        val sub = label("একা বা দুইজনে খেলুন", 15f, 0xFFB0BEC5.toInt(), false)
        sub.setPadding(0, dp(4), 0, dp(12))
        col.addView(sub)

        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.gravity = Gravity.CENTER
        val names = listOf("সব", "👤 ১ প্লেয়ার", "👥 ২ প্লেয়ার")
        for (i in names.indices) {
            val b = Button(this)
            b.text = names[i]
            b.textSize = 14f
            b.setAllCaps(false)
            b.setTextColor(0xFFFFFFFF.toInt())
            b.background = rounded(if (i == filter) 0xFF3D5AFE.toInt() else 0xFF2A3158.toInt(), 20)
            b.setOnClickListener { home(i) }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42))
            lp.setMargins(dp(4), 0, dp(4), 0)
            chips.addView(b, lp)
        }
        col.addView(chips)

        val list = games.filter { filter == 0 || (filter == 1 && !it.two) || (filter == 2 && it.two) }
        var i = 0
        while (i < list.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in 0..1) {
                val lp = LinearLayout.LayoutParams(0, dp(150), 1f)
                lp.setMargins(dp(6), dp(6), dp(6), dp(6))
                if (i + k < list.size) row.addView(card(list[i + k]), lp)
                else row.addView(View(this), lp)
            }
            col.addView(row)
            i += 2
        }
        scroll.addView(col)
        show(scroll, null)
    }

    private fun card(g: GameInfo): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(g.c1, g.c2))
        bg.cornerRadius = dp(22).toFloat()
        box.background = bg
        box.addView(label(g.emoji, 44f, 0xFFFFFFFF.toInt(), false))
        box.addView(label(g.name, 17f, 0xFFFFFFFF.toInt(), true))
        val tag = label(if (g.two) "👥 ২ প্লেয়ার" else "👤 ১ প্লেয়ার", 12f, 0xFFFFFFFF.toInt(), false)
        tag.alpha = 0.85f
        box.addView(tag)
        box.setOnClickListener { info(g) }
        return box
    }

    private fun info(g: GameInfo) {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.gravity = Gravity.CENTER
        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(g.c1, g.c2))
        header.background = bg
        header.setPadding(dp(20), dp(60), dp(20), dp(36))
        header.addView(label(g.emoji, 60f, 0xFFFFFFFF.toInt(), false))
        header.addView(label(g.name, 30f, 0xFFFFFFFF.toInt(), true))
        col.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(24), dp(24), dp(24), dp(24))
        val d = label(g.desc, 17f, 0xFFE0E6F5.toInt(), false)
        d.setPadding(0, 0, 0, dp(20))
        body.addView(d)
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        if (!g.two) {
            val lp = LinearLayout.LayoutParams(mp, wc)
            lp.topMargin = dp(10)
            body.addView(button("▶ খেলুন", 0xFF3D5AFE.toInt()) { show(g.make(0, 0)) { info(g) } }, lp)
        } else {
            val lp1 = LinearLayout.LayoutParams(mp, wc)
            lp1.topMargin = dp(10)
            body.addView(button("🤝 বন্ধুর সাথে খেলুন", 0xFF3D5AFE.toInt()) { show(g.make(MODE_FRIEND, 0)) { info(g) } }, lp1)
            val lp2 = LinearLayout.LayoutParams(mp, wc)
            lp2.topMargin = dp(12)
            body.addView(button("🤖 বটের সাথে খেলুন", 0xFF3D5AFE.toInt()) { diffPage(g) }, lp2)
        }
        val lp3 = LinearLayout.LayoutParams(mp, wc)
        lp3.topMargin = dp(12)
        body.addView(button("← ফিরে যান", 0xFF2A3158.toInt()) { home(lastFilter) }, lp3)
        col.addView(body, LinearLayout.LayoutParams(mp, 0, 1f))
        col.setBackgroundColor(0xFF10152B.toInt())
        show(col) { home(lastFilter) }
    }

    private fun diffPage(g: GameInfo) {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.setPadding(dp(28), dp(28), dp(28), dp(28))
        col.addView(label("${g.emoji} ${g.name}", 28f, 0xFFFFFFFF.toInt(), true))
        val sub = label("কঠিনতা বেছে নিন", 16f, 0xFFB0BEC5.toInt(), false)
        sub.setPadding(0, dp(6), 0, dp(10))
        col.addView(sub)
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        val opts = listOf("😊 সহজ" to 1, "😐 মাঝারি" to 2, "😈 কঠিন" to 3)
        for ((text, level) in opts) {
            val lp = LinearLayout.LayoutParams(mp, wc)
            lp.topMargin = dp(14)
            col.addView(button(text, 0xFF3D5AFE.toInt()) { show(g.make(MODE_BOT, level)) { info(g) } }, lp)
        }
        show(col) { info(g) }
    }
}
