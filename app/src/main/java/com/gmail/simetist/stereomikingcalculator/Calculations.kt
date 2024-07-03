package com.gmail.simetist.stereomikingcalculator

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow



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

val centerReverbLimitMicAngle		= 120.0
val sidesReverbLimitFitOutScale		= -118.6927869351978
val sidesReverbLimitFitOutMin		= 103.27731071204713
val sidesReverbLimitFitGrowthRate	= 0.07139444632383811
val sidesReverbLimitFitInMidpoint	= 17.618201102680768



fun calculate5thOrderPolynomial(coefficients: DoubleArray, x: Double): Double {
	if (coefficients.size != 6) {
		throw IllegalArgumentException("Function expects 6 coefficients")
	}
	
	return (0 until 6).fold(0.0) { acc, i ->
		acc + coefficients[i] * x.pow(i)
	}
}

fun calculateFittingParams(recordingAngle: Double): DoubleArray {
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

fun calculateOmniMicDistance(recordingAngle: Double): Double {
	return calculateCardioidMicDistance(recordingAngle, 0.0)
}

fun calculateAngularDistortion(micDistance: Double, micAngle: Double): Double {
	// TODO
	
	return 4.5
}

fun calculateReverbLimitExceeded(micDistance: Double, micAngle: Double): Pair<Boolean, Boolean> {
	val centerExceeds = micAngle >= centerReverbLimitMicAngle
	
	val sidesReverbLimit = sidesReverbLimitFitOutScale / (1 + exp(-sidesReverbLimitFitGrowthRate * (micDistance - sidesReverbLimitFitInMidpoint))) + sidesReverbLimitFitOutMin
	val sideExceeds = micAngle <= sidesReverbLimit
	
	return centerExceeds to sideExceeds
}