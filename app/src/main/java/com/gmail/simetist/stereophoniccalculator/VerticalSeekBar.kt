package com.gmail.simetist.stereophoniccalculator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import kotlin.math.roundToInt



@SuppressLint("AppCompatCustomView")
class VerticalSeekBar : SeekBar {
	constructor(context: Context) : super(context)
	
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context,
		attrs,
		defStyleAttr
	)
	
	@Synchronized
	override fun setMax(max: Int) {
		super.setMax(max)
	}
	
	@Synchronized
	override fun setMin(min: Int) {
		super.setMin(min)
	}
	
	@Synchronized
	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(h, w, oldh, oldw)
	}
	
	@Synchronized
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec)
		setMeasuredDimension(measuredHeight, measuredWidth)
	}
	
	@Synchronized
	override fun onDraw(canvas: Canvas) {
		canvas.rotate(-90f)
		canvas.translate(-height.toFloat(), 0f)
		super.onDraw(canvas)
	}
	
	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (!isEnabled) {
			return true
		}
		when (event.action) {
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
				progress = (max - (max * (event.y - paddingEnd)) / (height - paddingStart - paddingEnd)).roundToInt()
				onSizeChanged(width, height, 0, 0)
			}
			MotionEvent.ACTION_CANCEL -> {}
		}
		return true
	}
	
	override fun setThumb(thumb: Drawable) {
		super.setThumb(thumb)
	}
	
	@Synchronized
	override fun setProgress(progress: Int) {
		super.setProgress(progress)
	}
	
	@Synchronized
	override fun getProgress(): Int {
		return super.getProgress()
	}
	
	override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener) {
		super.setOnSeekBarChangeListener(l)
	}
}