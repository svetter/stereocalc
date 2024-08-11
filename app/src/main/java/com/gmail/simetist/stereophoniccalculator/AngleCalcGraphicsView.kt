/*
 * Copyright 2024 Simon Vetter
 * 
 * This file is part of Stereophonic Calculator.
 * 
 * Stereophonic Calculator is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * Stereophonic Calculator is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Stereophonic
 * Calculator. If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.simetist.stereophoniccalculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.atan
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt


class AngleCalcGraphicsView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
	private val linesPaint:		Paint = Paint()
	private val subjectPaint:	Paint = Paint()
	private val recAnglePaint:	Paint = Paint()
	private val recAreaPaint:	Paint = Paint()
	private val textPaint:		Paint = Paint()
	
	private val micVector:	Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_cardioid)
	
	
	private var micHeight		= -1.0
	private var subjectHeight	= -1.0
	private var subjectWidth	= -1.0
	private var horDistance		= -1.0
	
	
	
	init {
		init()
	}
	
	private fun init() {
		linesPaint		.color			= Color.GRAY
		linesPaint		.strokeWidth	= 5f
		linesPaint		.style			= Paint.Style.STROKE
		linesPaint		.isAntiAlias	= true
		
		subjectPaint	.color			= Color.GRAY
		subjectPaint	.style			= Paint.Style.FILL
		
		recAnglePaint	.color			= Color.RED
		recAnglePaint	.alpha			= 150
		recAnglePaint	.strokeWidth	= 5f
		recAnglePaint	.style			= Paint.Style.STROKE
		recAnglePaint	.strokeCap		= Paint.Cap.ROUND
		recAnglePaint	.isAntiAlias	= true
		
		recAreaPaint	.color			= recAnglePaint.color
		recAreaPaint	.alpha			= 50
		recAreaPaint	.style			= Paint.Style.FILL
		recAnglePaint	.isAntiAlias	= true
		
		textPaint		.textSize		= 40f
		textPaint		.isAntiAlias	= true
	}
	
	
	
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		val vGap = height * 0.02f
		val middleX	= width / 2f
		
		val micLength	= sqrt(width * height.toFloat()) * 0.08f
		val micWidth	= micLength / 4f
		val subjectMarkLineLength = 40f
		
		// Top view
		run {
			val topY = 0f
			val bottomY = height / 2f - vGap / 2
			
			val micLeftX	= middleX - micWidth / 2
			val micRightX	= middleX + micWidth / 2
			val micTopY		= bottomY - micLength
			val micCenterY	= micTopY + micWidth / 2
			
			val scale = min(width * 0.95f / 2000f, (micCenterY - topY) * 0.9f / 1000f)	// Pixels per cm
			
			val subjectBottomY	= micCenterY - horDistance.toFloat() * scale
			val subjectLeftX	= middleX - subjectWidth.toFloat() * scale / 2
			val subjectRightX	= middleX + subjectWidth.toFloat() * scale / 2
			
			// Draw center line
			canvas.drawLine(middleX, micTopY, middleX, subjectBottomY, linesPaint)
			
			// Draw recording area
			val recAngleTopY = micCenterY - (micCenterY - subjectBottomY) / (middleX - subjectLeftX) * middleX
			if (subjectWidth > 0.0) {
				canvas.drawPath(Path().apply {
					moveTo(middleX, micCenterY)
					lineTo(0f, recAngleTopY)
					lineTo(0f, topY)
					lineTo(width.toFloat(), topY)
					lineTo(width.toFloat(), recAngleTopY)
					close()
				}, recAreaPaint)
			}
			
			// Draw subject
			canvas.drawRect(subjectLeftX, topY, subjectRightX, subjectBottomY, subjectPaint)
			if (subjectWidth * scale < subjectMarkLineLength) {
				val leftX	= middleX - subjectMarkLineLength / 2
				val rightX	= middleX + subjectMarkLineLength / 2
				val y		= subjectBottomY - linesPaint.strokeWidth / 2
				canvas.drawLine(leftX, y, rightX, y, linesPaint)
			}
			
			// Draw recording angle lines
			if (subjectWidth == 0.0) {
				canvas.drawLine(middleX, micCenterY, middleX, topY, recAnglePaint)
			} else {
				canvas.drawLine(middleX, micCenterY, 0f, recAngleTopY, recAnglePaint)
				canvas.drawLine(middleX, micCenterY, width.toFloat(), recAngleTopY, recAnglePaint)
			}
			
			// Draw mic
			micVector?.let {
				it.setBounds(micLeftX.roundToInt(), micTopY.roundToInt(), micRightX.roundToInt(), bottomY.roundToInt())
				it.draw(canvas)
			}
		}
		
		// Side view
		run {
			val topY		= height / 2 + vGap / 2
			val bottomY		= height - micWidth / 2
			val maxSubjectX	= width - 20f
			
			val scale = min((maxSubjectX - micLength) / 1000f, (bottomY - topY) / 500f)	// Pixels per cm
			
			val micCenterY		= bottomY - micHeight.toFloat() * scale
			val subjectLeftX	= micLength + horDistance.toFloat() * scale
			val subjectTopY		= bottomY - subjectHeight.toFloat() * scale
			
			// Draw ground
			canvas.drawLine(0f, bottomY, width.toFloat(), bottomY, linesPaint)
			
			// Draw mic height
			canvas.drawLine(micLength, bottomY, micLength, micCenterY, linesPaint)
			
			// Draw recording/inclination angle line
			canvas.drawLine(micLength, micCenterY, subjectLeftX, subjectTopY, recAnglePaint)
			
			// Draw subject
			if (subjectHeight * scale < subjectMarkLineLength) {
				val lineX = subjectLeftX + linesPaint.strokeWidth / 2
				canvas.drawLine(lineX, bottomY, lineX, bottomY - subjectMarkLineLength, linesPaint)
			}
			canvas.drawRect(subjectLeftX, subjectTopY, width.toFloat(), bottomY, subjectPaint)
			
			// Draw mic
			val micAngle = 90 + if (horDistance != 0.0 || micHeight != subjectHeight) {
				Math.toDegrees(atan((subjectTopY - micCenterY) / (subjectLeftX - micLength).toDouble())).toFloat()
			} else 0f
			micVector?.let {
				canvas.save()
				canvas.rotate(micAngle, micLength, micCenterY)
				it.setBounds(
					(micLength - micWidth / 2).roundToInt(),
					(micCenterY - micLength / 8).roundToInt(),
					(micLength + micWidth / 2).roundToInt(),
					(micCenterY + micLength * 7 / 8).roundToInt()
				)
				it.draw(canvas)
				canvas.restore()
			}
		}
	}
	
	
	
	fun setParametersInCm(micHeight: Double, subjectHeight: Double, subjectWidth: Double, horDistance: Double) {
		this.micHeight		= micHeight
		this.subjectHeight	= subjectHeight
		this.subjectWidth	= subjectWidth
		this.horDistance	= horDistance
		invalidate()
	}
}