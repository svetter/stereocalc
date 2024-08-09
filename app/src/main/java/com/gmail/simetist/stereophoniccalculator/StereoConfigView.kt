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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
	
	// Graph view drawing cache
	private var graphViewCacheBitmap:	Bitmap? = null
	private var graphViewCacheCanvas:	Canvas? = null
	
	// Drawables
	private val cardioidMicVector:			Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_cardioid)
	private val cardioidMicVectorMirrored:	Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_cardioid)
	private val omniMicVector:				Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_omni)
	private val omniMicVectorMirrored:		Drawable? = ContextCompat.getDrawable(context!!, R.drawable.microphone_omni)
	private var micVector:					Drawable? = cardioidMicVector
	private var micVectorMirrored:			Drawable? = cardioidMicVectorMirrored
	private val micShadowImage:				Drawable? = ContextCompat.getDrawable(context!!, R.drawable.mic_shadow)
	private val cableVector:				Drawable? = ContextCompat.getDrawable(context!!, R.drawable.cable)
	private val cableShadowImage:			Drawable? = ContextCompat.getDrawable(context!!, R.drawable.cable_shadow)
	private val stereoBarVector:			Drawable? = ContextCompat.getDrawable(context!!, R.drawable.stereo_bar)
	private val micClampVector:				Drawable? = ContextCompat.getDrawable(context!!, R.drawable.mic_clamp)
	private val micClampWheelVector:		Drawable? = ContextCompat.getDrawable(context!!, R.drawable.mic_clamp_wheel)
	
	
	private val cmPerInch			= 2.54f
	
	private val minRecAngle			= 40
	private val maxRecAngle			= 180
	private val minMicDistanceCm	= 0
	private val maxMicDistanceCm	= 50
	private val minMicAngle			= 0
	private val maxMicAngle			= 180
	
	
	// Microphone view constants
	// Input graphics parameters
	private val micWidthCm				= 2f
	private val micHeightCm				= 8f
	private val micCenterYOffsetCm		= 1f
	private val cableWidthCm			= 11f
	private val cableHeightCm			= 16.5f
	private val cableCenterXCm			= 1f
	private val stereoBarWidthCm		= 53.4f
	private val stereoBarHeightCm		= 3.4f
	private val minClampPitchCm			= 3.25f
	private val micClampWidthCm			= 3f
	private val micClampHeightCm		= 3f
	private val micClampWheelOverhangCm	= 0.4f
	private val shadowOverhangCm		= 1f
	// Layout
	private val shadowXOffsetCm		= -0.5f
	private val shadowYOffsetCm		= 0.3f
	private val maxShadowAlpha		= 200
	
	private val halfCircleRadiusCm	= maxMicDistanceCm / 2f
	
	// Calculations
	private var pixelPerCm			= -1f
	private var micWidth			= -1f
	private var micHeight			= -1f
	private var micCenterYOffset	= -1f
	private var cableWidth			= -1f
	private var cableHeight			= -1f
	private var stereoBarWidth		= -1f
	private var stereoBarHeight		= -1f
	private var minClampPitch		= -1f
	private var micClampWidth		= -1f
	private var micClampHeight		= -1f
	private var shadowOverhang		= -1f
	private var shadowXOffset		= -1f
	private var shadowYOffset		= -1f
	
	private var centerX				= -1f
	private var centerY				= -1f
	private var left				= -1f
	private var right				= -1f
	private var top					= -1f
	
	
	// Graph view constants
	private val hmStep			= 4		// Heat map resolution in device pixels
	private var graphLeftX		= -1f
	private var graphBottomY	= -1f
	private var graphRightX		= -1f
	private var graphTopY		= -1f
	private var graphScaleX		= -1f
	private var graphScaleY		= -1f
	
	
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
		val picturedWidth	= maxMicDistanceCm + micWidthCm + 2 * shadowOverhangCm
		val picturedHeight	= halfCircleRadiusCm + micHeightCm - 1 + 1	// Circle radius + mic length - 1cm for the top of the mic + 1cm buffer
		pixelPerCm			= width / picturedWidth
		
		micWidth			= micWidthCm			* pixelPerCm
		micHeight			= micHeightCm			* pixelPerCm
		micCenterYOffset	= micCenterYOffsetCm	* pixelPerCm
		cableWidth			= cableWidthCm			* pixelPerCm
		cableHeight			= cableHeightCm			* pixelPerCm
		stereoBarWidth		= stereoBarWidthCm		* pixelPerCm
		stereoBarHeight		= stereoBarHeightCm		* pixelPerCm
		minClampPitch		= minClampPitchCm		* pixelPerCm
		micClampWidth		= micClampWidthCm		* pixelPerCm
		micClampHeight		= micClampHeightCm		* pixelPerCm
		shadowOverhang		= shadowOverhangCm		* pixelPerCm
		shadowXOffset		= shadowXOffsetCm		* pixelPerCm
		shadowYOffset		= shadowYOffsetCm		* pixelPerCm
		val halfMicDistance	= micDistance / 2f		* pixelPerCm
		
		centerX	= width / 2f
		centerY	= height - micHeight
		left	= 0f
		right	= width.toFloat()
		top		= 0f
		
		val leftMicCenterX		= centerX - halfMicDistance
		val leftMicLeftX		= leftMicCenterX - micWidth / 2
		val rightMicCenterX		= centerX + halfMicDistance
		val rightMicLeftX		= rightMicCenterX - micWidth / 2
		val cableCenterX		= cableCenterXCm * pixelPerCm
		val leftClampLeftX		= leftMicCenterX - micClampWidth / 2
		val rightClampLeftX		= rightMicCenterX - micClampWidth / 2
		val stereoBarLeftX		= centerX - stereoBarWidth / 2
		
		// Calculate microphone and cable positions
		val micTopY		= centerY - micCenterYOffset
		val micBottomY	= micTopY + micHeight
		val cableTopY	= micBottomY - 0.25f * pixelPerCm	// Slight overlap to line up the shadows
		
		// Calculate stereo bar and clamp positions
		// Distance from mic top to clamp top
		val defaultClampOffset	= 4.3f	* pixelPerCm
		val upperClampOffset	= 1f	* pixelPerCm
		val lowerClampOffset	= (micHeightCm - micClampHeightCm + micClampWheelOverhangCm - 0.1f) * pixelPerCm
		val clampYOffset = run {
			val micDistancePx = micDistance * pixelPerCm
			val sine = sin(Math.toRadians(micAngle / 2.0)).toFloat()
			fun rawClampPitch(clampOffset: Float): Float {
				val hypotenuse = clampOffset - micCenterYOffset + micClampHeight / 2
				return micDistancePx - 2 * hypotenuse * sine
			}
			fun clampPitch	(clampOffset: Float): Float		= abs(rawClampPitch(clampOffset))
			fun clampFitsAt	(clampOffset: Float): Boolean	= clampPitch(clampOffset) >= minClampPitch
			fun crossed		(clampOffset: Float): Boolean	= rawClampPitch(clampOffset) < 0
			
			return@run when {
				clampFitsAt(defaultClampOffset) -> {
					defaultClampOffset
				}
				crossed(lowerClampOffset) && clampFitsAt(lowerClampOffset) -> {
					// Slide clamp towards mic rear
					val opposite = (micDistancePx + minClampPitch) / 2
					opposite / sine + micCenterYOffset - micClampHeight / 2
				}
				!crossed(upperClampOffset) && clampFitsAt(upperClampOffset) -> {
					// Slide clamp towards mic front
					val opposite = (micDistancePx - minClampPitch) / 2
					opposite / sine + micCenterYOffset - micClampHeight / 2
				}
				else -> {
					// Slide clamp past rear stopper as fallback
					val opposite = (micDistancePx + minClampPitch) / 2
					opposite / sine + micCenterYOffset - micClampHeight / 2
				}
			}
		}
		val clampTopY			= micTopY + clampYOffset
		val stereoBarCenterY	= centerY + (clampYOffset - micCenterYOffset + micClampHeight / 2) * cos(Math.toRadians(micAngle / 2.0)).toFloat()
		val stereoBarTopY		= stereoBarCenterY - stereoBarHeight / 2
		
		// Calculate recording angle lines
		val angleWithXAxisDeg		= 90 - recAngle / 2
		val angleWithXAxisRad		= angleWithXAxisDeg * Math.PI.toFloat() / 180
		val recAngleLineY			= centerY - tan(angleWithXAxisRad) * centerX
		val halfAngleWithXAxisDeg	= 90 - recAngle * (0.25f + angularDist / 60f)
		val halfAngleWithXAxisRad	= halfAngleWithXAxisDeg * Math.PI.toFloat() / 180
		val halfRecAngleLineY		= centerY - tan(halfAngleWithXAxisRad) * centerX
		
		val circleRadius = halfCircleRadiusCm * pixelPerCm
		
		
		// Draw recording angle area
		canvas.drawPath(Path().apply {
			moveTo(centerX, centerY)
			lineTo(left, recAngleLineY)
			lineTo(left, top)
			lineTo(right, top)
			lineTo(right, recAngleLineY)
			close()
		}, recAreaPaint)
		
		// Draw bottom line and ticks
		canvas.drawLine(left, centerY, right, centerY, linesPaint)
		canvas.drawLine(centerX, centerY, centerX, 0f, linesPaint)
		val tickLength = 1 * pixelPerCm
		val shortTickBottomY = centerY + tickLength
		val longTickBottomY = centerY + 2 * tickLength
		for (i in -5..5) {
			val x = centerX + i * 5 * pixelPerCm
			canvas.drawLine(x, centerY, x, if (i % 2 == 0) longTickBottomY else shortTickBottomY, linesPaint)
		}
		
		// Draw recording angle lines
		canvas.drawLine(centerX, centerY, left,		recAngleLineY,		linesPaint)
		canvas.drawLine(centerX, centerY, right,	recAngleLineY,		linesPaint)
		canvas.drawLine(centerX, centerY, left,		halfRecAngleLineY,	linesPaint)
		canvas.drawLine(centerX, centerY, right,	halfRecAngleLineY,	linesPaint)
		
		// Draw half circle
		if (height >= picturedHeight * pixelPerCm) {
			canvas.drawArc(
				centerX - circleRadius, centerY - circleRadius,
				centerX + circleRadius, centerY + circleRadius,
				180f, 180f, false, linesPaint
			)
		}
		
		// Draw stereo bar
		draw(canvas, stereoBarVector, stereoBarLeftX, stereoBarTopY, stereoBarWidth, stereoBarHeight)
		
		val leftRotAngle	= -micAngle / 2
		val rightRotAngle	= micAngle / 2
		val leftCableCenterX	= leftMicLeftX + cableCenterX
		val rightCableCenterX	= rightMicLeftX + cableCenterX
		
		// Draw clamp wheels
		draw(canvas, micClampWheelVector, rightClampLeftX,	clampTopY, micClampWidth, micClampHeight, rightRotAngle,	rightCableCenterX, centerY)
		draw(canvas, micClampWheelVector, leftClampLeftX,	clampTopY, micClampWidth, micClampHeight, leftRotAngle,	leftCableCenterX, centerY)
		
		// Vary mirrored microphone alpha based on angle to simulate light reflections
		micVectorMirrored?.alpha = (128 * (1 - cos(Math.toRadians(micAngle / 2.0)))).roundToInt()
		
		// Draw shadows, microphones and clamps
		micShadowImage?.alpha = maxShadowAlpha
		//						drawable			leftX				topY		width			height			rotationAngle	rotationCenterX,	rotationCenterY
		// RIGHT
		drawShadow	(canvas,	cableShadowImage,	rightMicLeftX,		cableTopY,	cableWidth,		cableHeight,	rightRotAngle,	rightCableCenterX,	rotationCenterY = centerY)
		drawShadow	(canvas,	micShadowImage,		rightMicLeftX,		micTopY,	micWidth,		micHeight,		rightRotAngle,						rotationCenterY = centerY)
		draw		(canvas,	cableVector,		rightMicLeftX,		cableTopY,	cableWidth,		cableHeight,	rightRotAngle,	rightCableCenterX,	centerY)
		drawMic		(canvas, rightNotLeft = true,	rightMicLeftX,		micTopY)
		draw		(canvas,	micClampVector,		rightClampLeftX,	clampTopY, 	micClampWidth,	micClampHeight,	rightRotAngle,	rightCableCenterX,	centerY)
		// LEFT
		drawShadow	(canvas,	cableShadowImage,	leftMicLeftX,		cableTopY,	cableWidth,		cableHeight,	leftRotAngle,	leftCableCenterX,	rotationCenterY = centerY,	scaleX = -1f)
		drawShadow	(canvas,	micShadowImage,		leftMicLeftX,		micTopY,	micWidth,		micHeight,		leftRotAngle,						rotationCenterY = centerY)
		draw		(canvas,	cableVector,		leftMicLeftX,		cableTopY,	cableWidth,		cableHeight,	leftRotAngle,	leftCableCenterX,	rotationCenterY = centerY,	scaleX = -1f)
		drawMic		(canvas, rightNotLeft = false,	leftMicLeftX,		micTopY)
		draw		(canvas,	micClampVector,		leftClampLeftX,		clampTopY,	micClampWidth,	micClampHeight,	leftRotAngle,	leftCableCenterX,	centerY)
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
						(minMicDistanceCm / cmPerInch).roundToInt(),
						(maxMicDistanceCm / cmPerInch).roundToInt()
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
						val labelText = angleText(micAngle.toDouble(), useHalfAngles, addHalfDecimalForHalfAngles = false)
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
			
			val micDistanceAtMaxAngle = calculateCardioidMicDistance(recAngle.toDouble(), maxMicAngle.toDouble())
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
	
	
	
	private fun draw(
		canvas:				Canvas,
		drawable:			Drawable?,
		leftX:				Float,
		topY:				Float,
		width:				Float,
		height:				Float,
		rotationAngle:		Float = 0f,
		rotationCenterX:	Float = leftX + width / 2f,
		rotationCenterY:	Float = topY + height / 2f,
		offsetX:			Float = 0f,
		offsetY:			Float = 0f,
		scaleX:				Float = 1f,
		scaleY:				Float = 1f
	) {
		drawable?.let {
			canvas.save()
			if (rotationAngle != 0f) {
				canvas.rotate(rotationAngle, rotationCenterX + offsetX, rotationCenterY + offsetY)
			}
			if (scaleX != 1f || scaleY != 1f) {
				canvas.scale(scaleX, scaleY, rotationCenterX + offsetX, rotationCenterY + offsetY)
			}
			val left	= (leftX			+ offsetX).roundToInt()
			val top		= (topY				+ offsetY).roundToInt()
			val right	= (leftX + width	+ offsetX).roundToInt()
			val bottom	= (topY + height	+ offsetY).roundToInt()
			it.setBounds(left, top, right, bottom)
			it.draw(canvas)
			canvas.restore()
		}
	}
	
	private fun drawMic(
		canvas:			Canvas,
		rightNotLeft:	Boolean,
		leftX:			Float,
		topY:			Float
	) {
		draw(
			canvas, micVector, leftX, topY, micWidth, micHeight,
			(if (rightNotLeft) 1f else -1f) * micAngle / 2,
			leftX + micWidth / 2, centerY
		)
		draw(
			canvas, micVectorMirrored, leftX, topY, micWidth, micHeight,
			(if (rightNotLeft) 1f else -1f) * micAngle / 2,
			leftX + micWidth / 2, centerY,
			scaleX = -1f
		)
	}
	
	private fun drawShadow(
		canvas:				Canvas,
		drawable:			Drawable?,
		casterLeftX:		Float,
		casterTopY:			Float,
		casterWidth:		Float,
		casterHeight:		Float,
		rotationAngle:		Float = 0f,
		rotationCenterX:	Float = casterLeftX + casterWidth / 2,
		rotationCenterY:	Float = casterTopY + casterHeight / 2,
		scaleX:				Float = 1f,
		scaleY:				Float = 1f
	) = draw(
		canvas, drawable,
		casterLeftX - shadowOverhang, casterTopY - shadowOverhang,
		casterWidth + 2 * shadowOverhang, casterHeight + 2 * shadowOverhang,
		rotationAngle, rotationCenterX, rotationCenterY, shadowXOffset, shadowYOffset, scaleX, scaleY
	)
	
	
	
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
		if (useOmni) {
			micVector = omniMicVector
			micVectorMirrored = omniMicVectorMirrored
		} else {
			micVector = cardioidMicVector
			micVectorMirrored = cardioidMicVectorMirrored
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
