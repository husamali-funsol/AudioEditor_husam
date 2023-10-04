package com.example.audioeditor.lib.recorderwaveform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast

class MovingWaveform(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()

    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 6f
    private var width = 9f
    private var gap = 6f

    private var screenWidth = 0f
    private var screenHeight = 400f

    private var maxSpikes = 0

    init{
        paint.color = Color.rgb(25, 172, 255)

        screenWidth = resources.displayMetrics.widthPixels.toFloat()

        maxSpikes = (screenWidth / (width+gap)).toInt()
    }

    fun addAmplitude(amp:Float){
        //normalizing spike value
//        var norm =  Math.min(amp.toInt()/3 , screenHeight.toInt()).toFloat()

        var norm = amp.toInt()/100

        if(amp<5000f) {
            amplitudes.add(amp/100)
        }else{
            amplitudes.add(norm.toFloat())

        }

//        amplitudes.add(norm)

        spikes.clear()

        var amps = amplitudes.takeLast(maxSpikes)

        for (i in amps.indices){
            var left = 0f + i*(width+gap)
            var top = screenHeight/2 - amps[i]/2
            var right = left + width
            var bottom = top + amps[i]

            spikes.add(RectF(left, top, right, bottom))

        }
        invalidate()
    }

    fun clear() : ArrayList<Float> {
        val amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()

        return amps
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)

//        canvas?.drawRoundRect(RectF(20f, 30f, 50f, 90f), 6f, 6f, paint)

        spikes.forEach {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }

}