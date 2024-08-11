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

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.gmail.simetist.stereophoniccalculator.MainActivity.PrimaryValue.*
import java.io.Serializable
import kotlin.math.roundToInt



private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
	private val recAngleLowerBound		=  40.0
	private val recAngleUpperBound		= 180.0
	private val micDistanceLowerBound	=   0.0
	private val micDistanceUpperBound	=  50.0
	private val micAngleLowerBound		=   0.0
	private val micAngleUpperBound		= 180.0
	
	private val recAngleDefault			=  90.0
	private val micDistanceDefault		=  30.0
	private val micAngleDefault			=  calculateCardioidMicAngle(recAngleDefault, micDistanceDefault)
	
	private var currentRecAngle			=  -1.0
	private var currentMicDistance		=  -1.0
	private var currentMicAngle			=  -1.0
	
	private var useImperial 			= false
	private var useHalfAngles 			= false
	private var useOmni					= false
	private var holdRecAngle			= false
	private var showGraphView			= false
	
	private var ignoreListeners			= false
	
	private val animationDuration		= 1500	// ms
	
	private var customPresets = Array<StereoConfiguration?>(3) { null }
	
	
	private lateinit var mainLayout:					ConstraintLayout
	
	private lateinit var aboutButton:					Button
	
	private lateinit var unitsSwitch:					Switch
	private lateinit var halfAnglesSwitch:				Switch
	private lateinit var micTypeSwitch:					Switch
	
	private lateinit var holdRecAngleSwitch:			Switch
	private lateinit var calcRecAngleButton:			Button
	private lateinit var recAnglePlusMinusLabel:		TextView
	private lateinit var recAngleEdit:					EditText
	private lateinit var recAngleSlider:				SeekBar
	private lateinit var recAngleSliderTickLabels:		Array<Pair<Int, TextView>>
	
	private lateinit var graphicsViewLayout:			FrameLayout
	private lateinit var graphicsView:					StereoConfigView
	private lateinit var graphicsViewModeSwitch:		Switch
	
	private lateinit var micDistanceValueLabel:			TextView
	private lateinit var micDistanceSlider:				SeekBar
	private lateinit var micDistanceSliderTickLabels:	Array<Pair<Pair<Int, Int>, TextView>>
	
	private lateinit var micAngleValueLabel:			TextView
	private lateinit var micAngleSlider:				SeekBar
	private lateinit var micAngleSliderTickLabels:		Array<Pair<Int, TextView>>
	
	private lateinit var angularDistValueLabel:			TextView
	private lateinit var angularDistIndicator:			ProgressBar
	private lateinit var reverbLimitsWarnLabel: 		TextView
	
	private lateinit var ortfButton:					Button
	private lateinit var nosButton:						Button
	private lateinit var dinButton:						Button
	private lateinit var customPresetButtons:			Array<Button>
	
	
	private lateinit var recAngleCalcLauncher:	ActivityResultLauncher<Intent>
	private lateinit var animator:				ValueAnimator
	
	private lateinit var sharedPreferences:		android.content.SharedPreferences
	private lateinit var prefEditor:			android.content.SharedPreferences.Editor
	
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		populateUIElementMembers()
		ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		// Wait until layout is finished to unlock orientation
		mainLayout.post {
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
		}
		
		
		// Set up the recording angle calculator activity launcher
		recAngleCalcLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val resultValue = result.data?.getDoubleExtra("recAngle", -1.0)
					?: return@registerForActivityResult
				applyRecAngleCalcResult(resultValue)
			}
		}
		
		setupAnimations()
		
		
		sharedPreferences = getSharedPreferences("com.gmail.simetist.stereophoniccalculator", MODE_PRIVATE)
		prefEditor = sharedPreferences.edit()
		
		if (savedInstanceState != null) {
			Log.i(TAG, "Restoring saved state")
			restoreStateFromBundle(savedInstanceState)
		}
		else if (sharedPreferences.contains("recAngle")) {
			Log.i(TAG, "Restoring state from shared preferences")
			restoreStateFromSharedPrefs()
		}
		else {
			Log.i(TAG, "Using default values")
			setCurrentRecAngle(recAngleDefault)
			setCurrentMicDistance(micDistanceDefault)
			setCurrentMicAngle(micAngleDefault)
			recalculateAngularDistortion()
			recalculateReverbLimits()
		}
		
		setupUIListeners()
	}
	
	
	
	// VALUE BOUNDS
	
	private fun getLowerBound(what: PrimaryValue): Double {
		return when (what) {
			REC_ANGLE		-> recAngleLowerBound
			MIC_DISTANCE	-> micDistanceLowerBound
			MIC_ANGLE		-> micAngleLowerBound
		}
	}
	private fun getUpperBound(what: PrimaryValue): Double {
		return when (what) {
			REC_ANGLE		-> recAngleUpperBound
			MIC_DISTANCE	-> micDistanceUpperBound
			MIC_ANGLE		-> micAngleUpperBound
		}
	}
	
	
	
	// GET STATE
	
	fun isDarkMode(): Boolean {
		val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
		return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
	}
	
	
	
	// SET PRIMARY VALUES
	
	private fun setCurrentRecAngleSlider(recAngle: Double) {
		ignoreListeners = true
		recAngleSlider.progress = (recAngle - recAngleLowerBound).roundToInt()
		ignoreListeners = false
	}
	private fun setCurrentRecAngle(recAngle: Double) {
		currentRecAngle = recAngle
		setCurrentRecAngleSlider(recAngle)
		updateRecAngleEdit()
		graphicsView.setRecAngle(recAngle)
	}
	
	private fun setCurrentMicDistanceSlider(micDistance: Double) {
		ignoreListeners = true
		micDistanceSlider.progress = (micDistance * 10.0).roundToInt()
		ignoreListeners = false
	}
	private fun setCurrentMicDistance(micDistance: Double) {
		currentMicDistance = micDistance
		setCurrentMicDistanceSlider(micDistance)
		updateMicDistanceLabel()
		graphicsView.setMicDistance(micDistance)
	}
	
	private fun setCurrentMicAngleSlider(micAngle: Double) {
		ignoreListeners = true
		micAngleSlider.progress = (micAngle * 10.0).roundToInt()
		ignoreListeners = false
	}
	private fun setCurrentMicAngle(micAngle: Double) {
		currentMicAngle = micAngle
		setCurrentMicAngleSlider(micAngle)
		updateMicAngleLabel()
		graphicsView.setMicAngle(micAngle)
	}
	
	private fun getCurrent(what: PrimaryValue): Double {
		return when (what) {
			REC_ANGLE		-> currentRecAngle
			MIC_DISTANCE	-> currentMicDistance
			MIC_ANGLE		-> currentMicAngle
		}
	}
	private fun setCurrent(what: PrimaryValue, value: Double) {
		when (what) {
			REC_ANGLE		-> setCurrentRecAngle	(value)
			MIC_DISTANCE	-> setCurrentMicDistance(value)
			MIC_ANGLE		-> setCurrentMicAngle	(value)
		}
	}
	
	
	
	// UPDATE VALUE WIDGETS (EDIT/LABEL)
	
	private fun updateRecAngleEdit() {
		if (useHalfAngles) {
			recAngleEdit.setText("%.1f".format(currentRecAngle / 2))
		} else {
			recAngleEdit.setText(currentRecAngle.roundToInt().toString())
		}
	}
	
	private fun updateMicDistanceLabel() {
		micDistanceValueLabel.text = lengthText(currentMicDistance, useImperial)
	}
	
	private fun updateMicAngleLabel() {
		micAngleValueLabel.text = angleText(currentMicAngle, useHalfAngles, plusMinusSpace = true)
	}
	
	
	
	// RECALCULATE VALUES AFTER CHANGES
	
	private fun calculateValueCardioid(what: PrimaryValue, values: Map<PrimaryValue, Double>): Double {
		return when (what) {
			REC_ANGLE		-> calculateCardioidRecordingAngle	(values[MIC_DISTANCE]!!,	values[MIC_ANGLE]!!)
			MIC_DISTANCE	-> calculateCardioidMicDistance		(values[REC_ANGLE]!!,		values[MIC_ANGLE]!!)
			MIC_ANGLE		-> calculateCardioidMicAngle		(values[REC_ANGLE]!!,		values[MIC_DISTANCE]!!)
		}
	}
	
	private fun recalculateAngularDistortion() {
		val angularDistortion = calculateAngularDistortion(currentMicDistance, currentMicAngle)
		
		angularDistValueLabel.text = "≤ %.1f°".format(angularDistortion)
		angularDistIndicator.progress = (angularDistortion * 100).roundToInt()
		
		// Set progress color based on the value
		val progressRatio = angularDistIndicator.progress / angularDistIndicator.max.toFloat()
		val color = when {
			progressRatio < 0.5	-> Color.rgb(((-0.5f + progressRatio * 3f) * 255f).roundToInt().coerceAtLeast(0), 255, 0)
			else				-> Color.rgb(255, ((2.5f - progressRatio * 3f) * 255f).roundToInt().coerceAtLeast(0), 0)
		}
		angularDistIndicator.progressTintList = ColorStateList.valueOf(color)
		
		graphicsView.setAngularDist(angularDistortion)
	}
	
	private fun recalculateReverbLimits() {
		val (centerExceeds, sidesExceed) = calculateReverbLimitExceeded(currentMicDistance, currentMicAngle)
		
		reverbLimitsWarnLabel.text = when {
			centerExceeds	-> "Excessive reverb in the center"
			sidesExceed		-> "Excessive reverb on the sides"
			else			-> "Not exceeded"
		}
		reverbLimitsWarnLabel.setTextColor(when {
			centerExceeds || sidesExceed	-> Color.rgb(200, 0, 0)
			else							-> Color.rgb(0, 200, 0)
		})
	}
	
	
	
	// HANDLING USER CHANGES
	
	private var lastChangedPrimValue:			PrimaryValue = MIC_ANGLE
	private var secondToLastChangedPrimValue:	PrimaryValue = MIC_DISTANCE
	
	private fun handlePrimaryValueChangeByUser(changed: PrimaryValue) {
		return if (useOmni) {
			handlePrimaryValueChangeByUserForOmni(changed)
		} else {
			handlePrimaryValueChangeByUserForCardioid(changed)
		}
	}
	
	private fun handlePrimaryValueChangeByUserForCardioid(changed: PrimaryValue) {
		val stationary = if (holdRecAngle && changed != REC_ANGLE) {
			REC_ANGLE
		}
		else {	// !holdRecAngle || changed == PrimaryValue.REC_ANGLE
			if (lastChangedPrimValue != changed) lastChangedPrimValue else secondToLastChangedPrimValue
		}
		val mobile = PrimaryValue.remaining(changed, stationary)
		assert(mobile != stationary && mobile != changed && stationary != changed)
		
		// Calculate new value for mobile
		val changedValue		= getCurrent(changed)
		var stationaryValue		= getCurrent(stationary)
		val mobileCalcInputs	= mapOf(changed to changedValue, stationary to stationaryValue)
		var mobileValue = calculateValueCardioid(mobile, mobileCalcInputs)
		
		// Check bounds
		val min	= getLowerBound(mobile)
		val max	= getUpperBound(mobile)
		var stationaryWasChanged = false
		if (mobileValue < min || mobileValue > max) {
			mobileValue = mobileValue.coerceIn(min, max)
			
			// Stationary need to be recalculated
			val stationaryCalcInputs = mapOf(changed to changedValue, mobile to mobileValue)
			stationaryValue = calculateValueCardioid(stationary, stationaryCalcInputs)
			stationaryWasChanged = true
		}
		
		// Perform updates
		setCurrent(mobile, mobileValue)
		if (stationaryWasChanged) {
			setCurrent(stationary, stationaryValue)
		}
		recalculateAngularDistortion()
		recalculateReverbLimits()
		
		if (changed != lastChangedPrimValue) {
			secondToLastChangedPrimValue = lastChangedPrimValue
			lastChangedPrimValue = changed
		}
	}
	
	private fun handlePrimaryValueChangeByUserForOmni(changed: PrimaryValue) {
		if (changed == REC_ANGLE) {
			var recAngle = currentRecAngle
			var micDistance = calculateOmniMicDistance(recAngle)
			
			if (micDistance < micDistanceLowerBound || micDistance > micDistanceUpperBound) {
				micDistance = micDistance.coerceIn(micDistanceLowerBound, micDistanceUpperBound)
				recAngle = calculateOmniRecordingAngle(micDistance)
				setCurrentRecAngle(recAngle)
			}
			
			setCurrentMicDistance(micDistance)
		} else {
			assert(changed == MIC_DISTANCE)
			
			var micDistance = currentMicDistance
			var recAngle = calculateOmniRecordingAngle(micDistance)
			
			if (recAngle < recAngleLowerBound || recAngle > recAngleUpperBound) {
				recAngle = recAngle.coerceIn(recAngleLowerBound, recAngleUpperBound)
				micDistance = calculateOmniMicDistance(recAngle)
				setCurrentMicDistance(micDistance)
			}
			
			setCurrentRecAngle(recAngle)
		}
		
		// Perform updates
		recalculateAngularDistortion()
		recalculateReverbLimits()
		
		if (changed != lastChangedPrimValue) {
			secondToLastChangedPrimValue = lastChangedPrimValue
			lastChangedPrimValue = changed
		}
	}
	
	fun handle2DUserInput(micDistance: Double, micAngle: Double) {
		if (useOmni && micAngle > 0.0) {
			micTypeSwitch.performClick()
			vibrate(this, 100)
		}
		
		lastChangedPrimValue = MIC_DISTANCE
		secondToLastChangedPrimValue = MIC_ANGLE
		
		setCurrentMicDistance(micDistance.coerceIn(micDistanceLowerBound, micDistanceUpperBound))
		setCurrentMicAngle(micAngle.coerceIn(micAngleLowerBound, micAngleUpperBound))
		handlePrimaryValueChangeByUser(lastChangedPrimValue)
	}
	
	
	
	// LISTENER FUNCTIONS
	
	private fun setUnitsSetting(imperialNotMetric: Boolean) {
		if (unitsSwitch.isChecked != imperialNotMetric) unitsSwitch.isChecked = imperialNotMetric
		useImperial = imperialNotMetric
		
		// Update the mic distance slider ticks
		val maxTickCm = micDistanceSliderTickLabels.last().first.first.toFloat()
		if (useImperial) {
			micDistanceSliderTickLabels.forEach { (cmAndInches, label) ->
				if (cmAndInches.second == -1) {
					label.text = ""
				} else {
					label.text = "${cmAndInches.second}in"
				}
				// Reposition the tick label
				label.layoutParams = (label.layoutParams as ConstraintLayout.LayoutParams).apply {
					horizontalBias = cmAndInches.second.toFloat() / (maxTickCm / cmPerInch.toFloat())
				}
			}
		} else {
			micDistanceSliderTickLabels.forEach { (cmAndInches, label) ->
				label.text = "${cmAndInches.first}cm"
				// Reposition the tick label
				label.layoutParams = (label.layoutParams as ConstraintLayout.LayoutParams).apply {
					horizontalBias = cmAndInches.first.toFloat() / maxTickCm
				}
			}
		}
		
		// Update the mic distance label
		updateMicDistanceLabel()
		// Update preset buttons
		updatePresetButtonTexts()
		
		// Update the graph view
		graphicsView.setUseImperial(useImperial)
	}
	
	private fun setHalfAnglesSetting(halfNotFull: Boolean) {
		if (halfAnglesSwitch.isChecked != halfNotFull) halfAnglesSwitch.isChecked = halfNotFull
		useHalfAngles = halfNotFull
		
		// Update the rec angle slider ticks
		recAnglePlusMinusLabel.text = if (useHalfAngles) "±" else ""
		recAngleSliderTickLabels.forEach { (angle, label) ->
			label.text = angleText(angle.toDouble(), useHalfAngles, 0, false)
		}
		micAngleSliderTickLabels.forEach { (angle, label) ->
			label.text = angleText(angle.toDouble(), useHalfAngles, 0, false)
		}
		
		// Update the widgets displaying angles as numbers
		updateRecAngleEdit()
		updateMicAngleLabel()
		updatePresetButtonTexts()
		
		// Update the graph view
		graphicsView.setUseHalfAngles(useHalfAngles)
	}
	
	private fun setMicTypeSetting(omniNotCardioid: Boolean) {
		if (micTypeSwitch.isChecked != omniNotCardioid) micTypeSwitch.isChecked = omniNotCardioid
		useOmni = omniNotCardioid
		
		// Disable mic angle slider and hold rec angle switch if using omni
		micAngleSlider		.isEnabled = !useOmni
		holdRecAngleSwitch	.isEnabled = !useOmni
		ignoreListeners = true
		holdRecAngleSwitch.isChecked = if (useOmni) false else holdRecAngle
		ignoreListeners = false
		
		if (useOmni) {
			setCurrentMicAngle(0.0)
			val lastChangedRAorMD = if (lastChangedPrimValue != MIC_ANGLE) lastChangedPrimValue else secondToLastChangedPrimValue
			handlePrimaryValueChangeByUserForOmni(lastChangedRAorMD)
		}
		
		// Update the graphics view
		graphicsView.setUseOmni(useOmni)
	}
	
	private fun setHoldRecAngleSetting(enable: Boolean) {
		if (holdRecAngleSwitch.isChecked != enable) holdRecAngleSwitch.isChecked = enable
		holdRecAngle = enable
	}
	
	private fun setGraphicsModeSetting(graphNotMic: Boolean) {
		if (graphicsViewModeSwitch.isChecked != graphNotMic) graphicsViewModeSwitch.isChecked = graphNotMic
		showGraphView = graphNotMic
		graphicsView.setShowGraphView(showGraphView)
	}
	
	private fun updateAfterRecAngleEditChanged() {
		stopAnimation()
		
		var currentValue = recAngleEdit.text.toString().toIntOrNull() ?: return
		
		if (currentValue < recAngleLowerBound || currentValue > recAngleUpperBound) {
			currentValue = currentValue.coerceIn(recAngleLowerBound.roundToInt(), recAngleUpperBound.roundToInt())
			recAngleEdit.setText(currentValue.toString())
		}
		
		setCurrentRecAngle(currentValue.toDouble())
	}
	
	private fun updateAfterRecAngleSliderMoved() {
		stopAnimation()
		setCurrentRecAngle(recAngleSlider.progress.toDouble() + recAngleLowerBound)
		handlePrimaryValueChangeByUser(REC_ANGLE)
	}
	
	private fun updateAfterMicDistanceSliderMoved() {
		stopAnimation()
		setCurrentMicDistance(micDistanceSlider.progress.toDouble() / 10.0)
		handlePrimaryValueChangeByUser(MIC_DISTANCE)
	}
	
	private fun updateAfterMicAngleSliderMoved() {
		stopAnimation()
		setCurrentMicAngle(micAngleSlider.progress.toDouble() / 10.0)
		handlePrimaryValueChangeByUser(MIC_ANGLE)
	}
	
	
	
	// RECORDING ANGLE CALCULATOR CALLBACK
	
	private fun applyRecAngleCalcResult(recAngle: Double) {
		if (!holdRecAngle) setHoldRecAngleSetting(true)
		setCurrentRecAngle(recAngle)
		handlePrimaryValueChangeByUser(REC_ANGLE)
	}
	
	
	
	// PRESETS
	
	private fun applyNearCoincidentPreset(micDistance: Int, micAngle: Int) {
		if (useOmni) micTypeSwitch.performClick()
		
		startAnimation(false, micDistance.toDouble(), micAngle.toDouble())
	}
	
	private fun setCustomPreset(index: Int) {
		customPresets[index] = StereoConfiguration(
			useOmni,
			currentRecAngle,
			currentMicDistance,
			currentMicAngle
		)
		
		setCustomPresetButtonText(index)
	}
	
	private fun clearCustomPreset(index: Int) {
		customPresets[index] = null
		customPresetButtons[index].text = "Empty"
	}
	
	private fun setCustomPresetButtonText(index: Int) {
		val preset = customPresets[index] ?: return
		
		val recAngleText	= angleText	(preset.recAngle,		useHalfAngles, 	0, false)
		val micDistanceText	= lengthText(preset.micDistance,	useImperial,	0)
		val micAngleText	= angleText	(preset.micAngle,		useHalfAngles,	0, false)
		
		var detailsText = "$micDistanceText/$micAngleText"
		if (preset.micDistance == 0.0) {
			detailsText = "XY $micAngleText"
		} else if (preset.micAngle == 0.0) {
			detailsText = "AB $micDistanceText"
		}
		
		customPresetButtons[index].text = "$recAngleText: $detailsText"
	}
	
	private fun updatePresetButtonTexts() {
		customPresetButtons.indices.forEach { index ->
			setCustomPresetButtonText(index)
		}
	}
	
	private fun applyCustomPreset(index: Int) {
		val preset = customPresets[index] ?: return
		
		startAnimation(preset.omniNotCardioid, preset.micDistance, preset.micAngle)
	}
	
	
	
	// PORTRAIT / LANDSCAPE HANDLING
	
	// Saved layout states during landscape orientation
	private var portraitGraphicsFrameWidth			= 0
	private var portraitGraphicsFrameHeight			= 0
	private var portraitGraphicsFrameStartToStart	= 0
	private var portraitGraphicsFrameEndToEnd		= 0
	private var portraitGraphicsFrameTopToBottom	= 0
	private var portraitGraphicsFrameBottomToTop	= 0
	private var portraitGraphicsFrameLeftMargin		= 0
	private var portraitGraphicsFrameRightMargin	= 0
	private var portraitGraphicsFrameTopMargin		= 0
	private var portraitGraphicsFrameBottomMargin	= 0
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			// Restore graphicsFrame's constraints
			graphicsViewLayout.layoutParams = ConstraintLayout.LayoutParams(
				portraitGraphicsFrameWidth,
				portraitGraphicsFrameHeight
			).apply {
				startToStart	= portraitGraphicsFrameStartToStart
				endToEnd		= portraitGraphicsFrameEndToEnd
				topToBottom		= portraitGraphicsFrameTopToBottom
				bottomToTop		= portraitGraphicsFrameBottomToTop
				leftMargin		= portraitGraphicsFrameLeftMargin
				rightMargin		= portraitGraphicsFrameRightMargin
				topMargin		= portraitGraphicsFrameTopMargin
				bottomMargin	= portraitGraphicsFrameBottomMargin
			}
			// Remove background color for graphicsFrameLayout
			graphicsViewLayout.setBackgroundColor(Color.TRANSPARENT)
		}
		else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// Save layout values
			portraitGraphicsFrameWidth			= graphicsViewLayout.width
			portraitGraphicsFrameHeight			= graphicsViewLayout.height
			val params = graphicsViewLayout.layoutParams as ConstraintLayout.LayoutParams
			portraitGraphicsFrameStartToStart	= params.startToStart
			portraitGraphicsFrameEndToEnd		= params.endToEnd
			portraitGraphicsFrameTopToBottom	= params.topToBottom
			portraitGraphicsFrameBottomToTop	= params.bottomToTop
			portraitGraphicsFrameLeftMargin		= params.leftMargin
			portraitGraphicsFrameRightMargin	= params.rightMargin
			portraitGraphicsFrameTopMargin		= params.topMargin
			portraitGraphicsFrameBottomMargin	= params.bottomMargin
			
			// Make graphicsFrame take up the whole layout
			graphicsViewLayout.layoutParams = ConstraintLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
			// Set background color for graphicsFrameLayout
			graphicsViewLayout.setBackgroundColor(Color.BLACK)
		}
		
		graphicsView.resetCache()
	}
	
	
	
	// SAVE / RESTORE STATE
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		
		outState.putBoolean("useInches",		useImperial)
		outState.putBoolean("useHalfAngles",	useHalfAngles)
		outState.putBoolean("useOmni",			useOmni)
		outState.putBoolean("holdRecAngle",		holdRecAngle)
		outState.putBoolean("showGraphView",	showGraphView)
		
		outState.putDouble("recAngle",			currentRecAngle)
		outState.putDouble("micDistance",		currentMicDistance)
		outState.putDouble("micAngle",			currentMicAngle)
		
		outState.putSerializable("customPresets", customPresets)
	}
	
	private fun restoreStateFromBundle(savedInstanceState: Bundle) {
		setUnitsSetting			(savedInstanceState.getBoolean("useInches"))
		setHalfAnglesSetting	(savedInstanceState.getBoolean("useHalfAngles"))
		setMicTypeSetting		(savedInstanceState.getBoolean("useOmni"))
		setHoldRecAngleSetting	(savedInstanceState.getBoolean("holdRecAngle"))
		setGraphicsModeSetting	(savedInstanceState.getBoolean("showGraphView"))
		
		setCurrentRecAngle		(savedInstanceState.getDouble("recAngle"))
		setCurrentMicDistance	(savedInstanceState.getDouble("micDistance"))
		setCurrentMicAngle		(savedInstanceState.getDouble("micAngle"))
		
		recalculateAngularDistortion()
		recalculateReverbLimits()
		
		val serializable = savedInstanceState.getSerializable("customPresets") as Array<*>
		for (i in 0 until 3) {
			customPresets[i] = serializable[i] as StereoConfiguration?
		}
		updatePresetButtonTexts()
	}
	
	private fun saveStateToSharedPrefs() {
		prefEditor.putBoolean("useImperial",	useImperial)
		prefEditor.putBoolean("useHalfAngles",	useHalfAngles)
		prefEditor.putBoolean("useOmni",		useOmni)
		prefEditor.putBoolean("holdRecAngle",	holdRecAngle)
		prefEditor.putBoolean("showGraphView",	showGraphView)
		
		prefEditor.putFloat("recAngle",			currentRecAngle.toFloat())
		prefEditor.putFloat("micDistance",		currentMicDistance.toFloat())
		prefEditor.putFloat("micAngle",			currentMicAngle.toFloat())
		
		for (i in 0 until 3) {
			if (customPresets[i] == null) continue
			
			val omniNotCardioid	= customPresets[i]!!.omniNotCardioid
			val recAngle		= customPresets[i]!!.recAngle.toFloat()
			val micDistance		= customPresets[i]!!.micDistance.toFloat()
			val micAngle		= customPresets[i]!!.micAngle.toFloat()
			
			prefEditor.putBoolean(	"customPreset${i}_omniNotCardioid",	omniNotCardioid)
			prefEditor.putFloat(	"customPreset${i}_recAngle",		recAngle)
			prefEditor.putFloat(	"customPreset${i}_micDistance",		micDistance)
			prefEditor.putFloat(	"customPreset${i}_micAngle",		micAngle)
		}
		
		prefEditor.apply()
	}
	
	private fun restoreStateFromSharedPrefs() {
		setUnitsSetting			(sharedPreferences.getBoolean("useImperial",	false))
		setHalfAnglesSetting	(sharedPreferences.getBoolean("useHalfAngles",	false))
		setMicTypeSetting		(sharedPreferences.getBoolean("useOmni",		false))
		setHoldRecAngleSetting	(sharedPreferences.getBoolean("holdRecAngle",	false))
		setGraphicsModeSetting	(sharedPreferences.getBoolean("showGraphView",	false))
		
		setCurrentRecAngle		(sharedPreferences.getFloat("recAngle",		recAngleDefault		.toFloat()).toDouble())
		setCurrentMicDistance	(sharedPreferences.getFloat("micDistance",	micDistanceDefault	.toFloat()).toDouble())
		setCurrentMicAngle		(sharedPreferences.getFloat("micAngle",		micAngleDefault		.toFloat()).toDouble())
		
		recalculateAngularDistortion()
		recalculateReverbLimits()
		
		for (i in 0 until 3) {
			val neededKeys = arrayOf("omniNotCardioid", "recAngle", "micDistance", "micAngle")
			if (neededKeys.any { !sharedPreferences.contains("customPreset${i}_" + it) }) continue
			
			val omniNotCardioid	= sharedPreferences.getBoolean(	"customPreset${i}_omniNotCardioid",	false)
			val recAngle		= sharedPreferences.getFloat(	"customPreset${i}_recAngle",		-1f).toDouble()
			val micDistance		= sharedPreferences.getFloat(	"customPreset${i}_micDistance",		-1f).toDouble()
			val micAngle		= sharedPreferences.getFloat(	"customPreset${i}_micAngle",		-1f).toDouble()
			
			customPresets[i] = StereoConfiguration(omniNotCardioid, recAngle, micDistance, micAngle)
		}
		updatePresetButtonTexts()
	}
	
	override fun onPause() {
		saveStateToSharedPrefs()
		super.onPause()
	}
	override fun onDestroy() {
		saveStateToSharedPrefs()
		super.onDestroy()
	}
	
	
	
	// UI INITIALIZATION
	
	private fun populateUIElementMembers() {
		mainLayout					= findViewById(R.id.mainLayout)
		
		aboutButton					= findViewById(R.id.aboutButton)
		
		unitsSwitch					= findViewById(R.id.unitsSwitch)
		halfAnglesSwitch			= findViewById(R.id.halfAnglesSwitch)
		micTypeSwitch				= findViewById(R.id.micTypeSwitch)
		
		holdRecAngleSwitch			= findViewById(R.id.holdRecAngleSwitch)
		calcRecAngleButton			= findViewById(R.id.calcRecAngleButton)
		recAnglePlusMinusLabel		= findViewById(R.id.recAnglePlusMinusLabel)
		recAngleEdit				= findViewById(R.id.recAngleEdit)
		recAngleSlider				= findViewById(R.id.recAngleSlider)
		recAngleSliderTickLabels = arrayOf(
			40	to findViewById(R.id.recAngleSliderTick40Label),
			60	to findViewById(R.id.recAngleSliderTick60Label),
			90	to findViewById(R.id.recAngleSliderTick90Label),
			120	to findViewById(R.id.recAngleSliderTick120Label),
			150	to findViewById(R.id.recAngleSliderTick150Label),
			180	to findViewById(R.id.recAngleSliderTick180Label)
		)
		
		graphicsViewLayout			= findViewById(R.id.graphicsViewLayout)
		graphicsView				= findViewById(R.id.graphicsView)
		graphicsViewModeSwitch		= findViewById(R.id.graphicsViewModeSwitch)
		
		micDistanceValueLabel		= findViewById(R.id.micDistanceValueLabel)
		micDistanceSlider			= findViewById(R.id.micDistanceSlider)
		micDistanceSliderTickLabels = arrayOf(
			(0	to 0)	to findViewById(R.id.micDistanceSliderTick0Label),
			(10	to 5)	to findViewById(R.id.micDistanceSliderTick10Label),
			(20	to 10)	to findViewById(R.id.micDistanceSliderTick20Label),
			(30	to 15)	to findViewById(R.id.micDistanceSliderTick30Label),
			(40	to -1)	to findViewById(R.id.micDistanceSliderTick40Label),
			(50	to -1)	to findViewById(R.id.micDistanceSliderTick50Label)
		)
		
		micAngleValueLabel			= findViewById(R.id.micAngleValueLabel)
		micAngleSlider				= findViewById(R.id.micAngleSlider)
		micAngleSliderTickLabels = arrayOf(
			0	to findViewById(R.id.micAngleSliderTick0Label),
			60	to findViewById(R.id.micAngleSliderTick60Label),
			120	to findViewById(R.id.micAngleSliderTick120Label),
			180	to findViewById(R.id.micAngleSliderTick180Label)
		)
		
		angularDistValueLabel		= findViewById(R.id.angularDistValueLabel)
		angularDistIndicator		= findViewById(R.id.angularDistIndicator)
		reverbLimitsWarnLabel		= findViewById(R.id.reverbLimitsWarnLabel)
		
		ortfButton					= findViewById(R.id.ortfButton)
		nosButton					= findViewById(R.id.nosButton)
		dinButton					= findViewById(R.id.dinButton)
		customPresetButtons = arrayOf(
			findViewById(R.id.customPresetButton1),
			findViewById(R.id.customPresetButton2),
			findViewById(R.id.customPresetButton3)
		)
	}
	
	private fun setupUIListeners() {
		aboutButton.setOnClickListener {
			startActivity(Intent(this, AboutActivity::class.java))
		}
		
		unitsSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (ignoreListeners) return@setOnCheckedChangeListener
			setUnitsSetting(isChecked)
		}
		halfAnglesSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (ignoreListeners) return@setOnCheckedChangeListener
			setHalfAnglesSetting(isChecked)
		}
		micTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (ignoreListeners) return@setOnCheckedChangeListener
			setMicTypeSetting(isChecked)
		}
		holdRecAngleSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (ignoreListeners) return@setOnCheckedChangeListener
			setHoldRecAngleSetting(isChecked)
		}
		graphicsViewModeSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (ignoreListeners) return@setOnCheckedChangeListener
			setGraphicsModeSetting(isChecked)
		}
		
		calcRecAngleButton.setOnClickListener {
			val intent = Intent(this, AngleCalcActivity::class.java)
			intent.putExtra("useImperial", useImperial)
			intent.putExtra("useHalfAngles", useHalfAngles)
			recAngleCalcLauncher.launch(intent)
		}
		
		recAngleEdit.setOnKeyListener(object: View.OnKeyListener {
			override fun onKey(p0: View?, p1: Int, p2: KeyEvent?): Boolean {
				if (p2?.action != KeyEvent.ACTION_UP || p2.keyCode != KeyEvent.KEYCODE_ENTER) {
					return false
				}
				if (ignoreListeners) return true
				updateAfterRecAngleEditChanged()
				return true
			}
		})
		recAngleSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				if (ignoreListeners) return
				updateAfterRecAngleSliderMoved()
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		micDistanceSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				if (ignoreListeners) return
				updateAfterMicDistanceSliderMoved()
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		micAngleSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				if (ignoreListeners) return
				updateAfterMicAngleSliderMoved()
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		
		ortfButton.setOnClickListener {
			applyNearCoincidentPreset(17, 110)
		}
		nosButton.setOnClickListener {
			applyNearCoincidentPreset(30, 90)
		}
		dinButton.setOnClickListener {
			applyNearCoincidentPreset(20, 90)
		}
		
		customPresetButtons.forEachIndexed { index, button ->
			button.setOnClickListener {
				if (customPresets[index] != null) {
					applyCustomPreset(index)
				}
			}
			
			button.setOnLongClickListener {
				if (customPresets[index] == null) {
					setCustomPreset(index)
				} else {
                    clearCustomPreset(index)
				}
				vibrate(this)
				true
			}
		}
	}
	
	
	
	// ANIMATIONS
	
	private fun setupAnimations() {
		animator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = animationDuration.toLong()
			interpolator = FastOutSlowInInterpolator()
		}
	}
	
	private fun startAnimation(endOmniNotCardioid: Boolean, endMicDistance: Double, endMicAngle: Double) {
		stopAnimation()
		
		if (endOmniNotCardioid != useOmni) {
			if (!endOmniNotCardioid) {
				setMicTypeSetting(false)
			} else if (currentMicAngle == 0.0) {
				setMicTypeSetting(true)
			}
			else {
				animator.addListener(object: Animator.AnimatorListener {
					override fun onAnimationStart(p0: Animator) {}
					override fun onAnimationEnd(p0: Animator) {
						setMicTypeSetting(true)
					}
					override fun onAnimationCancel(p0: Animator) {}
					override fun onAnimationRepeat(p0: Animator) {}
				})
			}
		}
		
		val animationStartMD	= currentMicDistance
		val animationStartMA	= currentMicAngle
		animator.addUpdateListener {
			val progress = it.animatedValue as Float
			doAnimationStep(animationStartMD, animationStartMA, endMicDistance, endMicAngle, progress.toDouble())
		}
		animator.start()
	}
	
	private fun stopAnimation() {
		animator.cancel()
		animator.removeAllListeners()
	}
	
	private fun doAnimationStep(
		startMicDistance: Double, startMicAngle: Double,
		endMicDistance: Double, endMicAngle: Double,
		progress: Double
	) {
		val newMicDistance	= startMicDistance + (endMicDistance - startMicDistance) * progress
		var newMicAngle		= startMicAngle + (endMicAngle - startMicAngle) * progress
		var newRecAngle		= calculateCardioidRecordingAngle(newMicDistance, newMicAngle)
		
		// Check bounds
		if (newRecAngle < recAngleLowerBound || newRecAngle > recAngleUpperBound) {
			newRecAngle = newRecAngle.coerceIn(recAngleLowerBound, recAngleUpperBound)
			
			newMicAngle = calculateCardioidMicAngle(newRecAngle, newMicDistance)
		}
		
		// Perform updates
		setCurrentMicDistance(newMicDistance)
		setCurrentMicAngle(newMicAngle)
		setCurrentRecAngle(newRecAngle)
		recalculateAngularDistortion()
		recalculateReverbLimits()
	}
	
	
	
	private enum class PrimaryValue {
		REC_ANGLE,
		MIC_DISTANCE,
		MIC_ANGLE;
		
		companion object {
			fun remaining(first: PrimaryValue, second: PrimaryValue): PrimaryValue {
				if (first != REC_ANGLE		&& second != REC_ANGLE)		return REC_ANGLE
				if (first != MIC_DISTANCE	&& second != MIC_DISTANCE)	return MIC_DISTANCE
				return MIC_ANGLE
			}
		}
	}
	
	
	
	private class StereoConfiguration (
		val omniNotCardioid:	Boolean,
		val recAngle:			Double,
		val micDistance:		Double,
		val micAngle:			Double
	) : Serializable
}