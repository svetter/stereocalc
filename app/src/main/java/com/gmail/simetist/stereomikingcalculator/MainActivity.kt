package com.gmail.simetist.stereomikingcalculator

import android.content.Intent
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
import kotlin.math.roundToInt


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
	val recAngleLowerBound	= 40
	val recAngleUpperBound	= 180
	val recAngleDefault		= 90
	
	var useInches 				= false
	var useHalfAngles 			= false
	var useOmni					= false
	var holdRecAngle			= true
	
	var ignoreSliderListeners	= false
	
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
		
		
		
		// Set default recAngle values
		recAngleSlider.max = recAngleUpperBound - recAngleLowerBound
		recAngleSlider.progress = recAngleDefault - recAngleLowerBound
		updateRecAngleEdit()
		graphicsView.updateRecAngle(recAngleDefault.toDouble())
		// Set default micDistance value
		micDistanceSlider.progress = 300
		updateMicDistanceLabel()
		graphicsView.updateMicDistance(micDistanceSlider.progress.toDouble() / 10.0)
		recalculateMicAngle()
		recalculateAngularDistortion()
		recalculateReverbLimits()
		
		
		
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		
		
		// Set up listeners
		
		unitsSwitch.setOnCheckedChangeListener { _, isChecked ->
			useInches = isChecked
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
			updateMicDistanceLabel()
		}
		
		halfAnglesSwitch.setOnCheckedChangeListener { _, isChecked ->
			useHalfAngles = isChecked
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
			updateRecAngleEdit()
			updateMicAngleLabel()
		}
		
		micTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
			useOmni = isChecked
			// Disable mic angle slider if using omni
			micDistanceSlider.isEnabled	= !useOmni
			micAngleSlider.isEnabled	= !useOmni
			if (useOmni) {
				ignoreSliderListeners = true
				micAngleSlider.progress = 0
				ignoreSliderListeners = false
				updateMicAngleLabel()
				recalculateMicDistance()
			}
			graphicsView.setUseOmni(useOmni)
		}
		
		holdRecAngleSwitch.setOnCheckedChangeListener { _, isChecked ->
			holdRecAngle = isChecked
			if (holdRecAngle) {
				Log.i(TAG, "Hold rec angle on")
				// TODO
			} else {
				Log.i(TAG, "Hold rec angle off")
				// TODO
			}
		}
		
		aboutButton.setOnClickListener {
			startActivity(Intent(this, AboutActivity::class.java))
		}
		
		recAngleEdit.setOnKeyListener(object: View.OnKeyListener {
			override fun onKey(p0: View?, p1: Int, p2: KeyEvent?): Boolean {
				if (p2?.action != KeyEvent.ACTION_UP || p2.keyCode != KeyEvent.KEYCODE_ENTER) {
					return false
				}
				
				var currentValue = recAngleEdit.text.toString().toIntOrNull() ?: return false
				
				if (currentValue < recAngleLowerBound || currentValue > recAngleUpperBound) {
					currentValue = currentValue.coerceIn(recAngleLowerBound, recAngleUpperBound)
					recAngleEdit.setText(currentValue.toString())
				}
				
				recAngleSlider.progress = currentValue - recAngleLowerBound
				return false
			}
		})
		
		recAngleSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				if (ignoreSliderListeners) return
				
				updateRecAngleEdit()
				recalculateMicDistance()
				recalculateAngularDistortion()
				recalculateReverbLimits()
				graphicsView.updateRecAngle((recAngleLowerBound + p1).toDouble())
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		
		micDistanceSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				if (ignoreSliderListeners) return
				
				updateMicDistanceLabel()
				recalculateMicAngle()
				recalculateAngularDistortion()
				recalculateReverbLimits()
				graphicsView.updateMicDistance(p1.toDouble() / 10.0)
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		
		micAngleSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				if (ignoreSliderListeners) return
				
				updateMicAngleLabel()
				recalculateMicDistance()
				recalculateAngularDistortion()
				recalculateReverbLimits()
				graphicsView.updateMicAngle(p1.toDouble() / 10.0)
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
		
		
		
		if (savedInstanceState != null) {
			useInches		= savedInstanceState.getBoolean("useInches")
			useHalfAngles	= savedInstanceState.getBoolean("useHalfAngles")
			useOmni			= savedInstanceState.getBoolean("useOmni")
			holdRecAngle	= savedInstanceState.getBoolean("holdRecAngle")
			
			unitsSwitch.isChecked			= useInches
			halfAnglesSwitch.isChecked		= useHalfAngles
			micTypeSwitch.isChecked			= useOmni
			holdRecAngleSwitch.isChecked	= holdRecAngle
			
			recAngleSlider.progress		= savedInstanceState.getInt("recAngle")
			micDistanceSlider.progress	= savedInstanceState.getInt("micDistance")
			micAngleSlider.progress		= savedInstanceState.getInt("micAngle")
		}
	}
	
	
	
	fun updateRecAngleEdit() {
		val recAngle = recAngleSlider.progress + recAngleLowerBound
		if (useHalfAngles) {
			recAngleEdit.setText((recAngle / 2).toString())
		} else {
			recAngleEdit.setText(recAngle.toString())
		}
	}
	
	fun updateMicDistanceLabel() {
		val micDistanceCm = micDistanceSlider.progress.toDouble() / 10.0
		if (useInches) {
			val micDistanceInches = micDistanceCm / 2.54
			micDistanceValueLabel.text = "%.2fin".format(micDistanceInches)
		} else {
			micDistanceValueLabel.text = "%.1fcm".format(micDistanceCm)
		}
	}
	
	fun updateMicAngleLabel() {
		val micAngle = micAngleSlider.progress.toDouble() / 10.0
		if (useHalfAngles) {
			micAngleValueLabel.text = "± %.0f°".format(micAngle / 2)
		} else {
			micAngleValueLabel.text = "%.0f°".format(micAngle)
		}
	}
	
	fun recalculateMicDistance() {
		val recAngle = recAngleEdit.text.toString().toDouble()
		val micAngle = micAngleSlider.progress.toDouble() / 10.0
		
		val micDistance = if (useOmni) {
			calculateOmniMicDistance(recAngle)
		} else {
			calculateCardioidMicDistance(recAngle, micAngle)
		}
		
		ignoreSliderListeners = true
		micDistanceSlider.progress = (micDistance * 10).roundToInt()
		ignoreSliderListeners = false
		updateMicDistanceLabel()
		graphicsView.updateMicDistance(micDistance)
	}
	
	fun recalculateMicAngle() {
		if (useOmni) return
		
		val recAngle = recAngleEdit.text.toString().toDouble()
		val micDistance = micDistanceSlider.progress.toDouble() / 10.0
		
		val micAngle = calculateCardioidMicAngle(recAngle, micDistance)
		
		ignoreSliderListeners = true
		micAngleSlider.progress = (micAngle * 10).roundToInt()
		ignoreSliderListeners = false
		updateMicAngleLabel()
		graphicsView.updateMicAngle(micAngle)
	}
	
	fun recalculateAngularDistortion() {
		val micDistance = micDistanceSlider.progress.toDouble() / 10.0
		val micAngle = micAngleSlider.progress.toDouble() / 10.0
		
		val angularDistortion = calculateAngularDistortion(micDistance, micAngle)
		
		angularDistValueLabel.text = "%.1f°".format(angularDistortion)
		angularDistIndicator.progress = (angularDistortion * 100).roundToInt()
		
		// Set progress color based on the value
		val progressRatio = angularDistIndicator.progress / angularDistIndicator.max.toFloat()
		val color = when {
			progressRatio < 0.5	-> Color.rgb(((-0.5f + progressRatio * 3f) * 255f).roundToInt().coerceAtLeast(0), 255, 0)
			else				-> Color.rgb(255, ((2.5f - progressRatio * 3f) * 255f).roundToInt().coerceAtLeast(0), 0)
		}
		angularDistIndicator.progressTintList = ColorStateList.valueOf(color)
	}
	
	fun recalculateReverbLimits() {
		val micDistance = micDistanceSlider.progress.toDouble() / 10.0
		val micAngle = micAngleSlider.progress.toDouble() / 10.0
		
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
	
	
	
	fun applyNearCoincidentPreset(micDistanceCm: Int, micAngle: Int) {
		if (useOmni) micTypeSwitch.performClick()
		
		val recAngle = calculateCardioidRecordingAngle(micDistanceCm.toDouble(), micAngle.toDouble())
		
		ignoreSliderListeners = true
		recAngleSlider.progress = recAngle.roundToInt() - recAngleLowerBound
		micDistanceSlider.progress = micDistanceCm * 10
		micAngleSlider.progress = micAngle * 10
		ignoreSliderListeners = false
		
		updateRecAngleEdit()
		updateMicDistanceLabel()
		updateMicAngleLabel()
		graphicsView.updateAll(recAngle, micDistanceCm.toDouble(), micAngle.toDouble())
	}
	
	
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean("useInches",		useInches)
		outState.putBoolean("useHalfAngles",	useHalfAngles)
		outState.putBoolean("useOmni",			useOmni)
		outState.putBoolean("holdRecAngle",		holdRecAngle)
		outState.putInt("recAngle",				recAngleSlider.progress)
		outState.putInt("micDistance",			micDistanceSlider.progress)
		outState.putInt("micAngle",				micAngleSlider.progress)
	}
	
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
}