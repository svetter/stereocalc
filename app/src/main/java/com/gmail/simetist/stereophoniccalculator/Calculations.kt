package com.gmail.simetist.stereophoniccalculator

import kotlin.math.*



val outScalePolyCoefficients = doubleArrayOf(
	-1083.7497974240416,
	60.392830782435155,
	-1.8208592374781103,
	0.02794795842073617,
	-0.00021018770486760238,
	6.200269074837898e-07
)
val outMinPolyCoefficients = doubleArrayOf(
	626.5396928100497,
	-31.62186688569793,
	1.053463771694204,
	-0.017838982583940927,
	0.0001453232277659968,
	-4.583540499373026e-07
)
val growthRatePolyCoefficients = doubleArrayOf(
	-0.043951642915086415,
	0.005979059371898319,
	-0.00013471479523149304,
	1.6264816450493782e-06,
	-9.607278228562373e-09,
	2.0195378707059754e-11
)
val inMidpointPolyCoefficients = doubleArrayOf(
	193.86324507097913,
	-9.146793342578423,
	0.18738057667277322,
	-0.0018920423419732722,
	8.855562862615035e-06,
	-1.363129960341988e-08
)

const val centerReverbLimitMicAngle		= 120.0
const val sidesReverbLimitFitOutScale	= -118.6927869351978
const val sidesReverbLimitFitOutMin		= 103.27731071204713
const val sidesReverbLimitFitGrowthRate	= 0.07139444632383811
const val sidesReverbLimitFitInMidpoint	= 17.618201102680768



private fun calculate5thOrderPolynomial(coefficients: DoubleArray, x: Double): Double {
	if (coefficients.size != 6) {
		throw IllegalArgumentException("Function expects 6 coefficients")
	}
	
	return (0 until 6).fold(0.0) { acc, i ->
		acc + coefficients[i] * x.pow(i)
	}
}

private fun calculateFittingParams(recordingAngle: Double): DoubleArray {
	val hra = recordingAngle / 2.0
	
	val outScale	= calculate5thOrderPolynomial(outScalePolyCoefficients,		hra)
	val outMin		= calculate5thOrderPolynomial(outMinPolyCoefficients,		hra)
	val growthRate	= calculate5thOrderPolynomial(growthRatePolyCoefficients,	hra)
	val inMidpoint	= calculate5thOrderPolynomial(inMidpointPolyCoefficients,	hra)
	
	return doubleArrayOf(outScale, outMin, growthRate, inMidpoint)
}

fun calculateCardioidMicAngle(recordingAngle: Double, micDistance: Double): Double {
	val (outScale, outMin, growthRate, inMidpoint) = calculateFittingParams(recordingAngle)
	
	return outScale / (1 + exp(-growthRate * (micDistance - inMidpoint))) + outMin
}

fun calculateCardioidMicDistance(recordingAngle: Double, micAngle: Double): Double {
	val (outScale, outMin, growthRate, inMidpoint) = calculateFittingParams(recordingAngle)
	
	return inMidpoint - ln(- outScale / (outMin - micAngle) - 1) / growthRate
}

fun calculateCardioidRecordingAngle(micDistance: Double, micAngle: Double): Double {
	// Make very rough first approximation
	var recAngle = (270.0 - 4.0 * micDistance - 1.0 * micAngle).toInt().coerceIn(40, 180)
	
	var bestRecAngle		= recAngle
	var bestMicAngleError	= Double.MAX_VALUE
	var micAngleError		= bestMicAngleError.nextDown()
	// Determine step direction
	val increment = if (calculateCardioidMicAngle(recAngle.toDouble(), micDistance) > micAngle) 1 else -1
	
	// Step until the lowest error has been passed
	while (micAngleError < bestMicAngleError) {
		bestRecAngle = recAngle
		bestMicAngleError = micAngleError
		
		recAngle += increment
		val calculatedMicAngle = calculateCardioidMicAngle(recAngle.toDouble(), micDistance)
		micAngleError = abs(calculatedMicAngle - micAngle)
	}
	
	return bestRecAngle.toDouble()
}

fun calculateOmniMicDistance(recordingAngle: Double): Double {
	return calculateCardioidMicDistance(recordingAngle, 0.0)
}

fun calculateOmniRecordingAngle(micDistance: Double): Double {
	return calculateCardioidRecordingAngle(micDistance, 0.0)
}

fun calculateAngularDistortion(micDistance: Double, micAngle: Double): Double {
	return AngularDistortionNN.predict(micDistance, micAngle)
}

fun calculateReverbLimitExceeded(micDistance: Double, micAngle: Double): Pair<Boolean, Boolean> {
	val centerExceeds = micAngle >= centerReverbLimitMicAngle
	
	val sidesReverbLimit = sidesReverbLimitFitOutScale / (1 + exp(-sidesReverbLimitFitGrowthRate * (micDistance - sidesReverbLimitFitInMidpoint))) + sidesReverbLimitFitOutMin
	val sideExceeds = micAngle <= sidesReverbLimit
	
	return centerExceeds to sideExceeds
}



object AngularDistortionNN {
	private fun denseLayer(inputs: DoubleArray, weights: Array<DoubleArray>, bias: DoubleArray): DoubleArray {
		val output = DoubleArray(bias.size)
		for (i in bias.indices) {
			output[i] = bias[i]
			for (j in inputs.indices) {
				output[i] += inputs[j] * weights[j][i]
			}
		}
		return output
	}
	
	private fun applyActivation(inputs: DoubleArray, activation: String): DoubleArray {
		val output = DoubleArray(inputs.size)
		for (i in inputs.indices) {
			output[i] = when (activation) {
				"relu" -> max(0.0, inputs[i])
				"sigmoid" -> (1.0 / (1.0 + exp(-inputs[i])))
				else -> inputs[i]
			}
		}
		return output
	}
	
	private val denseWeights = arrayOf(
		doubleArrayOf(0.050552114844322205, -0.621399462223053, 0.7947633862495422, 0.47630852460861206, 0.5214497447013855, -0.689803957939148, -0.14295123517513275, -0.5771496295928955, 0.5864160656929016, 0.29007992148399353),
		doubleArrayOf(-0.6147325038909912, -0.28008368611335754, 1.3294757604599, 1.6732933521270752, -0.9143111705780029, -0.14986711740493774, 0.07585252076387405, -0.1127011775970459, 1.0455999374389648, 0.8773002624511719),
	)
	private val denseBiases = doubleArrayOf(-0.04911422356963158, 0.0, -0.279000461101532, -0.657450258731842, 0.0015515354461967945, 0.0, -0.08693814277648926, 0.0, -0.3828710913658142, 0.07013881206512451)
	
	private val dense1Weights = arrayOf(
		doubleArrayOf(-0.06669402122497559, -0.41437506675720215, 0.4012371599674225, 0.33698707818984985, -0.4580766558647156),
		doubleArrayOf(-0.07378596067428589, -0.06178539991378784, -0.3827940821647644, -0.5364109873771667, -0.25290656089782715),
		doubleArrayOf(-0.45742174983024597, -0.33134815096855164, 0.7989304661750793, -0.5234515070915222, 0.12709927558898926),
		doubleArrayOf(-0.5485283732414246, -0.46154189109802246, -0.30033057928085327, 1.4351824522018433, 1.1702848672866821),
		doubleArrayOf(-0.25804170966148376, -0.6322087049484253, -0.9339739084243774, 0.5020516514778137, 0.9352903366088867),
		doubleArrayOf(-0.009140372276306152, -0.3127635419368744, -0.5524733662605286, -0.23470979928970337, 0.2438488006591797),
		doubleArrayOf(-0.4871237277984619, -0.4629260003566742, -0.08642063289880753, -0.40860357880592346, -0.3502930998802185),
		doubleArrayOf(-0.4464275538921356, -0.2328273355960846, -0.19553989171981812, -0.49484264850616455, 0.3188565969467163),
		doubleArrayOf(-0.15538626909255981, 0.3766717314720154, -0.02138102427124977, -1.1788495779037476, -0.9573598504066467),
		doubleArrayOf(0.11749958992004395, 0.08343648910522461, 0.7223596572875977, 0.03965410590171814, 0.281604528427124),
	)
	private val dense1Biases = doubleArrayOf(0.0, 0.0, -0.0802600085735321, 0.1806495487689972, 0.14349153637886047)
	
	private val dense2Weights = arrayOf(
		doubleArrayOf(0.19079256057739258),
		doubleArrayOf(-0.9104411602020264),
		doubleArrayOf(-1.5485447645187378),
		doubleArrayOf(1.6872438192367554),
		doubleArrayOf(1.3242930173873901),
	)
	private val dense2Biases = doubleArrayOf(0.0980764701962471)
	
	private const val X_MIN = 0
	private const val X_MAX = 50
	private const val Y_MIN = 0
	private const val Y_MAX = 180
	private const val Z_MIN = 0
	private const val Z_MAX = 12
	
	fun predict(x: Double, y: Double): Double {
		val xNormalized = (x - X_MIN) / (X_MAX - X_MIN)
		val yNormalized = (y - Y_MIN) / (Y_MAX - Y_MIN)
		val inputs = doubleArrayOf(xNormalized, yNormalized)
		val denseOutput = denseLayer(inputs, denseWeights, denseBiases)
		val denseActivated = applyActivation(denseOutput, "relu")
		val dense1Output = denseLayer(denseActivated, dense1Weights, dense1Biases)
		val dense1Activated = applyActivation(dense1Output, "relu")
		val dense2Output = denseLayer(dense1Activated, dense2Weights, dense2Biases)
		val dense2Activated = applyActivation(dense2Output, "sigmoid")
		val z = dense2Activated[0] * (Z_MAX - Z_MIN) + Z_MIN
		return z
	}
}