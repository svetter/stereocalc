package com.gmail.simetist.stereophoniccalculator

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.pow
import kotlin.math.roundToInt



const val cmPerInch	= 2.54
const val cmPerFoot	= 30.48



fun lengthText(
	length:				Double,
	useImperial:		Boolean,
	numDecimalPlaces:	Int = if (useImperial) 2 else 1): String {
	return if (useImperial) {
		"%.${numDecimalPlaces}fin".format(length / cmPerInch)
	} else {
		"%.${numDecimalPlaces}fcm".format(length)
	}
}

fun angleText(
	angle:							Double,
	useHalfAngles:					Boolean = false,
	numDecimalPlaces:				Int = 0,
	addHalfDecimalForHalfAngles:	Boolean = true,
	plusMinusSpace:					Boolean = false,
	omitPlusMinusForZero:			Boolean = true
): String {
	val space = if (plusMinusSpace) " " else ""
	return if (useHalfAngles) {
		if (omitPlusMinusForZero) {
			if ((angle * 10.0.pow(numDecimalPlaces)).roundToInt() == 0) {
				return "%.${numDecimalPlaces}f°".format(0.0)
			}
		}
		if (addHalfDecimalForHalfAngles) {
			"±$space%.${numDecimalPlaces + 1}f°".format(angle.roundToInt() / 2.0)
		} else {
			"±$space%.${numDecimalPlaces}f°".format(angle / 2)
		}
	} else {
		"%.${numDecimalPlaces}f°".format(angle)
	}
}



fun vibrate(activity: Activity, duration: Int = 50) {
	val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		val vibratorManager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
		vibratorManager.defaultVibrator
	} else {
		activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
	}
	
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		vibrator.vibrate(VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
	} else {
		@Suppress("DEPRECATION")
		vibrator.vibrate(duration.toLong())
	}
}