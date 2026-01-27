package com.example.uploadingscreen

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class NumSeqActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var tvStatus: TextView
    private lateinit var btnRestart: Button
    private val cells = mutableListOf<TextView>()
    private val positionToNumber = mutableMapOf<Int, Int>()
    private var sequenceCount = 0
    private var nextExpected = 1
    private val handler = Handler(Looper.getMainLooper())
    private var roundActive = false
    private var roundFailed = false
    private var gridCreated = false
    private val minCount = 4
    private val maxCount = 6
    private val displayMillis: Long = 4500L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_num_seq)

        gridLayout = findViewById(R.id.gridLayout)
        tvStatus = findViewById(R.id.tvStatus)
        btnRestart = findViewById(R.id.btnRestart)

        generateGrid()

        btnRestart.setOnClickListener {
            if (gridCreated) startNewRound()
        }
    }

    //this function helps t convert dp->px for consistent tile margin(New)
    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()


    private fun generateGrid() {

        gridLayout.post { //post 
            if (gridCreated) return@post
            gridCreated = true

            gridLayout.removeAllViews()
            cells.clear()

            val totalWidth = gridLayout.width
            val margin = dpToPx(6)
            val size = (totalWidth - margin * 6) / 3

            for (i in 0 until 9) {  //this loop creates 9 tiles

                val tv = TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = size
                        height = size
                        setMargins(margin, margin, margin, margin)
                    }

                    gravity = Gravity.CENTER //create align numbers
                    textSize = 26f
                    setTextColor(Color.parseColor("#0B0B0B"))
                    background = ContextCompat.getDrawable(
                        this@NumSeqActivity,
                        R.drawable.tile_bg
                    )
                    text = ""
                    isClickable = true
                }

                tv.setOnClickListener {
                    if (!roundActive || roundFailed) return@setOnClickListener
                    handleTileTap(i)
                }

                cells.add(tv)
                gridLayout.addView(tv) // this adds up the tile to UI
            }


            startNewRound()
        }
    }

    private fun startNewRound() {
        handler.removeCallbacksAndMessages(null)

        positionToNumber.clear() //removes all the old mappings
        nextExpected = 1
        roundActive = false
        roundFailed = false

        tvStatus.text = "Watch carefully..."
        tvStatus.setTextColor(Color.parseColor("#222222"))

        cells.forEach { tv ->
            tv.text = ""
            tv.background = ContextCompat.getDrawable(this, R.drawable.tile_bg)
        }

        sequenceCount = (minCount..maxCount).random()
        val chosen = (0..8).shuffled().take(sequenceCount)

        chosen.forEachIndexed { idx, pos ->
            val num = idx + 1
            positionToNumber[pos] = num
            val tv = cells[pos]

            tv.text = num.toString()
            tv.background = ContextCompat.getDrawable(this, R.drawable.tile_bg_purple)
            tv.tag = num
        }

        handler.postDelayed({ hideNumbers() }, displayMillis)
    }


    private fun hideNumbers() {
        for (i in cells.indices) {

            val tv = cells[i]

            if (positionToNumber.containsKey(i)) {
                val fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
                tv.startAnimation(fade)

                handler.postDelayed({
                    tv.text = "?"
                    tv.background = ContextCompat.getDrawable(this, R.drawable.tile_bg)
                }, 180)

            } else {
                tv.text = ""
                tv.background = ContextCompat.getDrawable(this, R.drawable.tile_bg)
            }
        }

        nextExpected = 1
        roundActive = true
        tvStatus.text = "Tap in order!"
    }


    private fun handleTileTap(index: Int) {
        val tv = cells[index]
        val num = positionToNumber[index]

        if (num == nextExpected) {
            revealCorrect(tv, num)
            nextExpected++

            if (nextExpected > sequenceCount) {
                roundActive = false
                tvStatus.text = "🎉 You Win!"
                tvStatus.setTextColor(Color.parseColor("#088A29"))
                revealAllNumbers(true)
            }

        } else {
            roundFailed = true
            roundActive = false

            revealWrong(tv)
            tvStatus.text = "Bad luck! Try again"
            tvStatus.setTextColor(Color.parseColor("#D93025"))

            revealAllNumbers(false)
        }
    }


    private fun revealCorrect(tv: TextView, num: Int?) {
        tv.text = num.toString()
        tv.setBackgroundColor(Color.parseColor("#A5F59B"))

        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.08f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.08f, 1f)

        ObjectAnimator.ofPropertyValuesHolder(tv, scaleX, scaleY).apply {
            duration = 200
            start()
        }
    }

    private fun revealWrong(tv: TextView) {
        tv.text = tv.tag.toString()
        tv.setBackgroundColor(Color.parseColor("#FF9A9A"))
        tv.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
    }


    private fun revealAllNumbers(win: Boolean) {
        for ((pos, num) in positionToNumber) {
            val tv = cells[pos]
            tv.text = num.toString()

            tv.background = if (win)
                ContextCompat.getDrawable(this, R.drawable.tile_bg_purple)
            else
                ContextCompat.getDrawable(this, R.drawable.tile_bg)
        }
    }
}
