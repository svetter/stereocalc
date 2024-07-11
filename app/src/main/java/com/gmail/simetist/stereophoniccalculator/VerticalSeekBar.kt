package com.gmail.simetist.stereophoniccalculator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import kotlin.math.roundToInt



@SuppressLint("AppCompatCustomView")
class VerticalSeekBar : SeekBar {
	constructor(context: Context) : super(context)
	
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
	
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
	override fun draw(canvas: Canvas) {
		canvas.rotate(-90f)
		canvas.translate(-height.toFloat(), 0f)
		super.draw(canvas)
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Synchronized
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (!isEnabled) {
			return false
		}
		
		super.onTouchEvent(event)
		
		when (event.action) {
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
				val yDelta = event.y - paddingEnd
				val effectiveHeight = height - paddingStart - paddingEnd
				progress = (max - (max * yDelta) / effectiveHeight).roundToInt()
				onSizeChanged(width, height, 0, 0)
			}
		}
		
		if (event.action == MotionEvent.ACTION_MOVE) {
			thumb.state = intArrayOf(
				android.R.attr.state_window_focused,
				android.R.attr.state_enabled,
				android.R.attr.state_pressed,
				android.R.attr.state_accelerated
			)
		} else {
			thumb.state = intArrayOf(
				android.R.attr.state_window_focused,
				android.R.attr.state_enabled,
				android.R.attr.state_accelerated
			)
		}
		
		if (event.action == MotionEvent.ACTION_UP) {
			performClick()
		}
		
		return true
	}
}