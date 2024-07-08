package com.gmail.simetist.stereophoniccalculator

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gmail.simetist.stereophoniccalculator.MainActivity.PrimaryValue.*
import kotlin.math.roundToInt



private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
	private val recAngleLowerBound		= 40.0
	private val recAngleUpperBound		= 180.0
	private val recAngleDefault			= 90.0
	private val micDistanceLowerBound	= 0.0
	private val micDistanceUpperBound	= 50.0
	private val micDistanceDefault		= 30.0
	private val micAngleLowerBound		= 0.0
	private val micAngleUpperBound		= 180.0
	
	private var useInches 				= false
	private var useHalfAngles 			= false
	private var useOmni					= false
	private var holdRecAngle			= false
	
	private var ignoreListeners	= false
	
	
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
	
	private lateinit var graphicsFrameLayout:			FrameLayout
	private lateinit var graphicsView:					StereoConfigView
	
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
	
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		
		populateUIElementMembers()
		
		if (savedInstanceState != null) {
			Log.i(TAG, "Restoring saved state")
			restoreState(savedInstanceState)
		}
		else {
			Log.i(TAG, "Using default values")
			setCurrentRecAngle(recAngleDefault)
			setCurrentMicDistance(micDistanceDefault)
			setCurrentMicAngle(calculateCardioidMicAngle(recAngleDefault, micDistanceDefault))
			recalculateAngularDistortion()
			recalculateReverbLimits()
		}
		
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		// Wait until layout is finished to unlock orientation
		mainLayout.post {
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
	
	
	
	// GET / SET SLIDER VALUES
	
	private fun getCurrentRecAngle(): Double {
		return (recAngleLowerBound + recAngleSlider.progress)
	}
	private fun setCurrentRecAngleSlider(recAngle: Double) {
		ignoreListeners = true
		recAngleSlider.progress = (recAngle - recAngleLowerBound).roundToInt()
		ignoreListeners = false
	}
	private fun setCurrentRecAngle(recAngle: Double) {
		setCurrentRecAngleSlider(recAngle)
		updateRecAngleEdit()
		graphicsView.setRecAngle(recAngle)
	}
	
	private fun getCurrentMicDistance(): Double {
		return micDistanceSlider.progress / 10.0
	}
	private fun setCurrentMicDistanceSlider(micDistance: Double) {
		ignoreListeners = true
		micDistanceSlider.progress = (micDistance * 10.0).roundToInt()
		ignoreListeners = false
	}
	private fun setCurrentMicDistance(micDistance: Double) {
		setCurrentMicDistanceSlider(micDistance)
		updateMicDistanceLabel()
		graphicsView.setMicDistance(micDistance)
	}
	
	private fun getCurrentMicAngle(): Double {
		return micAngleSlider.progress / 10.0
	}
	private fun setCurrentMicAngleSlider(micAngle: Double) {
		ignoreListeners = true
		micAngleSlider.progress = (micAngle * 10.0).roundToInt()
		ignoreListeners = false
	}
	private fun setCurrentMicAngle(micAngle: Double) {
		setCurrentMicAngleSlider(micAngle)
		updateMicAngleLabel()
		graphicsView.setMicAngle(micAngle)
	}
	
	private fun getCurrent(what: PrimaryValue): Double {
		return when (what) {
			REC_ANGLE		-> getCurrentRecAngle()
			MIC_DISTANCE	-> getCurrentMicDistance()
			MIC_ANGLE		-> getCurrentMicAngle()
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
		val recAngle = getCurrentRecAngle()
		if (useHalfAngles) {
			recAngleEdit.setText("%.1f".format(recAngle / 2))
		} else {
			recAngleEdit.setText(recAngle.roundToInt().toString())
		}
	}
	
	private fun updateMicDistanceLabel() {
		val micDistance = getCurrentMicDistance()
		if (useInches) {
			val micDistanceInches = micDistance / 2.54
			micDistanceValueLabel.text = "%.2fin".format(micDistanceInches)
		} else {
			micDistanceValueLabel.text = "%.1fcm".format(micDistance)
		}
	}
	
	private fun updateMicAngleLabel() {
		val micAngle = getCurrentMicAngle()
		if (useHalfAngles) {
			micAngleValueLabel.text = "± %.1f°".format(micAngle.roundToInt() / 2.0)
		} else {
			micAngleValueLabel.text = "%.0f°".format(micAngle)
		}
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
		val micDistance = getCurrentMicDistance()
		val micAngle = getCurrentMicAngle()
		
		val angularDistortion = calculateAngularDistortion(micDistance, micAngle)
		
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
		val micDistance = getCurrentMicDistance()
		val micAngle = getCurrentMicAngle()
		
		val (centerExceeds, sidesExceed) = calculateReverbLimitExceeded(micDistance, micAngle)
		
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
			var recAngle = getCurrentRecAngle()
			var micDistance = calculateOmniMicDistance(recAngle)
			
			if (micDistance < micDistanceLowerBound || micDistance > micDistanceUpperBound) {
				micDistance = micDistance.coerceIn(micDistanceLowerBound, micDistanceUpperBound)
				recAngle = calculateOmniRecordingAngle(micDistance)
				setCurrentRecAngle(recAngle)
			}
			
			setCurrentMicDistance(micDistance)
		} else {
			assert(changed == MIC_DISTANCE)
			
			var micDistance = getCurrentMicDistance()
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
	
	
	
	// LISTENER FUNCTIONS
	
	private fun setUnitsSetting(imperialNotMetric: Boolean) {
		useInches = imperialNotMetric
		
		// Update the mic distance slider ticks
		val maxTickCm = micDistanceSliderTickLabels.last().first.first.toFloat()
		if (useInches) {
			micDistanceSliderTickLabels.forEach { (cmAndInches, label) ->
				if (cmAndInches.second == -1) {
					label.text = ""
				} else {
					label.text = "${cmAndInches.second}in"
				}
				// Reposition the tick label
				label.layoutParams = (label.layoutParams as ConstraintLayout.LayoutParams).apply {
					horizontalBias = cmAndInches.second.toFloat() / (maxTickCm / 2.54f)
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
	}
	
	private fun setHalfAnglesSetting(halfNotFull: Boolean) {
		useHalfAngles = halfNotFull
		
		// Update the rec angle slider ticks
		if (useHalfAngles) {
			recAnglePlusMinusLabel.text = "±"
			recAngleSliderTickLabels.forEach { (angle, label) ->
				label.text = "±${angle / 2}°"
			}
			micAngleSliderTickLabels.forEach { (angle, label) ->
				label.text = "±${angle / 2}°"
			}
		} else {
			recAnglePlusMinusLabel.text = ""
			recAngleSliderTickLabels.forEach { (angle, label) ->
				label.text = "${angle}°"
			}
			micAngleSliderTickLabels.forEach { (angle, label) ->
				label.text = "${angle}°"
			}
		}
		
		// Update the widgets displaying angles as numbers
		updateRecAngleEdit()
		updateMicAngleLabel()
	}
	
	private fun setMicTypeSetting(omniNotCardioid: Boolean) {
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
		holdRecAngle = enable
	}
	
	private fun updateAfterRecAngleEditChanged() {
		var currentValue = recAngleEdit.text.toString().toIntOrNull() ?: return
		
		if (currentValue < recAngleLowerBound || currentValue > recAngleUpperBound) {
			currentValue = currentValue.coerceIn(recAngleLowerBound.roundToInt(), recAngleUpperBound.roundToInt())
			recAngleEdit.setText(currentValue.toString())
		}
		
		setCurrentRecAngle(currentValue.toDouble())
	}
	
	private fun updateAfterRecAngleSliderMoved() {
		updateRecAngleEdit()
		graphicsView.setRecAngle(getCurrentRecAngle())
		handlePrimaryValueChangeByUser(REC_ANGLE)
	}
	
	private fun updateAfterMicDistanceSliderMoved() {
		updateMicDistanceLabel()
		graphicsView.setMicDistance(getCurrentMicDistance())
		handlePrimaryValueChangeByUser(MIC_DISTANCE)
	}
	
	private fun updateAfterMicAngleSliderMoved() {
		updateMicAngleLabel()
		graphicsView.setMicAngle(getCurrentMicAngle())
		handlePrimaryValueChangeByUser(MIC_ANGLE)
	}
	
	
	
	// APPLY PRESETS
	
	private fun applyNearCoincidentPreset(micDistance: Int, micAngle: Int) {
		if (useOmni) micTypeSwitch.performClick()
		
		val recAngle = calculateCardioidRecordingAngle(micDistance.toDouble(), micAngle.toDouble())
		
		setCurrentRecAngle		(recAngle)
		setCurrentMicDistance	(micDistance.toDouble())
		setCurrentMicAngle		(micAngle.toDouble())
		
		recalculateAngularDistortion()
		recalculateReverbLimits()
	}
	
	
	
	// PORTRAIT / LANDSCAPE HANDLING
	
	// Saved layout states during landscape orientation
	private var portraitGraphicsFrameHeight			= 0
	private var portraitGraphicsFrameTopToBottom	= 0
	private var portraitGraphicsFrameStartToStart	= 0
	private var portraitGraphicsFrameEndToEnd		= 0
	private var portraitGraphicsFrameTopMargin		= 0
	private var portraitGraphicsFrameLeftMargin		= 0
	private var portraitGraphicsFrameRightMargin	= 0
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			// Restore graphicsFrame's constraints
			(graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
				height			= portraitGraphicsFrameHeight
				topToBottom		= portraitGraphicsFrameTopToBottom
				startToStart	= portraitGraphicsFrameStartToStart
				endToEnd		= portraitGraphicsFrameEndToEnd
				topMargin		= portraitGraphicsFrameTopMargin
				leftMargin		= portraitGraphicsFrameLeftMargin
				rightMargin		= portraitGraphicsFrameRightMargin
			}
			// Remove background color for graphicsFrameLayout
			graphicsFrameLayout.setBackgroundColor(Color.TRANSPARENT)
		}
		else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// Save layout values
			portraitGraphicsFrameHeight			= graphicsFrameLayout.height
			portraitGraphicsFrameTopToBottom	= (graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).topToBottom
			portraitGraphicsFrameStartToStart	= (graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).startToStart
			portraitGraphicsFrameEndToEnd		= (graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).endToEnd
			portraitGraphicsFrameTopMargin		= (graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).topMargin
			portraitGraphicsFrameLeftMargin		= (graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).leftMargin
			portraitGraphicsFrameRightMargin	= (graphicsFrameLayout.layoutParams as ConstraintLayout.LayoutParams).rightMargin
			
			// Make graphicsFrame take up the whole layout
			graphicsFrameLayout.layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
			// Set background color for graphicsFrameLayout
			graphicsFrameLayout.setBackgroundColor(Color.BLACK)
		}
	}
	
	
	
	// SAVE / RESTORE STATE
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean("useInches",		useInches)
		outState.putBoolean("useHalfAngles",	useHalfAngles)
		outState.putBoolean("useOmni",			useOmni)
		outState.putBoolean("holdRecAngle",		holdRecAngle)
		outState.putDouble("recAngle",			getCurrentRecAngle())
		outState.putDouble("micDistance",		getCurrentMicDistance())
		outState.putDouble("micAngle",			getCurrentMicAngle())
	}
	
	private fun restoreState(savedInstanceState: Bundle) {
		// Restore saved state
		useInches		= savedInstanceState.getBoolean("useInches")
		useHalfAngles	= savedInstanceState.getBoolean("useHalfAngles")
		useOmni			= savedInstanceState.getBoolean("useOmni")
		holdRecAngle	= savedInstanceState.getBoolean("holdRecAngle")
		
		unitsSwitch.isChecked			= useInches
		halfAnglesSwitch.isChecked		= useHalfAngles
		micTypeSwitch.isChecked			= useOmni
		holdRecAngleSwitch.isChecked	= holdRecAngle
		
		setCurrentRecAngle		(savedInstanceState.getDouble("recAngle"))
		setCurrentMicDistance	(savedInstanceState.getDouble("micDistance"))
		setCurrentMicAngle		(savedInstanceState.getDouble("micAngle"))
		
		recalculateAngularDistortion()
		recalculateReverbLimits()
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
		
		graphicsFrameLayout			= findViewById(R.id.graphicsFrameLayout)
		graphicsView				= findViewById(R.id.graphicsView)
		
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
	}
	
	private fun setupUIListeners() {
		aboutButton.setOnClickListener {
			startActivity(Intent(this, AboutActivity::class.java))
		}
		
		unitsSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (!ignoreListeners) return@setOnCheckedChangeListener
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
}