package com.gmail.simetist.stereophoniccalculator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlin.math.tan



class StereoConfigView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
	// Paints
	// Microphone view
	private val linesPaint:				Paint = Paint()
	private val recAreaPaint:			Paint = Paint()
	// Graph view
	private val graphBorderPaint:		Paint = Paint()
	private val graphGridPaint:			Paint = Paint()
	private val graphMajorTickPaint:	Paint = Paint()
	private val graphMinorTickPaint:	Paint = Paint()
	private val graphReverbLimitsPaint:	Paint = Paint()
	private val graphHeatMapPaint:		Paint = Paint()
	private val graphTickTextPaint:		Paint = Paint()
	private val graphCurvesPaint:		Paint = Paint()
	private val graphMainCurvePaint:	Paint = Paint()
	private val graphCrossPaint:		Paint = Paint()
	private val graphReticulePaint:		Paint = Paint()
	
	private var graphViewCacheBitmap:	Bitmap? = null
	private var graphViewCacheCanvas:	Canvas? = null
	
	private val cardioidMicVector:	Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_cardioid)
	private val omniMicVector:		Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_omni)
	private var micVector:			Drawable? = cardioidMicVector
	
	private val cmPerInch			= 2.54f
	
	private val minRecAngle			= 40
	private val maxRecAngle			= 180
	private val minMicDistanceCm	= 0
	private val maxMicDistanceCm	= 50
	private val minMicAngle			= 0
	private val maxMicAngle			= 180
	
	// Microphone view constants
	private val micHeightCm			= 8f
	private val micWidthCm			= 2f
	private val halfCircleRadiusCm	= maxMicDistanceCm / 2f
	
	// Graph view constants
	val hmStep			= 4		// Heat map resolution in device pixels
	var graphLeftX		= -1f
	var graphBottomY	= -1f
	var graphRightX		= -1f
	var graphTopY		= -1f
	var graphScaleX		= -1f
	var graphScaleY		= -1f
	
	
	private val mainActivity: MainActivity = context as MainActivity
	
	private var graphMode		= false
	
	private var useImperial		= false
	private var useHalfAngles	= false
	private var useOmni			= false
	
	
	private var recAngle	= 0.0f
	private var micDistance	= 0.0f
	private var micAngle	= 0.0f
	private var angularDist	= 0.0f
	
	
	init {
		init()
	}
	
	private fun init() {
		// Microphone view
		
		linesPaint				.color			= Color.GRAY
		linesPaint				.strokeWidth	= 5f
		linesPaint				.strokeCap		= Paint.Cap.ROUND
		linesPaint				.style			= Paint.Style.STROKE
		linesPaint				.isAntiAlias	= true
		
		recAreaPaint			.color			= Color.RED
		recAreaPaint			.alpha			= 40
		recAreaPaint			.style			= Paint.Style.FILL
		recAreaPaint			.isAntiAlias	= true
		
		// Graph view
		
		graphBorderPaint		.color			= Color.WHITE
		graphBorderPaint		.strokeWidth	= 5f
		graphBorderPaint		.strokeCap		= Paint.Cap.SQUARE
		graphBorderPaint		.style			= Paint.Style.STROKE
		
		graphGridPaint			.color			= Color.WHITE
		graphGridPaint			.alpha			= 150
		graphGridPaint			.strokeWidth	= 1f
		graphGridPaint			.style			= Paint.Style.STROKE
		
		graphMajorTickPaint		.color			= graphBorderPaint.color
		graphMajorTickPaint		.strokeWidth	= graphBorderPaint.strokeWidth
		graphMajorTickPaint		.style			= Paint.Style.STROKE
		
		graphMinorTickPaint		.color			= graphMajorTickPaint.color
		graphMinorTickPaint		.strokeWidth	= 2f
		graphMinorTickPaint		.style			= Paint.Style.STROKE
		
		graphTickTextPaint		.color			= graphMajorTickPaint.color
		graphTickTextPaint		.textSize		= 30f
		graphTickTextPaint		.isAntiAlias	= true
		
		graphReverbLimitsPaint	.color			= Color.BLACK
		graphReverbLimitsPaint	.alpha			= 80
		graphReverbLimitsPaint	.style			= Paint.Style.FILL
		graphReverbLimitsPaint	.isAntiAlias	= true
		
		graphHeatMapPaint		.style			= Paint.Style.FILL
		
		graphCurvesPaint		.color			= Color.BLACK
		graphCurvesPaint		.alpha			= 200
		graphCurvesPaint		.strokeWidth	= 2f
		graphCurvesPaint		.style			= Paint.Style.STROKE
		graphCurvesPaint		.isAntiAlias	= true
		
		graphMainCurvePaint		.color			= Color.BLACK
		graphMainCurvePaint		.strokeWidth	= 3f
		graphMainCurvePaint		.style			= Paint.Style.STROKE
		graphMainCurvePaint		.isAntiAlias	= true
		
		graphCrossPaint			.color			= Color.WHITE
		graphCrossPaint			.alpha			= 255
		graphCrossPaint			.strokeWidth	= 2f
		graphCrossPaint			.style			= Paint.Style.STROKE
		
		graphReticulePaint		.color			= Color.RED
		graphReticulePaint		.style			= Paint.Style.FILL
		graphReticulePaint		.isAntiAlias	= true
	}
	
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		if (graphMode) {
			drawGraphView(canvas)
		} else {
			drawMicView(canvas)
		}
	}
	
	private fun drawMicView(canvas: Canvas) {
		val picturedWidth	= maxMicDistanceCm + micWidthCm * 1.5f		// Max mic distance (center to center) + sqrt(2) for each mic when at max distance and 45°
		val picturedHeight	= halfCircleRadiusCm + micHeightCm - 1 + 1	// Circle radius + mic length - 1cm for the top of the mic + 1cm buffer
		val pixelPerCm		= width / picturedWidth
		
		val micWidth		= micWidthCm * pixelPerCm
		val micLength		= micHeightCm * pixelPerCm
		val halfMicDistance	= micDistance * pixelPerCm / 2f
		
		val centerX		= width / 2f
		val centerY		= height - micLength
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
		val halfAngleWithXAxisDeg = 90 - recAngle * (0.25f + angularDist / 60f)
		val halfAngleWithXAxisRad = halfAngleWithXAxisDeg * Math.PI.toFloat() / 180
		val halfRecAngleLineY = centerY - tan(halfAngleWithXAxisRad) * centerX
		
		val circleRadius = halfCircleRadiusCm * pixelPerCm
		
		canvas.drawPath(Path().apply {
			moveTo(centerX, centerY)
			lineTo(left, recAngleLineY)
			lineTo(left, top)
			lineTo(right, top)
			lineTo(right, recAngleLineY)
			close()
		}, recAreaPaint)
		
		canvas.drawLine(left, centerY, right, centerY, linesPaint)
		canvas.drawLine(centerX, centerY, centerX, 0f, linesPaint)
		val tickLength = 1 * pixelPerCm
		val shortTickBottomY = centerY + tickLength
		val longTickBottomY = centerY + 2 * tickLength
		for (i in -5..5) {
			val x = centerX + i * 5 * pixelPerCm
			canvas.drawLine(x, centerY, x, if (i % 2 == 0) longTickBottomY else shortTickBottomY, linesPaint)
		}
		
		// Angle lines
		canvas.drawLine(centerX, centerY, left, recAngleLineY, linesPaint)
		canvas.drawLine(centerX, centerY, right, recAngleLineY, linesPaint)
		canvas.drawLine(centerX, centerY, left, halfRecAngleLineY, linesPaint)
		canvas.drawLine(centerX, centerY, right, halfRecAngleLineY, linesPaint)
		
		if (height >= picturedHeight * pixelPerCm) {
			// Half circle
			canvas.drawArc(
				centerX - circleRadius, centerY - circleRadius,
				centerX + circleRadius, centerY + circleRadius,
				180f, 180f, false, linesPaint
			)
		}
		
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
	
	private fun drawGraphView(canvas: Canvas) {
		graphLeftX		= 90f
		graphBottomY	= height - 55f
		graphRightX		= width - 40f
		graphTopY		= 15f
		graphScaleX		= (graphRightX - graphLeftX) / (maxMicDistanceCm - minMicDistanceCm)
		graphScaleY		= (graphBottomY - graphTopY) / (maxMicAngle - minMicAngle)
		
		val xCurveStep = 20.0
		val micDistStep = xCurveStep / graphScaleX
		
		if (graphViewCacheBitmap != null) {
			canvas.drawBitmap(graphViewCacheBitmap!!, 0f, 0f, null)
		}
		else {
			// Prepare cache
			graphViewCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
			graphViewCacheCanvas = Canvas(graphViewCacheBitmap!!)
			
			// Draw the angular distortion heat map
			run {
				val hmMicDistStep = hmStep / graphScaleX
				val hmMicAngleStep = hmStep / graphScaleY
				var micDistance = 0.0
				while (micDistance < maxMicDistanceCm) {
					var micAngle = 0.0
					while (micAngle < maxMicAngle) {
						val angularDist = calculateAngularDistortion(micDistance, micAngle)
						val maxBrightness = 200
						val color = when {
							angularDist < 5	-> Color.rgb(((-0.5f + angularDist * 0.3f) * maxBrightness).roundToInt().coerceAtLeast(0), maxBrightness, 0)
							else			-> Color.rgb(maxBrightness, ((2.5f - angularDist * 0.3f) * maxBrightness).roundToInt().coerceAtLeast(0), 0)
						}
						
						val leftX	= (graphLeftX + micDistance * graphScaleX).toFloat()
						val rightX	= (leftX + hmStep).coerceAtMost(graphRightX)
						val bottomY	= (graphBottomY - micAngle * graphScaleY).toFloat()
						val topY	= (bottomY - hmStep).coerceAtLeast(graphTopY)
						graphHeatMapPaint.color = color
						
						graphViewCacheCanvas!!.drawRect(leftX, topY, rightX, bottomY, graphHeatMapPaint)
						
						micAngle += hmMicAngleStep
					}
					micDistance += hmMicDistStep
				}
			}
			
			// Draw the reverberation limit areas
			run {
				// Upper area (center reverb limit)
				val upperLimit = centerReverbLimitMicAngle
				val upperLimitY = (graphBottomY - upperLimit * graphScaleY).toFloat()
				graphViewCacheCanvas!!.drawRect(graphLeftX, graphTopY, graphRightX, upperLimitY, graphReverbLimitsPaint)
				
				// Lower area (sides reverb limit)
				var prevX = graphLeftX
				var prevY = graphBottomY
				val path = Path().apply {
					moveTo(graphLeftX, graphBottomY)
				}
				
				var micDistance = 0.0
				while (micDistance <= maxMicDistanceCm) {
					var skipRest = false
					val micAngle = calculateSidesReverbLimit(micDistance)
					
					var x = (graphLeftX + micDistance * graphScaleX).toFloat()
					var y = (graphBottomY - micAngle * graphScaleY).toFloat()
					
					if (y > graphBottomY) {
						skipRest = true
						val slope = (y - prevY) / (x - prevX)
						y = graphBottomY
						x = prevX + (y - prevY) / slope
					}
					
					path.apply { lineTo(x, y) }
					
					if (skipRest) break
					prevX = x
					prevY = y
					micDistance += micDistStep
				}
				
				path.apply { close() }
				graphViewCacheCanvas!!.drawPath(path, graphReverbLimitsPaint)
			}
			
			// Draw grid, ticks and labels
			run {
				val mainTickLength = 20f
				
				// X axis
				graphTickTextPaint.textAlign = Paint.Align.CENTER
				val (micDistanceTickXs, micDistanceMajorStep) = if (useImperial) {
					IntRange(
						(minMicDistanceCm / cmPerInch).toInt(),
						(maxMicDistanceCm / cmPerInch).toInt()
					) to 5
				} else {
					IntRange(minMicDistanceCm, maxMicDistanceCm).step(5) to 10
				}
				val micDistanceTickStepX = graphScaleX * (if (useImperial) cmPerInch else 1f)
				for (micDistance in micDistanceTickXs) {
					val x = graphLeftX + micDistance * micDistanceTickStepX
					val majorTick = micDistance % micDistanceMajorStep == 0
					val tickLength = if (majorTick) mainTickLength else mainTickLength / 2
					val tickPaint = if (majorTick) graphMajorTickPaint else graphMinorTickPaint
					graphViewCacheCanvas!!.drawLine(x, graphBottomY, x, graphBottomY + tickLength, tickPaint)
					if (micDistance % micDistanceMajorStep == 0) {
						val labelText = if (useImperial) {
							"${micDistance}in"
						} else {
							"${micDistance}cm"
						}
						graphViewCacheCanvas!!.drawText(labelText, x, graphBottomY + 55, graphTickTextPaint)
						
						graphViewCacheCanvas!!.drawLine(x, graphBottomY, x, graphTopY, graphGridPaint)
					}
				}
				
				// Y axis
				graphTickTextPaint.textAlign = Paint.Align.RIGHT
				for (micAngle in (0..180 step 10)) {
					val y = (graphBottomY - micAngle * graphScaleY)
					val majorTick = micAngle % 30 == 0
					val tickLength = if (majorTick) mainTickLength else mainTickLength / 2
					val tickPaint = if (majorTick) graphMajorTickPaint else graphMinorTickPaint
					graphViewCacheCanvas!!.drawLine(graphLeftX, y, graphLeftX - tickLength, y, tickPaint)
					if (micAngle % 30 == 0) {
						val labelText = if (useHalfAngles && micAngle != 0) {
							"±${micAngle / 2}°"
						} else {
							"$micAngle°"
						}
						graphViewCacheCanvas!!.drawText(labelText, graphLeftX - mainTickLength - 10, y + 10, graphTickTextPaint)
						
						graphViewCacheCanvas!!.drawLine(graphLeftX, y, graphRightX, y, graphGridPaint)
					}
				}
			}
			
			// Draw static recording angle curves
			run {
				for (i in IntRange(minRecAngle, maxRecAngle).step(20)) {
					var prevX = graphLeftX
					var prevY = graphBottomY
					
					val micDistanceAtMaxAngle =
						calculateCardioidMicDistance(i.toDouble(), maxMicAngle.toDouble())
					val firstMicDistance = if (micDistanceAtMaxAngle.isNaN()) {
						0.0
					} else {
						micDistanceAtMaxAngle.coerceAtLeast(0.0)
					}
					var micDistance = firstMicDistance
					while (micDistance <= maxMicDistanceCm) {
						var skipRest = false
						var micAngle = calculateCardioidMicAngle(i.toDouble(), micDistance)
						
						if (micAngle < minMicAngle || micAngle > maxMicAngle) {
							if (micAngle < minMicAngle) skipRest = true
							micAngle = micAngle.coerceIn(minMicAngle.toDouble(), maxMicAngle.toDouble())
							micDistance = calculateCardioidMicDistance(i.toDouble(), micAngle)
						}
						
						val x = (graphLeftX + micDistance * graphScaleX).toFloat()
						val y = (graphBottomY - micAngle * graphScaleY).toFloat()
						if (micDistance > firstMicDistance) {
							graphViewCacheCanvas!!.drawLine(prevX, prevY, x, y, graphCurvesPaint)
						}
						
						if (skipRest) break
						prevX = x
						prevY = y
						if (micDistance == maxMicDistanceCm.toDouble()) break
						micDistance = (micDistance + micDistStep).coerceAtMost(maxMicDistanceCm.toDouble())
					}
				}
			}
			
			// Copy cache to canvas
			canvas.drawBitmap(graphViewCacheBitmap!!, 0f, 0f, null)
		}
		
		// Draw current recording angle curve
		run {
			var prevX = graphLeftX
			var prevY = graphBottomY
			
			val micDistanceAtMaxAngle =
				calculateCardioidMicDistance(recAngle.toDouble(), maxMicAngle.toDouble())
			val firstMicDistance = if (micDistanceAtMaxAngle.isNaN()) {
				0.0
			} else {
				micDistanceAtMaxAngle.coerceAtLeast(0.0)
			}
			var micDistance = firstMicDistance
			while (micDistance <= maxMicDistanceCm) {
				var skipRest = false
				var micAngle = calculateCardioidMicAngle(recAngle.toDouble(), micDistance)
				
				if (micAngle < minMicAngle || micAngle > maxMicAngle) {
					if (micAngle < minMicAngle) skipRest = true
					micAngle = micAngle.coerceIn(minMicAngle.toDouble(), maxMicAngle.toDouble())
					micDistance = calculateCardioidMicDistance(recAngle.toDouble(), micAngle)
				}
				
				val x = (graphLeftX + micDistance * graphScaleX).toFloat()
				val y = (graphBottomY - micAngle * graphScaleY).toFloat()
				if (micDistance > firstMicDistance) {
					canvas.drawLine(prevX, prevY, x, y, graphMainCurvePaint)
				}
				
				if (skipRest) break
				prevX = x
				prevY = y
				if (micDistance == maxMicDistanceCm.toDouble()) break
				micDistance = (micDistance + micDistStep).coerceAtMost(maxMicDistanceCm.toDouble())
			}
		}
		
		// Draw the current state cross
		val currentConfigX = (graphLeftX + micDistance * graphScaleX)
		val currentConfigY = (graphBottomY - micAngle * graphScaleY)
		canvas.drawLine(currentConfigX, graphBottomY, currentConfigX, graphTopY, graphCrossPaint)
		canvas.drawLine(graphLeftX, currentConfigY, graphRightX, currentConfigY, graphCrossPaint)
		
		// Draw graph outlines
		canvas.drawPath(Path().apply {
			moveTo(graphLeftX, graphBottomY)
			lineTo(graphRightX, graphBottomY)
			lineTo(graphRightX, graphTopY)
			lineTo(graphLeftX, graphTopY)
			close()
		}, graphBorderPaint)
		
		// Draw current state reticule
		canvas.drawCircle(currentConfigX, currentConfigY, 10f, graphReticulePaint)
	}
	
	
	
	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
		if (!graphMode) return false
		
		val inputMicDistance	= (event.x - graphLeftX) / graphScaleX
		val inputMicAngle		= (graphBottomY - event.y) / graphScaleY
		mainActivity.handle2DUserInput(inputMicDistance.toDouble(), inputMicAngle.toDouble())
		return true
	}
	
	
	
	fun setUseImperial(useImperial: Boolean) {
		this.useImperial = useImperial
		graphViewCacheBitmap = null
		invalidate()
	}
	
	fun setUseHalfAngles(useHalfAngles: Boolean) {
		this.useHalfAngles = useHalfAngles
		graphViewCacheBitmap = null
		invalidate()
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
	
	fun setShowGraphView(showGraphView: Boolean) {
		graphMode = showGraphView
		invalidate()
	}
	
	fun setRecAngle(recAngle: Double) {
		this.recAngle = recAngle.toFloat()
		invalidate()
	}
	
	fun setMicDistance(micDistance: Double) {
		this.micDistance = micDistance.toFloat()
		invalidate()
	}
	
	fun setMicAngle(micAngle: Double) {
		this.micAngle = micAngle.toFloat()
		invalidate()
	}
	
	fun setAngularDist(angularDist: Double) {
		this.angularDist = angularDist.toFloat()
		invalidate()
	}
	
	fun resetCache() {
		graphViewCacheBitmap = null
		invalidate()
	}
}
