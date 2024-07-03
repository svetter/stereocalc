package com.gmail.simetist.stereomikingcalculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.tan


class StereoConfigView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
	private val linesPaint:	Paint = Paint()
	private val areaPaint:	Paint = Paint()
	
	private val cardioidMicVector:	Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_cardioid)
	private val omniMicVector:		Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_omni)
	
	private var useOmni = false
	private var micVector: Drawable? = cardioidMicVector
	
	private var recAngle	= 0.0f
	private var micDistance	= 0.0f
	private var micAngle	= 0.0f
	
	init {
		init()
	}
	
	private fun init() {
		linesPaint.color = Color.GRAY
		linesPaint.strokeWidth = 5f
		linesPaint.style = Paint.Style.STROKE
		linesPaint.isAntiAlias = true
		
		areaPaint.color = Color.RED
		areaPaint.alpha = 40
		areaPaint.style = Paint.Style.FILL
		areaPaint.isAntiAlias = true
	}
	
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		val picturedWidth	= 50 + 2 * 1.5f	// 50cm max capsule distance + sqrt(2) for each mic when at max distance and 45°
		val pixelPerCm		= width / picturedWidth
		val micWidth		= 2 * pixelPerCm
		val micLength		= 8 * pixelPerCm
		val halfMicDistance	= micDistance * pixelPerCm / 2.0f
		
		val centerX		= width / 2f
		val centerY		= height - 8.5f * pixelPerCm
		val left		= 0f
		val right		= width.toFloat()
		val top			= 0f
		
		val leftMicCenterX	= centerX - halfMicDistance
		val leftMicLeftX	= (leftMicCenterX - micWidth / 2).toInt()
		val leftMicRightX	= (leftMicCenterX + micWidth / 2).toInt()
		val rightMicCenterX	= centerX + halfMicDistance
		val rightMicLeftX	= (rightMicCenterX - micWidth / 2).toInt()
		val rightMicRightX	= (rightMicCenterX + micWidth / 2).toInt()
		
		val micTopY		= (centerY - 1 * pixelPerCm).toInt()
		val micBottomY	= (micTopY + micLength).toInt()
		
		val angleWithXAxisDeg = 90 - recAngle / 2
		val angleWithXAxisRad = angleWithXAxisDeg * Math.PI.toFloat() / 180
		val recAngleLineY = centerY - tan(angleWithXAxisRad) * centerX
		val halfAngleWithXAxisDeg = 90 - recAngle / 4
		val halfAngleWithXAxisRad = halfAngleWithXAxisDeg * Math.PI.toFloat() / 180
		val halfRecAngleLineY = centerY - tan(halfAngleWithXAxisRad) * centerX
		
		val circleRadius = 25 * pixelPerCm
		
		canvas.drawPath(Path().apply {
			moveTo(centerX, centerY)
			lineTo(left, recAngleLineY)
			lineTo(left, top)
			lineTo(right, top)
			lineTo(right, recAngleLineY)
			close()
		}, areaPaint)
		
		canvas.drawLine(left, centerY, right, centerY, linesPaint)
		canvas.drawLine(centerX, centerY, centerX, 0f, linesPaint)
		val tickLength = 1 * pixelPerCm
		val shortTickBottomY = centerY + tickLength
		val longTickBottomY = centerY + 2 * tickLength
		for (i in -5..5) {
			val x = centerX + i * 5 * pixelPerCm
			canvas.drawLine(x, centerY, x, if (i % 2 == 0) longTickBottomY else shortTickBottomY, linesPaint)
		}
		
		canvas.drawLine(centerX, centerY, left, recAngleLineY, linesPaint)
		canvas.drawLine(centerX, centerY, right, recAngleLineY, linesPaint)
		canvas.drawLine(centerX, centerY, left, halfRecAngleLineY, linesPaint)
		canvas.drawLine(centerX, centerY, right, halfRecAngleLineY, linesPaint)
		
		canvas.drawArc(
			centerX - circleRadius, centerY - circleRadius,
			centerX + circleRadius, centerY + circleRadius,
			180f, 180f, false, linesPaint
		)
		
		micVector?.let {
			canvas.save()
			canvas.rotate(micAngle / 2, rightMicCenterX, centerY)
			it.setBounds(rightMicLeftX, micTopY, rightMicRightX, micBottomY)
			it.draw(canvas)
			canvas.restore()
			
			canvas.save()
			canvas.rotate(-micAngle / 2, leftMicCenterX, centerY)
			it.setBounds(leftMicLeftX, micTopY, leftMicRightX, micBottomY)
			it.draw(canvas)
			canvas.restore()
		}
	}
	
	fun setUseOmni(useOmni: Boolean) {
		this.useOmni = useOmni
		if (useOmni) micAngle = 0f
		micVector = if (useOmni) {
			omniMicVector
		} else {
			cardioidMicVector
		}
		invalidate()
	}
	
	fun updateRecAngle(recAngle: Double) {
		this.recAngle = recAngle.toFloat()
		invalidate()
	}
	
	fun updateMicDistance(micDistance: Double) {
		this.micDistance = micDistance.toFloat()
		invalidate()
	}
	
	fun updateMicAngle(micAngle: Double) {
		this.micAngle = micAngle.toFloat()
		invalidate()
	}
}
